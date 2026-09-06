#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ornekler.py -- ikojo'nun /yardim/ornekler liste sayfasını üretir.

`/ornek/<yol>` rotası (Devre 3) 122 betiğin hepsini açabiliyor ama gezinilebilir
bir liste yoktu: adresi bilmeyen betiklere ulaşamıyordu. Bu betik masaüstü
Koco'nun Örnekler ve Sergi menülerini kaynak alıp aynı gruplamayla bir liste
sayfası üretir; her satırda betiğin ikojo'da çalışıp çalışmadığını gösteren bir
rozet ve doğrudan `/ornek/...` bağlantısı olur.

Kaynaklar
  kojo/.../lite/AppMenu.scala          menü yapısı (grup + kalem sırası)
  kojo/.../lite/Bundle_tr.properties   Türkçe başlıklar (S_* anahtarları)
  ornekler/README.md                   10 ikojo örneğinin açıklamaları
  ornekler/masaustu/derleme.tsv        GERÇEK derleme sonucu (varsa yeğlenir)
  ornekler/masaustu/tarama.tsv         tanımlayıcı taraması (yedek)

Kullanım
  python3 kilavuz/ornekler.py                       # kilavuz/html/ornekler.html
  python3 kilavuz/ornekler.py --twirl <editor>/server/src/main/twirl/views
"""
import argparse
import html
import os
import re
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
KOK = os.path.dirname(BURASI)
sys.path.insert(0, BURASI)
from uret import twirl_yap  # noqa: E402  (aynı @ kaçışı ve @("") parçalaması)

ORNEKLER = os.path.join(KOK, 'ornekler')
MASAUSTU = os.path.join(ORNEKLER, 'masaustu')

# AppMenu.scala'daki kök -> ornekler/masaustu altındaki dizin. Masaüstü
# loadAndRunLocalizedResource ile "/samples/" kökünü Türkçe'de "/samples/tr/"
# yapıyor; kopyalar da o yollarla duruyor.
KOKLER = {
    '/samples/': 'src/main/resources/samples/tr',
    '/robosim/': 'src/main/resources/robosim/tr',
    '/mathgames/': 'src/main/resources/mathgames/tr',
}

MENU_RE = re.compile(
    r'(?P<menu>\w+)\.add\(menuItemFor(?P<kurulu>InstalledFile)?\("(?P<anahtar>S_\w+)",\s*'
    r'"(?P<dosya>[^"]+)"(?:,\s*"(?P<kok>[^"]+)")?\)\)')
YENI_MENU_RE = re.compile(r'val (?P<degisken>\w+) = newJMenu\(Utils\.loadString\("(?P<anahtar>S_\w+)"\)\)')

# Gruba göre şerit rengi (yardim sayfasının paletinden)
RENKLER = {
    'baslangic': 'var(--yesil)', 'sergi': 'var(--kirmizi)', 'oteki': 'var(--soluk)',
}
GRUP_RENKLERI = ['var(--mavi)', 'var(--turuncu)', 'var(--sari)', 'var(--yesil)', 'var(--kirmizi)']


def baslıklar(kojo):
    """Bundle_tr.properties -> {S_Anahtar: Türkçe başlık}"""
    yol = os.path.join(kojo, 'src/main/resources/net/kogics/kojo/lite/Bundle_tr.properties')
    d = {}
    with open(yol, encoding='utf-8') as f:
        for satır in f:
            if satır.startswith('S_') and '=' in satır:
                a, _, b = satır.partition('=')
                d[a.strip()] = b.strip()
    return d


def menuler(kojo):
    """AppMenu.scala -> [(grup başlığı anahtarı, [(anahtar, göreli yol)])], menü sırasıyla.

    Yalnız Örnekler ve Sergi menüleri; ikisi de betik açan kalemlerden oluşuyor.
    """
    yol = os.path.join(kojo, 'src/main/scala/net/kogics/kojo/lite/AppMenu.scala')
    s = open(yol, encoding='utf-8').read()
    # Örnekler menüsünün gövdesi
    bas = s.index('val samplesMenu = newJMenu')
    son = s.index('menuBar.add(samplesMenu)')
    ornek_govde = s[bas:son]
    bas2 = s.index('val showcaseMenu = newJMenu')
    son2 = s.index('menuBar.add(showcaseMenu)')
    sergi_govde = s[bas2:son2]

    def coz(govde, tek_grup=None):
        grup_adı = {}          # değişken -> S_ anahtarı
        gruplar = []           # [(S_ anahtarı, [(S_kalem, yol)])]
        sıra = {}
        for m in YENI_MENU_RE.finditer(govde):
            grup_adı[m.group('degisken')] = m.group('anahtar')
        for m in MENU_RE.finditer(govde):
            # yorum satırındaki kalemleri atla
            satır_başı = govde.rfind('\n', 0, m.start()) + 1
            if govde[satır_başı:m.start()].lstrip().startswith('//'):
                continue
            if m.group('kurulu'):
                # examples/othello/menu.kojo -> installer/examples/othello/menu_tr.kojo.installed
                d = m.group('dosya')
                kök, ad = os.path.split(d)
                gövde_adı = ad[:-len('.kojo')] if ad.endswith('.kojo') else ad
                göreli = 'installer/%s/%s_tr.kojo.installed' % (kök, gövde_adı)
            else:
                kök = KOKLER.get(m.group('kok') or '/samples/')
                if kök is None:
                    continue
                göreli = '%s/%s' % (kök, m.group('dosya'))
            g = tek_grup or grup_adı.get(m.group('menu'))
            if g is None:
                continue
            if g not in sıra:
                sıra[g] = len(gruplar)
                gruplar.append((g, []))
            gruplar[sıra[g]][1].append((m.group('anahtar'), göreli))
        return gruplar

    return coz(ornek_govde), coz(sergi_govde, tek_grup='S_Showcase')


def tarama_oku(yol):
    """tarama.tsv: betik -> (durum, engeller). Dosya yoksa boş harita."""
    d = {}
    if not os.path.exists(yol):
        return d
    with open(yol, encoding='utf-8') as f:
        for satır in f:
            if satır.startswith('#'):
                continue
            p = satır.rstrip('\n').split('\t')
            if len(p) >= 4 and p[0] not in ('betik', ''):
                d[p[0]] = (p[1], p[3])
    return d


def durumlar():
    """
    Betik -> (durum, ek). derleme.tsv varsa onu yeğler, ama taramayla BİRLEŞTİRİR.

    Gerçek derleme yalnız geçti/kaldı biliyor; tarayıcıda karşılığı olmayan bir
    özellik (Swing arayüzü, öykü, MIDI...) kullanan betikler de "kaldı" görünür.
    Bunları "eksik komut" diye göstermek yanıltıcı olur, çünkü eksik olan komut
    değil platform. Bu yüzden derleme "kaldı" derken tarama "platform" diyorsa
    "masaüstü" rozetini ve engel adını koruyoruz. `ek`, "platform"da engel
    listesi, "kaldı"da derleyicinin hata özeti (rozetin title'ında görünür).
    """
    derleme = os.path.join(MASAUSTU, 'derleme.tsv')
    tarama = os.path.join(MASAUSTU, 'tarama.tsv')
    tarama_h = tarama_oku(tarama)
    if os.path.exists(derleme):
        d = {}
        with open(derleme, encoding='utf-8') as f:
            for satır in f:
                if satır.startswith('#'):
                    continue
                p = satır.rstrip('\n').split('\t')
                if len(p) >= 2 and p[0] not in ('betik', ''):
                    # anahtar "masaustu/<yol>" biçiminde; kırp
                    ad = p[0][len('masaustu/'):] if p[0].startswith('masaustu/') else p[0]
                    durum, hata = p[1], (p[2] if len(p) > 2 else '')
                    t_durum, t_engel = tarama_h.get(ad, ('', ''))
                    if durum == 'kaldı' and t_durum == 'platform':
                        d[ad] = ('platform', t_engel)
                    else:
                        d[ad] = (durum, hata)
        return d, 'derleme', derleme
    return tarama_h, 'tarama', tarama


def ikojo_ornekleri():
    """ornekler/README.md tablosundan (dosya, açıklama) çiftleri."""
    s = open(os.path.join(ORNEKLER, 'README.md'), encoding='utf-8').read()
    out = []
    for m in re.finditer(r'^\|\s*`([^`]+\.kojo)`\s*\|\s*(.+?)\s*\|$', s, re.M):
        out.append((m.group(1), m.group(2)))
    return out


def md_ici(metin):
    """README hücresindeki `kod` ve **kalın** işaretlerini HTML'e çevirir."""
    s = html.escape(metin)
    s = re.sub(r'`([^`]+)`', r'<code>\1</code>', s)
    s = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', s)
    return s


# ikojo örneklerinin başlıkları: dosya adları ASCII slug olduğu için (01-ilk-adimlar)
# Türkçe harfler dosya adından türetilemiyor.
IKOJO_BASLIK = {
    '01-ilk-adimlar.kojo': 'İlk adımlar',
    '02-renkli-cicek.kojo': 'Renkli çiçek',
    '03-resimler.kojo': 'Resimler',
    '04-klavye-oyunu.kojo': 'Klavye oyunu',
    '05-sekme-oyunu.kojo': 'Sekme oyunu',
    '06-koleksiyonlar.kojo': 'Koleksiyonlar',
    '07-nokta-ve-yoney.kojo': 'Nokta ve yöney',
    '08-kumanda-kolu.kojo': 'Kumanda kolu',
    '09-nerede-ve-dokunma.kojo': 'Nerede ve dokunma',
    '10-anahtar-kelimeler.kojo': 'Anahtar kelimeler',
}

ROZETLER = {
    'çalışır': ('calisir', 'çalışır', 'ikojo\'da olduğu gibi çalışması bekleniyor'),
    'geçti':   ('calisir', 'çalışır', 'gerçek derlemeden geçti'),
    'eksik-ad': ('eksik', 'eksik komut', 'ikojo\'da henüz olmayan komutlar kullanıyor'),
    'kaldı':    ('eksik', 'eksik komut', 'gerçek derlemede kaldı'),
    'platform': ('masaustu', 'masaüstü', 'tarayıcıda karşılığı olmayan bir özellik kullanıyor'),
    'sunucu':   ('eksik', 'sunucu sınırı', 'betik derleyiciye ulaşamadı (gövde sınırı)'),
}
ENGEL_ADI = {
    'yükle': 'başka dosya içe alıyor', 'arayüz': 'Swing arayüzü', 'öykü': 'öykü anlatıcı',
    'ses': 'MIDI müzik', 'dosya': 'dosya/klavye girdisi', 'canlandırma': 'geçiş canlandırması',
    'çini': 'çini dünyası', 'tuvalçizim': 'tuval çizimi', 'geo': 'geometri', 'imge': 'imge işleme',
    'url': 'ağ erişimi',
}


def rozet(durum, ek):
    sınıf, etiket, açıklama = ROZETLER.get(durum, ('eksik', durum or '?', ''))
    if durum == 'platform' and ek:
        adlar = [ENGEL_ADI.get(e, e) for e in ek.split(',') if e]
        if adlar:
            açıklama = 'masaüstüne özgü: ' + ', '.join(adlar)
    elif durum == 'kaldı' and ek:
        # derleme.tsv'nin 3. sütunu: derleyicinin ilk hata özeti
        açıklama = '%s: %s' % (açıklama, ek)
    return '<span class="rozet %s" title="%s">%s</span>' % (sınıf, html.escape(açıklama), etiket)


def satır_html(baslik, göreli, durum_haritası, ikojo=False, aciklama=None):
    if ikojo:
        yol, anahtar = göreli, göreli
    else:
        yol, anahtar = 'masaustu/' + göreli, göreli
    durum, ek = durum_haritası.get(anahtar, ('', ''))
    r = '<span class="rozet calisir" title="ikojo için yazıldı">çalışır</span>' if ikojo \
        else rozet(durum, ek)
    ac = '<div class="ac">%s</div>' % aciklama if aciklama else ''
    return ('  <li><a href="/ornek/%s"><span class="ad">%s</span>%s</a>'
            '<span class="sag">%s<code>%s</code></span></li>\n'
            % (html.escape(yol), html.escape(baslik), ac, r, html.escape(os.path.basename(göreli))))


def bolum(baslik, renk, alt, satirlar):
    return ('<section>\n<h2><span class="im" style="background:%s"></span>%s</h2>\n'
            '<p class="alt">%s</p>\n<ul class="liste">\n%s</ul>\n</section>\n'
            % (renk, html.escape(baslik), alt, ''.join(satirlar)))


CSS = """
        .kapsayici { max-width: 900px; margin: 0 auto; padding: 32px 24px 80px; }
        .kahraman { background: linear-gradient(135deg, #fff 0%, #fdf3f4 100%);
            border: 1px solid var(--cizgi); border-radius: 14px; padding: 32px 32px 28px; margin-bottom: 28px; }
        .kahraman h1 { margin: 0 0 8px; font-size: 30px; font-weight: 700; }
        .kahraman h1 .vurgu { color: var(--kirmizi); }
        .kahraman p { margin: 0; font-size: 17px; color: #404551; max-width: 64ch; }
        section { margin-bottom: 40px; }
        h2 { font-size: 21px; font-weight: 700; margin: 0 0 4px; display: flex; align-items: center; gap: 10px; }
        h2 .im { width: 10px; height: 22px; border-radius: 3px; display: inline-block; }
        section > .alt { color: var(--soluk); margin: 0 0 14px; font-size: 15px; }
        ul.liste { list-style: none; margin: 0; padding: 0; background: #fff;
            border: 1px solid var(--cizgi); border-radius: 10px; overflow: hidden; }
        ul.liste li { display: flex; align-items: center; justify-content: space-between; gap: 14px;
            padding: 9px 14px; border-bottom: 1px solid var(--cizgi); }
        ul.liste li:last-child { border-bottom: none; }
        ul.liste li:hover { background: #fcfcfd; }
        ul.liste a { display: block; color: var(--metin); flex: 1; min-width: 0; }
        ul.liste a:hover { text-decoration: none; }
        ul.liste a:hover .ad { color: var(--kirmizi); text-decoration: underline; }
        ul.liste .ad { font-weight: 600; }
        ul.liste .ac { color: var(--soluk); font-size: 13.5px; }
        ul.liste .ac code { background: var(--zemin); border-radius: 3px; padding: 0 3px; font-size: 12.5px;
            margin: 0 1px; }
        ul.liste .sag { display: flex; align-items: center; gap: 10px; flex: none; }
        ul.liste .sag code { color: var(--soluk); font-size: 12.5px; }
        .rozet { font-size: 12px; font-weight: 600; padding: 2px 9px; border-radius: 20px;
            white-space: nowrap; cursor: help; }
        .rozet.calisir  { background: #e4f7e9; color: #1e7a3c; }
        .rozet.eksik    { background: #fdf1dc; color: #94631a; }
        .rozet.masaustu { background: #eceef1; color: #5b626c; }
        .aciklama { background: #fff; border: 1px solid var(--cizgi); border-radius: 10px;
            padding: 16px 18px; margin-bottom: 36px; font-size: 14.5px; color: #404551; }
        .aciklama p { margin: 0 0 8px; }
        .aciklama p:last-child { margin: 0; }
        .aciklama .rozet { margin-right: 4px; }
        footer { text-align: center; color: var(--soluk); font-size: 13px;
            border-top: 1px solid var(--cizgi); padding-top: 24px; }
        @media (max-width: 640px) {
            .ust-bar .geri { display: none; }
            ul.liste li { flex-direction: column; align-items: flex-start; gap: 6px; }
            ul.liste .sag { width: 100%; }
            .kahraman { padding: 24px; } .kahraman h1 { font-size: 25px; }
        }
"""

BAS = """<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <title>iKoco — Örnekler ve Sergi</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="shortcut icon" href="/assets/images/favicon.ico">
    <link href="https://fonts.googleapis.com/css?family=Raleway:300,400,500,700&display=swap" rel="stylesheet">
    <style>
        :root { --kirmizi:#E95065; --mavi:#46BDDF; --yesil:#52D273; --sari:#E5C453;
                --turuncu:#E57255; --metin:#2d2f33; --soluk:#6b7078; --cizgi:#e5e7eb; --zemin:#f7f7f8; }
        * { box-sizing: border-box; }
        html, body { margin: 0; padding: 0; }
        body { font-family: 'Raleway', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            color: var(--metin); background: var(--zemin); line-height: 1.6; -webkit-font-smoothing: antialiased; }
        a { color: var(--kirmizi); text-decoration: none; }
        a:hover { text-decoration: underline; }
        code { font-family: 'SF Mono', 'Menlo', 'Consolas', monospace; }
        .ust-bar { display: flex; align-items: center; justify-content: space-between;
            padding: 12px 24px; background: #fff; border-bottom: 1px solid var(--cizgi);
            position: sticky; top: 0; z-index: 10; }
        .ust-bar img { height: 34px; display: block; }
        .ust-bar .geri { font-weight: 500; font-size: 14px; color: var(--metin);
            border: 1px solid var(--cizgi); border-radius: 6px; padding: 7px 14px; background: #fff; }
        .ust-bar .geri:hover { background: var(--zemin); text-decoration: none; }
        .gezinti { display: flex; gap: 4px; flex-wrap: wrap; }
        .gezinti a { font-size: 14px; font-weight: 500; color: var(--metin); padding: 6px 12px; border-radius: 6px; }
        .gezinti a:hover { background: var(--zemin); text-decoration: none; }
        .gezinti a.secili { background: var(--kirmizi); color: #fff; }
%s    </style>
</head>
<body>
    <div class="ust-bar">
        <a href="/"><img src="/assets/images/scalafiddle-logo.png" alt="iKoco"></a>
        <nav class="gezinti">
            <a href="/yardim">Yardım</a>
            <a href="/yardim/skala">Skala</a>
            <a href="/yardim/komutlar">Komutlar</a>
            <a href="/yardim/ornekler" class="secili">Örnekler</a>
            <a href="/yardim/sozluk">Sözlük</a>
            <a href="/yardim/farklar">Farklar</a>
        </nav>
        <a href="/" class="geri">← Düzenleme penceresine dön</a>
    </div>

    <div class="kapsayici">
        <div class="kahraman">
            <h1><span class="vurgu">Örnekler</span> ve Sergi</h1>
            <p>Masaüstü Koco'nun bütün Türkçe örnekleri burada. Bir başlığa tıkla,
               betik düzenleme penceresinde açılsın; sonra <b>Çalıştır</b>'a bas.</p>
        </div>
"""


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--kojo', default=os.path.join(KOK, '..', 'kojo'), help='masaüstü kojo klonu')
    p.add_argument('--twirl', metavar='DİZİN', help='editör şablonunu da bu dizine yaz')
    a = p.parse_args()
    kojo = os.path.abspath(a.kojo)

    bas = baslıklar(kojo)
    ornek_gruplari, sergi_gruplari = menuler(kojo)
    durum_haritası, kaynak_türü, kaynak_yolu = durumlar()

    parçalar = [BAS % CSS]
    menüdekiler = set()
    eksik_dosya = []

    def satirlari_yap(kalemler):
        out = []
        for anahtar, göreli in kalemler:
            tam = os.path.join(MASAUSTU, göreli)
            if not os.path.exists(tam):
                eksik_dosya.append(göreli)
                continue
            menüdekiler.add(göreli)
            out.append(satır_html(bas.get(anahtar, os.path.basename(göreli)), göreli, durum_haritası))
        return out

    # 1) ikojo'nun kendi örnekleri
    satırlar = [satır_html(IKOJO_BASLIK.get(ad, ad), ad, durum_haritası,
                           ikojo=True, aciklama=md_ici(ac))
                for ad, ac in ikojo_ornekleri()]
    parçalar.append(bolum('Başlangıç', RENKLER['baslangic'],
                          'iKoco için yazılmış on örnek; sırayla ilerlemek için.', satırlar))

    # 2) Sergi
    for anahtar, kalemler in sergi_gruplari:
        parçalar.append(bolum(bas.get(anahtar, 'Sergi'), RENKLER['sergi'],
                              'Masaüstü Koco\'nun Sergi menüsündeki gösteri programları.',
                              satirlari_yap(kalemler)))

    # 3) Örnekler menüsü, masaüstündeki grup sırasıyla
    for i, (anahtar, kalemler) in enumerate(ornek_gruplari):
        parçalar.append(bolum(bas.get(anahtar, anahtar), GRUP_RENKLERI[i % len(GRUP_RENKLERI)],
                              'Masaüstü Örnekler menüsündeki grup.', satirlari_yap(kalemler)))

    # 4) Menülerde geçmeyen betikler (kılavuz parçaları, othello modülleri...)
    kalanlar = []
    for kök, _, dosyalar in os.walk(MASAUSTU):
        for f in sorted(dosyalar):
            if not (f.endswith('.kojo') or f.endswith('.kojo.installed')):
                continue
            göreli = os.path.relpath(os.path.join(kök, f), MASAUSTU)
            if göreli not in menüdekiler:
                kalanlar.append(göreli)
    if kalanlar:
        satırlar = [satır_html(os.path.basename(g), g, durum_haritası,
                               aciklama='<code>%s</code>' % html.escape(os.path.dirname(g)))
                    for g in sorted(kalanlar)]
        parçalar.append(bolum('Öteki betikler', RENKLER['oteki'],
                              'Menülerde geçmeyen dosyalar: kılavuz bölümleri, oyun modülleri, '
                              'başka betiklerin içe aldığı parçalar.', satırlar))

    # rozet açıklaması
    sayım = {}
    for g in menüdekiler | set(kalanlar):
        d = durum_haritası.get(g, ('', ''))[0]
        sayım[d] = sayım.get(d, 0) + 1
    çalışan = sayım.get('çalışır', 0) + sayım.get('geçti', 0)
    kaynak_cümlesi = ('Rozetler <b>gerçek derleme</b> sonucundan (<code>derleme.tsv</code>); '
                      '"masaüstü" rozeti ad taramasından geliyor.'
                      if kaynak_türü == 'derleme' else
                      'Rozetler bir <b>ad taramasından</b> geliyor (<code>tarama.tsv</code>), '
                      'gerçek derlemeden değil: "çalışır" diyen birkaç betik yine de hata verebilir.')
    parçalar.append(
        '<div class="aciklama">\n'
        '<p>%s<b>çalışır</b> — tarayıcıda çalışması beklenen %d betik. '
        '%s<b>eksik komut</b> — ikojo\'da henüz olmayan bir komut kullanıyor. '
        '%s<b>masaüstü</b> — Swing arayüzü, öykü anlatıcı ya da MIDI müzik gibi '
        'tarayıcıda karşılığı olmayan bir özelliğe dayanıyor.</p>\n'
        '<p>%s Eksik olanlar plan ilerledikçe azalıyor; ayrıntı için '
        '<a href="/yardim/farklar">Farklar</a> sayfasına bak.</p>\n</div>\n'
        % ('<span class="rozet calisir">çalışır</span>', çalışan,
           '<span class="rozet eksik">eksik komut</span>',
           '<span class="rozet masaustu">masaüstü</span>', kaynak_cümlesi))

    parçalar.append('<footer>Betikler <a href="https://github.com/bulent2k2/kojo" target="_blank">'
                    'masaüstü Koco</a> deposundan olduğu gibi alınmıştır.</footer>\n'
                    '    </div>\n</body>\n</html>\n')

    metin = ''.join(parçalar)
    çıktı = os.path.join(BURASI, 'html', 'ornekler.html')
    os.makedirs(os.path.dirname(çıktı), exist_ok=True)
    open(çıktı, 'w', encoding='utf-8').write(metin)
    toplam = len(menüdekiler) + len(kalanlar) + len(ikojo_ornekleri())
    print('yazıldı: %s  (%d betik; %d menüde, %d öteki, 10 ikojo)'
          % (os.path.relpath(çıktı, KOK), toplam, len(menüdekiler), len(kalanlar)))
    print('durum kaynağı: %s (%s)' % (kaynak_türü, os.path.relpath(kaynak_yolu, KOK)))
    if eksik_dosya:
        print('menüde olup kopyada olmayan (atlandı): %s' % ', '.join(eksik_dosya))
    if a.twirl:
        hedef = os.path.join(a.twirl, 'yardimOrnekler.scala.html')
        open(hedef, 'w', encoding='utf-8').write(twirl_yap(
            metin, 'kojojs-dev/kilavuz/ornekler.py (masaüstü menüleri + tarama/derleme.tsv)',
            'python3 kilavuz/ornekler.py --twirl <editor>/server/src/main/twirl/views'))
        print('twirl: %s' % hedef)


if __name__ == '__main__':
    main()
