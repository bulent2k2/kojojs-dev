# Dizinlerin (List) Kullanılışı

## Dizinler

Dizinler, algoritmaların işlevsel olarak tanımlanmasında en çok kullanılan veri yapısıdır. Bu bölümde dizinleri kolaylıkla tanımlamak ve kullanmak için en faydalı olan yöntemleri göreceğiz. En başta bir örnek Dizin tanımlayacağız. Sonra da onu diğer örnek yazılımcıklarda kullanacağız.

Daha önce görmüştük ama anımsatmakta fayda var: sık sık göreceğimiz üç im: `_ + _` şu adsız işlevin kısa yolu: `x,y => x + y`. Böyle iki girdili işlemler çok yaygın. Onun için bu kısa yol işimize yarayacak. Tabii toplama olması şart değil. Çarpma da olur: `_ * _`. Benzer şekilde `_.yöntem` ile de `e => e.yöntem` adsız işlemini kısaca ifade ediyoruz. Bu kısa yollarda kullandığımız alt çizginin her biri yeni bir girdiye karşılık geliyor. Birinci altçizgi ilk girdiyi, varsa ikinci altçizgi ikinci girdiyi, üçüncü altçizgi üçüncü girdiyi, vb.

Örnek dizinimiz şu (her örnekte bu tanımı yineliyoruz ki tek başına çalışsın):

```scala
dez dzn = "Gün" :: "bu gün" :: "." ::
  "An" :: "bu an" :: "." :: Boş
satıryaz(dzn)
```

İçinde "Gün", "bu an", "." gibi 6 tane değer olan yeni bir Dizin[Yazı] tanımladık. Şimdi dizinlerle yapılabilecekleri örneklerle görelim. Her satırın yanındaki açıklamayı okuyup çalıştır:

```scala
dez dzn = "Gün" :: "bu gün" :: "." :: "An" :: "bu an" :: "." :: Boş
// Bomboş bir Dizin. Ya da Boş değerini de kullanabilirsin:
satıryaz(Dizin())
// İçinde dört eleman olan bir Dizin[Yazı]:
satıryaz(Dizin("Zaman", "ok", "gibi", "uçar mı?"))
// Üç tane iki nokta üstüste işlemi dizinleri birleştirerek yeni bir dizin türetiyor. :: ile karşılaştır:
satıryaz(Dizin("tik", "tok") ::: Dizin("ding", "dong"))
// Dizinin 3. elemanını verir (sıfırdan başlarsak üçüncü):
satıryaz(dzn(3))
// Dizinin içinde tek harften oluşan kaç sözcük var?
satıryaz(dzn.say(söz => söz.boyu == 1))
// Dizinin içinde "bu an" elemanı var mı?
satıryaz(dzn.varMı(söz => söz == "bu an"))
// Dizinin ilk üç elemanı düşmüş kopyasını verir:
satıryaz(dzn.düşür(3))
// Dizinin son dört elemanı düşmüş kopyasını verir:
satıryaz(dzn.düşürSağdan(4))
// Dizinin 2 veya 3 harfli elemanlarını verir:
satıryaz(dzn.ele(söz => 1 < söz.boyu && söz.boyu < 4))
// Verilen işlevi dizinin elemanlarına uygular sonra da hepsini birleştirir:
satıryaz(dzn.düzİşle(_.dizine))
// Dizinin bütün elemanları noktayla bitiyor olsaydı doğru derdi:
satıryaz(dzn.hepsiİçinDoğruMu(söz => söz.sonundaMı(".")))
```

```scala
dez dzn = "Gün" :: "bu gün" :: "." :: "An" :: "bu an" :: "." :: Boş
// Dizinin elemanlarını sırayla yazar:
dzn.herbiriİçin(söz => yaz(söz))
satıryaz()
// Bir öncekinin kısa hali:
dzn.herbiriİçin(yaz)
satıryaz()
satıryaz(dzn.başı)    // İlk elemanı verir
satıryaz(dzn.kuyruğu) // İlk eleman hariç gerisini verir
satıryaz(dzn.önü)     // Son eleman hariç gerisini verir
satıryaz(dzn.boşMu)   // boş olsaydı doğru derdi
satıryaz(dzn.sonu)    // Son elemanı verir
satıryaz(dzn.boyu)    // Kaç eleman olduğunu söyler
// dizinin her sözcüğünün sonuna soru işareti ekleyerek yeni bir dizi oluşturur:
satıryaz(dzn.işle(söz => söz + "?"))
```

Bir önceki gibi ama noktaları soru işaretiyle değiştirip yazalım. İkinci hali birincinin kısa yazılışı:

```scala
dez dzn = "Gün" :: "bu gün" :: "." :: "An" :: "bu an" :: "." :: Boş
dzn.işle { söz =>
    söz eşle {
        durum "." => "?"
        durum s   => s
    }
}.herbiriİçin(satıryaz)

dzn.işle {
    durum "." => "?"
    durum s   => s
}.herbiriİçin(satıryaz)
```

```scala
dez dzn = "Gün" :: "bu gün" :: "." :: "An" :: "bu an" :: "." :: Boş
// Diziden elemanları arasına virgül koyarak bir yazı yapar:
satıryaz(dzn.yazıYap(", "))
// Dizinin bir kopyasını verir ama bir harfli elemanları atlar:
satıryaz(dzn.eleDeğilse(söz => söz.boyu == 1))
// Tekrar eden elemanları atlar. Tekrarları bulmak için == işlemini kullanır:
satıryaz(Dizin(1,6,2,1,6,3).yinelemesiz)
// Elemanlarını ters sırada olan bir kopya verir:
satıryaz(dzn.tersi)
// A'dan Z'ye sıraya sokulmuş bir kopya verir ama büyük küçük harf ayırımı yapmadan:
satıryaz(dzn.sırayaSok((söz, t) => söz.küçükHarfe < t.küçükHarfe))
```

Katlama işlemleri de çok işe yarar! Bu sefer sayı dizisi kuralım:

```scala
dez sayılar=Dizin(1,7,2,8,5,6,3,9,14,12,4,10)
// Soldan sağa elemanları topluyoruz. Başlangıçta soldan -81 giriyoruz:
satıryaz(sayılar.soldanKatla(-81)(_ + _))
// Sağlamasını yapalım:
satıryaz(sayılar.topla)
// Şimdi de sağdan sola mantıksal veya işlemiyle birleştiriyoruz sayıların
// parçacıklarını. Başlangıçta en sağdan onaltılık tabanda 20 giriyoruz:
satıryaz(sayılar.sağdanKatla(0x20)(_ | _))
```
