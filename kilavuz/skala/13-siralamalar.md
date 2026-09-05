# Sıralamalar (Tuple)

Diyelim ki bir işlevimiz var ve birden çok çıktısı olsun istiyoruz. Ya da bir koleksiyon oluşturacağız ama her elemanı birden fazla değer tutsun istiyoruz. Sözle anlamak zor, sabır, hemen örnek vereceğiz aşağıda. Bu tür durumlarda yeni bir tür tanımlamak zor olmaz elbet. Yeni bir sınıf nasıl oluşturulur gördük. Ama biraz zahmetli elbette. Epey kod yazmak gerekebiliyor, `durum sınıf` bile olsa. Sadece kod yazmak değil esas zorluk anlamlı isimler bulmak ve onları uzun uzun kullanmak aslında. Gerçekten gerekmedikçe yeni adlar üretmek zorunda kalmasak çok iyi olur, değil mi? Uzun lafın kısası, Scala dili, adsız değerleri gerektiği anda ve gerektiği yerde sıralamamıza izin vererek bu sorunu güzelce çözer. Sıralama dedik ya, İngilizce'si 'tuple' parantez içinde virgülle ayrılan bir ya da daha çok değerin bir araya gelmesinden ibaret. İşin güzel tarafı değerlerin türleri aynı olmak zorunda değil. Örneğin:

```scala
satıryaz((3, 'c'))
satıryaz((3.14, "pi sayısının yaklaşık değeri, 22/7 olarak da bilinir"))
satıryaz((22/7, 22/7.0, 3.14, "pi"))
```

Sıralamalar da birer nesnedir desem artık şaşırmazsın değil mi? Onun için nokta koyup arkasından içinden istediğimiz değere ulaşabiliriz. Ama bir sorun var. Bu iç değerlerin bir adı yok! Sorun değil. Sıralarını biliyoruz. Birinci elemanı okumak için `._1` dememiz yeter. İkinci için `._2` varsa üçüncü için `._3` vb... Hemen deneyelim.

```scala
satıryaz((3, 'c')._1)
satıryaz((3, 'c')._2)
```

Böyle birşey yapmak gereksiz geldi mi size de? Tabii genelde elimizde dez değişmezleri olur. Örneğin:

```scala
dez sıralıDörtlü = ("En sevdiğim sayı", 5, "karesi", 25)
satıryaz(sıralıDörtlü._1)
satıryaz(sıralıDörtlü._2)
satıryaz(sıralıDörtlü._3)
satıryaz(sıralıDörtlü._4)
```

```scala
dez sıralıDörtlü = ("En sevdiğim sayı", 5, "karesi", 25)
satıryaz(sıralıDörtlü._5)
```

Biraz fazla gittik! Hata verdi. "error: value _5 is not a member of (String, Int, String, Int)" yani 5. elemanı yok ki sıralamanın diyor bizim akıllı derleyici.

Bunları bilmekte fayda var ama esas sıralamaların içindeki değerleri toptan çözümlemeyle (tuple deconstruction) okumak çok daha yaygın ve faydalıdır. Örneğin:

```scala
dez (i, c) = (3, 'a')
satıryaz(i)
satıryaz(c)
```

Birazdan bir yazılımcık yazacağız ve onun içinde bu çözümleme yöntemi nasıl işe yarıyor göreceğiz. Diyelim ki bir yazının içinde hangi harften kaç tane var bulmak istiyoruz. İlk önce bir dizi sözcük oluşturalım bir tümceden. Bu hesaplamayı yapmak için uygulamayı düşündüğümüz yöntem şu: ilk önce harfleri büyültelim sonra da sıraya sokalım.

```scala
dez yazı = "Kojo ile oyun oynayarak Scala dilini öğrenmek ve hatta işlevsel ve nesneye yönelik yazılım becerisi edinmek harika değil mi "
dez sözcükDizini = yazı.böl(" ")
// Tek satırda epey iş var. Teker teker bak istersen
dez harfler = sözcükDizini.düzİşle(_.dizine).işle(_.büyükHarfe).sırayaSok(_ < _)
// Yani şöyle yapabilirsin:
dez g1 = sözcükDizini.düzİşle(_.dizine); satıryaz("g1", g1)
dez g2 = g1.işle(_.büyükHarfe); satıryaz("g2", g2)
satıryaz(harfler)
```

Ondan sonra da katlama (fold) yöntemini kullanarak arka arkaya tekrar eden harfleri sayıvereceğiz. Daha önce ne görmüştük? Katlama işlevi iki girdi alıyor: bir başlangıç değeri, bir de bir dizin, yani bizim sıraya sokulmuş harf dizimiz. Katlama ilk başlangıç değerini dizinin ilk elemanıyla bir işleme sokacak. Ne işlemi mi? Biz ne istersek o! Burada teker teker ele aldığımız harfler değişmedikçe sayısını bir artıracağımız bir sayacımız olacak. Yeni gelen harf değişik olursa yeni bir sayaç tanımlayacağız. Her sayaç tabii ki birden başlayacak. Karmaşık mı geldi biraz? Çok doğal. Görüp biraz üstünde düşününce daha anlaşılır olacak. Ne de olsa ileri yazılım tekniği bu!

Kısaca söylemek gerekirse amacımız ikili sıralamalardan oluşan bir dizin oluşturmak. Her ikilinin birinci alanında bir harf ikinci alanında da kaç tane olduğunu tutan sayacı olacak. Bu dizin harflerin sıklığını sunacak bize.

Şimdi de katlama yöntemimiz gelsin bakalım. Başladığımızda sunum boş olacak elbet. Nasıl tanımlarız istediğimiz boş dizini? `Dizin[(Harf, Sayı)]()` yani bir dizi (harf, sayaç) çifti. soldanKatla yöntemimiz ikinci girdi olarak ne bekler anımsadın mı? Bir işlev! Nasıl bir işlev gerekiyor biraz daha iyi tahmin edebilirsin belki şimdi. Adsız işlev olacak, bir. İki tane girdisi olacak, iki. İlk girdisi bizim çift dizinimiz, ikinci girdi de harflerden biri.

```scala
dez yazı = "Kojo ile oyun oynayarak Scala dilini öğrenmek ve hatta işlevsel ve nesneye yönelik yazılım becerisi edinmek harika değil mi "
dez sözcükDizini = yazı.böl(" ")
dez harfler = sözcükDizini.düzİşle(_.dizine).işle(_.büyükHarfe).sırayaSok(_ < _)

dez sıklık = harfler.soldanKatla(Dizin[(Harf, Sayı)]()) {
    durum ((önceki, sayaç) :: kuyruk, harf) eğer (önceki == harf) => (önceki, sayaç + 1) :: kuyruk
    durum (sunum, harf)                                         => (harf, 1) :: sunum
}
// Sonucu okunur bir halde yazalım bakalım ne bulduk:
satıryaz(sıklık.işle{p => s"${p._1}:${p._2}"}.yazıYap(" "))
```

Bunu anlayamadım diye üzülme sakın! Daha önce görmediğimiz bir kaç becerisi var Scala derleyicisinin burada!

1) adsız işlevimizi tanımlarken `durum` yani desen/örüntü eşleme yapısı kullanabiliriz. Bunun için normal parantez yerine kıvrık parantez kullanmamız yeter. `eşle` özel sözcüğüne gerek kalmadı. Ondan önce gelen değişmezlere de! Yani bu epey faydalı bir kısa yol oluyor ve bunu iyi bilmekte fayda var! Normal, yani kısaltılmamış halini anımsayalım hemen: `(a, b) => (a, b) eşle {durum ... => ...; durum ... => ...}`. eşle ve ondan önceki hiç birşeye gerek kalmıyor!

2) desen eşleme yani örüntülü eşleme yapmak için kullanmıştık bu `durum` yöntemini. Burada da `durum` sözcüğünden hemen sonra gelen kısımda elimizdeki iki girdiyi çözümlüyoruz. Biraz önce de dediğimiz gibi ilk girdi ufak ufak oluşturduğumuz yeni dizinimiz. `(önceki, sayaç) :: kuyruk` yeni kurduğumuz dizinin başı ve kuyruğuyla eşleşiyor ve onların üçüne de isim takıveriyor. kuyruk bariz. baş eleman da bir önceki harf ve ondan şu ana kadar kaç tane saydığımızı tutan sayaç. `kuyruk` değerinden sonra gelen `harf` ise katlama işlemini yaptığımız `harfler` dizinindeki harflerden biri. Katlama işlevi her harfin üstünden teker teker geçecek elbet.

3) ve son! İlk desen/örüntü eşleme satırında bir de koşul girdik `eğer` diyerek. Bu çok önemli. Yeni bir harfe geçip geçmediğimize dikkat etmemiz gerek! Eğer en son saydığımız harften aynısı geldiyse sayacı arttırmalıyız. Yoksa yeni bir sayaç başlatmalı.

Şimdi, bilgisayarın Scala derleyicisi sayesinde anladığı bu epey karmaşık görünen işlemi okuyalım bakalım daha iyi anlamış mıyız: esas girdimiz olan sıralanmış harfler dizisindeki her harf için teker teker şunu yapalım: eğer sıfırdan oluşturduğumuz sunum adlı yeni çift dizisinin başındaki çiftin harfi ile aynıysa, dizinin başını sayacın bir arttığı yeni bir başla değiştirelim. Yoksa, sunum dizisine elimizdeki harf için yeni bir baş ekleyelim ve harfin sayacını bire eşitleyelim. Çok da karmaşık değilmiş, öyle mi?

İki üç satırda ifade edebildiğimiz bu işlem hem epey düzgün hem de epey etkileyici bence. Ya sen ne dersin?

Elbette ki isteyen daha eski dillerde yapıldığı gibi yazabilir, Basic, Fortran, C, C++, Java örneğin. Yani:

```scala
tanım harfSıklığı(girdi: Dizin[Harf]): Dizin[(Harf, Sayı)] = {
    den sunum = Dizin[(Harf, Sayı)]()
    eğer (girdi.boşMu) sunum
    yoksa {
        den önceki = girdi.başı
        den kuyruk = girdi.kuyruğu
        den sayaç = 1
        yineleDoğruKaldıkça (!kuyruk.boşMu) {
            eğer (kuyruk.başı == önceki) sayaç += 1
            yoksa {
                sunum = (önceki, sayaç) :: sunum
                sayaç = 1
                önceki = kuyruk.başı
            }
            kuyruk = kuyruk.kuyruğu
        }
        (önceki, sayaç) :: sunum
    }
}

dez yazı = "Kojo ile oyun oynayarak Scala dilini öğrenmek ve hatta işlevsel ve nesneye yönelik yazılım becerisi edinmek harika değil mi "
dez sözcükDizini = yazı.böl(" ")
dez harfler = sözcükDizini.düzİşle(_.dizine).işle(_.büyükHarfe).sırayaSok(_ < _)
satıryaz(harfSıklığı(harfler))
```

Bu da çalıştı elbette. Ama bilmem sen hangisini daha çok beğendin. Bak biraz önce işlevsel yazılım esaslarını kullanarak nasıl yaptığımızı hepsi bir arada tekrar görelim ve iki farklı yöntemi yan yana karşılaştıralım:

```scala
dez harfSıklığı = "Kojo ile oyun oynayarak Scala dilini öğrenmek ve hatta işlevsel ve nesneye yönelik yazılım becerisi edinmek harika değil mi".
    böl(" ").düzİşle(_.dizine).işle(_.büyükHarfe).sırayaSok(_ < _).
    soldanKatla(Dizin[(Harf, Sayı)]()) {
        durum ((önceki, sayaç) :: kuyruk, harf) eğer (önceki == harf) => (önceki, sayaç + 1) :: kuyruk
        durum (sunum, harf)                                           => (harf, 1) :: sunum
    }

satıryaz(harfSıklığı.
    sırala(p => p._2).tersi.
    işle { p => s"${p._1}:${p._2}" }.
    yazıYap(" "))
```

Bilmem farkettin mi ama sadece iki komut dizisi yeterli oldu. Verilen cümlenin harflerinin sıklığını hesapladık ve sıraya sokup yazdırdık. Yukarıda adlarını saydığımız daha eski ve daha az becerikli dillerle bilgisayara istediğini yaptırmanın ne kadar zor olduğunu bilenlere bu kısacık yazılım o kadar etkileyici olur ki anlatamam!
