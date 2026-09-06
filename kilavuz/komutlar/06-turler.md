# Her değerin bir Türü var
<!-- hücreler: çalıştır -->

Burada ilk sütunda Türkçe türleri sıraladık. İkinci sütunda İngilizce karşılıkları var. Scala'daki her yazılım parçacığı bir nesne. Her nesnenin de bir türü var. Türlerin hepsi de birbirine bağlı. Bunu bir çizimle görmekte fayda var. İngilizcesini şu İnternet sayfasında görebilirsin: [Yönlü tür çizgesi (directed graph of Scala types)](https://docs.scala-lang.org/resources/images/tour/unified-types-diagram.svg). Türlerin birliği önemli bir konu. Hakkında daha çok bilgi: [Scala turundan sayfa](https://docs.scala-lang.org/tour/unified-types.html).

## Temel türler

| Türkçe | İngilizce | Örnek | Açıklama |
|---|---|---|---|
| `Nesne` | `Object` | (aşağıdaki ilk örnek) | Herşey bir Nesne olabilir mi? Evet! Java nesnelerinin temel türü |
| `Birim` | `Unit` | (ikinci örnek) | İşlevsel bir çıktısı olmayan yöntemler ve komutların çıktı türü |
| `Her` | `Any` | `dez h: Her = 1` | Her Scala nesnesinin temel türü. Kendi temeli de Nesne. Yönlü tür çizgesinin başlama noktası |
| `HerDeğer` | `AnyVal` | `dez h: HerDeğer = 2` | Her Scala değerinin temel türü. Kendi temeli de Her. |
| `HerGönder` | `AnyRef` | `dez h: HerGönder = Diz(1, 2, 3)` | İşlevlerin, karmaşık ve çoğul içeriği olan türlerin temeli. Ref yani İngilizce 'Reference' sözcüğünü gönderge olarak çevirdik. Gösteren de diyebiliriz. İngilizce 'pointer' sözcüğü de benzer bir kavramdır. |
| `Hiç` | `Nothing` | `dez y: Hiç = ???` | En özel ve hiç nesnesi olmayan tek tür. Onun için de bu örneği çalıştırınca hata verir. Yönlü tür çizgesinin bitiş noktası yani en özel tür bu nesnesiz tek türdür. |
| `Yok` | `Null` | `dez y: Yok = yok` | Java'dan kalma. Hiç kullanma! |
| `İkil` | `Boolean` | (üçüncü örnek) | En sade ve basit türlerden biri. Sadece iki değeri var. Ama çok da faydalı |
| `Seçim` | `Boolean` | (dördüncü örnek) | O kadar temel bir tür ki Türkçeye çevirirken iki isim verdik ona! |
| `Lokma` | `Byte` | (beşinci örnek) | Bir Lokma aslında sekiz parçadan oluşuyor. O küçük parçaların İngilizce adı 'Bit.' Her parça aslında bir İkil ya da bir Seçim |
| `Kısa` | `Short` | `satıryaz(Kısa.Enİrisi)` | Eğer küçük sayılarlaysa işimiz, o zaman bu tür yeter bize |
| `Sayı` | `Int` | `satıryaz(Sayı.Enİrisi)` | Genelde kullandığımız sayma sayılarının türü budur |
| `Uzun` | `Long` | `satıryaz(Uzun.Enİrisi)` | Eğer epey büyük sayılar gerekiyorsa, Sayı türü yerine bunu kullanırız |
| `İriSayı` | `BigInt` | (altıncı örnek) | Sayıların sınırı olmasın dersek, bu türü kullanabiliriz. Ama hem daha çok bellek tutar hem de daha uzun sürer çalışması |
| `Kesir` | `Double` | `satıryaz(Kesir.Enİrisi)` | Kesirli sayıların türü. Epey büyüktür en irileri. İki lokma bellek tutar her kesir sayı |
| `UfakKesir` | `Float` | `satıryaz(UfakKesir.Enİrisi)` | Kesirli sayıların tek lokma yer tutanı |
| `İriKesir` | `BigDecimal` | (yedinci örnek) | Kesirli sayıların en hassası (ama çok lokma yer tutar!) |
| `Harf` | `Char` | (sekizinci örnek) | Char ve Character diye iki tür var. Bizim Harf ikisini de kapsıyor |
| `Yazı` | `String` | `satıryaz("*-*" * 5)` | Çok işe yarar bu tür. Scala'ya Giriş kılavuzunda [kendi bölümü](/yardim/skala#b16) ve pek çok örneği var |
| `EsnekYazı` | `StringBuilder` | (dokuzuncu örnek) | Bu da biraz daha esnek olanı. Şuradan: collection.mutable |

```scala
dez n: Nesne = yeni Nesne()
satıryaz(n.aynıMı(n))
```

```scala
// ekrana yazmak bir yan etki
// Yani işlevsel bir çıktı değil
tanım söyle(adın: Yazı): Birim =
    yaz(s"merhaba $adın")

tanım satırBaşı(): Birim =
    satıryaz("")

söyle("Ayşe")
satırBaşı
söyle("Ali")

// Bu da faydasız bir değişmez
dez b: Birim = ()
b // birim etki yok etki
```

```scala
dez çatal1: İkil = (doğru || yanlış)
dez çatal2: Seçim = (doğru && yanlış)
satıryaz(çatal1, çatal2)
```

```scala
için(s <- Diz(doğru, yanlış, doğru))
    yaz(eğer(s) "Doğru "
    yoksa "Yanlış ")
```

```scala
dez b1: Lokma = 1 << 6
yaz(Lokma.EnUfağı, Lokma.Enİrisi)
// Hata verir "type mismatch":
// dez b2: Lokma = 1 << 7
```

```scala
satıryaz(Uzun.Enİrisi + 2)
dez i = İriSayı(Uzun.Enİrisi)
satıryaz(i * i * i)
```

```scala
dez ufak = 1.000001
dez payda = 10000000.0
satıryaz(ufak / payda)
dez x = İriKesir(ufak)
satıryaz(x / payda)
```

```scala
dez h = '☺'
dez j = (h + 1).harfe
yaz((h - 1).harfe, h, j)
```

```scala
dez ey = yeni EsnekYazı("Merhaba ")
dez ad = satıroku("Adın ne?")
ey.ekle(ad)
ey.ekle("! Ne var ne yok?")
satıryaz(ey)
```

## Koleksiyonlar ve diğerleri

| Türkçe | İngilizce | Açıklama |
|---|---|---|
| `Belki` | `Option` | |
| `Biri` | `Some` | |
| `Hiçbiri` | `None` | Aslında aynı adlı yegane bir değişmez değerin türü |
| `Aralık` | `Range` | |
| `Diz` | `collection.Seq` | |
| `Dizi` | `Seq` | |
| `Dizik` | `Array` | |
| `Dizim` | `Array` | Bu Dizik'in eskisi |
| `EsnekDizim` | `ArrayBuffer` | eskisi. collection.mutable'dan |
| `EsnekDizik` | `ArrayBuffer` | collection.mutable'dan |
| `Dizin` | `List` | |
| `SıralıDizi` | `IndexedSeq` | |
| `Eşlek` | `Map` | collection.immutable.Map |
| `Eşlem` | `Map` | collection.mutable.Map |
| `Küme` | `Set` | |
| `MiskinDizin` | `LazyList` | |
| `Kuyruk` | `Queue` | collection.mutable |
| `ÖncelikSırası` | `PriorityQueue` | collection.mutable |
| `Yığın` | `Stack` | collection.mutable |
| `Yöney` | `Vector` | collection.immutable |
| `Yineleyici` | `Iterator` | |
| `Gelecek` | `Future` | scala.concurrent |
| `İşletimBağlamı` | `ExecutionContext` | scala.concurrent |
| `Sayılar` | `Vector[Int]` | (aşağıdaki örnek) |
| `Boya` | `Paint` | java.awt'den |
| `Renk` | `Color` | java.awt'den |
| `Yazıyüzü` | `Font` | java.awt |
| `Hız` | `Speed` | net.kogics.kojo.core'dan |
| `Nokta` | `Point` | net.kogics.kojo.core'dan |
| `Dikdörtgen` | `Rectangle` | net.kogics.kojo.core'dan |
| `Üçgen` | `Triangle2D` | io.github.jdiemke.triangulation'dan |
| `Yöney2B` | `Vector2D` | net.kogics.kojo.util'den |
| `Resim` | `Picture` | |
| `BuAn` | `Now` | Now adında bir tür yok, ama BuAn adında bir durum sınıfımız yani türümüz var: `BuAn().yazıya`. ikojo'da `buSaniye` |
| `Takvim` | `Calendar` | java.util |
| `Tarih` | `Date` | java.util |
| `SaatDilimi` | `TimeZone` | java.util |
| `Bölümselİşlev` | `PartialFunction` | |
| `İşlev1` | `Function1` | |
| `İşlev2` | `Function2` | |
| `İşlev3` | `Function3` | |

```scala
Sayılar(0, 1, 2, 3)
    .işle(s => 10 * s * s)
    .herbiriİçin(yaz)
```

Bu tablolardaki türlerin hangilerinin ikojo'da bulunduğunu görmek için [Koco Sözlüğü](/yardim/sozluk)'ne bak; tarayıcıda Java kütüphaneleri (java.awt, java.util) olmadığı için o türlerin bir kısmı yalnız masaüstünde var.
