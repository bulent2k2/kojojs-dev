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
kaplumbağa/tuval komutları, resimler, matematik, koleksiyonlar…). 975 girdi.

## Yardım panelleri ve çapalar (Eylül 2026)

Yardım metni OLAN girdiler (975 satırın 273'ü) tıklanınca açılıyor: imza,
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

## Çalışan örnek bağlantıları

Panellerde "Bunu kullanan çalışan örnekler" bölümü var: komutu gerçekten
kullanan, sitede ÇALIŞAN betiklere `/?zrc=...` bağlantısı. 975 addan 317'sinde
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
işlendi (754 -> 975). Yeni "Koleksiyon Türleri" kategorisi Yığın/Kuyruk/
ÖncelikSırası/Eşlem/Eşlek/Küme/Aralık/Belki/MiskinDizin'e özgü adları topluyor;
ortak çekirdek "Diziler ve Yazılar" altında. Adlar `lite/i18n/tr/*.scala`
gövdelerinden üretildi (`def başıBelki ... = d.headOption` -> headOption =
başıBelki), sonra elle düzeltildi.

Canlı (Claude artifact): sözlük ve dokuz dilli dizin `/yardim`'den de bağlı.

## ikojo'da: `/yardim/sozluk`

`koco-sozlugu.html` ikojo'da da sunuluyor: <https://ikojo.fly.dev/yardim/sozluk>.
kojojs-editor bu dosyanın bir kopyasını `server/src/main/assets/sozluk/koco-sozlugu.html`
olarak (statik varlık, `/assets/sozluk/koco-sozlugu.html`) taşır; `yardimSozluk.scala.html`
şablonu onu yardım sayfalarının gezinti şeridi altında bir iframe içinde açar.
Buradaki dosya değişince o kopyayı da güncelleyin (`cp sozluk/koco-sozlugu.html
<kojojs-editor>/server/src/main/assets/sozluk/`). Kılavuzlar (`/yardim/skala`,
`/yardim/komutlar`) için bkz. `../kilavuz/README.md`.
