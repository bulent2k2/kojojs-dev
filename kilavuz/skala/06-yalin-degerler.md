# Yalın Değerler, Sayılar, Kesirler ve Yazılar

Bütün hesaplar gibi, bütün programların da yapıtaşları 'yalın' değerlerdir. Nedir onlar? Örneğin 1, 2, 1/3, 3.14, 'a', 'b', 'ç', "Merhaba Güzel Dünya!", ve aklın alamayacağı kadar çok sabit yani değişmez değerler. Hepsini biliyorsun elbette. Tabii bu örnekler biraz karışık oldu. Bir harfle bir sayıyı karıştırmak iyi olmaz. Onun için bunları türlerine ayırmak Scala'nın çok iyi becerdiği bir konu. Başka pek çok dil de buna dikkat eder. Ama göreceksin Scala bir başka. Uzatmadan en temelinden başlayalım.

## Temel Türler

Scala kendiliğinden bazı temel türler tanımlar. Daha ilerde göreceğimiz gibi sen de kendi türlerini tanımlayıp Scala'dakilere ekleyebilirsin. Ama ilk önce bu temel türlerle tanışalım, işi kolay kılalım.

| Tür | Açıklama |
|---|---|
| `Lokma` | İngilizcesi Byte. -128'den başlar, -127 -126 ... -1 0 1 2 ... 127'e kadar gider. Neden seçip ayırmış bu sayıları acaba? Daha önce de konuştuk ya, bilgisayar, her sayıyı sıfır ve bir sayılarının dizisi olarak saklar ve işler. Bu sayı dizilerindeki bir ve sıfır sayılarına parçacık demiştik. Sekiz tanesini bir arada ele alırsak, ona Lokma diyoruz. Eksi sayıları artılardan ayırmak için o parçacıklardan birine özel bir anlam veriyoruz. Geriye kalıyor 7 parçacık. İkinin üstlerini anımsayalım: 2 4 8 16 32 64 128. Yani 7 parçacıkla 128'e kadar sayabiliyoruz. İngilizcede buna "8-bit signed 2's complement" derler. Uzattık ama temelin temeli bu! Neyse, şimdilik bu kadar yetsin |
| `Kısa` | (Short). İki lokma yani 16 parçacık. -32,768'den başlayıp 32,767'yle biter |
| `Sayı` | (Integer) Dört lokma yani 32 parçacık. -2,147,483,648'den 2,147,483,647'e kadar gider |
| `Uzun` | (Long) İki sayı yani 64 parçacık. -2^63 ile 2^63-1 arası |
| `UfakKesir` | (Float) 32 parçacık kullanarak kesirli sayıları ifade eder. İşini ciddiye alır ve IEEE'nin 754 no'lu standardına göre yapar. Bilirsin pi sayısının basamakları bitmek bilmez. UfakKesir olarak ama tam değeri şudur: 3.1415927 |
| `Kesir` | (Double) Ufak kesirden iki kat daha hassas. Bunu da 64 parçacık kullanarak sağlar. Pi sayısını şöyle bilir: 3.141592653589793. Aynı ufak kesir gibi ciddi standarda uyar. |
| `Harf` | (Char) Unicode karakter. 16 parçacık (bit) yani 2 lokma kullanır. Eksi artı sıkıntısı yoktur. İngilizce'de 'unsigned' denir. |
| `Yazı` | (String) Sınırsız bir harf dizisidir. Çok basit olsa da çok faydalıdır. |
| `İkil` | (Boolean) doğru ve yanlış değerlerini bilir başka da birşey bilmez. Başka bir lakabı da Seçim. |

## Sayma Sayıları

Tabloda da gördüğümüz gibi dört temel tür var (bir de iri sayılar var. Çok büyük sayma sayılarıyla oynamak için tasarlanmış sayı türünün adı İriSayı (BigInt). Eğer canın mümkün olan bütün briç ellerini saymak isterse Uzun yetmez İriSayı gerekir. Merak edersen bana haber ver. Konuşuruz.) Bunları tekrar anımsayalım çünkü çok faydalılar: Lokma Kısa Sayı ve Uzun. Yalın sayı değerlerini girmenin tek yolu var sanma. Farklı tabanlar kullanabiliriz. Sadece onluk tabanla sınırlı değil becerimiz. Onaltılık taban da bazen işe yarar. Bunu belirtmenin yolu ilk rakamı 0 yapmak.

Bize en doğal gelen onluk tabanın yalın sayıları 1 2 3 ... 8 ya da 9 ile başlar (decimal):

```scala
satıryaz(17)
satıryaz(653)
dez benimSayım = 653
dez benimSayım2: Sayı = 653
satıryaz(benimSayım == benimSayım2)
```

Onaltılık taban da bilgisayara ve bazı algoritmalara çok doğal gelir (hexadecimal): Sıfırla başlar ve x harfini ekleriz. Büyük X de olur. Farketmez. Sonra da onaltılık tabanın 16 sayısından seçeneklerle devam ederiz. İlk onunu herkes bilir, 0 1 2 ... 8 9. Sonra değeri onluk tabanda 10 olan a ya da A, sonra 11'e denk b/B ... en son da 15'e denk olan f/F. Ne ilginç değil mi? 13 yazmak yerine 0xd ile batıl inançlara da son verebiliriz belki!

```scala
dez x1 = 13
dez x2 = 0xd
eğer (x1 == x2) satıryaz("kim korkar d'den?!")
satıryaz(0x23)   // onaltılık taban. Yani onluk tabanda 35
satıryaz(0x01FF) // onluk tabanda 511
satıryaz(0xcb17) // onluk tabanda 51991
```

Varsayılan durum bunların Sayı türünde olduğudur. Ama bazen Uzun sayı olması gerekebilir. Onu ifade etmek de kolay. Sonuna "l" ya da "L" (yani İngilizce Long) koyarız. U veya u da iyi olurdu ama bunu Kojo'ya eklemeyi bilemedim ben. Belki de Scala, hatta Scala'nın atası olan Java'yı güncellemek gerekmesin?

```scala
satıryaz(0XFAF1L) // Uzun sayı onluk tabanda 64241
```

Yalın değerlerin Kısa ve Lokma türlerinde olmasını isteriz bazen. Ama, dikkat edelim ki geçerli aralığı aşmasın. Yoksa ne olur? Deneyerek daha iyi anlarsın sanıyorum.

```scala
dez birLokma: Lokma = 27
dez kısaSayı: Kısa = 1024
satıryaz(birLokma, kısaSayı)
```

```scala
dez sorunYok: Lokma = 127
dez sorunsuz: Lokma = -128
dez hataVerir: Lokma = 128
```

Son satır hata verdi. "Error... type mismatch" gibi birşeyler dedi. Yani türler uyuşmadı diyor. 128 sayısı Lokma'ya sığmıyor.

### Kesirli sayılar

Kesirli sayıları yalın olarak ifade etmek için nokta kullanırız elbet. Sıfır dışında bir sayıyla başlarlar. İşin ilginci sonuna E veya e harfi ekleyebiliriz. O İngilizce exponent yani üssü anlamına gelir ve hemen arkasından bir tam sayı (eksi de olabilir) ekleriz. Örneklerle anlamak daha kolay olacak:

```scala
satıryaz(9.876)
dez çokKüçük = 1.2345e-5
dez epeyBüyük = 9.87E45
satıryaz(çokKüçük, epeyBüyük)
```

Daha iyi anlamak için şu örneklere bakalım:

```scala
satıryaz(2e3)   // yani 2000. üç sıfır ekliyoruz yani
satıryaz(3e-2)  // yani 0.03. yüzle çarparsak 3 oluyor, fark ettin mi?
satıryaz(0.5e1) // 5.0 yazsak daha açık olurdu. Sadece daha iyi anlamak için
satıryaz(8e9)
```

Son satır 2022'nin son günlerinde yeryüzündeki insan kardeşlerimizin sayısını gösteriyordu! [Nüfusumuz kaç oldu?](https://www.worldometers.info/world-population)

Varsayılan, kesirli yalın sayıların Kesir türünde olmasıdır. Ama istersen UfakKesir de olabilirler. Sonuna f ya da F getirmen yeter. Bu İngilizce "float" sözcüğünden gelir o da bizim noktanın adı "floating point" olduğu için. Eğer Kesir olduğunu açık etmek istersen sonuna d veya D getiriver. O da "double" sözcüğünün baş harfi. Neden double? Çünkü UfakKesir 32 parçacık kullanırken, Kesir 64, yani iki katı parçacık yani daha çok bellek kullanır. Bellek büyüdükçe de hassasiyet artar. Öte yandan ne kadar çok bellek kullanırsak, hem belleğin sınırlarını aşma tehlikesi artar hem de program daha yavaş çalışır. Onun için bazı programları yazarken hangi tür sayıları seçeceğimize çok dikkat ederiz.

```scala
dez ufakkesir1 = 1.5324F
dez ufak2 = 3e5f
satıryaz(ufakkesir1, ufak2)
```

### Harfler

Bilgisayar, okumayı yazmayı bilmese de pek çok karakteri ya da harfi tanır. Sadece Türkçe ve İngilizce alfabeler değil hem de. Daha başka pek çok tek harf dediğimiz sembol, ya da karakter var. İngilizce adı da zaten Char, yani character sözcüğünün kısaltması. Bu harfleri yalın olarak girmek kolay. Tek tırnak içine alıverelim:

```scala
dez harf = 'A'
satıryaz(harf)
```

Bu değeri girmenin daha zor yolları da var! Şaka şaka. Daha genel yolları bilmekte fayda var: Evrensel kodlama şöyle oluyor. `'\u0000'` ve `'\uFFFF'` arasında yani onaltı tabanlı bir sayı seçiyoruz.

```scala
dez a = '\u0041' // A oldu
dez e = '\u0045' // E oldu
satıryaz(a, e)
```

[Evrensel Kod (bunların genel adı Unicode) hakkında Vikipedi makalesi](https://tr.wikipedia.org/wiki/Unicode)

En son olarak, 'kaçış' yalın harfi diye de bilinen bir kaç özel duruma bakalım. Bunlara harf demek doğru olmuyor ama ne yapalım. Referans kaynağından yalın kaçış harfleri listesi: [Java'nın teknik özelliklerini anlatan siteden](https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6). Her türlü yazılım sorularına yanıt bulabildiğimiz meşhur bir siteden de benzer bilgiler edinebiliriz: [stackoverflow.com'dan](https://stackoverflow.com/questions/1367322/what-are-all-the-escape-characters).

### Yazılar

Yalın bir yazı, çift tırnaklar arasına aldığımız harflerden oluşur. Birkaç tane örnek gördük bile. Birkaç tane daha olsun:

```scala
dez merhaba = "merhaba dünya!"
satıryaz(merhaba)
```

Bazı özel karakterler var ki onları yazıya eklemek için özel bir kaçış karakteri kullanmamız gerekiyor. Bunların hepsi iki karakterden oluşuyor. Birincisi `\` olan kaçış karakteri. Arkasından da bir özel karakter daha gelir. En yaygın olanlar ve işlevleri şöyle:

| Kaçış | Anlamı | Kaçış | Anlamı |
|---|---|---|---|
| `\n` | yeni satır | `\b` | geri adım |
| `\t` | büyük ara | `\f` | bir sayfa at |
| `\r` | satır başı | `\"` | çift tırnak |
| `\'` | tek tırnak | `\\` | geri taksim işareti |

```scala
dez kaçışKarışıklığı = "\\\"\'"
satıryaz(kaçışKarışıklığı)
```

Bu son örnekte gördüğümüz karışıklıktan kurtulmak için Scala üç tane çift tırnak (`"""`) arasına aldığımız yazıları olduğu gibi yazar. Yani yeni satır, tek tırnak, çift tırnak ne varsa olduğu gibi alır. Bakın bir örnekle anlayalım:

```scala
satıryaz("""Kojo'ya hoşgeldin!
"Kaplumbağa" ile çizim yapmak hakkında daha çok bilgi edinmek için
\Kaplumbağacığın Kullanılışı\ adlı bölüme bakabilirsin.""")
```

### İkil

İngilizcesi boolean olan bu tür, mantıksal işlemlerde ve koşullar ifade etmek için kullanılır. Çok özel bir türdür. Sadece iki yalın değerle iş biter:

```scala
dez büyükOlsunMu = doğru
dez birazŞapşalMı = yanlış
satıryaz(büyükOlsunMu, birazŞapşalMı)
```
