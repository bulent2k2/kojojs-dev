# Kaplumbağacığın Kullanılışı
<!-- hücreler: çalıştır -->

Bu bölümü en başa, hatta Başlayalım adlı bölümden de öne koyduk. Çünkü hem okuması, anlaması ve oynaması daha kolay hem de daha görsel ve onun için de biraz daha cazip. Kojo'nun kaplumbağacığını zaten iyi tanıyorsan [bir sonraki bölüme](#b03) atlayıp Scala yani epey ciddi bir yazılım dili öğrenmeye başlayabilirsin.

Kaplumbağacığı hareket ettirerek ona çizgi çizdirten pek çok komutumuz var. Çoğunu aşağıdaki tabloda bulacaksın. Onlara tıklayıver ki kaplumbağacık neler yapabiliyor göresin: her komut düzenleyicide açılır, **Çalıştır**'a basman yeter. Tablodaki sırayı izlemene gerek yok. İstediklerine birkaç kere tıklayabilirsin.

Aşağıdaki ilk örnekte bir işlev (ya da komut) tanımlıyoruz. Ne yaptığını anladın mı? Çalıştırınca göreceksin. Bir üçgen çiziyor. Fark ettin mi, `yinele` komuduyla `ileri` ve `sağ` komutlarını üçer kere çağırıyoruz.

```scala
tanım birÜçgenÇiz() = {
    yinele(3) {
        ileri(100)
        sağ(120)
    }
}
sil()
birÜçgenÇiz()
sol()
birÜçgenÇiz()
```

Çalıştırdın ve kaplumbağanın iki tane üçgen çizmesini seyrettin, değil mi? Bu kılavuzdaki kod bloklarının her birini denemeyi unutma. Genelde sırayla gitmende fayda var. Ama bazılarını birden çok da çalıştırabilirsin, bazılarını da es geçebilirsin. Rahat takıl. Biraz oku, biraz dene. Düzenleyicide değişiklikler yap ve tekrar çalıştır. İçinden geldiği gibi davran ki daha keyifle keşfedesin bilgisayar programlamayı!

| Komut | Ne yapar |
|---|---|
| `ileri(100)` | 100 adım ilerler |
| `sil()` | Tuvali temizler ve başlangıç noktasına döndürür |
| `geri(50)` | 50 adım geriye gider |
| `eksenleriGöster()` | X ve Y eksenlerini gösterir |
| `ızgarayıGöster()` | Tuvalin ızgarasını çizer |
| `konumuKur(150, 100)` | Çizgi çizmeden koordinatları verilen (x, y) noktasına gider. Ama baktığı doğrultu değişmez |
| `noktayaGit(0, -100)` | Doğrultusunu koordinatları verilen noktaya çevirir ve o noktaya kadar ilerler |
| `eksenleriGizle()` | Eksenleri saklar |
| `ızgarayıGizle()` | Izgarayı gizler |
| `dön(30)` | Saat yönünün tersine doğru 30 derece döner. Eksi girersen saat yönünde döner |
| `sağ()` | Sağa döner. Yani saat yönünde 90 derece döner |
| `sağ(60)` | Sağa doğru 60 derece döner |
| `sol()` | Sola döner. Yani saat yönünün tersinde 90 derece döner |
| `sol(30)` | Sola doğru 30 derece döner |
| `noktayaDön(40, 60)` | Doğrultusunu koordinatları verilen (x, y) noktasına çevirir |
| `açıyaDön(30)` | Diyelim ki 0 derece ekranın sağı, 90 derece ekranın üstü olsun. Verilen açıya döner |
| `satıryaz(doğrultu)` | Şu anda baktığımız doğrultuyu açı olarak çıktıyla bildirir. 180 sola bakıyor demek. 270 de aşağıya |
| `ev()` | Evine yani (0, 0) noktasına döner ve 90 dereceye yani yukarı bakar |
| `satıryaz(konum)` | Şu andaki konumu çıktı olarak bildirir |
| `ışınlarıAç()` | Dört yönü belirten farlar yansın |
| `ışınlarıKapat()` | Farları söndürelim |
| `satıryaz(canlandırmaHızı)` | 100 adımı şu anda kaç milisaniyede attığını bildirir |
| `kaplumbağa.geri(100)` | Başlangıçtaki kaplumbağamızın adı `kaplumbağa`. Onu yöntemleriyle de çağırabiliriz |

Tuvali silmek için `sil()` komutunu kullan (masaüstünde tuvale sağ tıklayıp Temizle'ye de basabilirsin). Şimdi biraz daha uzun örnekler:

```scala
sil
kalemiKaldır()
ileri(100)
kalemiİndir()
ileri(100)
```

Kalem kalkıkken hareket ederse çizim yapmaz. Kalem inince çizmeye devam eder.

```scala
tanım birÜçgenÇiz() = {
    yinele(3) {
        ileri(100)
        sağ(120)
    }
}
sil()
kalemRenginiKur(mavi)
birÜçgenÇiz()
```

Rengini değiştirelim. Çizimin içini boyamak da kolay. Kojo'nun kendi `üçgen` komudu da var; girdisini değiştirerek değişik boylarda üçgen çizebiliriz:

```scala
sil()
boyamaRenginiKur(kırmızı)
üçgen(80)
```

Çizim yaptığı kalem kalınlığını da giriyoruz:

```scala
sil()
kalemKalınlığınıKur(10)
üçgen(90)
kalemKalınlığınıKur(1)
```

`görünmez` ve `görünür` komutları da şöyle çalışıyor. Kaplumbağacık görünmese de çizim yapmaya devam edebiliyor:

```scala
sil()
görünmez()
ileri(100)
görünür()
dön(120)
ileri(100)
```

Bulunduğu noktaya yazı yazdırabiliriz. İngilizce'den çevirdiğimiz bazı türlerin adı İngilizce olabilir. Şaşırtmasın seni!

```scala
sil
yazı("Merhaba Kardeş!")
zıpla()
yazı(Aralık(10, 0, -1).yazıya)
zıpla()
yazı(Dizi(1, 2, 3).yazıya)
zıpla()
```

Kaplumbağacığın hızını belirlemek için bir süre giriyoruz. 100 adımı girdiğimiz kadar milisaniyede atıveriyor. Epey hızlı canım! Başlangıçta 100 adım atmak 1000 milisaniye yani bir saniye alıyor.

```scala
sil()
ileri(-100)
canlandırmaHızınıKur(10)
dön(120)
ileri(100)
```

Verilen (x, y) noktasında yeni bir kaplumbağa canlandırmak da mümkün. İlk yeni kaplumbağaya ad takmayı unuttuk!

```scala
yeniKaplumbağa(50, 50)
dez yk2 = yeniKaplumbağa(100, 100)
yk2.geri(180)
// ikinci kaplumbağacığı da hızlandıralım:
yk2.canlandırmaHızınıKur(10)
yk2.sol()
yk2.ileri(300)
```

`yaklaş(oran, x, y)` tuvali verilen oran kadar büyültür ya da küçültür ve verilen noktayı tam tuvalin merkezine getirir. ikojo'da aynı işi `yaklaşXY(oran, oran, x, y)` yapar:

```scala
sil()
üçgen(100)
yaklaş(0.5, 10, 10)
```

```scala
sil()
üçgen(100)
yaklaşXY(0.5, 0.5, 10, 10)
```

İleri bölümlerde başka pek çok örnek göreceğiz. Matematiksel işlevleri sevenlere iki üç bölüm sonra kaplumbağalı bir sürpriz de var: Kaplumbağamız [Yazılım Akışı](#b05) bölümünün sonuna yakın hızla bir parabol çizecek! Devam etmek için bir sonraki bölüme geç. Ya da soldaki listeden istediğin bölüme atla.
