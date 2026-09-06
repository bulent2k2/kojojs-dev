# İlerledikçe faydalı olacak türler
<!-- hücreler: çalıştır -->

| Türkçe | İngilizce | Açıklama |
|---|---|---|
| `KuralDışı` | `Exception` | |
| `ÇalışmaSırasıKuralDışı` | `RuntimeException` | |
| `BaskınYazıyaYöntemiyle` | | bir tür özelliği ([Nesneler ve Sınıflar](/yardim/skala#b08) bölümüne bak) |
| `Eşsizlik` | | bir tür özelliği |
| `UzunlukBirimi` | `UnitLen` | net.kogics.kojo.core'dan |
| `Biçim` | `Shape` | java.awt'den |
| `GeoYol` | `GeneralPath` | java.awt.geom'dan |
| `GeoNokta` | `VertexShape` | net.kogics.kojo.core'dan |
| `Grafik2B` | `Graphics2D` | scala.swing'den |
| `İmge` | `Image` | java.awt.Image |
| `İmgeİşlemi` | `ImageOp` | net.kogics.kojo.picture.ImageOp |
| `Bellekteİmge` | `BufferedImage` | java.awt.image'dan |
| `Bellekteİmgeİşlemi` | `BufferedImageOp` | java.awt.image'dan |
| `ÇiniDünyası` | `tiles` | net.kogics.kojo.tiles |
| `ÇiniXY` | `TileXY` | tiles'dan |
| `BirSayfaKostüm` | `SpriteSheet` | tiles'dan |
| `Mp3Çalar` | `KMp3` | net.kogics.kojo.music'den |
| `Canlandırma` | `Animation` | net.kogics.kojo'dan |
| `BKK` | `URL` | Birörnek Kaynak Konumlayıcısı: java.net.URL'den (aşağıdaki örnek) |

```scala
dez bkk = BKK(
  "https://upload.wikimedia.org/",
  "wikipedia/commons/thumb/a/a5/",
  "Flower_poster_2.jpg/",
  "330px-Flower_poster_2.jpg")
Resim.imge(bkk).veBüyüt(0.3).çiz
```

ikojo'da bir imgeyi adresinden doğrudan çizebilirsin: `Resim.imge("https://.../330px-Flower_poster_2.jpg")` — [Kojo ile öğren](/kojo-ile-ogren) sayfasındaki resim örneklerine bak.

## Kuraldışı durumlar ve hatalar

| Türkçe | İngilizce | Örnek |
|---|---|---|
| `BelirtimHatası` | `java.lang.AssertionError` | (aşağıdaki ilk örnek) |
| `KuraldışıGirdiHatası` | `java.lang.IllegalArgumentException` | (ikinci örnek) |
| `EksikTanımHatası` | `scala.NotImplementedError` | (üçüncü örnek) |
| `SınırDışınaTaşmaHatası` | `java.lang.IndexOutOfBoundsException` | |
| `BoşGöstergeHatası` | `java.lang.NullPointerException` | |
| `MatematikselHata` | `java.lang.ArithmeticException` | |
| `İşParçacığıÖlümü` | `java.lang.ThreadDeath` | |

```scala
belirt(
  5 < 7,
  "ilk girdi ikinciden küçük olmalı")
// 5 yerine 8 yazıp tekrar çalıştır
```

```scala
durum sınıf Kişi(ad: Yazı, yaş: Sayı) {
  gerekli(yaş > 0 && yaş < 1000,
    "kişinin yaşı yanlış")
  satıryaz(s"$ad $yaş yaşında")
}
dez k1 = Kişi("Mustafa Kemal", 143)
 // bir de 1 yerine -1 girip çalıştır
dez k2 = Kişi("Garip Durum", 1)
```

```scala
// bazen ne istediğimizi biliriz
// ama nasıl olacağını bilemeyiz ya,
// o zaman böyle boş bir tanım
// yazmak faydalı olabilir
tanım deney1(girdi: Sayı) = ???
// bunu doğru yapıp tekrar çalıştır:
dez hazırsa = yanlış
eğer(hazırsa) deney1(42)
```

## Tür eşi olmayan nesneler

Tür eşi olmayan nesneler de var. Onların yöntemleri çok işimize yarar:

| Türkçe | İngilizce | Açıklama |
|---|---|---|
| `ay` | `UI` | arayüz'ün kısaltması: Swing düğmeleri, yazı girdileri, salındıraçlar. Yalnız masaüstünde; tarayıcıda karşılığı henüz yok. (aşağıdaki ilk örnek) |
| `tuvalAlanı` | `canvasBounds` | tuval alanı hakkında faydalı bilgiler. ikojo'da adı `tuvalSınırları`. (ikinci örnek) |

```scala
silVeSakla
dez ay_uzunluk = ay.Yazıgirdisi(60)
dez ay_renkler = ay.Salındıraç("mavi", "yeşil", "sarı")
Resim.arayüz(ay.Tanıt("Sayı ve Renk Seç:")).veGötür(-100,120).çiz
Resim.arayüz(ay_uzunluk).veGötür(-100, 100).çiz
Resim.arayüz(ay_renkler).veGötür(-100, 80).çiz
Resim.arayüz(ay.Düğme("Sayı ve Renk Seçimlerini Yaz"){
  satıryaz("=" * 10)
  satıryaz(s"Sayı: ${ay_uzunluk.değeri}")
  satıryaz(s"Renk: ${ay_renkler.değeri}")}
).veGötür(-100, 60).çiz
```
<!-- masaüstü: ay, Resim.arayüz -->

```scala
dez solAltKöşe = Nokta(tuvalAlanı.x, tuvalAlanı.y)
dez sağAltKöşe = Nokta(tuvalAlanı.x + tuvalAlanı.eni, tuvalAlanı.y)
dez solÜstKöşe = Nokta(tuvalAlanı.x, tuvalAlanı.Y)
dez sağÜstKöşe = Nokta(tuvalAlanı.x + tuvalAlanı.eni, tuvalAlanı.Y)
dez hepsi = yeni Dikdörtgen(solAltKöşe, sağÜstKöşe)
yaz(hepsi)
```

ikojo'da aynı bilgi `tuvalSınırları` ile alınır:

```scala
dez ta = tuvalSınırları
satıryaz(s"sol alt: (${ta.x}, ${ta.y})  en: ${ta.width}  boy: ${ta.height}")
```

Şimdilik bu kadar. Devamı yarın.
