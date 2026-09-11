# Kaplumbağanın anladığı komutlar
<!-- hücreler: çalıştır -->

Kaplumbağamız yürürken çizgi çizip, boyama yapar. Onu yürüten ve yürürken çizim yaptıran komutları aşağıda A'dan Z'ye sıraladık. Her komut için kısa bir açıklama ve örnekler verdik. Tablodaki komutlara tıklayınca düzenleyicide açılırlar. Ama önce şu komutları bilmekte fayda var:

| Komut | Açıklama | Örnekler |
|---|---|---|
| `gizle` | Kaplumbağayı gizle. Kaplumbağacık görünmese de çizim yapmaya devam eder. | (aşağıdaki ilk örnek) |
| `göster` | Kaplumbağayı göster | |
| `kalemiİndir` | Kalemi indir ve çizim yapmaya devam et. Örnek aşağıda. | |
| `kalemiKaldır` | Kalemi kaldır ki bundan sonra hareket ederken çizim yapma. | (aşağıdaki ikinci örnek) |
| `sil` | Tuvali temizler ve kaplumbağayı başlangıç noktasına döndürür | |
| `silipSakla` | Tuvali temizler, kaplumbağayı başlangıç noktasına döndürür ve gizler. Diğer adı 'silVeSakla' (ikojo'da bu ad var) | `silVeSakla` |

```scala
yinele(3) {
    gizle()
    ileri(100)
    göster()
    dön(120)
    ileri(100)
}
```

```scala
ileri
kalemiKaldır()
ileri
kalemiİndir()
ileri
```

Başlangıçta kalem inik. Onun için yürürken çizim yapacak. Üstteki ve alttaki komutları istediğin sırada çalıştırabilirsin. Tıklaman yeter.

| Komut | Açıklama | Örnekler |
|---|---|---|
| `dön(120, 100)` | Yarıçapı 100 olan bir yay çizerek saat yönünün tersine 120 derece döner | |
| `ev` | Eve yani x=0, y=0 noktasına dön ve doğrultuyu kuzeye çevir | `eksenleriGöster` `eksenleriGizle` |
| `geri(50)` | 50 adım geriye gider | `geri` `ileri(20)` |
| `ileri(100)` | 100 adım ilerler | `ileri` `geri(20)` |
| `ilerle(-200, -100)` | Tuvalde x=-200, y=-100 noktasına ilerle | `ızgarayıGöster` `ızgarayıGizle` |
| `sağ(60, 100)` | Sağa doğru 60 derecelik bir yay çiz. Yarıçapı 100 uzunluğunda olan bir çemberin yayı olsun | |
| `sol(120, 50)` | Sola doğru 120 derecelik bir yay çiz. Yarıçapı 50 uzunluğunda olan bir çemberin yayı olsun | |
| `yay(100, -135)` | dön komutu gibi ama önce yarıçapı giriyoruz sonra yayın açısını | |

Yönünü belirleyen ve yerini değiştiren ama çizim yapmayan komutlar (A'dan Z'ye sıralı):

| Komut | Açıklama | Örnekler |
|---|---|---|
| `atla(200, 100)` | Çizim yapmadan x=200, y=100 konumuna atla | `atla(0, 0)` `atla(-100, -200)` |
| `açıyaDön(210)` | Doğrultunu 210 açısına çevir | `kuzey` `açıyaDön(-45)` |
| `batı` | Doğrultunu batıya çevir. Benzer komutlar yanda | `doğu` `güney` `kuzey` |
| `doğu` | Doğrultunu doğuya çevir. Benzer komutlar yanda | `batı` `güney` `kuzey` |
| `dön(30)` | Saat yönünün tersine doğru 30 derece döner. Eksi girersen saat yönünde döner | |
| `güney` | Doğrultunu güneye çevir. Benzer komutlar yanda | `batı` `doğu` `kuzey` |
| `konumuDeğiştir(40, 30)` | Doğrultuyu değiştirmeden 40 sağa, 30 yukarı git | `konumuDeğiştir(-50, -50)` |
| `konumuKur(150, 100)` | Doğrultuyu değiştirmeden x=150, y=100 konumuna git | |
| `konumVeYönüBelleğeYaz` | Şu anda bulunduğu konumu ve doğrultuyu belleğe yaz | |
| `konumVeYönüGeriYükle` | Bellekteki konuma git ve yine bellekteki doğrultuya dön. Eğer belleğe konum ve yön yazılı değilse hata verir: 'java.lang.IllegalStateException: No saved Position and Heading to restore.' Yani bellekte bilgi yok henüz diyor. | |
| `kuzey` | Doğrultunu kuzeye çevir. Benzer komutlar yanda | `batı` `doğu` `güney` |
| `noktayaDön(40, 60)` | Doğrultunu koordinatları verilen (x, y) noktasına çevir | |
| `noktayaGit(0, -100)` | Doğrultunu x=0, y=-100 konumuna çevir ve o noktaya kadar ilerle | |
| `noktayaGit(Nokta(40, 30))` | Yukarıdaki komutu böyle de çağırabiliriz. Programlama yaparken faydalı olur | |
| `sağ` | Sağa döner. Yani saat yönünde 90 derece döner | |
| `sağ(60)` | Sağa doğru 60 derece döner | `sağ(10)` `sağ(45)` |
| `sol` | Sola döner. Yani saat yönünün tersinde 90 derece döner | |
| `sol(65)` | Sola doğru 65 derece dön | `sol(10)` `sol(45)` |
| `zıpla()` | 25 adım öteye zıpla | |
| `zıpla(100)` | 100 adım öteye zıpla | |

Basit çizim komutları:

| Komut | Açıklama | Örnekler |
|---|---|---|
| `daire(20)` | Yarıçapı 20 olan bir daire çiz. | `üçgen(100)` `kare(100)` `nokta(100)` |
| `kare(30)` | Kenar uzunluğu 30 olan bir kare çiz | `kare()` |
| `nokta(30)` | 30 kalınlığında bir kalemle bir nokta çiz | `zıpla; nokta(10)` `ileri(100); nokta` |
| `üçgen(30)` | Kenar uzunluğu 30 olan bir üçgen çiz | |

Şimdilik son bir örnek daha görelim. Biraz daha uzun. Ama sakın irkilme. Bunun çoğu bölümün başındaki ilk örnekle aynı. `yinele(3)` döngüsünü aldık ve iki döngü içinde tekrar kullandık:

```scala
sil
canlandırmaHızınıKur(0) // anında çiz: kare kare canlandırma yok
// daha önce kullandığımız hızıKur(hız) komutuna benziyor,
// ama daha hassas ayar yapmamıza yarıyor. Girdisi adım atma süresini belirliyor
// onun için de bu iki komut ters çalışıyor:
// hız tarifi ve karşılık gelen adım atma süreleri şöyle:
//   çokHızlı: 0  <- SIFIR. Canlandırma büsbütün kapanır, çizim anında biter
//   hızlı:    10
//   orta:     100
//   yavaş:    1000 (varsayılan -- hiç hız vermezsen kaplumbağa böyle gider)
// 0 ile 1 arasındaki fark büyük: 0 canlandırmayı kapatıyor, 1 ise hâlâ
// canlandırıyor -- ve her adım en az bir ekran karesi (~16 ms) yiyor. Bu
// yüzden 1, 2 ve 10 neredeyse aynı hızda çıkıyor; aşağıdaki çizim 840 adım
// olduğu için hepsinde ~15 saniye sürüyor, 0 ile ~1 saniye.
// Eksi (sıfırdan küçük) değer VERME: kabul edilmiyor, hata alırsın.
yaklaş(0.2)
kalemKalınlığınıKur(20)
yinele(12) {
    sağ(30)
    yinele(10) {
        ileri(20)
        yinele(3) {  // gerisi ilk örnekle aynı
            ileri(100)
            dön(120)
            ileri(100)
        }
    }
}
```

ikojo'da `yaklaş(0.2)` yerine `yaklaşXY(0.2, 0.2, 0, 0)` yaz.
