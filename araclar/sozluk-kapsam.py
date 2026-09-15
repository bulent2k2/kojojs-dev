#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sozluk-kapsam.py -- koco-sozlugu.html ile masaüstü Koco'nun ÜRETİLMİŞ çeviri
sözlüğünü (bulent2k2/kojo: src/main/resources/i18n/tr/ceviri-sozlugu.tsv)
karşılaştırır.

NEDEN: sözlük sayfasındaki 1022 satır bir kez üretilip SONRA ELLE düzeltildi
(bkz. sozluk/README.md). Yani bugün onu kaynağa bağlayan bir şey yok: Türkçe
katmana yeni bir ad girdiğinde sayfa sessizce eskiyor. Eylül 2026'daki tur
201 adı elle işledi (754 -> 980); o turu kimse tetiklemedi, göz kararı
başladı. Bu araç aradaki farkı SAYIYLA söylüyor.

NE DEĞİL: bir kapı değil, bir rapor. Çıkış kodu her zaman 0. Eksik adların
çoğu sayfaya girmemeli (Görünüş'ün imge yolları, iç tür takma adları); karar
küratörün, aracın işi listeyi ayıklanabilir biçimde önüne koymak. Sayfayı da
DEĞİŞTİRMİYOR: koco-sozlugu.html'e dokunmak artifact'i ve ikojo'daki kopyayı
yeniden yayımlamayı gerektiriyor (README'deki beş adım), o ayrı bir tur.

NEDEN KOPYA YOK: TSV bu depoda DURMUYOR, kojo klonundan okunuyor. Kopyalasak
"üretilmiş dosya kaynağından ayrıldı" sınıfını yeniden açardık -- sozluk-denetle.py
tam onu kapatmak için var. ornek-dizini.py'nin kojojs-editor dizinini argüman
alması da aynı gerekçe.

SÖZLÜĞÜN İKİ YARISI: üretilmiş TSV tek başına masaüstü sözlüğü DEĞİL. Yanında
`ceviri-kurallar.tsv` duruyor: üretecin yanlış seçimlerini geçersiz kılmak için
tam bu amaçla yazılmış elle kurallar. Yalnız üretilmişi okumak sahte çelişki
üretiyor -- ölçüldü, `zıpla` sayfada `hop`, üretilmiş TSV'de `saveStyle`, ve
kural dosyası zaten `tr>en zıpla * hop` diyor (kendi notu: "gövdeli tanım;
ilk-zincir sezgisi saveStyle'ı görüyordu"). Çevirmen `kaplumbağa.zıpla(30)` için
`turtle0.hop(30)` üretiyor, yani sayfa haklı.

SAYFANIN ÜÇ YAZIM BİÇİMİ: bir adı sayfada aramak salt hücre karşılaştırması
değil. Sayfa (a) üye adlarını NİTELEYEREK yazıyor -- üretilmiş TSV `dikdörtgen`
diyor, sayfa `Resim.dikdörtgen`; öğrenci de nitelenmişini yazıyor, yani sayfa
haklı. (b) eşanlamlıları ayrı satıra değil NOTA koyuyor -- `["react","tepkiVer",
"alt: canlan"]`, yani `canlan` sayfada var. Yalnız hücreye bakan bir sürüm bu
46 adı eksik sayıyordu (453 -> 407); ölçüldü, küratör turunun altıda biri sahte
işmiş. İkisi de İNGİLİZCE TARAF DA TUTUYORSA örtük sayılıyor: `Görünüş.daire`
(bir imge yolu) sayfadaki `daire`=`circle` ile kapanmıyor, `Resim.sil`
(`erasePictures`) sayfadaki `sil`=`clear` ile kapanmıyor, `arayüz`
(`Picture.widget`) `interface`/`arabirim` satırının "alt: arayüz" notuyla
kapanmıyor -- üçü de kuyrukta kalıyor.

Örtük sınıfları:
  niteleme   -- sayfa aynı adı niteleyerek yazmış (Resim.dikdörtgen)
  takma      -- sayfa adı bir satırın "alt:" notunda anmış

Çelişki sınıfları (aynı Türkçe ad, örtüşmeyen İngilizce karşılık):
  kural      -- elle kural sayfayı DOĞRULUYOR; üretilmiş satır ölü veri, bakılacak
                bir şey yok
  ayrı       -- iki taraf da yalın ad, yine de tutmuyor; gerçek aday
  niteleme   -- yalnız niteleyici farkı (sayfa collection.Seq, TSV Seq); kozmetik.
                SINIR: bileşke de buraya düşebiliyor (`tersİşle` sayfa `reverse.map`,
                TSV `map`) -- son parça karşılaştırması niteleyici ile zinciri
                ayırt etmiyor. Etkisi düşük: bu kova zaten incelenmiyor.
  imza       -- sayfanın hücresi yalın ad DEĞİL (`scale(x, y)`, `round(n, digits)`,
                `log base t`): sayfa yer yer imza ya da düzyazı yazıyor,
                tanımlayıcı gibi karşılaştırmak sahte çelişki üretiyor -- ölçüldü,
                47 "ayrı"nın 20'si buymuş.

Kullanım:
  araclar/sozluk-kapsam.py                  # ../kojo klonunu bekler
  araclar/sozluk-kapsam.py --kojo ~/src/kojo
  araclar/sozluk-kapsam.py --eksik          # eksik adların tam listesi
  araclar/sozluk-kapsam.py --celisen        # çelişen çiftlerin tam listesi
  araclar/sozluk-kapsam.py --ortuk          # sayfanın başka biçimde kapsadıkları
  araclar/sozluk-kapsam.py --json /tmp/kapsam.json
"""
import argparse
import collections
import io
import json
import os
import re
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
IKOJO = os.path.dirname(BURASI)
SAYFA = os.path.join(IKOJO, 'sozluk', 'koco-sozlugu.html')
TSV_YOLU = os.path.join('src', 'main', 'resources', 'i18n', 'tr', 'ceviri-sozlugu.tsv')
KURAL_YOLU = os.path.join('src', 'main', 'resources', 'i18n', 'tr', 'ceviri-kurallar.tsv')

# Sayfadaki veri satırı: ["ingilizce","türkçe","açıklama"]  (tek satır; baştaki
# boşluk yutuluyor -- sayfa yeniden biçimlenip girintilenirse satır SESSİZCE
# düşmesin, sessiz kaymayı ölçen bir araçta bu bedava dayanıklılık).
SAYFA_SATIRI = re.compile(r'^[ \t]*\["((?:[^"\\]|\\.)*)","((?:[^"\\]|\\.)*)"', re.M)
# Üçüncü hücre (açıklama) ayrıca okunuyor: "alt: canlan" notu sayfanın eşanlamlı
# yazma biçimi, satır açmıyor.
SAYFA_SATIRI3 = re.compile(
    r'^[ \t]*\["((?:[^"\\]|\\.)*)","((?:[^"\\]|\\.)*)","((?:[^"\\]|\\.)*)"\]', re.M)
# Sayfa eşanlamlıyı üç sözle yazıyor: "alt:", "eski adı:", "takma ad:" -- ölçüldü,
# sırasıyla 30/7/2 not. Üçü de aynı şey: satır o adı da kapsıyor.
TAKMA_AD = re.compile(r'(?:alt|eski adı|takma ad)\s*:\s*([^,;()]+)')


def oku(yol):
    with io.open(yol, encoding='utf-8') as d:
        return d.read()


def sayfaÇiftleri(yol):
    """Sayfadaki (türkçe, ingilizce) çiftleri."""
    çiftler = [(m.group(2), m.group(1)) for m in SAYFA_SATIRI.finditer(oku(yol))]
    if not çiftler:
        sys.exit('%s içinde ["en","tr",...] biçiminde satır bulunamadı' % yol)
    return çiftler


def sayfaTakmaAdları(yol):
    """Sayfanın "alt: X" notlarından takma ad -> o satırın (İngilizce, Türkçe) adı.

    Türkçesi de tutuluyor: notun İngilizcesi satırın okunur hâli olabiliyor
    (`ColorHSB(h, s, b)`) ve üretilmiş sözlüğün karşılığıyla (`hsla`) tutmuyor.
    O zaman ikinci kanıt satırın KENDİ Türkçe adı: sözlük hem `RenkADA`ya hem
    `RenkArıRenkDoygunlukAydınlık`a aynı İngilizceyi veriyorsa, sayfa
    "biri ötekinin takma adı" derken haklı."""
    takma = collections.defaultdict(set)
    for m in SAYFA_SATIRI3.finditer(oku(yol)):
        en, tr, açıklama = m.group(1), m.group(2), m.group(3)
        for t in TAKMA_AD.finditer(açıklama):
            for ad in t.group(1).split('/'):
                ad = ad.strip()
                if ad:
                    takma[ad].add((en, tr))
                    # Not da niteleyerek yazılabiliyor ("alt: Resim.köşegen"),
                    # üretilmiş sözlükteki karşılığı ise yalın (`köşegen`).
                    takma[sonParça(ad)].add((en, tr))
    return takma


def tsvSatırları(yol):
    """Üretilmiş sözlüğün (cins, tr, en, kaynak) satırları.

    Yalnız ilk dört sütun okunuyor: beşinci sütun (sayı) ve altıncı (not) bu
    aracın kararlarına girmiyor, böylece TSV'ye sütun eklenince araç kırılmıyor.
    """
    satırlar = []
    for l in oku(yol).split('\n'):
        if not l.strip() or l.startswith('#'):
            continue
        a = l.split('\t')
        if len(a) < 4:
            sys.exit('%s: bozuk satır: %s' % (yol, l))
        satırlar.append(tuple(a[:4]))
    return satırlar


def kuralHedefleri(yol):
    """tr>en kurallarının Türkçe ad -> hedef kümesi. `-` (çevirme) atlanır,
    `^` (alıcıyı yut) ve `(2,1)` (argüman sırası) imleri soyulur."""
    hedefler = collections.defaultdict(set)
    if not os.path.exists(yol):
        return hedefler
    for l in oku(yol).split('\n'):
        if not l.strip() or l.startswith('#'):
            continue
        a = l.split('\t')
        if len(a) < 4 or a[0] != 'tr>en':
            continue
        hedefler[a[1]].add(re.sub(r'\(\d+(,\d+)*\)$', '', a[3].lstrip('^')))
    return hedefler


Çevirme = '-'


İMZA_KUYRUĞU = re.compile(r'\([^()]*\)$')


def çıplak(ad):
    """Hücrenin imza kuyruğunu atar: `RenkADA(arıRenk, doygunluk, aydınlık)` -> `RenkADA`.

    Sayfa bir adı yer yer imzasıyla yazıyor (okunurluk için, bkz. `imza` sınıfı).
    Üretilmiş sözlükte karşılığı hep yalın ad, yani imzalı hücre karşılaştırmaya
    hiç girmiyordu: `RenkADA` sayfada dururken EKSİK sayılıyordu."""
    return İMZA_KUYRUĞU.sub('', ad).strip()


def sonParça(ad):
    return çıplak(ad).rsplit('.', 1)[-1]


YALIN_AD = re.compile(r'^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$')


def yalınMı(ad):
    """Hücre bir tanımlayıcı mı, yoksa imza/düzyazı mı (`scale(x, y)`, `minute(s)`)."""
    return bool(YALIN_AD.match(ad))


def karşılaştır(sayfa, tsv, kurallar, takmaAdlar=None):
    takmaAdlar = takmaAdlar or {}
    sayfaHedefleri = collections.defaultdict(set)
    for tr, en in sayfa:
        sayfaHedefleri[tr].add(en)
        sayfaHedefleri[çıplak(tr)].add(en)
    # Sayfa üye adlarını niteleyerek yazıyor: son parçadan tam satıra geri dönüş.
    sayfaSonParça = collections.defaultdict(set)
    for tr, en in sayfa:
        if '.' in çıplak(tr):
            sayfaSonParça[sonParça(tr)].add((tr, en))
    tsvHedefleri = collections.defaultdict(set)
    tsvKaynağı = {}
    for cins, tr, en, kaynak in tsv:
        tsvHedefleri[tr].add(en)
        tsvKaynağı.setdefault(tr, (cins, kaynak))

    def örtükMü(tr, enler):
        """Sayfa bu adı başka bir yazım biçimiyle kapsıyor mu? İngilizce taraf da
        tutmalı: yoksa `Resim.sil`(erasePictures) sayfadaki `sil`(clear) ile
        kapanmış görünürdü."""
        parçalar = {sonParça(e) for e in enler}
        for sayfaTr, sayfaEn in sorted(sayfaSonParça.get(sonParça(tr), ())):
            if sonParça(sayfaEn) in parçalar:
                return {'sınıf': 'niteleme', 'sayfa': ['%s = %s' % (sayfaTr, sayfaEn)]}
        for ad in (tr, sonParça(tr)):
            for sayfaEn, sayfaTr in sorted(takmaAdlar.get(ad, ())):
                if (sonParça(sayfaEn) in parçalar
                        or tsvHedefleri.get(çıplak(sayfaTr), set()) & enler):
                    return {'sınıf': 'takma', 'sayfa': ['alt: %s (%s)' % (ad, sayfaEn)]}
        return None

    eksik = []
    örtük = []
    for tr in sorted(tsvHedefleri):
        if tr in sayfaHedefleri:
            continue
        cins, kaynak = tsvKaynağı[tr]
        satır = {'tr': tr, 'en': sorted(tsvHedefleri[tr]), 'cins': cins, 'kaynak': kaynak}
        ö = örtükMü(tr, tsvHedefleri[tr])
        if ö:
            satır.update(ö)
            örtük.append(satır)
        else:
            eksik.append(satır)

    çelişen = []
    ortak = 0
    for tr in sorted(tsvHedefleri):
        if tr not in sayfaHedefleri:
            continue
        ortak += 1
        s, t = sayfaHedefleri[tr], tsvHedefleri[tr]
        if s & t:
            continue
        sYalın = {x for x in s if yalınMı(x)}
        k = kurallar.get(tr, set())
        kYalın = {x for x in k if x != Çevirme}
        # Elle kural sayfayı doğruluyorsa üretilmiş satır ölü veri: kuyruktan düşer.
        # Doğrulamayan kural (kalemBoyu: kural penThickness, sayfa penWidth) kuyrukta
        # KALIR ama listede görünür -- küratör hikâyeyi bir bakışta görsün.
        if kYalın and (kYalın & s or {sonParça(x) for x in kYalın} & {sonParça(x) for x in sYalın}):
            sınıf = 'kural'
        elif not sYalın:
            sınıf = 'imza'
        elif {sonParça(x) for x in sYalın} & {sonParça(x) for x in t}:
            sınıf = 'niteleme'
        else:
            sınıf = 'ayrı'
        çelişen.append({'tr': tr, 'sayfa': sorted(s), 'tsv': sorted(t), 'sınıf': sınıf,
                        'kural': sorted(k), 'kaynak': tsvKaynağı[tr][1]})

    # Ters yön: sayfada olup üretilmiş sözlükte hiç geçmeyen Türkçe ad. Çoğu
    # beklenen (arayüz sözcükleri, kavram çevirileri, ikojo'ya özgü adlar);
    # yine de sayısı kaymanın ikinci ölçüsü.
    # Yalnız sayfanın YAZDIĞI hâl sayılıyor: çıplak(tr) ile eklenen ikizler
    # sayımı şişirirdi.
    sayfadaFazla = sorted({tr for tr, _ in sayfa} - set(tsvHedefleri))
    return {'eksik': eksik, 'örtük': örtük, 'çelişen': çelişen, 'ortak': ortak,
            'sayfadaFazla': sayfadaFazla}


def kuralNotu(ç):
    """Elle kural varsa listede göster: `-` "bilerek çevrilmiyor" demek."""
    if not ç['kural']:
        return ''
    return '[kural: %s]' % ', '.join('çevirme' if x == Çevirme else x for x in ç['kural'])


def yaz(r, sayfaSayısı, tsvSayısı, tümEksik, tümÇelişen, tümÖrtük=False):
    print('sayfa: %d satır, %d ayrı Türkçe ad     üretilmiş sözlük: %d satır, %d ayrı Türkçe ad'
          % (sayfaSayısı[0], sayfaSayısı[1], tsvSayısı[0], tsvSayısı[1]))
    print('ortak ad: %d    sözlükte olup sayfada olmayan: %d    sayfada olup sözlükte olmayan: %d'
          % (r['ortak'], len(r['eksik']), len(r['sayfadaFazla'])))
    sayımÖ = collections.Counter(ö['sınıf'] for ö in r['örtük'])
    print('örtük kapsanan: %d (sayfa niteleyerek yazmış: %d, "alt:" notunda: %d) '
          '-- kuyruğa girmiyor, --ortuk ile listelenir'
          % (len(r['örtük']), sayımÖ['niteleme'], sayımÖ['takma']))
    if tümÖrtük:
        print('\n== örtük kapsanan adlar')
        for ö in r['örtük']:
            print('  %-9s %-28s %-28s %s' % (ö['sınıf'], ö['tr'], ', '.join(ö['en'][:2]),
                                             ', '.join(ö['sayfa'])))

    print('\n== sayfada olmayan adlar, kaynak dosyaya göre')
    dosyalar = collections.Counter(e['kaynak'] for e in r['eksik'])
    for dosya, n in dosyalar.most_common():
        örnek = [e['tr'] for e in r['eksik'] if e['kaynak'] == dosya]
        print('  %-22s %4d   %s%s' % (dosya, n, ', '.join(örnek[:5]), ' …' if n > 5 else ''))
    if tümEksik:
        print('\n-- tam liste')
        for e in r['eksik']:
            print('  %-28s %-30s %-6s %s' % (e['tr'], ', '.join(e['en'][:2]), e['cins'], e['kaynak']))

    sayım = collections.Counter(ç['sınıf'] for ç in r['çelişen'])
    ayrı = [ç for ç in r['çelişen'] if ç['sınıf'] == 'ayrı']
    print('\n== çelişen çiftler: %d (ayrı: %d, kural sayfayı doğruluyor: %d, '
          'yalnız niteleme farkı: %d, sayfa imza yazmış: %d)'
          % (len(r['çelişen']), sayım['ayrı'], sayım['kural'], sayım['niteleme'], sayım['imza']))
    if tümÇelişen:
        print('   tamamı, sınıfıyla:')
        for ç in r['çelişen']:
            print('  %-9s %-24s sayfa: %-28s sözlük: %-34s %s' % (ç['sınıf'], ç['tr'],
                  ', '.join(ç['sayfa'][:2]), ', '.join(ç['tsv'][:3]), kuralNotu(ç)))
    else:
        print('   incelenmesi gereken yalnız "ayrı" olanlar:')
        for ç in ayrı[:20]:
            print('  %-24s sayfa: %-28s sözlük: %-34s %s' % (ç['tr'], ', '.join(ç['sayfa'][:2]),
                  ', '.join(ç['tsv'][:3]), kuralNotu(ç)))
        if len(ayrı) > 20:
            print('  … (%d tane daha; --celisen ile tamamı, sınıf sütunuyla)' % (len(ayrı) - 20))
    print('\nBu bir rapor, kapı değil: hangi adın sayfaya gireceğine küratör karar verir.')


def main():
    p = argparse.ArgumentParser(description=__doc__.split('\n')[1])
    p.add_argument('--kojo', default=os.path.join(IKOJO, '..', 'kojo'),
                   help='masaüstü Koco klonu (varsayılan: ../kojo)')
    p.add_argument('--sayfa', default=SAYFA, help='koco-sozlugu.html yolu')
    p.add_argument('--eksik', action='store_true', help='eksik adların tam listesi')
    p.add_argument('--celisen', action='store_true', help='çelişen çiftlerin tamamı')
    p.add_argument('--ortuk', action='store_true',
                   help='sayfanın başka bir yazım biçimiyle kapsadığı adlar')
    p.add_argument('--json', dest='json_yolu', help='tam raporu bu dosyaya JSON yaz')
    a = p.parse_args()

    tsvYolu = os.path.join(a.kojo, TSV_YOLU)
    if not os.path.exists(tsvYolu):
        sys.exit('üretilmiş sözlük bulunamadı: %s\n(--kojo ile masaüstü Koco klonunu gösterin)' % tsvYolu)

    sayfa = sayfaÇiftleri(a.sayfa)
    tsv = tsvSatırları(tsvYolu)
    kurallar = kuralHedefleri(os.path.join(a.kojo, KURAL_YOLU))
    r = karşılaştır(sayfa, tsv, kurallar, sayfaTakmaAdları(a.sayfa))
    yaz(r, (len(sayfa), len({t for t, _ in sayfa})), (len(tsv), len({s[1] for s in tsv})),
        a.eksik, a.celisen, a.ortuk)
    if a.json_yolu:
        with io.open(a.json_yolu, 'w', encoding='utf-8') as d:
            d.write(json.dumps(r, ensure_ascii=False, indent=1, sort_keys=True))
        print('tam rapor: %s' % a.json_yolu)


if __name__ == '__main__':
    try:
        main()
    except BrokenPipeError:
        # `| head` ile kesilince Python iz döküyor; çıktı bir rapor, kesilmesi olağan.
        try:
            sys.stdout.close()
        except Exception:
            pass
        os._exit(0)
