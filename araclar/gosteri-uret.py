#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gosteri-uret.py -- yardimKomutlar sayfasına eklenen kısa gösterilerin KAYNAĞI.

Aşağıdaki G tablosu tek kaynak. Betik iki şey üretir:

  --scala   src/test/scala/kojo/OrnekDerlemeDeneme.scala
            Gösterilerin ikojo API'sine karşı DERLENDİĞİNİ sınar. Bir komut
            adı ya da imzası değişirse `sbt Test/compile` kırılır.
  --html    yardimKomutlar.scala.html'e yapıştırılacak <tr> satırları
            (zrc bağlantısı burada üretiliyor; sarmalayıcı sayfadaki MEVCUT
            bir bağlantıdan alınıyor ki biçim birebir olsun)

Üretilen her zrc gidiş-dönüş sınanır: çözülüp koda eşit mi diye bakılır.

DİKKAT: derleme denetiminde dez/tanım gibi anahtar sözcükler İngilizcelerine
çevrilir, çünkü kojojs-dev STOK Scala.js ile derleniyor -- yamalı (Türkçe
anahtar sözcüklü) derleyici yalnız masaüstünde. Yani anahtar sözcüklerin
sitede çalıştığı buradan doğrulanamaz, yalnız API adları doğrulanır.

Kullanım:
  araclar/gosteri-uret.py --scala
  araclar/gosteri-uret.py --html <kojojs-editor dizini>
"""
import base64
import gzip
import io
import os
import re
import sys

# -*- coding: utf-8 -*-
# ad -> (kısa gösteri kodu, açıklama)
# Hepsi kojojs-dev'e karşı derlenerek sınanıyor (OrnekDerlemeDeneme.scala üretilir).
#
# ikojo'da OLMADIĞI için çıkarılanlar: soluk, bulanık, eksenler, çizVeSakla,
# merkezeTaşı, büyütXY -- bunlar masaüstü Koco'da var, ikojo'da yok.
G = {
# --- dönüştürücüler: çiz(dönüştürücü -> resim) ---
"döndür": ("""silVeSakla
çiz(döndür(30) -> Resim.dikdörtgen(120, 30))""",
 "Resmi saat yönünün tersine verilen açı kadar döndürür."),
"büyüt": ("""silVeSakla
çiz(büyüt(2) -> Resim.daire(25))""",
 "Resmi verilen oranda büyütür. 1'den küçük oran küçültür."),
"götür": ("""silVeSakla
çiz(götür(100, 50) -> Resim.daire(25))""",
 "Resmi verilen kadar öteler (taşır)."),
"kalemRengi": ("""silVeSakla
çiz(kalemRengi(kırmızı) -> Resim.dikdörtgen(120, 60))""",
 "Resmin çizgi rengini kurar."),
"kalemBoyu": ("""silVeSakla
çiz(kalemBoyu(6) * kalemRengi(mavi) -> Resim.dikdörtgen(120, 60))""",
 "Resmin çizgi kalınlığını kurar."),
"saydamlık": ("""silVeSakla
çiz(boyaRengi(mavi) -> Resim.daire(50))
çiz(saydamlık(0.5) * götür(40, 0) * boyaRengi(kırmızı) -> Resim.daire(50))""",
 "Resmi yarı saydam yapar: 0 görünmez, 1 tümüyle donuk."),
"boyaRengi": ("""silVeSakla
çiz(boyaRengi(yeşil) -> Resim.daire(40))""",
 "Resmin içini verilen renkle boyar."),

# --- resim yapıcıları ---
"Resim.dikdörtgen": ("""silVeSakla
çiz(Resim.dikdörtgen(120, 60))""",
 "Verilen en ve boyda bir dikdörtgen resmi."),
"Resim.daire": ("""silVeSakla
çiz(Resim.daire(50))""",
 "Verilen yarıçapta bir çember resmi."),
"Resim.elips": ("""silVeSakla
çiz(Resim.elips(70, 40))""",
 "Verilen yarıçaplarda bir elips resmi."),
"Resim.yatay": ("""silVeSakla
çiz(kalemRengi(mavi) -> Resim.yatay(150))""",
 "Verilen uzunlukta yatay bir çizgi resmi."),
"Resim.dikey": ("""silVeSakla
çiz(kalemRengi(mavi) -> Resim.dikey(150))""",
 "Verilen uzunlukta dikey bir çizgi resmi."),
"Resim.yazı": ("""silVeSakla
çiz(Resim.yazı("Merhaba"))""",
 "Yazıdan bir resim yapar."),

# --- resimleri bir arada ---
"Resim.diziYatay": ("""silVeSakla
çiz(Resim.diziYatay(
  boyaRengi(kırmızı) -> Resim.daire(25),
  boyaRengi(mavi) -> Resim.daire(25)))""",
 "Resimleri yan yana dizer."),
"Resim.diziDikey": ("""silVeSakla
çiz(Resim.diziDikey(
  boyaRengi(kırmızı) -> Resim.daire(25),
  boyaRengi(mavi) -> Resim.daire(25)))""",
 "Resimleri alt alta dizer."),
"Resim.dizi": ("""silVeSakla
çiz(Resim.dizi(
  boyaRengi(kırmızı) -> Resim.daire(40),
  boyaRengi(mavi) -> Resim.daire(20)))""",
 "Resimleri üst üste bindirir."),
"Resim.yatayBoşluk": ("""silVeSakla
çiz(Resim.diziYatay(
  Resim.daire(25), Resim.yatayBoşluk(40), Resim.daire(25)))""",
 "Yan yana dizilen resimlerin arasına boşluk koyar."),
"Resim.dikeyBoşluk": ("""silVeSakla
çiz(Resim.diziDikey(
  Resim.daire(25), Resim.dikeyBoşluk(40), Resim.daire(25)))""",
 "Alt alta dizilen resimlerin arasına boşluk koyar."),

# --- çizilmiş resmi kımıldatmak: önce bir ada koy, çiz, sonra oynat ---
"kaydır": ("""silVeSakla
dez r = boyaRengi(mavi) -> Resim.daire(30)
r.çiz()
r.kaydır(60, 40)""",
 "Çizilmiş bir resmi bulunduğu yerden verilen kadar kaydırır."),
"yansıtX": ("""silVeSakla
dez r = Resim.yazı("Koco")
r.çiz()
r.yansıtX()""",
 "Çizilmiş resmi X ekseninde yansıtır (baş aşağı çevirir)."),
"yansıtY": ("""silVeSakla
dez r = Resim.yazı("Koco")
r.çiz()
r.yansıtY()""",
 "Çizilmiş resmi Y ekseninde yansıtır (aynadaki gibi)."),
"döndürMerkezli": ("""silVeSakla
dez r = boyaRengi(mavi) -> Resim.dikdörtgen(100, 20)
r.çiz()
r.döndürMerkezli(45, 0, 0)""",
 "Çizilmiş resmi verilen nokta çevresinde döndürür."),

# --- tuval ve genel ---
"çizMerkezde": ("""silVeSakla
çizMerkezde(boyaRengi(mavi) -> Resim.daire(40))""",
 "Resmi tuvalin ortasına çizer."),
"çizMerkezdeYazı": ("""silVeSakla
çizMerkezdeYazı("Merhaba Koco", kırmızı, 30)""",
 "Tuvalin ortasına yazı yazar."),
# DİKKAT: burada durakla(1) İŞE YARAMIYOR. çiz ve resimleriSil EŞZAMANLI
# (Picture.draw ve KojoWorld.erasePictures doğrudan çalışıyor), durakla ise
# kuyruğa girip hemen dönüyor (Turtle.sıraya -> commandQ). Betiğin gövdesi tek
# bir eşzamanlı blok olduğu için tarayıcı arada hiç boyama yapamıyor: daire
# çizilip aynı karede siliniyor, çocuk boş tuval görüyor. Silmeyi de
# eşzamansız yola almak gerekiyor -- canlandır tam bunun için var.
"resimleriSil": ("""silVeSakla
çiz(boyaRengi(mavi) -> Resim.daire(40))
den kare = 0
canlandır {
  kare += 1
  eğer (kare == 60) { resimleriSil(); canlandırmayıDurdur() }
}""",
 "Çizilmiş bütün resimleri siler."),

# --- kaplumbağa tarafındaki eksik ---
"uzaklık": ("""dez k2 = yeniKaplumbağa(100, 50)
satıryaz(kaplumbağa.uzaklık(k2))""",
 "İki kaplumbağa arasındaki uzaklığı verir."),
}

SCALA_BASLIK = """package kojo

/**
 * Sitedeki ScalaFiddle sarmalayıcısının aynısı (JSExport olmadan).
 * yardimKomutlar sayfasındaki kısa gösterilerin ikojo API'sine karşı
 * DERLENDİĞİNİ sınar -- yani sayfaya bozuk örnek girmiyor.
 *
 * Üretilmiştir; kaynak: araclar/gosteri-uret.py
 * Anahtar sözcükler burada İngilizce (bkz. o betiğin başlığı).
 */
object OrnekDerlemeDeneme {
  import kojo.KojoWorldImpl
  import kojo.doodle.Color._
  import kojo.Speed._
  import kojo.RepeatCommands._
  import kojo.syntax.Builtins
  implicit val kojoWorld: kojo.KojoWorld = new KojoWorldImpl()
  val builtins = new Builtins()
  import builtins._
  import trTurtle._

"""



ANAHTAR = [('dez ', 'val '), ('den ', 'var '), ('tanım ', 'def '), ('eğer ', 'if '),
           ('yoksa ', 'else '), ('durum ', 'case '), ('için ', 'for '), ('ver ', 'yield ')]
SARMAL = 'server/src/main/twirl/views/yardimKomutlar.scala.html'


def ingilizce(kod):
    for tr, en in ANAHTAR:
        kod = re.sub(r'(?<![A-Za-zÇĞİIÖŞÜçğıöşü])' + tr, en, kod)
    return kod


def scalaYaz(hedef):
    def yontemAdi(ad): return 'g_' + re.sub(r'[^A-Za-z0-9]', '_', ad)
    govde = ['  // %s\n  def %s(): Unit = {\n%s\n  }' % (ad, yontemAdi(ad),
             '\n'.join('    ' + l for l in ingilizce(kod).split('\n')))
             for ad, (kod, _) in G.items()]
    src = SCALA_BASLIK + '\n\n'.join(govde) + '\n}\n'
    io.open(hedef, 'w', encoding='utf-8').write(src)
    print('%s yazıldı (%d gösteri)' % (hedef, len(G)))


def sarmalayici(editorDizini):
    """Biçim birebir olsun diye sarmalayıcıyı MEVCUT bir bağlantıdan alıyoruz."""
    s = io.open(os.path.join(editorDizini, SARMAL), encoding='utf-8').read()
    z = re.findall(r'href="[^"]*?[?&]zrc=([^"&]+)"', s)[0]
    tam = gzip.decompress(base64.b64decode(z.replace('-', '+').replace('_', '/') + '=' * (-len(z) % 4))).decode('utf-8')
    i = tam.index('// $FiddleStart') + len('// $FiddleStart')
    j = tam.index('// $FiddleEnd')
    return tam[:i], tam[j:]


def zrcYap(on, arka, kod):
    kaynak = on + '\n' + kod + '\n\n  ' + arka
    ham = gzip.compress(kaynak.encode('utf-8'), mtime=0)
    # DOLGU KIRPILMIYOR: sayfadaki mevcut bağlantıların hepsi '=' dolgusunu
    # taşıyor (len % 4 == 0). Sunucu kırpılmışını da çözüyor (marklister'ın
    # base64Url'ü strictPadding=false), ama biçimi ayırmak için bir sebep yok
    # -- "olduğu gibi taşınıyor" sözü bağlantı biçimi için de geçerli olsun.
    return base64.b64encode(ham).decode('ascii').replace('+', '-').replace('/', '_')


def coz(z):
    t = gzip.decompress(base64.b64decode(z.replace('-', '+').replace('_', '/') + '=' * (-len(z) % 4))).decode('utf-8')
    a = t.index('// $FiddleStart') + len('// $FiddleStart')
    return t[a:t.index('// $FiddleEnd')].strip()


def htmlYaz(editorDizini):
    on, arka = sarmalayici(editorDizini)
    def esc(t): return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    for ad, (kod, aciklama) in G.items():
        z = zrcYap(on, arka, kod)
        if coz(z) != kod.strip():                     # gidiş-dönüş şart
            sys.exit('GİDİŞ-DÖNÜŞ HATASI: ' + ad)
        # İKİ sütun: bu bölümde tablonun altında <pre> gösterisi yok, gösteri
        # doğrudan komut adına bağlı. Üçüncü ("Örnekler") sütunu 25 satırın
        # 25'inde de boş kalıyordu, yalnız yer kaplıyordu.
        print('<tr><td><a class="calistir" href="/?zrc=%s" target="_blank" rel="noopener" '
              'title="Editörde aç"><code>%s</code></a></td><td>%s</td></tr>'
              % (z, esc(ad), esc(aciklama)))


if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == '--scala':
        kok = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
        scalaYaz(os.path.join(kok, 'src/test/scala/kojo/OrnekDerlemeDeneme.scala'))
    elif len(sys.argv) > 2 and sys.argv[1] == '--html':
        htmlYaz(sys.argv[2])
    else:
        sys.exit(__doc__)
