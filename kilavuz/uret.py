#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
uret.py -- kilavuz/skala/*.md ve kilavuz/komutlar/*.md kaynaklarından ikojo'nun
çevrim içi kılavuz sayfalarını üretir.

Çıktılar:
  kilavuz/html/skala.html, kilavuz/html/komutlar.html
      Tek başına açılabilen saf HTML (sol menü, bölümler, kod blokları, "editörde
      aç" bağlantıları). Tarayıcıda dosya olarak açıp bakmak için.
  --twirl DİZİN  ->  DİZİN/yardimSkala.scala.html, DİZİN/yardimKomutlar.scala.html
      Aynı içerik, kojojs-editor'un Twirl şablonu olarak: başa "@()" imzası,
      içerikteki her "@" -> "@@" (Twirl'de @ özel karakter). Bağlantılar sunucuya
      göreli (/?zrc=...), böylece hangi dağıtımda çalışıyorsa orayı açar.
  kilavuz/eksik-adlar.tsv
      Kod örneklerinde geçen ama ikojo'da bulunmayan masaüstü adlarının sıklığı.

"Editörde aç" bağlantısı: <taban>/?zrc=<gzip + base64url kaynak>. Editör bunu
zaten çözüyor (kojojs-editor Application.scala decodeSource: base64Url ->
GZIPInputStream). URL 2048 karakteri aşarsa bağlantı yerine "Kopyala" düğmesi.

Masaüstü rozeti: bir kod örneğindeki ad masaüstü Koco API'sinde var ama ikojo'da
(kojo/TurkishTurtle.scala + kojo/tr/*.scala + İngilizce yüzey) yoksa bloğun
altına "masaüstü" rozeti ve "ikojo'da yok: ad (→ karşılık)" satırı düşer.
Masaüstü ad listesi kojo klonundan okunur (--kojo, yoksa bilinen yollar) ve
kilavuz/masaustu-adlar.txt'ye önbelleklenir; klon yoksa önbellek kullanılır.
Markdown'daki elle işaretler (README'ye bak): kod bloğunun hemen ardındaki
  <!-- masaüstü -->                 rozeti zorla
  <!-- masaüstü: ad→karşılık, ad2 --> rozet + karşılık bilgisi ekle
  <!-- ikojo -->                    otomatik bulguyu sustur (yanlış alarm)
  <!-- ikojo: ad1, ad2 -->          yalnız bu adları sustur

Harici kütüphane yok; python3 standart kütüphanesi yeter.
"""
import argparse
import base64
import collections
import glob
import gzip
import html
import os
import re
import sys

BURASI = os.path.dirname(os.path.abspath(__file__))
IKOJO = os.path.dirname(BURASI)

# --- ad tarama temelleri (araclar/ucurum.py ile aynı; oraya bağımlı olmamak için burada da)
# def/val/var/object/class/trait/type tanımlarından ad çıkarır (geri tırnaklılar dahil)
TANIM = re.compile(r'\b(?:def|val|var|object|class|trait|type|lazy\s+val)\s+(`[^`]+`|[^\s(\[:=,)]+)')
# Yamalı scala-tr derleyicisinin tanıdığı Türkçe anahtar kelimeler
ANAHTAR = {
    'dez', 'den', 'tanım', 'eğer', 'yoksa', 'yineleDoğruKaldıkça', 'için', 'ver', 'eşle', 'durum',
    'sınıf', 'nesne', 'özellik', 'yayar', 'birlikte', 'yeni', 'bu', 'üst', 'baskın', 'soyut', 'son',
    'damgalı', 'örtük', 'miskin', 'gizli', 'koru', 'deste', 'getir', 'geriDön', 'dene', 'yakala',
    'sonunda', 'bildir', 'yap', 'tür', 'doğru', 'yanlış', 'yok',
}
# Çok genel Scala/Java adları -- eksik diye sayılmaz
GENEL = {'apply', 'toString', 'length', 'size', 'map', 'foreach', 'filter', 'to', 'until', 'by', 'scala', 'java',
         'x', 'y', 'z', 'a', 'b', 'n', 'i', 'j', 'k'}


def soy(s):
    """Yorumları ve dizgeleri at; geriye yalnız kod kalsın."""
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s = re.sub(r'//[^\n]*', '', s)
    s = re.sub(r'"""(?:.|\n)*?"""', '""', s)
    s = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\\n])'", "''", s)
    return s

URL_SINIRI = 2048
TABAN_HTML = 'https://ikojo.fly.dev'
TABAN_TWIRL = ''

KILAVUZLAR = collections.OrderedDict([
    ('skala', {
        'başlık': "Scala'ya Hızlı Giriş",
        'alt': "Skala/Kojo öğreticisi — masaüstü Koco'daki 19 sayfalık öykünün tarayıcı sürümü.",
        'twirl': 'yardimSkala.scala.html',
        'sekme': 'Skala',
    }),
    ('komutlar', {
        'başlık': "Koco Komutları",
        'alt': "Kaplumbağanın anladığı komutlar, tuval, genel komutlar ve türler — masaüstü Koco belgelerinin tarayıcı sürümü.",
        'twirl': 'yardimKomutlar.scala.html',
        'sekme': 'Komutlar',
    }),
])

# Yardım sayfaları arası gezinti şeridi (mevcut yardim.scala.html ile aynı sıra)
GEZINTI = [('/yardim', 'Yardım'), ('/yardim/skala', 'Skala'), ('/yardim/komutlar', 'Komutlar'),
           ('/yardim/ornekler', 'Örnekler'), ('/yardim/sozluk', 'Sözlük'), ('/yardim/farklar', 'Farklar')]

# ikojo'da olmayan masaüstü adı -> ikojo karşılığı (bilinenler). Boş dizge: karşılığı yok.
KARSILIK = {
    'tuvalAlanı': 'tuvalSınırları',
    'silipSakla': 'silVeSakla',
    'yaklaş': 'yaklaşXY(k, k, x, y)',
    'yaklaşmayıSil': 'yaklaşXY(1, 1, 0, 0)',
    'durdur': 'canlandırmayıDurdur',
    'oyunSüresiniGeriyeSayarakGöster': 'oyunSüresiniGöster',
    'rastgeleİkil': 'rastgeleSeçim',
    'rastgeleÇanEğrisinden': 'rastgeleDoğalKesir',
    'gerekli': 'require',
    'tuşaBasınca': 'onKeyPress',
    'BuAn': 'buSaniye',
    'buAn': 'buSaniye',
    'çıktıyıSil': '',
    'gridiGöster': '',
    'gridiGizle': '',
    'ızgarayıGöster': '',
    'ızgarayıGizle': '',
    'eksenleriGöster': '',
    'eksenleriGizle': '',
    'kaplumbağa0': '',
    'yeniKaplumbağa': '',
    'fareKonumu': '',
    'tuvaleYaz': 'yazı',
}

# Kod örneklerinde değişken adı olarak sık geçen, masaüstü API'sinde de tesadüfen
# bulunan sözcükler; eksik diye sayılmaz.
GENEL_EK = {'en', 'boy', 'adı', 'hepsi', 'zaman', 'değeri', 'değer', 'olay', 'gün', 'çıktı', 'sınıfı',
            'tr', 'içinde', 'üzerinde', 'İngilizce', 'Çizim', 'düz', 'vuruş', 'rb', 'olmaz', 'renk',
            'harf', 'sayı', 'kural', 'saat', 'dakika', 'saniye', 'ad', 'yaş', 'nesil', 'oran', 'yön',
            'satır', 'sütun', 'nokta', 'noktalar', 'kare', 'top', 'raket', 'skor', 'mesaj', 'tohum',
            'ağaç', 'desen', 'seç', 'küme', 'sunum', 'sayaç', 'önceki', 'kuyruk', 'girdi', 'koşul',
            'eğri', 'aralık', 'çizgi', 'çizgiler', 'renkDizisi', 'yardımcı', 'toplam', 'birim', 'satış',
            'ara', 'çift', 'tek', 'ele', 'bul', 'Y', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'm', 'n', 'p', 's', 't', 'u', 'v', 'x', 'y', 'z'}

TANIMLAYICI = re.compile(r'[^\W\d]\w*', re.U)
# Betik içindeki yerel tanımlar (Türkçe ve İngilizce anahtar sözcüklerle)
YEREL_TANIM = re.compile(
    r'\b(?:tanım|dez|den|nesne|sınıf|özellik|tür|def|val|var|object|class|trait|type|lazy\s+val)\s+'
    r'(`[^`]+`|[^\s(\[:=,)]+)')
YEREL_DESEN = [
    re.compile(r'\b(?:dez|den|val|var)\s*\(([^)]*)\)'),          # dez (a, b) = ...
    re.compile(r'(?:\(([^()]*)\)|\b(\w+))\s*=>'),                  # (a, b) => / a =>
    re.compile(r'\b(\w+)\s*<-'),                                   # a <- ...
    re.compile(r'\bdurum\s+\w+\(([^)]*)\)'),                       # durum Yaprak(a, değer)
    re.compile(r'\((\w+(?:\s*:\s*[\w\[\]]+)?(?:\s*,\s*\w+(?:\s*:\s*[\w\[\]]+)?)*)\)\s*(?::\s*[\w\[\]]+\s*)?=\s*[{(]?'),  # işlev parametreleri
]


# ---------------------------------------------------------------- ad kümeleri

def tanimlar(yollar):
    adlar = set()
    for y in yollar:
        try:
            with open(y, encoding='utf-8') as f:
                s = f.read()
        except OSError:
            continue
        s = soy(s)  # yorum içindeki "// def ..." sayılmasın
        for m in TANIM.finditer(s):
            ad = m.group(1).strip('`')
            if ad and ad != '_' and not ad.startswith('$'):
                adlar.add(ad)
    return adlar


def ad_kumeleri(kojo):
    src = os.path.join(IKOJO, 'src/main/scala/kojo')
    if not os.path.isdir(src):
        return set(), set()
    ikojo = tanimlar([os.path.join(src, 'TurkishTurtle.scala')] + glob.glob(os.path.join(src, 'tr/*.scala'))
                     + glob.glob(os.path.join(src, '*.scala')) + glob.glob(os.path.join(src, 'syntax/*.scala'))
                     + glob.glob(os.path.join(src, 'doodle/*.scala')))
    onbellek = os.path.join(BURASI, 'masaustu-adlar.txt')
    masa = set()
    adaylar = [kojo] if kojo else [os.path.join(IKOJO, '..', 'kojo'), os.path.expanduser('~/kojo'),
                                   os.path.expanduser('~/src/kojo')]
    for k in adaylar:
        i18n = os.path.join(k, 'src/main/scala/net/kogics/kojo/lite/i18n')
        if os.path.isdir(i18n):
            api_degil = re.compile(r'/(?:dict|help|templates|translate|data\w*)\.scala$')
            masa = tanimlar([os.path.join(i18n, 'trInit.scala')] +
                            [f for f in glob.glob(os.path.join(i18n, 'tr/*.scala')) if not api_degil.search(f)])
            with open(onbellek, 'w', encoding='utf-8') as f:
                f.write('# uret.py: masaüstü Koco TR API adları (%s). Elle düzenleme.\n' % os.path.basename(k))
                f.write('\n'.join(sorted(masa)) + '\n')
            break
    if not masa and os.path.exists(onbellek):
        with open(onbellek, encoding='utf-8') as f:
            masa = {s.strip() for s in f if s.strip() and not s.startswith('#')}
    return ikojo, masa


class Denetci:
    def __init__(self, kojo):
        self.ikojo, self.masa = ad_kumeleri(kojo)
        self.sayac = collections.Counter()
        self.nerede = collections.defaultdict(set)

    def eksikler(self, kod, sustur, bolum):
        if not self.masa:
            return []
        temiz = soy(kod)
        yerel = set()
        for m in YEREL_TANIM.finditer(temiz):
            yerel.add(m.group(1).strip('`'))
        for rx in YEREL_DESEN:
            for m in rx.finditer(temiz):
                for grup in m.groups():
                    if grup:
                        for parca in grup.split(','):
                            yerel.add(parca.split(':')[0].strip())
        tokenler = set(TANIMLAYICI.findall(temiz))
        eksik = sorted(t for t in tokenler
                       if t in self.masa and t not in self.ikojo and t not in ANAHTAR
                       and t not in yerel and t not in GENEL_EK and t not in sustur and t not in GENEL)
        for t in eksik:
            self.sayac[t] += 1
            self.nerede[t].add(bolum)
        return eksik


# ---------------------------------------------------------------- markdown

SATIR_ICI_KOD = re.compile(r'`([^`]+)`')


def satir_ici(metin, baglam):
    """Satır içi Markdown -> HTML. Kod parçaları HTML'den korunur."""
    parcalar = []

    def kod_yerine(m):
        parcalar.append('<code>%s</code>' % html.escape(m.group(1)))
        return '\x00%d\x00' % (len(parcalar) - 1)
    s = SATIR_ICI_KOD.sub(kod_yerine, metin)
    s = html.escape(s, quote=False)
    s = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', s)
    s = re.sub(r'(?<![\w*])\*([^*\n]+?)\*(?![\w*])', r'<i>\1</i>', s)

    def baglanti(m):
        # s yukarıda html.escape'ten geçti; href'i geri açıp bir kez, tırnaklı kaçır
        # (yoksa & içeren adres &amp;amp; olur)
        href = html.unescape(m.group(2))
        href = href.replace('{skala}', baglam['skala']).replace('{komutlar}', baglam['komutlar'])
        hedef = '' if href.startswith(('#', '/', 'skala.html', 'komutlar.html')) else ' target="_blank" rel="noopener"'
        return '<a href="%s"%s>%s</a>' % (html.escape(href, quote=True), hedef, m.group(1))
    s = re.sub(r'\[([^\]]+)\]\(([^)\s]+)\)', baglanti, s)
    s = re.sub('\x00(\\d+)\x00', lambda m: parcalar[int(m.group(1))], s)
    return s


def bloklar(metin):
    """Markdown alt kümesini blok listesine çevirir."""
    satirlar = metin.split('\n')
    out = []
    i = 0
    n = len(satirlar)
    while i < n:
        s = satirlar[i]
        if not s.strip():
            i += 1
            continue
        if s.startswith('```'):
            dil = s[3:].strip()
            j = i + 1
            kod = []
            while j < n and not satirlar[j].startswith('```'):
                kod.append(satirlar[j])
                j += 1
            j += 1
            isaret = ''
            if j < n and re.match(r'\s*<!--\s*(masaüstü|ikojo)', satirlar[j]):
                isaret = satirlar[j].strip()
                j += 1
            out.append(('kod', dil, '\n'.join(kod), isaret))
            i = j
            continue
        m = re.match(r'(#{1,4})\s+(.*)', s)
        if m:
            out.append(('başlık', len(m.group(1)), m.group(2).strip()))
            i += 1
            continue
        if s.startswith('<!--'):
            out.append(('yorum', s))
            i += 1
            continue
        if s.lstrip().startswith('|'):
            j = i
            satirlar_t = []
            while j < n and satirlar[j].lstrip().startswith('|'):
                satirlar_t.append(satirlar[j].strip())
                j += 1
            out.append(('tablo', satirlar_t))
            i = j
            continue
        if re.match(r'\s*[-*]\s+', s):
            j = i
            ogeler = []
            while j < n and re.match(r'\s*[-*]\s+', satirlar[j]):
                ogeler.append(re.sub(r'^\s*[-*]\s+', '', satirlar[j]))
                j += 1
            out.append(('liste', ogeler))
            i = j
            continue
        if s.startswith('>'):
            j = i
            sat = []
            while j < n and satirlar[j].startswith('>'):
                sat.append(satirlar[j][1:].strip())
                j += 1
            out.append(('not', ' '.join(sat)))
            i = j
            continue
        j = i
        sat = []
        while j < n and satirlar[j].strip() and not satirlar[j].startswith(('```', '#', '|', '>', '<!--')) \
                and not re.match(r'\s*[-*]\s+', satirlar[j]):
            sat.append(satirlar[j].strip())
            j += 1
        out.append(('p', ' '.join(sat)))
        i = j
    return out


def hucreler(satir):
    ic = satir.strip()
    if ic.startswith('|'):
        ic = ic[1:]
    if ic.endswith('|') and not ic.endswith('\\|'):
        ic = ic[:-1]
    parcalar = re.split(r'(?<!\\)\|', ic)
    return [p.replace('\\|', '|').strip() for p in parcalar]


def ayirici_mi(satir):
    return re.match(r'^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?$', satir.strip()) is not None


# ---------------------------------------------------------------- html üretimi

# Editörün ?zrc= bağlantısı TAM ScalaFiddle kaynağı bekler: istemci
# (FiddleEditor.extractCode) kaynağı $FiddleStart / $FiddleEnd işaretlerine göre
# böler, yalnız gövdeyi gösterir ve derlemeye gönderirken sarmalayıcıyı geri
# ekler. Yalın gövde gönderilirse işaret yok -> her şey "main" olur -> derleyiciye
# `object ScalaFiddle` olmadan gider: "expected class or object definition".
# Bu şablon kojojs-editor application.conf `scalafiddle.defaultSource` ile ve
# ornekleri-dogrula.sh `sar()` ile AYNI olmalı (aynı ders: OrnekYukleyici.sar).
SARMAL_BAS = """import fiddle.Fiddle.println
import scalajs.js

@js.annotation.JSExportTopLevel("ScalaFiddle")
object ScalaFiddle {
    import kojo.{SwedishTurtle, TurkishTurtle, Turtle, KojoWorldImpl, Vector2D, Picture}
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

  // $FiddleStart
"""
SARMAL_SON = """
  // $FiddleEnd
}
"""


def sarmala(kod):
    """Gövdeyi editörün beklediği tam ScalaFiddle kaynağına sarar (işaretler dahil)."""
    govde = kod.rstrip('\n') or '// (boş betik)'
    return SARMAL_BAS + govde + '\n' + SARMAL_SON


def zrc(kod):
    return base64.urlsafe_b64encode(gzip.compress(sarmala(kod).encode('utf-8'), mtime=0)).decode('ascii')


def isaret_coz(isaret):
    """<!-- masaüstü: a→b, c --> / <!-- ikojo: a --> -> (zorla, sustur_hepsi, sustur, karşılıklar)"""
    zorla, sustur_hepsi, sustur, karsilik = False, False, set(), {}
    if not isaret:
        return zorla, sustur_hepsi, sustur, karsilik
    m = re.match(r'<!--\s*(masaüstü|ikojo)\s*:?\s*(.*?)\s*-->', isaret)
    tur, govde = m.group(1), m.group(2)
    adlar = [a.strip() for a in govde.split(',') if a.strip()] if govde else []
    if tur == 'masaüstü':
        zorla = True
        for a in adlar:
            if '→' in a or '->' in a:
                ad, k = re.split(r'→|->', a, 1)
                karsilik[ad.strip()] = k.strip()
            else:
                karsilik[a] = None
    else:
        if adlar:
            sustur = set(adlar)
        else:
            sustur_hepsi = True
    return zorla, sustur_hepsi, sustur, karsilik


def rozet_html(eksik, karsilik):
    if not eksik:
        return ''
    parcalar = []
    for ad in eksik:
        k = karsilik.get(ad)
        if k is None:
            k = KARSILIK.get(ad)
        if k:
            parcalar.append('<code>%s</code> → <code>%s</code>' % (html.escape(ad), html.escape(k)))
        else:
            parcalar.append('<code>%s</code>' % html.escape(ad))
    return ('<span class="rozet" title="Bu örnekteki bazı adlar yalnız masaüstü Koco\'da var">masaüstü</span>'
            '<span class="eksik">ikojo\'da yok: %s</span>' % ', '.join(parcalar))


def kod_blogu(kod, isaret, denetci, bolum, taban):
    zorla, sustur_hepsi, sustur, karsilik = isaret_coz(isaret)
    eksik = [] if sustur_hepsi else denetci.eksikler(kod, sustur, bolum)
    for ad in karsilik:
        if ad not in eksik:
            eksik.append(ad)
    if zorla and not eksik:
        eksik = ['(masaüstü)']
    kodu = kod.strip('\n')
    z = zrc(kodu)
    url = '%s/?zrc=%s' % (taban, z)
    if len(url if taban else TABAN_HTML + url) <= URL_SINIRI:
        ac = '<a class="ac" href="%s" target="_blank" rel="noopener">Editörde aç →</a>' % url
    else:
        ac = '<button type="button" class="kopyala" title="Kod bağlantıya sığmayacak kadar uzun; panoya kopyala, editöre yapıştır">Kopyala</button>'
    rozet = rozet_html(eksik, karsilik) if eksik else ''
    return ('<div class="kod%s"><pre class="kod-blok">%s</pre>'
            '<div class="kod-alt">%s%s</div></div>' % (' masa' if eksik else '', html.escape(kodu), ac, rozet))


def hucre_html(h, calistir, denetci, bolum, taban, baglam):
    m = re.fullmatch(r'`([^`]+)`', h.strip())
    if m and calistir:
        kod = m.group(1)
        eksik = denetci.eksikler(kod, set(), bolum)
        url = '%s/?zrc=%s' % (taban, zrc(kod))
        rozet = ' <span class="rozet mini" title="ikojo\'da yok: %s">masaüstü</span>' % html.escape(', '.join(eksik)) if eksik else ''
        return '<a class="calistir" href="%s" target="_blank" rel="noopener" title="Editörde aç"><code>%s</code></a>%s' % (
            url, html.escape(kod), rozet)
    return satir_ici(h, baglam)


def bolum_html(no, dosya, denetci, taban, baglam):
    with open(dosya, encoding='utf-8') as f:
        metin = f.read()
    calistir = re.search(r'<!--\s*hücreler:\s*çalıştır\s*-->', metin) is not None
    kimlik = 'b%02d' % no
    parcalar = []
    baslik = ''
    for b in bloklar(metin):
        tur = b[0]
        if tur == 'başlık':
            if b[1] == 1 and not baslik:
                baslik = b[2]
                continue
            seviye = min(b[1] + 1, 5)  # dosyadaki ## -> h3 (h2 bölüm başlığı)
            parcalar.append('<h%d>%s</h%d>' % (seviye, satir_ici(b[2], baglam), seviye))
        elif tur == 'p':
            parcalar.append('<p>%s</p>' % satir_ici(b[1], baglam))
        elif tur == 'kod':
            parcalar.append(kod_blogu(b[2], b[3], denetci, kimlik, taban))
        elif tur == 'liste':
            parcalar.append('<ul>%s</ul>' % ''.join('<li>%s</li>' % satir_ici(o, baglam) for o in b[1]))
        elif tur == 'not':
            parcalar.append('<div class="not">%s</div>' % satir_ici(b[1], baglam))
        elif tur == 'tablo':
            sat = b[1]
            basliklar = None
            if len(sat) >= 2 and ayirici_mi(sat[1]):
                basliklar = hucreler(sat[0])
                sat = sat[2:]
            th = ''
            if basliklar:
                th = '<thead><tr>%s</tr></thead>' % ''.join('<th>%s</th>' % satir_ici(h, baglam) for h in basliklar)
            tr = ''.join('<tr>%s</tr>' % ''.join(
                '<td>%s</td>' % hucre_html(h, calistir, denetci, kimlik, taban, baglam) for h in hucreler(s))
                for s in sat if not ayirici_mi(s))
            parcalar.append('<div class="tablo-kap"><table>%s<tbody>%s</tbody></table></div>' % (th, tr))
        elif tur == 'yorum':
            continue
    return kimlik, baslik, '\n'.join(parcalar)


CSS = r"""
        :root { --kirmizi:#E95065; --mavi:#46BDDF; --yesil:#52D273; --sari:#E5C453; --turuncu:#E57255;
                --metin:#2d2f33; --soluk:#6b7078; --cizgi:#e5e7eb; --zemin:#f7f7f8; }
        * { box-sizing: border-box; }
        html, body { margin: 0; padding: 0; }
        html { scroll-behavior: smooth; scroll-padding-top: 70px; }
        body { font-family: 'Raleway', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: var(--metin);
               background: var(--zemin); line-height: 1.6; -webkit-font-smoothing: antialiased; }
        a { color: var(--kirmizi); text-decoration: none; }
        a:hover { text-decoration: underline; }
        code, pre { font-family: 'SF Mono', 'Menlo', 'Consolas', monospace; }
        .ust-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 24px;
                   background: #fff; border-bottom: 1px solid var(--cizgi); position: sticky; top: 0; z-index: 10; }
        .ust-bar img { height: 34px; display: block; }
        .ust-bar .geri { font-weight: 500; font-size: 14px; color: var(--metin); border: 1px solid var(--cizgi);
                         border-radius: 6px; padding: 7px 14px; background: #fff; white-space: nowrap; }
        .ust-bar .geri:hover { background: var(--zemin); text-decoration: none; }
        .gezinti { display: flex; gap: 4px; flex-wrap: wrap; }
        .gezinti a { font-size: 14px; font-weight: 500; color: var(--metin); padding: 6px 12px; border-radius: 6px; }
        .gezinti a:hover { background: var(--zemin); text-decoration: none; }
        .gezinti a.secili { background: var(--kirmizi); color: #fff; }
        .sayfa { display: grid; grid-template-columns: 250px minmax(0, 1fr); gap: 32px; max-width: 1180px;
                 margin: 0 auto; padding: 28px 24px 80px; }
        .yan { position: sticky; top: 72px; align-self: start; max-height: calc(100vh - 90px); overflow-y: auto;
               background: #fff; border: 1px solid var(--cizgi); border-radius: 12px; padding: 14px 8px; }
        .yan .yan-baslik { font-size: 12px; text-transform: uppercase; letter-spacing: .05em; color: var(--soluk);
                           font-weight: 600; padding: 4px 10px 8px; }
        .yan ol { list-style: none; margin: 0; padding: 0; counter-reset: b; }
        .yan li a { display: block; padding: 6px 10px; border-radius: 6px; color: var(--metin); font-size: 14px; line-height: 1.35; }
        .yan li a::before { counter-increment: b; content: counter(b) ". "; color: var(--soluk); font-variant-numeric: tabular-nums; }
        .yan li a:hover { background: var(--zemin); text-decoration: none; }
        .kahraman { background: linear-gradient(135deg, #fff 0%, #fdf3f4 100%); border: 1px solid var(--cizgi);
                    border-radius: 14px; padding: 28px 32px 24px; margin-bottom: 36px; }
        .kahraman h1 { margin: 0 0 8px; font-size: 30px; font-weight: 700; }
        .kahraman h1 .vurgu { color: var(--kirmizi); }
        .kahraman p { margin: 0; font-size: 16.5px; color: #404551; max-width: 70ch; }
        section.bolum { background: #fff; border: 1px solid var(--cizgi); border-radius: 14px; padding: 28px 32px; margin-bottom: 28px; }
        section.bolum > h2 { font-size: 24px; font-weight: 700; margin: 0 0 14px; display: flex; align-items: baseline; gap: 12px; }
        section.bolum > h2 .no { color: var(--kirmizi); font-variant-numeric: tabular-nums; font-size: 20px; }
        h3 { font-size: 19px; font-weight: 700; margin: 26px 0 8px; }
        h4 { font-size: 16px; font-weight: 700; margin: 20px 0 6px; }
        h5 { font-size: 15px; font-weight: 600; margin: 16px 0 4px; color: var(--soluk); }
        p { margin: 0 0 12px; }
        ul { margin: 0 0 12px; padding-left: 22px; }
        p code, li code, td code, .not code { background: var(--zemin); border-radius: 4px; padding: 1px 6px; font-size: 13.5px; }
        .not { background: #fffaf0; border-left: 4px solid var(--sari); border-radius: 8px; padding: 10px 14px; margin: 0 0 14px; font-size: 14.5px; }
        .kod { margin: 0 0 16px; border-radius: 10px; overflow: hidden; border: 1px solid #23252a; }
        pre.kod-blok { background: #2b2d31; color: #e6e6e6; padding: 16px 18px; overflow-x: auto; font-size: 13.5px;
                       line-height: 1.55; margin: 0; tab-size: 4; }
        .kod-alt { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; background: #1f2124; padding: 6px 12px; font-size: 13px; }
        .kod-alt .ac { color: #fff; font-weight: 600; }
        .kod-alt .kopyala { background: transparent; color: #fff; border: 1px solid #5a5e66; border-radius: 6px;
                            padding: 3px 10px; font: inherit; font-weight: 600; cursor: pointer; }
        .kod-alt .kopyala:hover { background: #3a3d44; }
        .rozet { display: inline-block; background: var(--turuncu); color: #fff; font-size: 11px; font-weight: 700;
                 text-transform: uppercase; letter-spacing: .04em; border-radius: 4px; padding: 1px 7px; line-height: 1.6; }
        .rozet.mini { font-size: 9.5px; padding: 0 5px; vertical-align: middle; }
        .kod-alt .eksik { color: #c9ccd3; }
        .kod-alt .eksik code { color: #ffd9a8; background: transparent; padding: 0; }
        .tablo-kap { overflow-x: auto; margin: 0 0 16px; }
        table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid var(--cizgi); border-radius: 10px; overflow: hidden; }
        th, td { text-align: left; padding: 8px 12px; border-bottom: 1px solid var(--cizgi); vertical-align: top; font-size: 14.5px; }
        tr:last-child td { border-bottom: none; }
        th { background: #fafbfc; font-weight: 600; font-size: 13px; color: var(--soluk); text-transform: uppercase; letter-spacing: .03em; }
        a.calistir code { color: #0b6b86; background: #eaf7fb; border: 1px solid #cfeaf3; }
        a.calistir:hover code { background: #d5eff7; }
        a.calistir { text-decoration: none; white-space: nowrap; }
        .bolum-alt { display: flex; justify-content: space-between; gap: 12px; margin-top: 22px; padding-top: 14px;
                     border-top: 1px solid var(--cizgi); font-size: 14px; }
        footer { text-align: center; color: var(--soluk); font-size: 13px; border-top: 1px solid var(--cizgi); padding-top: 24px; }
        @media (max-width: 860px) {
            /* minmax(0, 1fr), düz 1fr DEĞİL: ızgara öğesinin varsayılan
               min-width:auto değeri sütunu içindeki en uzun kırılamaz şeye
               (uzun bir kod satırı) kadar şişiriyor ve sayfa telefonda yatay
               kayıyordu. Yan sütunda da min-width: 0 aynı gerekçeyle. */
            .sayfa { grid-template-columns: minmax(0, 1fr); padding: 20px 14px 60px; }
            .yan, .sayfa > main { min-width: 0; }
            /* Bölüm listesi telefonda dikey bir kule olarak içeriği çok aşağı
               itiyordu; sarılan etiketler hâline geliyor. */
            .yan { position: static; max-height: none; overflow: visible; padding: 12px; }
            .yan ol { display: flex; flex-wrap: wrap; gap: 5px; }
            .yan li a { padding: 5px 9px; font-size: 13px; background: var(--zemin);
                        border-radius: 6px; line-height: 1.25; }
            section.bolum { padding: 20px 16px; }
            .kahraman { padding: 22px 18px; }
            .kahraman h1 { font-size: 25px; }
            .ust-bar { padding: 10px 14px; }
            .ust-bar .geri { display: none; }
            .bolum-alt { flex-wrap: wrap; }
            /* dar ekranda hücre dolgusunu kıs: iki sütunlu komut tabloları
               kendi içlerinde yatay kaymadan sığsın (kod adları bölünmesin) */
            table { font-size: 13.5px; }
            th, td { padding: 8px 9px; }
            td code, th code { font-size: 12.5px; padding: 1px 4px; }
        }
"""

JS = r"""
    document.addEventListener('click', function (e) {
        var b = e.target.closest('.kopyala');
        if (!b) return;
        var pre = b.closest('.kod').querySelector('pre');
        var bitti = function () { b.textContent = 'Kopyalandı ✓'; setTimeout(function () { b.textContent = 'Kopyala'; }, 1500); };
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(pre.textContent).then(bitti);
        } else {
            var r = document.createRange(); r.selectNodeContents(pre);
            var s = window.getSelection(); s.removeAllRanges(); s.addRange(r);
            try { document.execCommand('copy'); } catch (err) {}
            s.removeAllRanges(); bitti();
        }
    });
"""


def sayfa_html(anahtar, bolumler, twirl):
    bilgi = KILAVUZLAR[anahtar]
    yol = {'skala': '/yardim/skala', 'komutlar': '/yardim/komutlar'} if twirl else \
          {'skala': 'skala.html', 'komutlar': 'komutlar.html'}
    if twirl:
        gezinti = ''.join('<a href="%s"%s>%s</a>' % (h, ' class="secili"' if h == yol[anahtar] else '', ad)
                          for h, ad in GEZINTI)
        logo = '<a href="/"><img src="/assets/images/scalafiddle-logo.png" alt="iKoco"></a>'
        geri = '<a href="/" class="geri">← Düzenleme penceresine dön</a>'
        fontlar = '<link href="https://fonts.googleapis.com/css?family=Raleway:300,400,500,700&display=swap" rel="stylesheet">'
        favicon = '<link rel="shortcut icon" href="/assets/images/favicon.ico">'
    else:
        gezinti = ''.join('<a href="%s"%s>%s</a>' % (
            TABAN_HTML + h if not h.endswith(('skala', 'komutlar')) else yol[h.rsplit('/', 1)[1]],
            ' class="secili"' if h.endswith(anahtar) else '', ad) for h, ad in GEZINTI)
        logo = '<a href="%s"><b style="font-size:18px">iKoco</b></a>' % TABAN_HTML
        geri = '<a href="%s" class="geri">← ikojo.fly.dev</a>' % TABAN_HTML
        fontlar = '<link href="https://fonts.googleapis.com/css?family=Raleway:300,400,500,700&display=swap" rel="stylesheet">'
        favicon = ''
    menu = ''.join('<li><a href="#%s">%s</a></li>' % (k, html.escape(b)) for k, b, _ in bolumler)
    govde = []
    for i, (k, b, ic) in enumerate(bolumler):
        onceki = '<a href="#%s">← %s</a>' % (bolumler[i - 1][0], html.escape(bolumler[i - 1][1])) if i > 0 else '<span></span>'
        sonraki = '<a href="#%s">%s →</a>' % (bolumler[i + 1][0], html.escape(bolumler[i + 1][1])) if i + 1 < len(bolumler) else '<a href="#ust">↑ Başa dön</a>'
        govde.append('<section class="bolum" id="%s"><h2><span class="no">%d</span>%s</h2>\n%s\n'
                     '<div class="bolum-alt">%s%s</div></section>' % (k, i + 1, html.escape(b), ic, onceki, sonraki))
    return """<!DOCTYPE html>
<html lang="tr">
<head>
<meta charset="UTF-8">
<title>iKoco — %(baslik)s</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
%(favicon)s
%(fontlar)s
<style>%(css)s</style>
</head>
<body>
<div class="ust-bar" id="ust">
    %(logo)s
    <nav class="gezinti">%(gezinti)s</nav>
    %(geri)s
</div>
<div class="sayfa">
    <aside class="yan">
        <div class="yan-baslik">Bölümler</div>
        <ol>%(menu)s</ol>
    </aside>
    <main>
        <div class="kahraman">
            <h1><span class="vurgu">%(vurgu)s</span>%(kalan)s</h1>
            <p>%(alt)s</p>
        </div>
%(govde)s
        <footer>Koco · Kojo'nun Türkçesi · Kaynak: masaüstü Koco'nun Türkçe öykü betikleri · Üretim: kojojs-dev/kilavuz/uret.py</footer>
    </main>
</div>
<script>%(js)s</script>
</body>
</html>
""" % dict(baslik=html.escape(bilgi['başlık']), favicon=favicon, fontlar=fontlar, css=CSS, logo=logo, gezinti=gezinti,
           geri=geri, menu=menu, vurgu=html.escape(bilgi['başlık'].split(' ')[0]),
           kalan=html.escape(bilgi['başlık'][len(bilgi['başlık'].split(' ')[0]):]),
           alt=html.escape(bilgi['alt']), govde='\n'.join(govde), js=JS)


def twirl_yap(html_metin, kaynak, komut='python3 kilavuz/uret.py --twirl <editor>/server/src/main/twirl/views'):
    govde = html_metin.replace('@', '@@')
    # Twirl statik metni Scala dizge sabitlerine çevirir; JVM'de bir sabit en çok
    # 65535 bayt olabilir. Twirl'ün yeni sürümleri uzun metni kendisi bölüyor ama
    # buna güvenmeyelim: bölüm ve kod bloğu sınırlarına çıktısı boş bir @("")
    # ifadesi koyarak metni parçalara ayırıyoruz (etiketler arasında; <pre> dışında).
    govde = govde.replace('</section>\n<section', '</section>\n@("")\n<section')
    govde = govde.replace('</pre><div class="kod-alt">', '</pre>@("")<div class="kod-alt">')
    return ('@()\n@* ÜRETİLMİŞ DOSYA -- elle düzenleme. Kaynak: %s, üretim:\n'
            '   %s\n'
            '   İçerikteki her "@" Twirl için "@@" yapılmıştır. *@\n' % (kaynak, komut)) + govde


def uret(anahtar, denetci, twirl_dizin):
    kaynak = os.path.join(BURASI, anahtar)
    dosyalar = sorted(glob.glob(os.path.join(kaynak, '[0-9][0-9]-*.md')))
    if not dosyalar:
        sys.exit('kaynak bulunamadı: %s' % kaynak)
    for twirl in ([False, True] if twirl_dizin else [False]):
        taban = TABAN_TWIRL if twirl else TABAN_HTML
        baglam = {'skala': '/yardim/skala', 'komutlar': '/yardim/komutlar'} if twirl else \
                 {'skala': 'skala.html', 'komutlar': 'komutlar.html'}
        bolumler = []
        for i, d in enumerate(dosyalar, 1):
            if twirl:
                denetci_yerel = Denetci.__new__(Denetci)  # ikinci geçişte sayaçları şişirme
                denetci_yerel.ikojo, denetci_yerel.masa = denetci.ikojo, denetci.masa
                denetci_yerel.sayac, denetci_yerel.nerede = collections.Counter(), collections.defaultdict(set)
                bolumler.append(bolum_html(i, d, denetci_yerel, taban, baglam))
            else:
                bolumler.append(bolum_html(i, d, denetci, taban, baglam))
        metin = sayfa_html(anahtar, bolumler, twirl)
        if twirl:
            hedef = os.path.join(twirl_dizin, KILAVUZLAR[anahtar]['twirl'])
            metin = twirl_yap(metin, 'kojojs-dev/kilavuz/%s/*.md' % anahtar)
        else:
            os.makedirs(os.path.join(BURASI, 'html'), exist_ok=True)
            hedef = os.path.join(BURASI, 'html', anahtar + '.html')
        with open(hedef, 'w', encoding='utf-8') as f:
            f.write(metin)
        print('yazıldı: %s (%d bölüm, %d KB)' % (os.path.relpath(hedef), len(bolumler), len(metin.encode()) // 1024))
    return [b[1] for b in bolumler]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--kojo', help='masaüstü kojo klonu (masaüstü ad listesi için)')
    ap.add_argument('--twirl', metavar='DİZİN', help='kojojs-editor Twirl şablonlarını da bu dizine yaz')
    ap.add_argument('--sadece', choices=list(KILAVUZLAR), help='yalnız bu kılavuzu üret')
    a = ap.parse_args()
    denetci = Denetci(a.kojo)
    if not denetci.masa:
        print('uyarı: masaüstü ad listesi yok (kojo klonu ve önbellek bulunamadı); rozetler yalnız elle işaretlerden', file=sys.stderr)
    for anahtar in KILAVUZLAR:
        if a.sadece and anahtar != a.sadece:
            continue
        basliklar = uret(anahtar, denetci, a.twirl)
        for i, b in enumerate(basliklar, 1):
            print('   %2d. %s' % (i, b))
    rapor = os.path.join(BURASI, 'eksik-adlar.tsv')
    with open(rapor, 'w', encoding='utf-8') as f:
        f.write('# uret.py: kılavuz örneklerinde geçen, ikojo\'da bulunmayan masaüstü adları\n')
        f.write('ad\törnek\tbölümler\tikojo karşılığı\n')
        for ad, n in denetci.sayac.most_common():
            f.write('%s\t%d\t%s\t%s\n' % (ad, n, ' '.join(sorted(denetci.nerede[ad])), KARSILIK.get(ad, '')))
    print('ikojo\'da olmayan ad: %d (%d örnekte); rapor: %s' % (
        len(denetci.sayac), sum(denetci.sayac.values()), os.path.relpath(rapor)))
    for ad, n in denetci.sayac.most_common(12):
        print('   %-36s %3d' % (ad, n))


if __name__ == '__main__':
    main()
