#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
cevir-derle.py -- Türkçe örnekler İngilizceye çevrilince iKojo'da derleniyor mu.

NEDEN (#183, Aşama 0): iKojo'ya bir "Çevir" komutu geliyor; masaüstü Koco'nun
çevirmenini (bulent2k2/kojo: lite/i18n/tr/cevirmen.scala) kullanacak. Kaç
örneğin çevirisinin gerçekten derlendiğini SAYIYLA bilmek, ve bu sayının
sessizce düşmemesi gerekiyor. Bu araç o ölçümü tekrarlanabilir ve CI'da
koşan bir kapı yapıyor.

NE YAPIYOR: ornekler/*.kojo (iKojo) ve ornekler/masaustu/** (masaüstü Koco)
altındaki her Türkçe betik için:
  1. Türkçe ASLINI derler.
  2. Masaüstü çevirmeniyle (CevirmenMain --tr2en) İngilizceye çevirir ve
     ÇEVİRİYİ derler.
  3. İki durumu da yazar ve araclar/cevir-derle.tsv'deki beklenenle
     karşılaştırır.

İki sütunun nedeni: bir çeviri kalıyorsa suç çevirmenin mi, yoksa betik
Türkçesiyle de mi derlenmiyor (masaüstüne özgü bir özellik, iKojo'da eksik bir
ad)? Asıl sayı "Türkçesi geçiyor, çevirisi kalıyor" olanlar: ÇEVİRİ AÇIĞI.

NASIL DERLİYOR: sitedeki derleme sunucusu gibi, YAMALI (Türkçe anahtar
kelimeli) derleyiciyle -- iki dil için de aynı derleyici (koco-deploy/build.sh
sitede bunu kuruyor). iKojo kitaplığı önce stok derleyiciyle sbt'de derlenir
(kaynaklarında `yeni`, `üst` gibi Türkçe anahtar kelimeler AD olarak geçiyor,
yamalı derleyici onları derleyemez -- ölçüldü); betikler sonra derlenmiş
sınıflara karşı `scala.tools.nsc.Main -Ystop-after:refchecks` ile tür
denetiminden geçirilir. Her betik AYRI derlenir: tek derlemede bir betiğin
ayrıştırma hatası (ör. XML değişmezi) ötekilerin tür hatalarını gizliyordu;
bu aracın ilk sürümü o yüzden 16/16 dedi, doğrusu 12/16'ydı (ölçüldü).
refchecks'ten sonraki evreler (Scala.js dönüşümü, bağlama) koşmuyor: bu bir
tür denetimi kapısı, "sitede çalışır" kanıtı değil.

KAPI (CevirmenDerlemeTest.bilinenTrEn ile aynı ilke): beklenen "geçti" bir
betik kalırsa GERİLEME, beklenmeyen bir betik geçerse İLERLEME -- ikisi de
kırmızı. İlerleme de kırmızı, çünkü düzelen betik listeden çıkarılmazsa bir
sonraki gerileme görünmez olur. Yalnız durumlar karşılaştırılır; hata sayısı
ve ilk ileti TSV'de bilgi olarak durur (bir iletinin sözcüğü değişince kapı
kırılmasın). Bir ayrıntı: `kaldı` ile `ayrıştırma` etiketinin kendisi ilk
hata iletisinin bir düzenli ifadeyle (AYRISTIRMA) sınıflanmasından geliyor;
bir betiğin ilk hatası tür hatasından ayrıştırma hatasına dönerse durum
değişir ve kapı kırmızı olur. Sabit derleyiciyle bu belirlenimli.

Masaüstü sürümü SABİT: CI, çevirmeni araclar/kojo-cevirmen-surumu.txt'deki
commit'ten alır. Masaüstündeki ilgisiz bir değişiklik buradaki PR'ları
kırmızıya çevirmesin; sürüm bilinçli olarak yükseltilir, TSV onunla tazelenir.

  araclar/cevir-derle.py --kojo ../kojo              # karşılaştır
  araclar/cevir-derle.py --kojo ../kojo --guncelle   # TSV'yi yeniden yaz
"""

import argparse
import concurrent.futures
import os
import re
import shutil
import subprocess
import sys
import tempfile

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
ORNEKLER = os.path.join(KOK, 'ornekler')
TSV = os.path.join(BURASI, 'cevir-derle.tsv')
SURUM = os.path.join(BURASI, 'kojo-cevirmen-surumu.txt')

# ornekleri-dogrula.sh'daki sar() ile aynı içe aktarmalar (kojojs-editor'ün
# application.conf defaultSource'u). `fiddle.Fiddle.println` ve dışa aktarma
# ek açıklaması yalnız sitede var; burada Predef.println yetiyor.
ONSOZ = """package cevirdenetimi
object {ad} {{
    import kojo.{{SwedishTurtle, TurkishTurtle, Turtle, KojoWorldImpl, Vector2D, Picture}}
    import kojo.doodle.Color._
    import kojo.Speed._
    import kojo.RepeatCommands._
    import kojo.syntax.Builtins
    implicit val kojoWorld: kojo.KojoWorld = new KojoWorldImpl()
    val builtins = new Builtins()
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
"""
ONSOZ_SATIR = ONSOZ.count('\n')

# Ayrıştırıcının iletileri: yalnız etiket için ("ayrıştırma" / "kaldı").
AYRISTIRMA = re.compile(
    r"expected but|' expected|illegal start|unclosed|Missing closing brace|"
    r"not a legal formal parameter|identifier expected|expected start of definition|"
    r"illegal character|unbound placeholder|end of file|To compile XML syntax")

HATA = re.compile(r'^(.+\.scala):(\d+): error: (.*)$')

sb = lambda s: s.replace('\t', ' ').replace('\n', ' ').strip()


def betikler():
    """(anahtar, yol) çiftleri; anahtar ornekler/'e göreli ve kararlı."""
    sonuc = []
    for kok, _, dosyalar in os.walk(ORNEKLER):
        for d in dosyalar:
            if d.endswith('.kojo') or d.endswith('.kojo.installed'):
                yol = os.path.join(kok, d)
                sonuc.append((os.path.relpath(yol, ORNEKLER), yol))
    return sorted(sonuc)


def ortam():
    o = dict(os.environ)
    o['LANG'] = o['LC_ALL'] = 'C.UTF-8'  # Türkçe adlı .class dosyaları (bkz. sinamalar.yml)
    return o


# Takılan bir scalac/sbt işin 45 dakikalık sınırına kadar bekletmesin (#184 incelemesi).
# Betik başına derleme/çeviri normalde saniyeler (en uzun ~30 sn); sbt ile sınıf yolu
# ilk soğuk koşuda dakikalar sürebiliyor.
BETIK_ZAMAN_ASIMI = 300
SBT_ZAMAN_ASIMI = 1200


def kos(komut, dizin=None, zaman_asimi=BETIK_ZAMAN_ASIMI):
    """(çıkış kodu, çıktı); zaman aşımında (None, çıktının sonu)."""
    try:
        p = subprocess.run(komut, cwd=dizin, env=ortam(), stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True, encoding='utf-8',
                           timeout=zaman_asimi)
        return p.returncode, p.stdout
    except subprocess.TimeoutExpired as e:
        cikti = e.stdout.decode('utf-8', 'replace') if isinstance(e.stdout, bytes) else (e.stdout or '')
        return None, cikti


def sinif_yolu(dizin, launcher, kapsam):
    """sbt 'export <kapsam>/fullClasspath' -- gerekiyorsa önce derler."""
    kod, cikti = kos(['java', '-Xms512M', '-Xmx2g', '-Xss2M', '-Dfile.encoding=UTF-8',
                      '-Dsun.jnu.encoding=UTF-8', '-Dsbt.log.noformat=true', '-jar', launcher,
                      'export %s/fullClasspath' % kapsam], dizin, SBT_ZAMAN_ASIMI)
    if kod is None:
        sys.exit('HATA: %s sınıf yolu %d sn içinde alınamadı (sbt takıldı?):\n%s'
                 % (dizin, SBT_ZAMAN_ASIMI, cikti[-3000:]))
    # Aynı makinede iki koşu aynı sbt kilidini istiyor (ölçüldü: ikinci koşu düştü)
    if kod != 0 and ('sbt.boot.lock' in cikti or 'Address already in use' in cikti):
        sys.exit('HATA: %s için başka bir sbt koşuyor (sbt.boot.lock / Address already in use). '
                 'Aynı makinede iki cevir-derle.py ya da sbt eşzamanlı koşamaz; ötekinin bitmesini bekleyin.'
                 % dizin)
    satirlar = [s for s in cikti.splitlines() if s.strip() and not s.startswith('[')]
    if kod != 0 or not satirlar:
        sys.exit('HATA: %s sınıf yolu alınamadı (derleme hatası?):\n%s' % (dizin, cikti[-3000:]))
    # Göreli girdiler (kojo'da ./scala-tr/...) o dizine göre
    return os.pathsep.join(os.path.normpath(os.path.join(dizin, y))
                           for y in satirlar[-1].split(os.pathsep))


def cevir(kojo_yolu, girdi, cikti):
    kod, metin = kos(['java', '-Dfile.encoding=UTF-8', '-Dsun.jnu.encoding=UTF-8', '-cp', kojo_yolu,
                      'net.kogics.kojo.lite.i18n.tr.CevirmenMain', '--tr2en', girdi, '-o', cikti])
    # Çıkış 1 = "kaynak dilin anahtar sözcüğü kaldı" uyarısı; çıktı yine yazılmış olur
    if kod is None:
        return False, 'zaman aşımı (%d sn)' % BETIK_ZAMAN_ASIMI
    return os.path.exists(cikti), metin


def derle(derleyici, ikojo_yolu, govde, ad, dizin):
    """Tek betik; ('geçti'|'kaldı'|'ayrıştırma', hata sayısı, ilk hata)."""
    kaynak = os.path.join(dizin, ad + '.scala')
    with open(kaynak, 'w', encoding='utf-8') as f:
        # Son satır yorumsa `}` yoruma girmesin diye baştaki \\n (ornekleri-dogrula.sh ile aynı)
        f.write(ONSOZ.format(ad=ad) + govde + '\n}\n')
    cikis = os.path.join(dizin, ad + '.out')
    os.makedirs(cikis)
    kod, metin = kos(['java', '-Xss4m', '-Xmx1g', '-Dfile.encoding=UTF-8', '-cp', derleyici,
                      'scala.tools.nsc.Main', '-cp', ikojo_yolu, '-d', cikis,
                      '-Ystop-after:refchecks', '-Xmaxerrs', '100000', kaynak])
    if kod is None:
        return ('zaman aşımı', 0, 'derleme %d sn içinde bitmedi' % BETIK_ZAMAN_ASIMI)
    hatalar = [(int(m.group(2)), m.group(3)) for m in map(HATA.match, metin.splitlines()) if m]
    if kod == 0 and not hatalar:
        return ('geçti', 0, '')
    if not hatalar:
        return ('kaldı', 0, sb(metin)[-200:])
    satir, ileti = hatalar[0]
    d = 'ayrıştırma' if any(AYRISTIRMA.search(i) for _, i in hatalar) else 'kaldı'
    return (d, len(hatalar), '%d: %s' % (satir - ONSOZ_SATIR, sb(ileti)))


def olc(kojo):
    launcher = os.path.join(kojo, 'sbt-launch.1.5.5.jar')
    pack = os.path.join(kojo, 'scala-tr', 'build', 'pack', 'lib')
    if not os.path.exists(launcher) or not os.path.isdir(pack):
        sys.exit('HATA: --kojo bir bulent2k2/kojo klonu olmalı (%s, %s)' % (launcher, pack))
    derleyici = os.pathsep.join(os.path.join(pack, j) for j in
                                ('scala-compiler.jar', 'scala-reflect.jar', 'scala-library.jar'))
    print('kojo derleniyor (çevirmen)...', flush=True)
    kojo_yolu = sinif_yolu(kojo, launcher, 'Runtime')
    print('iKojo derleniyor...', flush=True)
    ikojo_yolu = sinif_yolu(KOK, launcher, 'Compile')

    gecici = tempfile.mkdtemp(prefix='cevir-derle-')
    try:
        liste = betikler()
        print('%d betik çevriliyor ve derleniyor (Türkçe + İngilizce)...' % len(liste), flush=True)

        def isle(i, anahtar, yol):
            d = os.path.join(gecici, '%03d' % i)
            os.makedirs(d)
            tr = derle(derleyici, ikojo_yolu, open(yol, encoding='utf-8').read(), 'T%03d' % i, d)
            en_dosya = os.path.join(d, 'en.kojo')
            tamam, metin = cevir(kojo_yolu, yol, en_dosya)
            if not tamam:
                return anahtar, tr, ('çevrilemedi', 0, sb(metin)[-200:])
            en = derle(derleyici, ikojo_yolu, open(en_dosya, encoding='utf-8').read(), 'E%03d' % i, d)
            return anahtar, tr, en

        durum = {}
        with concurrent.futures.ThreadPoolExecutor(max_workers=max(2, min(8, os.cpu_count() or 2))) as h:
            for anahtar, tr, en in h.map(lambda x: isle(*x), [(i, a, y) for i, (a, y) in enumerate(liste)]):
                durum[anahtar] = (tr, en)
        return durum
    finally:
        shutil.rmtree(gecici, ignore_errors=True)


def oku(tsv):
    beklenen = {}
    if os.path.exists(tsv):
        for s in open(tsv, encoding='utf-8'):
            if s.startswith('#') or s.startswith('betik\t') or not s.strip():
                continue
            p = s.rstrip('\n').split('\t')
            beklenen[p[0]] = (p[1], p[2])
    return beklenen


def sabit_surum():
    return [s.strip() for s in open(SURUM, encoding='utf-8') if s.strip() and not s.startswith('#')][0]


def tsv_surumu(tsv):
    for s in open(tsv, encoding='utf-8') if os.path.exists(tsv) else []:
        if s.startswith('# çevirmen: bulent2k2/kojo@'):
            return s.split('@', 1)[1].strip()
    return None


def ozet(durum):
    satirlar = []
    for baslik, suz in (('iKojo örnekleri', lambda a: '/' not in a),
                        ('masaüstü örnekleri', lambda a: a.startswith('masaustu/'))):
        ds = [v for a, v in durum.items() if suz(a)]
        tr = sum(1 for t, _ in ds if t[0] == 'geçti')
        en = sum(1 for _, e in ds if e[0] == 'geçti')
        acik = sum(1 for t, e in ds if t[0] == 'geçti' and e[0] != 'geçti')
        satirlar.append('%s (%d): Türkçesi derlenen %d, çevirisi derlenen %d, çeviri açığı %d'
                        % (baslik, len(ds), tr, en, acik))
    return satirlar


def yaz(tsv, durum, surum):
    with open(tsv, 'w', encoding='utf-8') as f:
        f.write('# araclar/cevir-derle.py sonucu -- ELLE DÜZENLEMEYİN, --guncelle ile yenileyin\n')
        f.write('# çevirmen: bulent2k2/kojo@%s\n' % surum)
        for s in ozet(durum):
            f.write('# %s\n' % s)
        f.write('# "çeviri açığı": Türkçesi derleniyor, İngilizce çevirisi derlenmiyor\n')
        f.write('betik\ttr\ten\ten_hata\ten_ilk_hata\ttr_ilk_hata\n')
        for a in sorted(durum):
            (td, _, ti), (ed, en, ei) = durum[a]
            f.write('%s\t%s\t%s\t%d\t%s\t%s\n' % (a, td, ed, en, ei, ti))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[1])
    ap.add_argument('--kojo', required=True, help='bulent2k2/kojo klonu')
    ap.add_argument('--guncelle', action='store_true', help='cevir-derle.tsv yeniden yazılsın')
    a = ap.parse_args()
    kojo = os.path.abspath(a.kojo)
    surum = kos(['git', 'rev-parse', 'HEAD'], kojo)[1].strip() or '?'
    # Üç sürüm aynı olmalı: klon, sabit dosya, TSV başlığı. Biri kayarsa ölçüm
    # başka bir çevirmene karşı yapılmış olur ve fark "gerileme" diye okunur.
    sabit = sabit_surum()
    if surum != sabit:
        sys.exit('HATA: --kojo klonu %s, ama sabit sürüm %s (%s).\n'
                 '  git -C %s checkout %s' % (surum[:12], sabit[:12], os.path.relpath(SURUM, KOK), a.kojo, sabit))
    if not a.guncelle and tsv_surumu(TSV) != sabit:
        sys.exit('HATA: cevir-derle.tsv %s ile üretilmiş, sabit sürüm %s. Sürüm yükseltildiyse TSV\'yi\n'
                 '  araclar/cevir-derle.py --kojo <kojo> --guncelle ile tazeleyip aynı commit\'te gönderin.'
                 % ((tsv_surumu(TSV) or '?')[:12], sabit[:12]))

    durum = olc(kojo)
    for s in ozet(durum):
        print(s)
    if a.guncelle:
        yaz(TSV, durum, surum)
        print('yazıldı: %s' % os.path.relpath(TSV, KOK))
        return

    beklenen = oku(TSV)
    gerileme, ilerleme, fark = [], [], []
    for b in sorted(set(beklenen) | set(durum)):
        if b not in durum or b not in beklenen:
            fark.append('%s: %s' % (b, 'yeni betik' if b not in beklenen else 'betik kaldırılmış'))
            continue
        for dil, e, (s, _, ileti) in (('tr', beklenen[b][0], durum[b][0]), ('en', beklenen[b][1], durum[b][1])):
            if e == s:
                continue
            if e == 'geçti':
                gerileme.append('%s [%s]: geçti -> %s (%s)' % (b, dil, s, ileti))
            elif s == 'geçti':
                ilerleme.append('%s [%s]: %s -> geçti' % (b, dil, e))
            else:
                fark.append('%s [%s]: %s -> %s (%s)' % (b, dil, e, s, ileti))
    for baslik, liste in (('GERİLEME', gerileme), ('İLERLEME (listeyi tazeleyin)', ilerleme),
                          ('FARK', fark)):
        if liste:
            print('\n%s:' % baslik)
            for s in liste:
                print('  ' + s)
    if gerileme or ilerleme or fark:
        print('\ncevir-derle.tsv beklenenle uyuşmuyor. Değişiklik bilinçliyse:\n'
              '  araclar/cevir-derle.py --kojo <kojo> --guncelle')
        sys.exit(1)
    print('beklenenle aynı.')


if __name__ == '__main__':
    main()
