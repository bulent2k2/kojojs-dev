# İleri Eşleme Yöntemleri: İkil Ağaç

Şimdiki örneğimizin adı ikil ağaç (binary tree). Neden böyle deniyor çok yakında anlayacağız. Ağaç diye adlandırdığımız veri yapılarının bilgisayar mühendisliğinde önemi büyük. Çünkü verileri saklamak ve hızlı bulmak için bire birler. İlk önce veri ile ne demek istiyoruz onu görelim. Pek çok tür veri olabilir elbet. Çok yaygın olan anahtar/değer çiftleri bu örneğimizde bizim işimizi görecek. Örneğin `("a" -> 30)` "a" anahtarıyla saklayıp sonra da arayıp bulacağımız 30 değeri. Ağacın tasarımında iki gereksinim olacak: 1) ağacın çatallanarak dallanmasını sağlamak, 2) ağacın yapraklarıyla verileri saklamak. Unutmayalım ki arama işleminin çabuk çalışmasını istiyoruz. Bunun için de her seferinde bütün yapraklara bakmak yerine daha verimli bir düzen istiyoruz. Arama işlemini yapacak elbette bir işlevimiz olacak. Ağacı gezerken anahtara göre hangi dala gitmesi faydalı olur nasıl bilebilir bu işlev? Düşündüğünden kolay olacak. Bak gör! Çatalların özelliği iki dala ayrılmaları (onun için ikil dedik!) ve de bir anahtar tutmaları. Diyelim ki sağ daldaki veriler sözlükte hep bu anahtardan önce gelsin. O zaman daha sonra gelen bir anahtar arıyorsak sadece sol dala bakmamız yeter! O sayede arama hızlı çalışacak. Yapraklara gelince, durum daha basit. Her yaprak sadece bir anahtar ve onunla saklanan değeri tutacak (boş yere anahtar/değer çifti demedik).

```scala
/*
 *           [b]
 *          /  \
 *        [a]  c,20
 *       /  \
 *     a,30  b,10
 */
```

İlk görevimiz çatal ve yaprak türlerini tanımlamak. Şimdi sorun şu. Her çatal iki dala ayrılıyor ama her iki dal da ya başka bir çatal olabilir ya da bir yaprak olabilir! Yani çatalın iki çocuğu olmalı ve bu çocuklar sadece bu iki türden biri olmalılar, ya bir çatal ya da bir yaprak, ama başka da hiç bir tür olmamalılar. Yanlışlıkla bir sayı, yazı ya da dizin olursa başımız derde girer (buna type unsafe, yani tür güvensizliği derler ve eski çağlarda bilgisayar programcılarını çok terletmiştir). Scala bu zorluğu sınıf hiyerarşisi dediğimiz yöntemle çözer. İlk önce genel bir ağaç türü ya da sınıfı oluşturalım sonra da buna iki tane alt tür ekleyelim. Bakın nasıl da kolay:

```scala
sınıf Ağaç
durum sınıf Çatal(anahtar: Yazı, sol: Ağaç, sağ: Ağaç) yayar Ağaç
durum sınıf Yaprak(anahtar: Yazı, değer: Sayı) yayar Ağaç
dez ağaç1: Ağaç = Yaprak("c", 24)
satıryaz(ağaç1)
```

Çatal ve Yaprak için Ağaç türünün alt türü deriz. Bunu `yayar` özel sözcüğüyle belirliyoruz. Ağaç da üst tür olarak bilinecek bundan sonra. Çatal ve Yaprak alt türlerine ataları olan Ağaç üst türünün bütün özellikleri miras kalır, hem de Ağaç daha ölmeden! :-) Bakın çok ilginç birşey daha gördük hemen: Bir değerin türü Ağaç olsun dedik ama onun gerçek değeri bir Yaprak. Bu nasıl oluyor? Yaprak Ağaç'ın uzantısıydı ya. O sayede Ağaç deyince genel olarak ya Çatal ya da Yaprak demiş oluyoruz. Bu sayede Çatal'ın tanımındaki sol ve sağ değerlerinin türü neden Ağaç oldu anladık değil mi? Ama tersini yapamayız ona göre. Neden? Çünkü Yaprak dedik mi artık yeterince özelleşmiş oluyor ve anahtar ve değeri olarak bir sayı gerekiyor. Ne Ağaç ne de Çatal'da bir sayı yok, değil mi?

Şimdi en başta bahsettiğimiz arama işlevine geldi sıra. Desen eşleme değil mi bu kısmın adı? Bakın bu işlev ağacımızı alacak ve özyineleme yöntemiyle verimli arama işlemini gerçekleştirecek. Yani bir çatala geldiğinde kendi kendini sağ ya da sol küçük ağaç ile tekrar çağıracak. Ama eğer bir yaprak görürse elbette yineleme duracak. Desen nerede o zaman? Küçük ağaç bir çatal mı yoksa yaprak mı onu belirleyecek desenlerimiz (ya da örüntülerimiz). Çok lafa gerek yok. Yazılımcık yalın ve kendi kendini anlatıveriyor:

```scala
sınıf Ağaç
durum sınıf Çatal(anahtar: Yazı, sol: Ağaç, sağ: Ağaç) yayar Ağaç
durum sınıf Yaprak(anahtar: Yazı, değer: Sayı) yayar Ağaç

tanım bul(ağaç: Ağaç, ne: Yazı): Sayı = {
  ağaç eşle {
    durum Yaprak(a, değer)   => eğer (a == ne) değer yoksa 0
    durum Çatal(a, sol, sağ) => bul((eğer (a >= ne) sol yoksa sağ), ne)
  }
}

// Bir örnek gerek. İkil ağacımız şöyle olsun:
dez ağaç = Çatal("b",
  Çatal("a", Yaprak("a", 30), Yaprak("b", 10)),
  Yaprak("c", 20))
/*
 *           [b]
 *          /  \
 *        [a]  c,20
 *       /  \
 *     a,30  b,10
 */

// 'durum sınıf' sayesinde Çatal ve Yaprak nesnelerini kolayca tanımladık.
// Haydi şimdi yapalım bir iki arama:
satıryaz(bul(ağaç, "a"))  // 30
satıryaz(bul(ağaç, "c"))  // 20
```

Bu kadarla kalmaz elbet. Ağaca yeni anahtar/değer çiftleri eklemek için de bir işlev iyi olur. Ha, bir de yaprağı koparmak gerekebilir. Bütün bu işlevleri yeni bir İkilAğaç sınıfı tanımlayıp içine koymaya ne dersin? Onu sana bırakıyorum. Biraz düşün, Kojo'da birşeyler yazıp çiziştir bakalım. Çok daha iyi öğreneceksin o sayede. Kolay gelsin!

Bu arada desenler/örüntüler yukarda gördüğümüz gibi değişken olmak zorunda değil. Yalın bir değer de kullanabiliriz örüntü olarak. [Bir önceki bölümde](#b09) de görmüştük hani sayıdan yazıya ve tersini yaparken. Bir örnek daha verelim yine de. Diyelim ki (c -> 20) çiftinin bulunmasını istemiyoruz. Nedense. Bakın nasıl kolay:

```scala
sınıf Ağaç
durum sınıf Çatal(anahtar: Yazı, sol: Ağaç, sağ: Ağaç) yayar Ağaç
durum sınıf Yaprak(anahtar: Yazı, değer: Sayı) yayar Ağaç

dez ağaç = Çatal("b",
  Çatal("a", Yaprak("a", 30), Yaprak("b", 10)),
  Yaprak("c", 20))

tanım bul2(ağaç: Ağaç, ne: Yazı): Sayı = {
  ağaç eşle {
    durum Çatal(a, sol, sağ) => bul2(eğer (a >= ne) sol yoksa sağ, ne)
    durum Yaprak("c", _)     => 0
    durum Yaprak(a, değer)   => eğer (a == ne) değer yoksa 0
  }
}

satıryaz(bul2(ağaç, "c"))  // 0
satıryaz(bul2(ağaç, "a"))  // 30
```

Burada yine `_` imini joker gibi kullanarak bütün değerlerle eşleşmesini sağladık. Ayrıca bilelim ki bu eşleştirme işlemi yukarıdan aşağıya sırayla gidiyor. İlk eşleşme ile iş bitiyor. Bariz tabii ama yine de benden söylemesi.

Burada örneğini gördüğümüz sınıf hiyerarşisi ve alt türlerin üst türü uzatması, OOP, yani nesneye yönelik yazılımdaki en temel kavramlardan. Desen eşleme yöntemi sayesinde alt türleri birbirinden ayırıp gereğini yapabiliyoruz. Ne dahice, değil mi?!
