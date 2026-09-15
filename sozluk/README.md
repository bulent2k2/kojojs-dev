# Koco Sözlükleri

## Dosyalar
- `koco-sozlugu.html` — Koco (Türkçe) terim sözlüğü, aranabilir
- `kojo-sozlukleri.html` — dokuz dilli Kojo sözlük dizini (Koco dahil)

## Canlı (Claude artifact)
- Koco Sözlüğü: <https://claude.ai/code/artifact/c0d067a5-4df9-4761-b7ba-19ac95304372>
- Kojo Sözlükleri (dizin): <https://claude.ai/code/artifact/10a6030f-b400-43b0-928c-14d6b925058d>

DİKKAT: artifact'i yeniden yayımlamak paylaşılan bağlantıyı KENDİLİĞİNDEN
güncellemiyor. Bağlantıyı açanlar, paylaşım iğnesi (share pin) yeni sürüme
taşınana dek eski sürümü görmeye devam ediyor; iğne artifact sayfasının
paylaşım menüsünden taşınıyor. Repodaki dosya ve /yardim/sozluk bundan
etkilenmiyor, onlar hemen güncel oluyor.

Bu iki HTML, o artifact'lerin repoya alınmış kopyalarıdır (frame-runtime çıkarılıp
tek başına açılabilir hâle getirildi).

---


`koco-sozlugu.html` — masaüstü Koco'nun (bulent2k2/kojo) Türkçe↔İngilizce
programlama terim sözlüğü, aranabilir tek dosyalık HTML. İki yönlü, aksan
katlamalı (`cizim` → `çizim`), kategorilere ayrılmış (anahtar kelimeler, türler,
kaplumbağa/tuval komutları, resimler, matematik, koleksiyonlar…). 980 girdi.

## Yardım panelleri ve çapalar (Eylül 2026)

Yardım metni OLAN girdiler (980 satırın 273'ü) tıklanınca açılıyor: imza,
açıklama ve SINANMIŞ örnek. Her satırın bir çapası var, yani doğrudan
bağlanabiliyor:

    https://ikojo.fly.dev/yardim/sozluk#katla

Bu içerik ELLE YAZILMIYOR, masaüstü Koco'dan üretiliyor. Kaynak tek:
`bulent2k2/kojo` deposundaki `lite/i18n/tr/help.scala`. Oradaki örnekler
KoleksiyonYardımıTest ve YardımÖrnekleriTest ile doğrulanıyor.

Ama o testler MASAÜSTÜ API'sine karşı koşuyor, sözlük ise ikojo'da sunuluyor.
İki API %100 örtüşmediği sürece "Örnek sınanıyor" sözü burada da tutulmalı:

    araclar/yardim-derleme-uret.py

`yardim.json`'daki 190 örneği ikojo API'sine karşı derleyen
`src/test/scala/kojo/YardimOrnekDerlemeDeneme.scala` dosyasını üretiyor.
İlk koşuşunda 3 örnek yakalandı (Eşlek.anahtarlar/değerler için Yinelenebilir
sarmalayıcısı ve Yineleyici turunun eksik kalan `bellekli.başı` yöntemi);
ikisi de eklendi. yardim.json her tazelendiğinde bu betik de koşturulmalı.

Yenilemek için (kojo deposunda):

    ./sbt.sh 'Test/runMain net.kogics.kojo.araclar.YardımDışaAktar \
              <kojojs-dev>/sozluk/yardim.json'

sonra koco-sozlugu.html içindeki `const YARDIM = {...}` bloğunu bu dosyayla
değiştirin (tek dosya kalsın diye gömülü: sayfa hem iframe'de hem file://
ile açılıyor, fetch çalışmazdı).

### `türler` alanı

Her yöntem girdisinde yöntemin hangi türlerde bulunduğunu söyleyen bir
`türler` alanı var; panelde son satır olarak görünüyor:

| alan | panelde görünen |
|---|---|
| `""` (boş) | Dizi, Dizin, Yöney, Küme, Kuyruk gibi topluluklarda aynı biçimde çalışır. |
| `"EsnekYazı, Yazı"` | Şu türlerde var: EsnekYazı, Yazı. |

Boş olması "ortak dizi çekirdeği" demek, yani beşinin hepsinde var — bilgi
eksikliği değil. Bugün 190 girdinin 77'si boş, 113'ü dolu.

Bu alan ELLE TUTULMUYOR: masaüstünde `help.scala`'daki tablodan geliyor ve
orada `KoleksiyonYardımıTest.türListesiKaynakla_uyuşuyor` listeyi
`lite/i18n/tr/*.scala` kaynağından yeniden türetip karşılaştırıyor. Buradaki
sayfa yalnız gösteriyor.

`content`'te birleşen iki girdide (`sil`, `yazı`) alan yok sayılıyor: onların
gömülü `html`'i cümleyi zaten taşıyor, ikinci kez basmak yineleme olurdu.

## Çalışan örnek bağlantıları

Panellerde "Bunu kullanan çalışan örnekler" bölümü var: komutu gerçekten
kullanan, sitede ÇALIŞAN betiklere `/?zrc=...` bağlantısı. 980 addan 317'sinde
en az bir örnek var (599 bağlantı, 373 benzersiz betik).

Kaynak: kojojs-editor'daki yardım sayfalarının kendi "çalıştır" bağlantıları
(kojoOgren, yardimKomutlar, yardimSkala, benzetim -- toplam 595 betik).
`zrc` dizgeleri OLDUĞU GİBİ taşınıyor, yeniden sıkıştırma yok; yani bozuk
bağlantı üretme riski yok.

    araclar/ornek-dizini.py <kojojs-editor dizini>

12 satırdan uzun betikler ATLANIYOR: onlar adı GÖSTERMİYOR, içinde geçiyor
sadece. Her ad için en çok 3 örnek, kısadan uzuna.

ornekler.json gömülmüyor, ayrı duruyor (182 KB) ve sayfa açılınca çekiliyor.
Sebep: bağlantılar zaten yalnız site ayaktayken anlamlı. Dosya gelmezse o
bölüm görünmüyor, sözlüğün gerisi çalışmaya devam ediyor. DİKKAT: sunulan
kopyaya koco-sozlugu.html ile BİRLİKTE ornekler.json de kopyalanmalı.

## Kapsam denetimi

Sayfa kaynağa bağlı DEĞİL: bir kez üretilip elle düzeltildi, yeni Türkçe adlar
girdiğinde sessizce eskiyor. Farkı ölçen araç:

    araclar/sozluk-kapsam.py --kojo <kojo klonu>

Masaüstünün üretilmiş çeviri sözlüğüyle (`ceviri-sozlugu.tsv` + elle
`ceviri-kurallar.tsv`) karşılaştırıp üç liste veriyor: sayfada olmayan adlar
(kaynak dosyaya göre öbeklenmiş), sayfanın başka bir yazım biçimiyle örtük
kapsadıkları, ve çelişen çiftler. Rapor, kapı değil; hangi adın sayfaya
gireceğine küratör karar verir. Ayrıntı: `../araclar/README.md`.

## Kaynak
Şu dosyalardan derlendi (bulent2k2/kojo):
- `lite/i18n/tr/dict.scala` — kavram çevirileri
- `lite/i18n/trInit.scala`, `lite/i18n/tr/*.scala` — API adları
- `l10n-level2/level2_tr.properties` — arayüz
- `samples/tr/` — örneklerdeki eşleşmeler

Bu ikojo katmanı (kojojs-dev/src/main/scala/kojo/tr/) için eklenen yeni Türkçe
terimler de sözlüğe işlendi (koyu renkler, Dönüştürücü, renkliYazı, birleştirici
aliaslar, oyun API'leri, bölünüyorMu/belirt/buSaniye, Yığın koy/al/tane…).

Eylül 2026 turu: standart kütüphane sarmalayıcılarının kapsamı %41-50'den
masaüstünde %95'e, ikojo'da %94'e çıkarıldı; oradan gelen 201 yeni ad sözlüğe
işlendi (754 -> 980). Yeni "Koleksiyon Türleri" kategorisi Yığın/Kuyruk/
ÖncelikSırası/Eşlem/Eşlek/Küme/Aralık/Belki/MiskinDizin'e özgü adları topluyor;
ortak çekirdek "Diziler ve Yazılar" altında. Adlar `lite/i18n/tr/*.scala`
gövdelerinden üretildi (`def başıBelki ... = d.headOption` -> headOption =
başıBelki), sonra elle düzeltildi.

Eylül 2026, ikinci tur (araç destekli ilk tur): `sozluk-kapsam.py` 453 ad eksik
saydı; aracın üç yazım biçimini (nitelenmiş ad, "alt:/eski adı:/takma ad:" notu,
imzalı hücre) tanıması bunun 49'unun sahte olduğunu gösterdi, kalan gerçek kuyruk
404. Turda 211 satır eklendi, 21 satırın notuna takma ad işlendi (1022 -> 1235
satır); kuyruk **404 -> 147**'ye indi.

Eklenenler, kaynak dosyaya göre: `resim.scala`'nın Resim sınıfı yöntemleri ve
Resim nesnesi yapıcıları (en büyük gerçek boşluktu), `geo.scala` yol/şekil
noktaları, `tuvalcizim.scala` (TuvalÇizim/CanvasDraw), `cinidunyasi.scala` çini
dünyası, `kumanda.scala` oyun kumandası, `dosya.scala`, `arayuz.scala`'nın
`ay.*` katmanı (Arayüz kategorisi 17 satırdan 49'a), `klavye.scala` tuş kodları
(`tuşlar.*`), `Instrument.scala` çalgıları, İngilizce karşılığı olan giysiler,
`yoney2b.scala`/`matematik.scala`, ve öğrencinin hata iletisinde göreceği
`turler.scala` hata türleri.

Bilerek DIŞARIDA kalan 147: `turler.scala`'nın Java interop takma adları ve
kutulama dönüştürücüleri (54), `cizim.scala`/`ses.scala`'nın İngilizce karşılığı
olmayan ham imge/ses yolları (45), `buan.scala`'nın takvim katmanı (13, ayrı bir
tur), `klavye.scala`'nın eskitilmiş alt_çizgili yazımları (8), iç adlar
(`richBuiltins`, `Col`, `Iter`, `a_kalıp`, `codeTemplates`, `helpContent`,
`log2_e`, `PNokta`/`pNokta`/`tNokta`), ve tek bir Kojo adına karşılık gelmeyen
bileşikler (`çarpışma`, `çarpışmalar`).

Eylül 2026, üçüncü tur (ters yön): araç şimdiye dek yalnız "sözlükte var, sayfada
yok" yönünü ölçüyordu. Öteki kova (`sayfada olup sözlükte olmayan`) hiç küratör
görmemişti ve `renkliYazı` hatası tam oradaydı. Ölçüt simetrik yapıldı (351 -> 275,
aradaki 76'sı yazım artefaktı: 72 niteleme + 4 imza) ve kalan API adları ayıklandı. Altı ad **yalnız
ikojo'da** çıktı, notlarına işlendi: `fareBasılınca` (masaüstünde `fareyeBasınca`),
`kur`, `bölünüyorMu`, `belirgin`, `resimleriSil` (masaüstünde `Resim.sil`),
`Dönüştürücü`. `ay.*` arayüz katmanının tamamı ters yönde: masaüstüne özgü,
tarayıcıda yok — kategori notuna bir kez yazıldı.

Aynı turda ikinci turun üç hatası da düzeltildi: `Resim.sil` NESNENİN yöntemi
(`erasePictures`), sınıfın `r.sil()`'i `erase`; `noktaIşık`/`sahneIşığı` için
yazdığım `PointLightEffect` var ama BİRLEŞTİRİLEMEZ, doğrusu `picture.pointLight`
(bkz. kojo#68, üreteç zincir kaçağı).

Eylül 2026, dördüncü tur (yansıt): `#68`/`#90` birleşince kapsam aracını yeni
sözlükle koşturunca `ayrı` çelişki 37'den 29'a düşmüştü; kalan iki taneden biri
gerçek bir sayfa hatası çıktı. `["flip","yansıt"]` satırı **üç ayrı işlemi** tek
satıra sıkıştırıyordu:

    yansıtY   = flipY   (Kojo'da flip ve flipAroundY de aynı)   resmi Y ekseninde çevirir
    yansıtX   = flipX   (flipAroundX de aynı)                    X ekseninde çevirir
    yansıt(n) = reflect(n)                                       resmin YANINA aynalı kopya koyar
    Yöney2B.yansıt(y) = bounceOff(y)                             yöneyi yüzeyden sektirir

Sayfa `flip`i `yansıt`a bağlıyordu, oysa `yansıt(n)` bambaşka bir şey yapıyor
(`HPics(pic, trans(n,0)(FlipY(copy)))` -- çevirmiyor, aynalı ÇİFT üretiyor).
Dördü de ayrı satır oldu. `reflect` yalnız masaüstünde; `Yöney2B.yansıt` ikisinde
de var. Çevirmen zaten doğru ayırıyordu (alıcı bağlamına göre `y1.yansıt` ->
`bounceOff`, yalın `yansıt(120)` -> `reflect`) -- yanlış olan yalnız sayfaydı.

Canlı (Claude artifact): sözlük ve dokuz dilli dizin `/yardim`'den de bağlı.

## ikojo'da: `/yardim/sozluk`

`koco-sozlugu.html` ikojo'da da sunuluyor: <https://ikojo.fly.dev/yardim/sozluk>.
kojojs-editor bu dosyanın bir kopyasını `server/src/main/assets/sozluk/koco-sozlugu.html`
olarak (statik varlık, `/assets/sozluk/koco-sozlugu.html`) taşır; `yardimSozluk.scala.html`
şablonu onu yardım sayfalarının gezinti şeridi altında bir iframe içinde açar.
Buradaki dosya değişince o kopyayı da güncelleyin (`cp sozluk/koco-sozlugu.html
<kojojs-editor>/server/src/main/assets/sozluk/`). Kılavuzlar (`/yardim/skala`,
`/yardim/komutlar`) için bkz. `../kilavuz/README.md`.
