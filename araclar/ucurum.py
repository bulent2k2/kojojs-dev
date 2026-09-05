#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ucurum.py -- masaüstü Koco betikleri ile ikojo'nun Türkçe API yüzeyi arasındaki
uçurumu ölçer.

Her .kojo betiğini yorum ve dizgelerden arındırıp tanımlayıcılarına ayırır, her
adı üç kümeyle karşılaştırır:
  * masaüstü TR API'si  (kojo: lite/i18n/trInit.scala + lite/i18n/tr/*.scala)
  * ikojo TR API'si     (kojojs-dev: kojo/TurkishTurtle.scala + kojo/tr/*.scala)
  * ikojo İngilizce yüzeyi (kojojs-dev: kojo/*.scala, kojo/syntax, kojo/doodle)
ve platform engellerini (Swing arayüzü, #yükle, ses, öykü, dosya...) düzenli
ifadelerle işaretler. Sonuç: betik başına durum + eksik adların sıklık listesi.

Bu bir TANIMLAYICI TARAMASIDIR, derleme değil: imza farklarını (parametre
türü/sayısı) göremez. Gerçek derleme denetimi için ornekler/ornekleri-dogrula.sh.

Kullanım:
  araclar/ucurum.py                       # ../kojo klonu, ornekler/masaustu
  araclar/ucurum.py --kojo ~/src/kojo --tsv ornekler/masaustu/tarama.tsv
  araclar/ucurum.py --json rapor.json --en-sik 40
"""
import argparse
import collections
import glob
import json
import os
import re
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
IKOJO = os.path.dirname(BURASI)

# def/val/var/object/class/trait/type tanımlarından ad çıkarır (geri tırnaklılar dahil)
TANIM = re.compile(r'\b(?:def|val|var|object|class|trait|type|lazy\s+val)\s+(`[^`]+`|[^\s(\[:=,)]+)')
TANIMLAYICI = re.compile(r'[^\W\d]\w*', re.U)

# Yamalı scala-tr derleyicisinin tanıdığı Türkçe anahtar kelimeler
ANAHTAR = {
    'dez', 'den', 'tanım', 'eğer', 'yoksa', 'yineleDoğruKaldıkça', 'için', 'ver', 'eşle', 'durum',
    'sınıf', 'nesne', 'özellik', 'yayar', 'birlikte', 'yeni', 'bu', 'üst', 'baskın', 'soyut', 'son',
    'damgalı', 'örtük', 'miskin', 'gizli', 'koru', 'deste', 'getir', 'geriDön', 'dene', 'yakala',
    'sonunda', 'bildir', 'yap', 'tür', 'doğru', 'yanlış', 'yok',
}

# Tarayıcıda karşılığı olmayan / henüz taşınmamış özellik aileleri.
# Ad -> (düzenli ifade, açıklama)
PLATFORM = collections.OrderedDict([
    ('yükle',        (re.compile(r'#\s*(?:yükle|include)\b'),
                      'başka dosya içe alır; ikojo tek dosya derler')),
    ('arayüz',       (re.compile(r'\bay\.'),
                      'Swing arayüz nesneleri (ay.*)')),
    # Mp3 çalma (howler) ve notaÇal (Web Audio) ikojo'da var; engel yalnız MIDI
    # partisyon çalma (MusicScore/playMusic, jfugue)
    ('ses',          (re.compile(r'\b(?:MusicScore|playMusic|müzikÇal|Müzik\b|Nota\b|Ritim|Enstrüman)'),
                      'MIDI partisyon (MusicScore); tarayıcıda karşılığı yok')),
    ('öykü',         (re.compile(r'\b(?:hikaye|Story|öykü\w*|Öykü\w*|stPlayStory|Sayfa|Page)\b'),
                      'öykü anlatıcı (Story/Page)')),
    ('dosya',        (re.compile(r'\b(?:Dosya|dosya\w*|satıroku|sayıOku|kesirOku|readln|readInt|scala\.io|java\.io|Source\.from)\b'),
                      'dosya okuma / klavye girdisi')),
    ('imge',         (re.compile(r'\b(?:Staging|İmgeİşlemi|BufferedImage|Bellekteİmge|ImageOp)\b'),
                      'Java2D imge işleme')),
    ('canlandırma',  (re.compile(r'\b(?:Canlandırma|Geçiş|canlandırmaDizisi|Transition|animSeq)\b'),
                      'Transition/Canlandırma API')),
    ('tuvalçizim',   (re.compile(r'\b(?:Grafik2B|canlandırTuvalÇizimle|tuvalÇizim\w*)\b'),
                      'Graphics2D tuval çizimi')),
    ('çini',         (re.compile(r'\b(?:ÇiniDünyası|TileWorld|BirSayfaKostüm|SpriteSheet)\b'),
                      'Çini dünyası (TileWorld)')),
    ('geo',          (re.compile(r'\b(?:GeoYol|GeoNokta|geo\.)\b'),
                      'JTS geometri')),
    ('url',          (re.compile(r'\b(?:BKK|URL)\b'),
                      'ağ erişimi')),
])

# Çok genel Scala/Java adları -- eksik diye sayılmaz
GENEL = {'apply', 'toString', 'length', 'size', 'map', 'foreach', 'filter', 'to', 'until', 'by', 'scala', 'java',
         'x', 'y', 'z', 'a', 'b', 'n', 'i', 'j', 'k'}


def tanimlar(yollar):
    adlar = set()
    for y in yollar:
        try:
            with open(y, encoding='utf-8') as f:
                s = f.read()
        except OSError:
            continue
        for m in TANIM.finditer(s):
            ad = m.group(1).strip('`')
            if ad and ad != '_' and not ad.startswith('$'):
                adlar.add(ad)
    return adlar


def soy(s):
    """Yorumları ve dizgeleri at; geriye yalnız kod kalsın."""
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s = re.sub(r'//[^\n]*', '', s)
    s = re.sub(r'"""(?:.|\n)*?"""', '""', s)
    s = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\\n])'", "''", s)
    return s


def betikler(kok):
    out = []
    for d, _, dosyalar in os.walk(kok):
        for f in sorted(dosyalar):
            if f.endswith('.kojo') or f.endswith('.kojo.installed'):
                out.append(os.path.join(d, f))
    return sorted(out)


def olc(kojo, ikojo, kok):
    if not os.path.isdir(os.path.join(kojo, 'src/main/scala/net/kogics/kojo/lite/i18n')):
        sys.exit(f'kojo klonu bulunamadı: {kojo}  (--kojo ile göster)')
    i18n = os.path.join(kojo, 'src/main/scala/net/kogics/kojo/lite/i18n')
    # dict/help/templates/translate/data* API değil (sözlük, yardım metni, şablon,
    # çıktı çevirisi); onlardaki adlar tarama gürültüsü yapar
    api_degil = re.compile(r'/(?:dict|help|templates|translate|data\w*)\.scala$')
    tr_dosyalar = [os.path.join(i18n, 'trInit.scala')] + [
        f for f in glob.glob(os.path.join(i18n, 'tr/*.scala')) if not api_degil.search(f)]
    masa_tr = tanimlar(tr_dosyalar)
    src = os.path.join(ikojo, 'src/main/scala/kojo')
    ikojo_tr = tanimlar([os.path.join(src, 'TurkishTurtle.scala')] + glob.glob(os.path.join(src, 'tr/*.scala')))
    ikojo_en = tanimlar(glob.glob(os.path.join(src, '*.scala'))
                        + glob.glob(os.path.join(src, 'syntax/*.scala'))
                        + glob.glob(os.path.join(src, 'doodle/*.scala')))

    dosyalar = collections.OrderedDict()
    eksik_dosya = collections.Counter()
    eksik_kez = collections.Counter()
    engel_dosya = collections.Counter()
    for y in betikler(kok):
        ham = open(y, encoding='utf-8', errors='replace').read()
        kod = soy(ham)
        tokenler = set(TANIMLAYICI.findall(kod))
        kullanilan = tokenler & (masa_tr | ANAHTAR)
        eksik = sorted(t for t in kullanilan
                       if t not in ikojo_tr and t not in ikojo_en and t not in ANAHTAR and t not in GENEL)
        # #yükle/#include satırları yorum içindedir; ham metinde ara
        engeller = [ad for ad, (rx, _) in PLATFORM.items() if rx.search(ham if ad == 'yükle' else kod)]
        if engeller:
            durum = 'platform'
        elif eksik:
            durum = 'eksik-ad'
        else:
            durum = 'çalışır'
        gorelli = os.path.relpath(y, kok)
        dosyalar[gorelli] = {
            'satır': ham.count('\n') + 1,
            'kullanılan': len(kullanilan),
            'eksik': eksik,
            'engel': engeller,
            'durum': durum,
        }
        for t in eksik:
            eksik_dosya[t] += 1
            eksik_kez[t] += len(re.findall(r'\b' + re.escape(t) + r'\b', kod))
        for e in engeller:
            engel_dosya[e] += 1

    ozet = collections.Counter(v['durum'] for v in dosyalar.values())
    return {
        'kök': os.path.relpath(kok, ikojo),
        'masaüstü_tr_ad': len(masa_tr),
        'ikojo_tr_ad': len(ikojo_tr),
        'betik': len(dosyalar),
        'satır': sum(v['satır'] for v in dosyalar.values()),
        'özet': dict(ozet),
        'engeller': [(e, n, PLATFORM[e][1]) for e, n in engel_dosya.most_common()],
        'eksik': [(t, n, eksik_kez[t]) for t, n in eksik_dosya.most_common()],
        'dosyalar': dosyalar,
    }


def yazdir(r, en_sik):
    print(f"betik: {r['betik']}   satır: {r['satır']}   "
          f"masaüstü TR ad: {r['masaüstü_tr_ad']}   ikojo TR ad: {r['ikojo_tr_ad']}")
    o = r['özet']
    print(f"durum: çalışır {o.get('çalışır', 0)}   eksik-ad {o.get('eksik-ad', 0)}   platform {o.get('platform', 0)}")
    print("\n== dizin bazında (betik / çalışır / eksik-ad / platform)")
    dizin = collections.defaultdict(collections.Counter)
    for yol, v in r['dosyalar'].items():
        dizin[os.path.dirname(yol)][v['durum']] += 1
    for d in sorted(dizin):
        c = dizin[d]
        print(f"  {d:44s} {sum(c.values()):3d}  {c['çalışır']:3d}  {c['eksik-ad']:3d}  {c['platform']:3d}")
    print("\n== platform engelleri (betik sayısı)")
    for e, n, aciklama in r['engeller']:
        print(f"  {e:14s} {n:3d}   {aciklama}")
    print(f"\n== en sık eksik adlar (ilk {en_sik}; betik sayısı / toplam kullanım)")
    for t, n, kez in r['eksik'][:en_sik]:
        print(f"  {t:40s} {n:3d}  {kez:4d}")


def tsv_yaz(r, yol):
    with open(yol, 'w', encoding='utf-8') as f:
        f.write("# ucurum.py tarama sonucu (tanımlayıcı taraması, derleme değil)\n")
        f.write("betik\tdurum\tsatır\tengel\teksik\n")
        for b, v in r['dosyalar'].items():
            f.write(f"{b}\t{v['durum']}\t{v['satır']}\t{','.join(v['engel'])}\t{' '.join(v['eksik'])}\n")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--kojo', default=os.path.join(IKOJO, '..', 'kojo'), help='masaüstü kojo klonu (varsayılan: ../kojo)')
    p.add_argument('--ikojo', default=IKOJO, help='kojojs-dev kökü (varsayılan: bu repo)')
    p.add_argument('--betikler', default=os.path.join(IKOJO, 'ornekler', 'masaustu'), help='taranacak betik kökü')
    p.add_argument('--tsv', help='betik başına durumu bu TSV dosyasına yaz')
    p.add_argument('--json', help='tam raporu bu JSON dosyasına yaz')
    p.add_argument('--en-sik', type=int, default=30, help='listelenecek eksik ad sayısı')
    a = p.parse_args()
    r = olc(os.path.abspath(a.kojo), os.path.abspath(a.ikojo), os.path.abspath(a.betikler))
    yazdir(r, a.en_sik)
    if a.tsv:
        tsv_yaz(r, a.tsv)
        print(f"\nTSV yazıldı: {a.tsv}")
    if a.json:
        with open(a.json, 'w', encoding='utf-8') as f:
            json.dump(r, f, ensure_ascii=False, indent=1)
        print(f"JSON yazıldı: {a.json}")


if __name__ == '__main__':
    main()
