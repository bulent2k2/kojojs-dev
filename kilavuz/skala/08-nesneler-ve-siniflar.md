# Nesneler ve Sınıflar

## Herşey bir Nesne!

Scala nesneye yönelik bir yazılım dili olarak tasarlanmıştır. İngilizce'de Object Oriented Programming, hatta kısaca OOP diye bilinen bu kavramı destekleyen pek çok dil var. Bu kavramın temelinde nesneler yatıyor. Nesne ne demek biliriz elbet. Ama yazılım yazmaya gelince nesne ne demek? Herhangi bir anda programın içinde bulunduğu evre (state) nesnelerin yapısında saklanıyor demek. Bu ne demek? Programdaki nesnelere bakarak program hakkındaki herşeyi bilebiliriz, bir. İki, programın çalışması ve gerekli değişiklikleri yerine getirmesi için de içindeki nesnelerden bir kısmının değişmesini sağlamak yeter. Üçüncü önemli nokta da şu: nesnelerin değişmesi ancak nesnelerin üzerinde tanımlanmış yöntemleri kullanarak sağlanır. Kojo da sadece bir nesne kümesinden ibaret! Epey çok nesne var elbette Kojo yazılımının içinde. Bunlar düzenleyiciye yazdığımız komutları işleyerek kaplumbağayı hareket ettiriyor, çizgiler çizdiriyor ya da çıktı alanına yazılar yazıyor.

Temel türleri hatırlıyorsun, değil mi? Sayı türleri, yazı türleri... Yeni nesneler tanımlamak için de yeni bir tür tanımlarız ilk önce. Yani yeni nesnenin nasıl bir evre ve davranışları olduğunu belirleyen bir sınıf oluştururuz (sınıf ve tür çok benzer kavramlar). Sınıfın tanımı, nesnelerinin ne değişkenler içereceğini ve ne yöntemler (yani davranışlar) sunacağını belirler. Yöntem dediğimiz de aslında birer işlevdir ve nesnenin uzuvları gibidirler, nesnenin içindeki değişkenleri bilir, onlara göre davranır ve hatta o değişkenleri değiştirebilirler. Sınıfın içinde tanımlanan bu yöntemler de `tanım` anahtar komutu, ya da özel yazılım sözcüğü, kullanarak tanımlanırlar. Aynı daha önce gördüğümüz işlevler gibi. Yöntemler yöntem ya da arı işlev olabilirler. Sana ve ne yapmak istediğine bakar.

Yeni bir sınıf tanımladığında yeni tür bir ya da daha çok nesne tanımlamış oluyorsun. Bu yeni tür, daha önce tanımlanmış, hatta Scala'yla hazır gelen Sayı, Kesir gibi türlerin hepsiyle bir tutulur. Yani Scala hiç ayırım yapmaz! Scala'nın türler ve sınıflar arasında taraf tutmaması, yani herşeyin bir nesne ve her nesnenin de bir türü olması ne kadar yararlıdır yakında göreceğiz.

Haydi iki boyutlu bir nokta tanımı yaparak başlayalım yeni türler üretmeye! İki tane değişkeni olacak, x ve y. Sınıf adlarını da geleneksel olarak büyük harfle başlıyoruz, Sayı, Harf gibi temel türlerde olduğu gibi. Ama sınıf içindeki değerler, değişkenler ve yöntemler küçük harflerle yazılıyor. Bakın nesnenin tanımı şu kadar! Daha doğrusu sınıf belirlendi. Yeni bir tür yarattık bile. Bu sınıfın bir elemanı, yani bu türden yeni bir nesne oluşturmak da çok kolay. Sizi fazla şaşırtmayacak `yeni` anahtar sözcüğünü kullanıyoruz. Nesnenin iç değerlerini ele almak ya da değiştirmek için de nesnenin adına nokta yani `.` ekliyor arkasından da iç değişkenin adını yazıyoruz. İç değişkenlerin değerini okumak için de aynı şeyi yapıyoruz:

```scala
// Anahtar kelimemiz sınıf:
sınıf Nokta {
    den x = 0
    den y = 0
}
// x ve y için başlangıç değeri gerekli. Ve 0 mantıklı gibi.
// Ama daha da iyi bir çözüm bulacağız yakında..
dez n = yeni Nokta
n.x = 3
n.y = 4
satıryaz(n.x, n.y)
```

Bu biraz zahmetli oldu yalnız. Her yeni nokta için x ve y değerlerini bu şekilde belirlememiz fazla zamanımızı alıyor. Bunun daha kolay bir yolu var. Nesneyi oluştururken noktanın koordinatlarını girmek daha kullanışlı olurdu. Bunun için yapmamız gereken değişiklik küçük. Sonra da yeni bir nokta tanımlayalım:

```scala
sınıf Nokta(den x: Sayı, den y: Sayı)
dez n = yeni Nokta(3, 4)
satıryaz(n.x, n.y)
```

Bakın ilk nokta tanımda 0 değerini vererek x ve y'nin Sayı türünde olduğunu belirlemiştik aslında. İkinci tanımda ise tür bilgisini açık açık verdik. Mantıklı, değil mi? Derleyici ne yapması gerektiğini biliyor iki durumda da. Herneyse, bu biraz ileri bir yazılım kavramı oldu. Biz noktalara dönelim. Her nokta aslında iki boyutlu bir vektör demektir. İki noktayı toplamak demek iki vektörü birbirine eklemek demek. Nasıl yapacağız? Nokta sınıfına yeni bir yöntem eklemek en mantıklısı. Deneyelim. Bakın bu basit bir işlev ama sınıf tanımının içinde olması onu özel bir yöntem haline getiriyor. Neden özel? Çünkü nesnenin iç değerlerini okuyabiliyor ve iç değişkenlerini değiştirebiliyor. Bakın şimdi iki vektörü birbirine ekleyelim, bakalım ne bulacağız:

```scala
sınıf Nokta(den x:Sayı, den y:Sayı) {
    tanım vektörToplama(yeniNokta: Nokta): Nokta = {
        yeni Nokta(x + yeniNokta.x, y + yeniNokta.y)
    }
}
dez n1 = yeni Nokta(3,4)
dez n2 = yeni Nokta(7,1)
dez n3 = n1.vektörToplama(n2)
satıryaz(n3.x, n3.y)
```

Eğer Java yazılım yazmayı biliyorsan bu çok tanıdık gelecektir. Ama `n1+n2` yazmak çok daha doğal olurdu, değil mi? Scala ile mümkün! Gelin deneyelim. Hatta vektör çıkarmayı da tanımlayıverelim:

```scala
sınıf Nokta(den x: Sayı, den y:Sayı) yayar BaskınYazıyaYöntemiyle {
    tanım +(yeniNokta: Nokta): Nokta = {
        yeni Nokta(x + yeniNokta.x, y + yeniNokta.y)
    }
    tanım -(yeniNokta: Nokta): Nokta = {
       yeni Nokta(x - yeniNokta.x, y - yeniNokta.y)
    }
    baskın tanım yazıya = "Nokta("+x+", "+y+")"
}
dez n1 = yeni Nokta(3,4)
dez n2 = yeni Nokta(7,2)
dez n3 = yeni Nokta(-2,2)
dez n4 = n1+n2-n3
satıryaz(n4)
```

Bu örnekle iki özel/anahtar sözcükle tanışıyoruz. `yayar` ve `baskın`. İkincisi daha kolay. Onunla başlayalım: üstüne yazmak ya da yeniden tanımlamak anlamlarına geliyor. Neyin üzerine yazıyor ve tekrar tanımlıyoruz? `yazıya` adlı işlevi. Peki aslı nereden geliyor ki tekrar tanımlayalım? Hani dedik ya herşey bir nesne. Gerçekten de adı Nesne olan bir tür var ve aslında bütün türlerin temelini oluşturuyor. Bu temel sınıf ne demek daha sonraya bırakalım. Ama bilmemiz gereken şey şu: bu temel sınıfın `yazıya` adlı bir yöntemi var ve bu yöntemin her nesne için çalışması gerek. Bu yöntem her nesneyi okunabilecek bir yazı olarak ifade ediyor. Aslında o temel (ya da üst) sınıf İngilizce'yle yazılı. `yazıya` değil `toString` adında. Onun için şöyle de yazabiliriz:

```scala
dez n0 = yeni Nesne()
satıryaz(n0.yazıya)
// her tür kendisi için yazıya yöntemini yeniden tanımlıyor:
dez n1 = 9
satıryaz(n1.yazıya)

dez n2 = Dizin(9, 99)
satıryaz(n2.yazıya)
// bu 'durum sınıf' nedir birazdan göreceğiz!
durum sınıf AkıllıSayı(dez sayı: Sayı) /* yayar BaskınYazıyaYöntemiyle */ {
    baskın tanım toString = "Ben bir sayıyım. Değerim de " + sayı + "'dur."
    // baskın tanım yazıya = "Ben bir sayıyım. Değerim de " + sayı + "'dur."
}

dez n3 = AkıllıSayı(99)
satıryaz(n3.yazıya)
```

Çalıştı, değil mi? Şimdi AkıllıSayı türümüzü tanımladığımız satırdaki `/*` ve `*/` imgelerini silip tekrar dene! Derleyici bir hata verecek ve şöyle diyecek:

> `Error: class AkıllıSayı needs to be abstract. Missing implementation for member of trait BaskınYazıyaYöntemiyle: def yazıya: Yazı = ???`

Bu sorunu çözmek için de `toString` tanımını silelim ve altındaki `yazıya` tanımını etkinleştirelim:

```scala
durum sınıf AkıllıSayı(dez sayı: Sayı) yayar BaskınYazıyaYöntemiyle {
    baskın tanım yazıya = "Ben bir sayıyım. Değerim de " + sayı + "'dur."
}

dez n3 = AkıllıSayı(99)
satıryaz(n3)
// 'yazıya' yöntemini açık açık söylememiz bile gerekmiyor!
// Leb demeden leblebiyi anlıyor akıllı Scala derleyicisi 8-)
// ve gizlice 'yazıya' yöntemini çağırıyor ki 'satıryaz' doğru çalışsın.
```

Geldik `yayar` anahtar sözcüğüne. Bu nesne odaklı yazılımın ana konularından biri. [İki bölüm sonra](#b10) Ağaç ve Yaprak türleri oluştururken detaylıca inceleyeceğiz. Ama kısacası daha temel bir türün özelliklerini kullanan bir alt tür oluşturmak istersek `yayar` sözcüğünü kullanıyoruz. Buradaki temel tür BaskınYazıyaYöntemiyle. Onun sayesinde `toString` yerine Türkçesi olan `yazıya` yöntemini yeniden tanımlayınca herşey güzelce çalışıyor. Tam anlaması biraz zor olabilir. Hiç dert etme. Sırası gelince hemen anlayıvereceksin.

Bu çok daha okunaklı, değil mi? Scala ve Kojo'yla kocaman bir vektör cebiri (ya da tam Türkçesiyle yöney ölçülümü) yaratmak işten bile değil!

Yukarıda basit bir örneğini gördüğümüz `durum` anahtar sözcüğüyle sınıf tasarımı biraz daha kolaylaşıyor. Bu `durum` sözcüğü yazılımda önemli bir kavrama işaret ediyor. İleride göreceğimiz `eşle` anahtar sözcüğüyle beraber de kullanılıyor. İngilizce'de durum, olay ve olgu gibi anlamlara gelir. Kullandıkça daha iyi anlayacağız.

```scala
durum sınıf Nokta(x: Sayı, y: Sayı) {
    tanım +(yeniNokta: Nokta) = Nokta(x + yeniNokta.x, y + yeniNokta.y)
    tanım -(yeniNokta: Nokta) = Nokta(x - yeniNokta.x, y - yeniNokta.y)
}
dez n1 = Nokta(3, 4)
dez n2 = Nokta(7, 2)
dez n3 = Nokta(-2, 2)
satıryaz(n1+n2-n3)
```

Burada `durum` sayesinde `yeni` anahtar sözcüğüne gerek kalmadı. n1, n2 ve n3 nesnelerini yaratmak için sadece `Nokta(...)` yetti. Bu Scala derleyicisinin Java derleyicisinden daha becerikli olduğunu ortaya koyan örneklerden biri.

İki husus daha var üzerinde durmamızda fayda olan. 1. + ve - işlevleri tanımlarken `tanım` anahtar sözcüğünü kullandık ama eşittir işaretinden sonra kıvrık parantezlere gerek duymadık. Bunun nedeni basit. Sağ tarafta sadece bir tane deyiş ya da sadece bir tane komut varsa kıvrık parantezlere gerek kalmıyor. Bu sadece `durum sınıf` içinde değil bütün `tanım` işlevleri için geçerli. 2. + ve - yöntemlerinin tanımında çıktı türü belirtmeye de gerek duymadık. Bu Scala derleyicisinin çok faydalı becerilerinden biridir: tanımda kullanılan deyiş ve komutlara bakarak çıktı türünü kendisi belirleyiveriyor! Buna İngilizce'de özel bir ad takılmıştır: "type inference" yani tür çıkarımı.

Herşey bir nesne demiştik. Bunu anımsamakta fayda var. Peki o zaman neden Java gibi nesneye yönelik dillerdeki biçimde yazmadık:

```scala
durum sınıf Nokta(x: Sayı, y: Sayı) {
    tanım +(yeniNokta: Nokta) = Nokta(x + yeniNokta.x, y + yeniNokta.y)
    tanım -(yeniNokta: Nokta) = Nokta(x - yeniNokta.x, y - yeniNokta.y)
}
dez n1 = Nokta(3, 4)
dez n2 = Nokta(7, 2)
dez n3 = Nokta(-2, 2)
satıryaz(n1.+(n2.-(n3))) // n1.+(n2).-(n3) ?
```

Scala'nın sözdizimini kolaylaştıran ve kodun daha okunur hale gelmesini sağlayan becerilerinden biri sayesinde oldu. Parantezleri eklememize gerek kalmıyor çünkü derleyici onların nerede olması gerektiğini biliyor. Derleyicinin böyle becerikli olması bir tesadüf değil elbette. Scala DSL tasarımını desteklemeyi amaçlamış. DSL İngilizce Domain Specific Language sözcüklerinin baş harfleri. Değişik uğraş alanlarının kendine özgü terimleri var elbet. Onları Scala kullanarak tanımlayıp uzmanlarının kolaylıkla kullanmalarını sağlayabiliyor uzman Scala mühendisleri. Şimdilik bilgin olsun. İlerde örneklerini gördükçe daha anlamlı olacak bu bahsettiklerimiz.

Sayılar da herşey gibi birer nesne. Ayrıca matematik işlemleri de özel değil. Onlar da hep bir nesne türü üzerinde tanımlı birer işlev yani yöntem. Örneğin + yöntemini 1 sayısı üzerinde kullanmanın ikinci bir yolu da nesne adı ve sonra nokta koyup yöntem adını eklemek. Scala için `+`, `-`, `*` ve `/` gibi matematik işlemlerinin hepsi birer yöntem:

```scala
satıryaz(1.+(2))
```

Son kere anımsatalım: Scala'da herşey bir nesne ve herşeyin bir türü (ya da sınıfı) var. Bütün işlevler de bir yöntem ve hep bir nesne ve türe bağlı.

Bu arada son yazdığımız satıra dikkat. Scala derleyicisi `1.` deyişini `1.0` yani kesirli bir sayı sanmadı. Yoksa, bir çift parantez daha gerekecekti ve şöyle yazacaktık:

```scala
satıryaz((1).+(2))
```

Tabii bu da hala çalışıyor ama böyle yazmak zorunda kalmamamız iyi birşey. Scala'nın daha yaşlı versiyonları bu kadar akıllı değillerdi sanıyorum. Hatta daha hala pek çok eski dil var ki bu konuda kafaları karışık olabiliyor. Bilmende fayda olabilir.
