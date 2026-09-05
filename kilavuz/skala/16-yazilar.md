# Yazıların (String) Kullanılışı
<!-- hücreler: çalıştır -->

## Yazı Türü

Ekrana yazı yazmak bilgisayar programlamada epey sık karşılaşılan bir sorun! Bu bölümde bazı tanımlar ve yazı türünün faydalı yöntemlerinden bazılarını göreceğiz. Bu yöntemlerin benzerleri Dizin türünde ve ona benzeyen sıra sıra elemanlar içeren başka türlerde de var, Dizim (Array), Yöney (Vector), Dizi (Seq) gibi hafifçe başkalaşmış ama benzer türler. [Bir sonraki bölümde](#b17) Dizin yöntemlerini görünce benzerliği farkedeceksin.

Aşağıdaki tanımlarda G1, G2, ... ile fonksiyonlara girilen değerleri ifade ediyoruz kısaca. Yani G1.işlevAdı(G2, G3, ...). Tablolardaki komutlara tıklayınca düzenleyicide açılır; sonucu görmek için `satıryaz` içine aldık.

### Yazılar için kaçış karakterleri

| Kaçış | Anlamı | Kaçış | Anlamı |
|---|---|---|---|
| `\n` | yeni satır | `\b` | geri adım |
| `\t` | büyük ara | `\f` | bir sayfa at |
| `\r` | satır başı | `\"` | çift tırnak |
| `\'` | tek tırnak | `\\` | geri taksim işareti |

### Ekleme

Yazıları aynı toplama yapıyormuş gibi `+` imiyle ekleyebiliriz. Girdi olan yazılar değişmez. Yeni bir çıktı oluşur sadece. Yazı değerlerini hep `dez` kullanarak sabit tutmakta fayda var. Ama çok istersek `den` anahtar sözcüğü kullanarak değerini değiştirebileceğimiz yazı değişkenleri de tanımlayabiliriz.

```scala
dez a = "Büyük"
dez b = "Patlama"
dez c = a + " " + b
satıryaz(a, b, c)
sil
den gerekYokAslında = "böyle değişkenlere\n"
gerekYokAslında += "pek de gerek yok"
yazı(gerekYokAslında)
```

Sanırım hemen hemen bütün nesnelerin `yazıya` diye bir yöntemi var. Bu yöntem nesnenin neye benzediğini yazı olarak ortaya koyar ve çok faydalıdır. Mantık önermeleri için tanımladığımız sınıfı anımsadın mı? [Bir önceki bölüme](#b15) bakıver istersen. Orada kendi yazıya yöntemimizi tanımlamış ve kullanmıştık.

```scala
dez x = (2).yazıya + " " + (3.1F).yazıya
satıryaz(x)
```

### Uzunluk veya Boy

| Komut | Açıklama |
|---|---|
| `satıryaz("dört".boyu)` | G1 yazısının uzunluğu yani kaç karakterden oluştuğunu bildirir. |

### Karşılaştırma

| Komut | Açıklama |
|---|---|
| `satıryaz("uzun".kıyasla("uzuner"))` | G1 ile G2'yi karşılaştırır. Eğer G1 G2'den önce geliyorsa eksi bir sayı, aynıysa sıfır, sonra geliyorsa artı bir sayı verir. A ile a'yı karşılaştır istersen |
| `satıryaz("uzun".kıyaslaKüçükHarfBüyükHarfAyrımıYapmadan("Uzun"))` | Karşılaştırma yaparken harflerin büyük ya da küçük olmasını göz ardı eder |
| `satıryaz("kitap".eşitMi("film"))` | İki yazı aynı ise doğru der, yoksa yanlış |
| `satıryaz("kitap".eşitMiKüçükHarfBüyükHarfAyrımıYapmadan("KITAP"))` | Eşitliğe bakarken harflerin büyük ya da küçük olmasını göz ardı eder |
| `satıryaz("kitaplık".başındaMı("kitap"))` | G1 yazısının başında G2 var mı? |
| `satıryaz("kalınkitap".başındaMı("kitap", 5))` | G1 yazısının G3. harfinden itibaren G2 geliyor mu? |
| `satıryaz("kitaparası".sonundaMı("arası"))` | G1'in sonunda G2 var mı? |

### Aramak ve bulmak

Bir yazı içinde başka bir harfi ya da yazıyı arayıp bulmak için kullanabileceğimiz yöntemler bunlar. Eğer aradığımızı bulamazsa -1 çıkar. İlk harfin konumu 0 kabul edilir. Gariptir biraz ama bilgisayarda hep sıfırdan saymaya başlarız.

| Komut | Açıklama |
|---|---|
| `satıryaz("imrendim".içeriyorMu("ren"))` | G2, G1'in içinde geçiyor mu? |
| `satıryaz("imrendim".sırası("nd"))` | G2, G1'in içinde kaçıncı konumda? Birden fazla varsa, ilk konumu verir. |
| `satıryaz("imrendirdim".sırası("di", 7))` | G2, G1'in G3'üncü harfinden sonra hangi konumda? |
| `satıryaz("imrendirdi".sırası('r'))` | G2 karakterinin G1 içindeki ilk konumu? |
| `satıryaz("imrendirdik".sırası('r', 4))` | G2, G1'in G3'üncü harfinden sonra hangi konumda? |
| `satıryaz("imrendiler".sırasıSondan('e'))` | G2'nin G1 içindeki son konumu nedir? |
| `satıryaz("imrenerek".sırasıSondan('e', 6))` | G2'nin G1 içindeki G3. harf veya daha önceki son konumu nedir? |
| `satıryaz("dipdirildi".sırasıSondan("di"))` | G2'nin G1 içindeki son konumu |
| `satıryaz("dipdirildi".sırasıSondan("di", 5))` | G2'nin G1 içindeki G3. harf ya da daha önceki son konumu |

### Yazıdan parça çıkarmak

| Komut | Açıklama |
|---|---|
| `satıryaz("aslangibi".harf(3))` | G1'in G2 konumundaki harfi |
| `satıryaz("aslangibi".parçası(3))` | G1'in G2 konumundaki harfinden sonuna kadar olan parçası |
| `satıryaz("aslangibi".parçası(3, 5))` | G1'in G2 konumundan G3'e kadarki parçası. G3. harf hariç. |

### Yazıdan başka bir yazı türetmek

| Komut | Açıklama |
|---|---|
| `satıryaz("Merhaba Kardeş".küçükHarfe)` | Bütün harfleri küçük olacak şekilde G1'in yeni bir kopyası |
| `satıryaz("Merhaba Kardeş".büyükHarfe)` | Bütün harfleri büyük olacak şekilde G1'in yeni bir kopyası |
| `satıryaz("  Merhaba Kardeş   ".kısalt)` | G1'in başındaki ve sonundaki boşlukların silinmiş kopyası |
| `satıryaz("Savar".değiştir('a', 'e'))` | G1'in içindeki bütün G2 harflerinin G3 ile değiştirilmiş kopyası |
| `satıryaz("Saye saye".değiştir("ay", "ev"))` | G1'in içindeki bütün G2 yazılarının G3 ile değiştirilmiş kopyası |

### Yazıya çeviren yöntemler

| Komut | Açıklama |
|---|---|
| `satıryaz(Yazı.olarak(Dizin(1,2,3)))` | G1'i yazıya çevirir. G1 herhangi temel bir tür ya da herhangi bir sınıfın nesnesi olabilir. |
| `satıryaz(Dizin(1,3,3,1).yazıYap("-ve-"))` | G1 dizisinin elemanlarını aralarına G2 ekleyerek yazıya çevirir |
| `satıryaz(Küme(1, 3, 5, 3, 1).yazıYap("{", " ", "}"))` | Bir öncekinde olduğu gibi G1'in elemanlarının aralarına G3'ü ekleyerek yazıya çevirir ama en başa G2, en sona da G4 yazılarını ekler. |
