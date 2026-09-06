# Çizim, boyama ve tuval komutları
<!-- hücreler: çalıştır -->

Bazı komutlar kaplumbağanın nasıl çizim ve boyama yapacağını belirliyor. Örneğin:

| Komut | Açıklama | Örnekler |
|---|---|---|
| `biçimleriBelleğeYaz` | Kurulu biçim ayarlarını belleğe yaz: kalem rengi, kalınlığı ve yazısı, inik mi, kalkık mı ve boyama rengi | (aşağıdaki ilk örnek) |
| `biçimleriGeriYükle` | Bir önceki komutla kaydedilen biçim ayarlarını geri yükler | |
| `kalemKalınlığınıKur(5)` | Başlangıçta 2 kalınlığıyla çizer. | (ikinci örnek) |
| `kalemRenginiKur(mavi)` | Kırmızı yerine mavi kalemle çiz | `boyamaRenginiKur(yeşil); üçgen()` |
| `boyamaRenginiKur(mavi)` | Şekilleri maviye boya. Kenarların rengi kalemRenginiKur komutuyla değişir. | (üçüncü örnek ve devamı) |

```scala
silVeSakla
yaklaş(3)
ileri; sağ
biçimleriBelleğeYaz()
kalemKalınlığınıKur(10)
kalemRenginiKur(mavi)
ileri; sağ
biçimleriGeriYükle()
ileri; sağ
```

```scala
sil()
kalemKalınlığınıKur(10)
üçgen()
kalemKalınlığınıKur(1)
ileri
üçgen()
```

```scala
silVeSakla
boyamaRenginiKur(mavi)
kare(40)
ileri(40)
boyamaRenginiKur(yeşil)
kare(40)
```

Türkçe'ye çevirdiğimiz temel renkler doğrudan adlarıyla kullanılabilir; beyazla renksiz'in farkına dikkat!

```scala
silVeSakla
hızıKur(hızlı)
yaklaş(0.5, 240, 160)
artalanıKur(Renk(107, 4, 189))
kalemRenginiKur(renksiz)
dez renkDizisi = Dizi(
  sarı, yeşil, pembe, kahverengi,
  siyah, gri, koyuGri, açıkGri,
  renksiz, beyaz, kırmızı, turuncu,
  mavi, mor, morumsu, camgöbeği
)
//beyazla renksiz'in farkına dikkat!

yineleDizinli(renkDizisi.boyu) { i =>
  boyamaRenginiKur(renkDizisi(i-1))
  kare(100); ileri(100)
  eğer(i % 4 == 0)
     konumuKur(100 * i / 4, 0)
}
```

Türkçe'ye çevirdiğimiz renklerin hepsini `Renkler` altında topladık. Henüz Türkçeleştiremediğimiz renkler de var; onlar İngilizce adlarıyla `renkler` altında:

```scala
yineleİçin(Dizin(
    Renkler.açıkAltınbaşakSarısı,
    Renkler.koyuDenizYeşili,
    Renkler.gökMavisi,
    Renkler.haki)
) { renk =>
    boyamaRenginiKur(renk)
    kare(100); ileri(100)
}
konumuKur(500, 0)
// Henüz Türkçeleştiremediğimiz
// renkler de var.
// İşte birkaç örnek:
yineleİçin(Dizin(
    renkler.aliceBlue,
    renkler.hotpink,
    renkler.darkTurquoise,
    renkler.yellowGreen)
) { renk =>
    boyamaRenginiKur(renk)
    kare(100); ileri(100)
}
```

Masaüstü düzenleyicisinde diğer renkleri görmek istersen boş bir satıra `Renkler.` yazdıktan sonra Kontrol tuşunu basılı tutup boşluk tuşuna basıver; adı a harfiyle başlayan renkleri bulmak için `Renkler.a` yazıp Kontrol-Boşluk. İngilizce renkler de `renkler.` altında. Tam liste için [Koco Sözlüğü](/yardim/sozluk)'ne bak.

## Tuvalle ilgili komutlar

| Komut | Açıklama | Örnekler |
|---|---|---|
| `canlandırmaHızınıKur(10)` | 100 adımı atması 10 milisaniye alsın. Başlangıçta 1000 milisaniye aldığı için 100 kat hızlanır! | |
| `eksenleriGizle()` | X ve Y eksenlerini saklar | |
| `eksenleriGöster()` | X ve Y eksenlerini gösterir | |
| `ızgarayıGizle()` | Izgarayı gizler. Bir başka adı da grid. | `gridiGizle()` |
| `ızgarayıGöster()` | Izgarayı çizer | `gridiGöster()` |
| `hızıKur(hızlı)` | canlandırmaHızınıKur komutunun basit hali | (aşağıdaki hız örneği) |
| `ışınlarıAç` | Dört yöne ışın tut. Öne doğru olan uzun olsun | |
| `ışınlarıKapat` | Işınları söndür | |
| `tuvaleYaz("merhaba dünya!")` | Yazı komutunun uzun adı. Oradaki örneğe de bak | `yazı(Dizi(1, 2, 3).yazıya)` |
| `tuvaliKaydır(100, 50)` | Tuvali kaydırır | `üçgen(); tuvaliKaydır(-100, -50)` |
| `tuvaliDöndür(45)` | Tuvali döndürür | `üçgen(); tuvaliDöndür(-45)` |
| `yaklaş(3.0)` | Tuvale verilen oranda yaklaşarak çizimleri daha büyük göster. ikojo'da: `yaklaşXY(3, 3, 0, 0)` | `sil(); üçgen(); yaklaş(2, 100, 50)` `yaklaş(0.4)` |
| `yaklaş(2.0, 200, 50)` | x=200, y=50 konumunu merkez alarak yaklaş ya da uzaklaş | `yaklaşmayıSil` `yaklaş(1.0, 0, 0)` |
| `yaklaşmayaİzinVerme()` | Fareyle yaklaşıp uzaklaşmayı kapatır | |
| `yaklaşmayıSil()` | Yaklaşmayı sıfırlar | |
| `yazı(Aralık(1, 200, 7).yazıya)` | tuvaleYaz komutunun kısa adı. Durduğu konumun hemen sağına verilen yazıyı yazar. Masaüstünde girdi yazı değilse yazıya çevrilir; ikojo'da `.yazıya` gerekir. | (aşağıdaki yazıyüzü örnekleri) |
| `yazıBoyunuKur(24)` | Başlangıçta 18 boyunda yazar. | (aşağıdaki yazı boyu örneği) |
| `yazıyüzleri` | Sistemdeki yazıyüzlerinin listesi | |
| `yazıYüzünüKur(yazıyüzü("Times New Roman", 36))` | Kaplumbağanın yazısının görünüşünü değiştirir | |

Hız örneği:

```scala
sil
konumuKur(100, 0)
hızıKur(yavaş)
daire(); durakla(0.5)
hızıKur(orta)
daire(50); durakla(0.5)
hızıKur(hızlı)
daire(100); durakla(0.5)
hızıKur(çokHızlı)
yinele(50) {
    daire(200)
    kalemRenginiKur(rastgeleRenk)
    konumuDeğiştir(3, 0)
}
```

Yazı boyu örneği:

```scala
silVeSakla
eksenleriGöster
ızgarayıGöster
zıpla(4)
yazı("1234")
sol; zıpla(50); sağ
// biraz küçültelim:
yazıBoyunuKur(10)
yazı("1234")
yaklaş(4.0, 0, 0)
```

Yazıyüzleri (yalnız masaüstünde; tarayıcıda sistem yazıyüzlerine erişim yok):

```scala
silVeSakla()
kalemRenginiKur(koyuGri)
canlandırmaHızınıKur(10)
kalemiKaldır()
noktayaGit(-300, -200)
açıyaDön(90)
kalemiİndir()
yazı(s"${yazıyüzleri.boyu} tane yazıyüzü var")
yazıyüzleri.ikileSırayla.herbiriİçin {
    durum(adı, sırası) =>
        zıpla()
        yazıYüzünüKur(yazıyüzü(adı, 18))
        yazı(f"$sırası%-3d: \t $adı")
}
yazıyüzleri.ikileSırayla.herbiriİçin(satıryaz)
satıryaz("Kaplumbağa bütün yazıyüzlerini" ++
    " yazabilmek için\nkuzeye" ++
    s" doğru epey yol aldı: y=${konum.y}")
```
<!-- masaüstü -->

```scala
// buna her tıklayışında
// tuval bir sayfa yukarı kayar
tuvaliKaydır(0, tuvalAlanı.boyu)
//bu da aşağı dönmek için:
// tuvaliKaydır(0, -tuvalAlanı.boyu)
```

```scala
dez yy = yazıyüzleri(127)
satıryaz(s"Sistemde ${yazıyüzleri.boyu} tane yazı yüzü var. Birinin adı: $yy.")
satıryaz("Onunla tuvale birşeyler yazalım:")
silVeSakla
kalemRenginiKur(mor)
yazıYüzünüKur(yazıyüzü(yy,18))
yazı("Yazma denemesi yapalım...")
```
<!-- masaüstü -->

## Bilgi veren komutlar

Bu komutların değerini masaüstünde çıktı gözü kendiliğinden gösterir; tarayıcıda `satıryaz` içine al.

| Komut | Açıklama | Örnekler |
|---|---|---|
| `satıryaz(canlandırmaHızı)` | Kaplumbağanın 100 adım atması kaç milisaniye alır? Onu bildir. Başlangıçta 1000 milisaniye sürer. Örnekte de gördüğümüz gibi hızlandırılabilir. | (aşağıdaki süre örneği) |
| `satıryaz(doğrultu)` | Şu anda baktığımız doğrultuyu açı olarak çıktıyla bildirir. 180 sola bakıyor demek. 270 de aşağıya | |
| `satıryaz(kalemİnikMi)` | Bu bilgi programlama yaparken faydalı olabilir | `kalemiİndir` `kalemiKaldır` |
| `satıryaz(konum)` | Şu anki konumu bildir | |

```scala
sil
canlandırmaHızınıKur(100)
kalemKalınlığınıKur(10)
boyamaRenginiKur(mavi)
den say = 1
dez başlangıçAnı = buAn
yinele(8) {
    ileri(100)
    eğer(say%4 == 0) ileri(100)
    yoksa sağ
    say = say + 1
}
yinele(2) {
    sol
    ileri(100)
}
dez süre = yuvarla(
  (buAn - başlangıçAnı) / 1000.0,
1)
satıryaz(s"Çizim $süre saniye sürdü")
```

ikojo'da `buAn` yerine `buSaniye` var (saniye cinsinden; `/ 1000.0` bölmesini kaldır).

## Kaplumbağanın görünüşünü değiştiren komutlar

Bu komutlar (giysiler, `Görünüş`) yalnız masaüstü Koco'da var.

| Komut | Açıklama | Örnekler |
|---|---|---|
| `birsonrakiGiysi` | Kurulu giysilerden bir sonrakini giy. Sonuncuyu giymişse en baştakine döner. Tek giysi varsa hiç birşey yapmaz | (aşağıdaki ilk giysi örneği) |
| `giysiKur(Görünüş.araba)` | Kaplumbağamızı tamamen değiştirmek de mümkün. | |
| `giysileriKur(Görünüş.yarasa1, Görünüş.yarasa2)` | Kaplumbağanın bir dizi giysisi olsun ki giysisini kolaylıkla değiştirebilelim. | (ikinci giysi örneği) |
| `giysiyiBüyült(2.0)` | Küçültmek için de 0.5 dene. | |
| `görünmez` | gizle komutuyla eş: kaplumbağayı gizler | |
| `görünür` | göster komutuyla eş: gizliyse, kaplumbağayı gösterir | |

```scala
sil
durakla(0.5)
giysileriKur(Görünüş.araba,
  Görünüş.yarasa1,
  Görünüş.yarasa2)
durakla(0.5)
birsonrakiGiysi()
durakla(0.5)
birsonrakiGiysi()
durakla(0.5)
birsonrakiGiysi()
durakla(0.5)
```

```scala
silVeSakla
kalemRenginiKur(renksiz)
göster
giysileriKur(
  Görünüş.yarasa1,
  Görünüş.yarasa2)
yinele(20) {
    durakla(0.1)
    atla(konum.x + 10,
         konum.y + 5)
    birsonrakiGiysi()
}
```

## Birden çok kaplumbağa

Kaplumbağamız bir yazılım nesnesi olduğu için komutlarını bir nesnenin yöntemi olarak da çağırabiliriz. Ama hangi nesne? Aşağıdaki ilk satıra bak.

Scala ve Kojo'daki herşey bir yazılım nesnesi ve bunlara bizim kaplumbağamız da dahil! Adı aşağıda. Hatta bir kaplumbağa ile yetinmek zorunda değiliz. Birkaç tane kaplumbağa olsa daha da çok çizim yapabilir, hatta birbirleriyle etkileşime sokabiliriz. Yeni kaplumbağalar oluşturmak ve onlara çizim yaptırmak için aşağıdaki komutlara bakıver (masaüstünde Örnekler menüsünün Çoğul Kaplumbağa adlı alt menüsündeki örnek yazılımcıklara bakmayı da unutma!).

| Komut | Açıklama | Örnekler |
|---|---|---|
| `kaplumbağa` | Kaplumbağamızın nesne adı bu. | `kaplumbağa.ileri(100)` `kaplumbağa.ev` |
| `yeniKaplumbağa(100, 100)` | x=100 y=100 noktasında yeni bir kaplumbağa canlandırır. | `dez k = yeniKaplumbağa(50, 50)` `k.ileri(50)` |

```scala
sil
dez yk2 = yeniKaplumbağa(0, 100)
yk2.giysiKur(Görünüş.araba)
yk2.geri(180)
// iki kat hızlandıralım:
yk2.canlandırmaHızınıKur(500)
yk2.sağ()
yk2.ileri(300)
// ilki hala yavaş
sağ; ileri(300)
```
