# İşlevler de Birer Nesnedir

## İşlevler de Birer Nesnedir

Scala dilinde herşey bir nesnedir demiştik. İşlevler de aynen öyle! Bir işlevi başka işlevlere girdi yapabiliriz. Bir işlevin çıktısı da bir işlev olabilir. Ayrıca değişkenlerin ya da değişmezlerin değeri de bir işlevin kendisi olabilir. Bunların örneklerini biraz sonra göreceğiz. Bu özellikler Scala dilinin çok faydalı bir becerisidir. Karşımıza sık sık çıkan bazı zorlukları çok kısa ve güzel bir şekilde çözmemizi sağlarlar. Bunlar arasında program akışını yönlendirme teknikleri de var. Örneğin, Scala dilinde yazılmış eski adı "Actors" yeni adıyla "Akka" birimi (İngilizce'de module ya da library denen ve başka yazılımlar tarafından kullanılan bir yazılım bütününe birim denir) işlevleri nesne gibi kullanarak eşzamanlı işlevlerin yazılımlarını kolaylaştırır. Ama biz dizinlerin kullanılmasıyla başlayalım. Bakın göreceksiniz işlevlerin nesne olarak kullanılmasına güzel bir giriş olacak.

Nesneleri bir dizi olarak ele almak çok doğal ve faydalı, değil mi? Ne tür örnekler geliyor senin aklına? Her sözcük, örneğin, bir dizi harften oluşur. Gün içerisinde yaptığımız şeyleri bir dizi eylem olarak düşünebiliriz. Scala dili Dizin adını verdiğimiz bir tür tanımlamıştır. Bunu daha önce pek görmedik. Ama aslında çok basit bir kavram. Dizin türü bir dizi nesneyi ele almayı ve işlemeyi çok kolaylaştırır ve hemen hemen her yazılımcık ve daha büyük yazılımlarda sık sık kullanılır. Dizin içindeki nesnelerin belli bir sırası vardır. Dizin türünün sunduğu pek çok yöntem ve komut sayesinde Dizin tanımlamak ve işlemek kolaylaşır. Daha önce Nokta sınıfıyla gördüğümüz gibi Dizin oluşturmanın bir yolu Dizin sınıfının yapıcı yöntemini (constructor) kullanmak. Bu yapıcı yönteme bir ya da daha çok girdi sokarız ve yapıcı o girdilerin oluşturduğu bir Dizin yapıverir. İlk örneğimizle hemen başlayalım. Bir dizi sayı oluşturalım. Bu dizinin türü `Dizin[Sayı]` olacak. Buradaki köşeli parantezler dizinin içindeki elemanların türünü belirliyor, yani Sayı. Her eleman bir Sayı olduğu için Scala tür çıkarımı yaparak `dzn` değişmezinin türünü Dizin[Sayı] olarak belirler. Yani bizim bunu açık açık yazmak için zahmet etmemiz gerekmez.

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
satıryaz(dzn)
```

Tamam, şimdi elimizde bir dizin var. Dizinleri kullanmanın üç temel yöntemi var: `başı`, `kuyruğu` ve `::`. Her dizinin bir başı var. `başı` yöntemi bize ilk elemanı verir. `kuyruğu` yöntemi, dizinin başı hariç diğer elemanlarından oluşan kısmını verir. Ve son olarak da çift iki nokta üstüste yani `::` bir dizinin başına yeni bir eleman, yani yeni bir baş ekler. Bu baş ve kuyruk deyişi sana yılanları anımsattıysa haklısın. Dizinler yılanlara benziyor: hep başından tutmakta fayda var! Şaka bir yana Dizin türü çok gelişmiştir ve daha pek çok kullanışlı yöntemi vardır. [Dizinlerin Kullanılışı](#b17) bölümünde daha pek çok Dizin yöntemi göreceğiz.

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
// 'başı' ilk yani en soldaki elemanı verir. dzn örneğinde bu '1' olacak:
satıryaz(dzn.başı)
// 'kuyruğu' başı yani ilk elemanı atlar ve dizinin gerisini verir:
satıryaz(dzn.kuyruğu)
// '::' soldaki elemanı sağdaki dizinin başına ekleyerek oluşan yeni dizini verir:
satıryaz(23 :: dzn)
```

Burada önemli bir gözlem yapalım. Bu yöntemler dzn dizisini değiştirmez! Hep yeni bir Dizin üretirler. Bunun için Dizin türüne değişmez (immutable) bir veri yapısı da denir.

Bu üç temel yöntemle aklına gelen her dizini tanımlayabilir ve dizinlerle yapılabilecek ne varsa yapabilirsin. Bir örnek olarak gel bizim örnek dizinimizin içindeki tek sayıları bulalım:

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
tanım tek(girdi: Dizin[Sayı]): Dizin[Sayı] = {
    eğer (girdi == Boş) Boş
    yoksa eğer (girdi.başı % 2 == 1) girdi.başı :: tek(girdi.kuyruğu)
    yoksa tek(girdi.kuyruğu)
}
satıryaz(tek(dzn))
```

Farkettin mi? Bu işlev dizinin başına bakar ve duruma göre kuyrukla özyineler, yani kendi kendini çağırır. Bunu yaparken `kuyruğu` yöntemini kullanır ve bu sayede dizinin elemanlarını teker teker ele alır. `Boş` özel bir değer ve içi boş olan diziyi belirtiyor. Unutmadan, içi boş olan tek dizi var aynı yegane boş küme gibi. Boş yerine şöyle de yazabilirdik: `Dizin[Sayı]()`. Neyse ki Boş tanımlanmış. Daha kısa ve anlaşılır oldu değil mi? Dizinin sonuna gelince özyineleme son bulur ve tek sayılardan oluşan yeni bir Dizin çıkar ortaya.

Ne kadar yalın bir çözüm, değil mi? Tek yerine çift sayıları bulmak da artık çok kolay:

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
tanım çift(girdi: Dizin[Sayı]): Dizin[Sayı] = {
    eğer (girdi == Boş) Boş
    yoksa eğer (girdi.başı % 2 == 0) girdi.başı :: çift(girdi.kuyruğu)
    yoksa çift(girdi.kuyruğu)
}
satıryaz(çift(dzn))
```

Ama, dur, burada epey bir tekrarlama oldu. Görüyor musun? `tek` ve `çift` işlevleri neredeyse aynı. İkisi arasındaki farklılıkları bulalım: Elbette işlevlerin adı farklı. Tek ve çift. Onun dışındaki tek fark eleme için kullandığımız koşul. `==` iminin sağında 0 ya da 1 var. Bu farkı işlevin içinden çekip çıkarsak ortaya daha genel ve daha sade bir çözüm çıkıverecek! Onun için tek mi, çift mi sorusunu bir girdi olarak düşünelim. Bu eleme koşulunu bir işlev olarak tanımlamak çok basit:

```scala
tanım tekMi(s: Sayı): İkil = s % 2 == 1
// % imini anımsadın umarım. Burada sayıyı ikiye bölüp kalanı buluyor.
satıryaz(tekMi(13))
satıryaz(tekMi(24))
```

Bu arada tekMi adlı işlevin çıktı türünü `İkil` diye açık açık belirtmeye gerek yok aslında. Sadece Türkçesini görmen ve anımsaman için yazdım. Neyse, biz bu işlevi şimdi nesne olarak nasıl kullanacağız onu görelim. Bunun için yeni bir işlev tanımlayacağız. Adı elemek sözcüğünün kökü olsun. Elemanları eleyerek bize istediğimiz elemanları versin. Ama neye göre eleyecek? Tabii ki yukarıdaki tekMi işlevi gibi bir işlevi kullanarak! Nasıl kullanacak? Yeni bir girdi olarak! İşte başta da bahsettiğimiz konuya geldik. Bir işlev başka bir işleve girdi olacak şimdi.

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
tanım tekMi(s: Sayı): İkil = s % 2 == 1
tanım ele(girdi: Dizin[Sayı], koşul: (Sayı) => İkil): Dizin[Sayı] = {
    eğer (girdi == Boş) Boş
    yoksa eğer (koşul(girdi.başı)) girdi.başı :: ele(girdi.kuyruğu, koşul)
    yoksa ele(girdi.kuyruğu, koşul)
}
satıryaz(ele(dzn, tekMi))
```

`ele` işlevinin ilk girdisinin adı girdi ve bu bizim sayı dizimiz. İkinci argüman ise eleme koşulumuz, yani yeni tanımladığımız işlev. Türünü nasıl yazdık farkedelim: koşulun türü bir sayıyı girdi olarak alıp bir İkil veren, yani koşul sağlanıyorsa doğru yoksa da yanlış diyen bir işlev. `ele` adlı yöntemimiz her işlevi girdi olarak kabul etmez. Sadece Sayı alıp İkil veren işlev türü ile çalışır. `ele` yönteminin içinde de `koşul` adlı değeri aynı diğer işlevler gibi kullandık. Bu ele adlı işlevi daha önceki tek ve çift işlevleriyle karşılaştırmanı öneririm. Genellemenin nasıl yapıldığını iyice özümsemende fayda var. Bu genelleme günlük hayattaki istisnası bol onun için de bizi sık sık yanıltan genellemelere benzemez. Zararı yok sayılır (ikinci bir girdi girmemiz gerekiyor, yani zahmeti biraz arttı) ama kazancı çok. Birazdan bir kaç örnek daha görünce daha iyi anlayacaksın. Bu arada, eğer çok istersek bu zararı iyice yok edebiliriz. Örneğin, bu işlevi sık sık tek sayıları bulmak için kullanacaksak, şöyle yapalım ki iyice kolaylaşsın kullanımı:

```scala
tanım tekMi(s: Sayı): İkil = s % 2 == 1
tanım ele(girdi: Dizin[Sayı], koşul: (Sayı) => İkil = tekMi): Dizin[Sayı] = {
    eğer (girdi == Boş) Boş
    yoksa eğer (koşul(girdi.başı)) girdi.başı :: ele(girdi.kuyruğu, koşul)
    yoksa ele(girdi.kuyruğu, koşul)
}
dez ilkOnSayı = Aralık(1, 11).dizine
satıryaz(ele(ilkOnSayı))
```

Bak bu sayede, eğer işlev girdisi vermezsek, ele adlı işlev tekMi koşulunu kullanacak. Yani varsayılan koşul tekMi ile ifade ettiğimiz tek sayı olmak olacak. Aynı `tek` adını verdiğimiz işlev gibi çalıştı, değil mi? Ama, ele adlı işlevimiz hala daha genel. İstersek ikinci argüman olarak başka bir koşul girer, başka tür bir eleme yaparız.

Daha önce görmediğimiz ama çok faydalı olan bu Aralık türünün İngilizce adı 'Range'. Bir Aralık yazmak yerine kısa bir yolu var: `(1 |- 11).dizine`. İngilizce olarak '1 until 11' yazılır. Ya da onun yerine `1 |-| 10`, İngilizce'si: '1 to 10' da yazabilirdik. Bunların daha genel hali de var: `a |- b adım c` ve 'a until b by c' veya `a |-| b adım c` ve 'a to b by c' yani a sayısından b sayısına kadar c adımlarıyla sayıyoruz ama `|-` ya da 'until' olursa b sayısından hemen önce duruyoruz. Örneğin:

```scala
satıryaz(3 |- 22 adım 3)
// Uzun ve işlev haliyle:
satıryaz(Aralık(3, 22, 3))
// Geri geri de gidebiliriz:
satıryaz(30 |-| -35 adım -5)
satıryaz(Aralık(30, -36, -5))
satıryaz(Aralık.kapalı(30, -35, -5))
// Aralığın sonuna dizine yöntemini ekle bakalım ne olacak. İngilizcesi toList:
satıryaz((1 |- 11).dizine)
```

Çift sayıları bulmak için de benzer bir yöntem kullanabiliriz elbet. Yani çiftMi diye yeni bir işlev tanımlar ve sonra kullanırız. Ama adsız (anonymous) bir işlev kullanarak çok daha kısaca ifade edebiliriz dileğimizi. Ne demek adsız? Yani `tanım` özel sözcüğünü kullanmadan ve işlevin adını vermeden bir işlev tanımlayacağız. Bu işlev basitçe girdiyi alacak ve onu değerlendirecek. Bu özel yöntemde `=>` imini kullanıyoruz. Solunda girdiler, sağında da işlevin eylemleri gelecek.

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
tanım ele(girdi: Dizin[Sayı], koşul: (Sayı) => İkil): Dizin[Sayı] = {
    eğer (girdi == Boş) Boş
    yoksa eğer (koşul(girdi.başı)) girdi.başı :: ele(girdi.kuyruğu, koşul)
    yoksa ele(girdi.kuyruğu, koşul)
}
satıryaz(ele(dzn, (v: Sayı) => v % 2 == 0))
```

`ele` komutunu böyle çağırarak çift sayıları buluverdik. Yeni bir çiftMi işlevi tanımlamamıza gerek kalmadı. Adsız işlevler hep böyle kullanılır ve işimizi çok kolaylaştırırlar. O kadar faydalılar ki özel bir adları ve matematik ve bilgisayar biliminde özel bir yerleri bile var. 'lambda kalkülüsü' (lambda calculus) diye bilinir. Bilgisayar bilimi okumak istersen ilerde görür ve daha iyi öğrenirsin.

### Genelleyici Programlamanın Tadına Bakalım

Şimdi diyelim ki yeni bir eleme koşulu tanımlamak istiyorsun, çünkü tam sayılarla yetinmek olmaz, kesirli sayıları da herhangi bir koşula göre eleyebilmek istiyorsun. Yukarıda tanımladığımız kısacık `ele` işlevini kopyalar ve basitçe her gördüğün 'Sayı' sözcüğünü 'Kesir' sözcüğüyle değiştirirsin olur biter. Sonra da adını eleKesir gibi birşey koyarsın. Tabii Kesir de yetmez. Başka her hangi bir tür için eleHerneyse dersin ve bu sayede birbirine çok benzeyen, daha doğrusu neredeyse apaynı bir sürü işlevin olur: ele, eleKesir, eleYazı, vb.. Ne güzel, değil mi? Hiç de güzel değil! Çok tekrar oldu. Nerede çokluk orada çok ayıp...

Scala'yla yazarken bu kadar tekrara, bu zahmete girmeye hiç gerek yok. İşi iyi bilenlerin yaptığı gibi bir 'tür değişkeni' (type variable ya da generic type diye de bilinir) kullanarak genel bir çözüm sayesinde işler kolaylaşıverir. Tür değişkenleri diğer `dez` değişmezlerine benzer. Gerçek değer yerine değişkenin adını kullanırız her yerde. Sonra Scala derleyicisi gerekli yerleştirmeleri yapıverir.

Aşağıda `T` harfini kullanacağız Tür anlamında. Ama başka her hangi bir harf ya da sözcük de olur elbette. Nerede Sayı varsa onun yerine T gelecek. Bir de işlevin adının hemen arkasında `[T]` gelecek. Bu da yeni işlevimizin genelleyici bir işlev olduğunu belirtiyor. Peki o halde yeni halini görelim:

```scala
tanım ele[T](girdi: Dizin[T], koşul: (T) => İkil): Dizin[T] = {
    eğer (girdi == Boş) Boş
    yoksa eğer (koşul(girdi.başı)) girdi.başı :: ele(girdi.kuyruğu, koşul)
    yoksa ele(girdi.kuyruğu, koşul)
}
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
satıryaz(ele(dzn, (v: Sayı) => v % 2 == 0))

// Aynı eskisi gibi çalıştı! Ama işin iyi tarafı çok daha genel oldu bu haliyle.
// Bak şimdi kesirli sayılar arasından sadece beşten büyük olanları seçmek de çok kolay:
dez kesirler = Dizin(
    1.5, 7.4, 2.3,
    8.1, 5.6, 6.2,
    3.5, 9.2, 14.6,
    12.91, 4.23, 10.04)
satıryaz(ele(kesirler, (v: Kesir) => v > 5))

// Ya da bir cümle içindeki uzunca sözcükleri seçmek için de kullanabiliriz
// aynı genellenmiş ele adlı işlevimizi:
dez dizinler = Dizin(
    "Bugün", "sizi",
    "gördüğüme", "çok",
    "memnun", "oldum",
    "Nasılsınız")
satıryaz(ele(dizinler, (v: Yazı) => v.boyu > 4))
```

Yazı türünün `boyu` adlı yöntemi yazıda kaç harf olduğunu gösterir. Dizinlerle ilgili bölümde de göreceğimiz gibi Dizin türünün `ele` adlı bir yöntemi var. Onu kullanarak da benzer eleme işlemlerini yapabiliriz:

```scala
dez dizinler = Dizin("Bugün", "sizi", "gördüğüme", "çok", "memnun", "oldum", "Nasılsınız")
satıryaz(dizinler.ele((v: Yazı) => v.boyu < 5))
```

Bu genelleyici işlevlere daha sonra yine bakacağız ve başka özelliklerini keşfedeceğiz. Ama şimdilik konumuza dönelim ve işlevleri nesne olarak kullanmaya bakalım.

### 'İşlevler De Birer Nesnedir' Hakkında Başka Birkaç Şey Daha

Biraz önce örneklerini gördüğümüz yönteme 'kapsamalar' (comprehensions) deniyor. Yani, dizin gibi bir sürü elemandan oluşan bir kümenin bütün elemanlarına bir işlevi uygulayıvermek. Kapsamaların ne kadar becerikli olduklarını ve onlar sayesinde kısacık ama çok iş beceren yazılımlar yapabildiğimizi bir kaç örnek daha görerek pekiştireceğiz şimdi.

Yazılımcıklar yazdıkça, bazı çok basit ve çoğunlukla adı olmayan işlevleri başka işlevlere girdi yapmak isteyeceksin. O durumlarda bazı kısa yollar bilmende fayda olacak. Daha önce de üzerinde durduğumuz gibi tür çıkarımı bize fayda sağlıyor. Bakın bir önceki `dizinler.ele` örneğini tür çıkarımı sayesinde sadeleştirelim. Derleyici adsız işlevin girdisinin Yazı türünde olması gerektiğini biliyor (nasıl biliyor? Adı dizinler olan dez değerinin türünden!). O sayede parantezleri ve tür bilgisini yazmaktan kurtuluyoruz.

Bu adsız işlev gibi iki ve ya bir girdili işlevler çok yaygın olduğu için, Scala dili bize bir kısa yol daha sunuyor. `(x, y) => x + y` adsız işlevini şöyle yazabiliyoruz: `_ + _`. Benzer şekilde `s => s.yöntem` yerine `_.yöntem` da yazabiliriz. `_` imi girdi yerine kullanılıyor yani. O sayede adsız işlevler içinde isimler uydurmak ve kullanmaktan kurtuluyoruz. Bu sayede, son örneğin en kısa halini görelim:

```scala
dez dizinler = Dizin("Bugün", "sizi", "gördüğüme", "çok", "memnun", "oldum", "Nasılsınız")
satıryaz(dizinler.ele(v => v.boyu > 3))
satıryaz(dizinler.ele(_.boyu > 3))
```

Başkalarının yazdığı kodları okumak çok faydalıdır. O durumlarda burada gördüğümüz kısa yolları görünce artık şaşırmazsın. Ama bazı durumlarda uzun ve açık hallerini kullanmak gerekir. Onları da sonra göreceğiz.

Bir kaç tane daha yaygın kullanımı olan örnekler görelim. Bunlar dizinleri işlemekte çok faydalı olurlar. Sen de yakında göreceksin.

#### düzİşle (flatMap)

```scala
dez dizinler = Dizin("Bugün", "sizi", "gördüğüme", "çok", "memnun", "oldum", "Nasılsınız")
satıryaz(dizinler.düzİşle(_.dizine))
satıryaz(dizinler.işle(_.dizine))
```

Flat İngilizce'de kat kat olmayan yani düz anlamında kullanılır. Bu örnekte bizim yazı dizinimizi aldık, içindeki her bir yazıyı ilk önce `dizine` yöntemini kullanarak birer harf dizisine çevirdik ve onların hepsini birleştirdik. Sonucunu gördün, değil mi? `düzİşle` yerine `işle` (map) yöntemini kullanarak aradaki farkı daha iyi anlarsın (ikinci satır). map İngilizce'de hem harita hem de eşlemek anlamlarına gelir. Ama matematikte ve bilgisayar biliminde yeni bir anlam kazanmıştır. Bir çerçeveden başka bir çerçeveye geçiş, birinci çerçeveyi işleyerek ikinci çerçeveye varış gibi anlamlara gelir. Ama anlatması zor. Nasıl çalıştığını görüp anlayıverelim.

#### sıralamak (sort)

Sort da sıraya dizmek anlamına gelir. `sırayaSok` (sortWith) yöntemiyle kendi girdiğimiz bir koşul ile sıralıyoruz. Sözcükleri a'dan z'ye sıralayalım, bir de ters sıraya sokalım:

```scala
dez dizinler = Dizin("Bugün", "sizi", "gördüğüme", "çok", "memnun", "oldum", "Nasılsınız")
satıryaz(dizinler.sırayaSok(_ < _))
satıryaz(dizinler.sırayaSok(_ > _))
// Farkettiysen büyük harfliler başta geliyor. Onun yerine harflerin büyük
// küçük olduğuna bakmadan sıralamak istersek şöyle yaparız:
satıryaz(dizinler.sırayaSok(_.büyükHarfe < _.büyükHarfe))
```

Yani hepsini büyük harfe çeviriverdik karşılaştırmadan önce. İstediğimiz koşulu girerek istediğimiz şekilde sıralama yapıverdik. Yani sıralama yöntemini baştan yazmamız gerekmedi. İşte işlevlerin nesne olmasının faydaları!

#### katlama (fold)

Soldan ve sağdan katlama bir dizinin elemanlarını bir araya getirmekte kullanılan çok yaygın ve faydalı yöntemlerdir. Elemanları nasıl bir araya getirmek istediğimizi iki girdi alan bir işlev girerek belirtiriz. Bu birleştirme işlemi soldan ya da sağdan başlar. Ve başlarken de yine girdiğimiz bir değer kullanır. Yani iki tane girdisi var bu katlama yöntemlerinin. Bu iki girdiyi daha kolay okunsun diye iki parantez grubuyla gireriz. Birazdan bunu `tanım` komuduyla işlevi tanımlarken nasıl yapıldığını göreceğiz. Yeni bir örnekle başlayalım:

```scala
dez dzn = Dizin(1, 7, 2, 8, 5, 6, 3, 9, 14, 12, 4, 10)
satıryaz(dzn.soldanKatla(0)(_ + _))
```

Soldan işleyeceğiz. İkinci girdimiz adsız bir işlev ve iki sayıyı topluyor: `_+_`. Bunun kapsama olduğunu da biliyoruz, yani dizinin içindeki bütün elemanların üstünden geçecek. İlk önce ilk girdisi olan 0 ile dizinin başındaki 1'i toplar ve 1 bulur. Sonra ona ikinci eleman olan 7'yi ekler ve böylece sonuna kadar gider.

#### Tekrarlardan Kurtulalım

Diyelim ki bir cümlemiz var ve içinde hangi harfleri kullandığımızı bulmak istiyoruz. Örneğin:

```scala
dez dilek = Dizin("Haydi", "gelin", "bir",
    "oyun", "oynayalım", "hep", "beraber")
satıryaz(dilek.düzİşle(_.dizine).işle(_.büyükHarfe).yinelemesiz.sırayaSok(_ < _))
```

`düzİşle` sözcüklerin bütün harflerini tek bir dizine sokuyor. Daha önce de gördüğümüz `işle` harfleri büyük harfe çeviriyor. `yinelemesiz` yöntemi sadece farklı harfleri veriyor, tekrarları vermiyor. En sonunda da a'dan z'ye sıralıyoruz.

#### Kendi akış yöntemimizi tanımlayalım

İşlevlere girdi olarak adlı ya da adsız başka işlevler girebildiğimiz gibi bir dizi komut da girebiliriz. Bu sayede kendi akış yöntemlerimizi yaratabiliriz. Örneğin, bir kare çizmek için kaplumbağa komutlarını yineleyebilmek iyi olur, değil mi? Daha önce gördüğümüz `için` yapısını kullanabiliriz:

```scala
sil
için (i <- 1 |-| 4) { ileri(100); sağ() }
```

Ama, aşağıdaki gibi yazabilsek çok daha iyi olmaz mı?

```scala
sil
yinele(4) { ileri(115); sağ() }
```

Scala dili 'eğer/yoksa' ve 'yineleDoğruKaldıkça' gibi yapılar sunar ama `yinele` yapısı Kojo tarafından tanımlanmıştır. Bunu sen de Scala ile yapabilirsin. Şöyle başlayalım:

```scala
tanım yinele2(ys: Sayı, dk: => Her): Birim = {
    için (i <- 1 |-| ys) dk
}
sil
yinele2(4, { ileri(130); sağ() })
```

ys yineleme sayısının kısaltması. dk de dizi komut anlamında. Şu ana kadar işlevlere girdiğimiz bütün girdiler değer olarak yollandı (pass by value denir İngilizce). Yani işlev çağrılmadan önce değer hesaplanır ve işleve yollanır. yinele2 tanımında kullandığımız kalın ok yani `=>` imi durumu değiştiriyor. dk yani dizi komut olduğu gibi (call by name denir) yollanıyor yinele2 işlevine. Yani dizi komutların değerlendirilmesi geciktirilir. Ancak işlevin işleme sırası geldiğinde işlevin içine girilince dizi komut çalıştırılır. Bu yinele2 işlevimizde `için` döngüsü ys sayısı kadar çalışacak ve her seferinde dk yani girilen dizi komut çalıştırılacak.

Hiç fena değil. Neredeyse istediğimiz hale geldi. Tek sorun 4 sayısından sonra gelen virgül. Scala istersek her girdiyi kendi parantezi içinde girmemizi destekler. Buna 'curried işlev' de denir. Haskell Curry adında bir matematikçi buna esin kaynağı olduğu için. Bakın şöyle değiştirelim tanımı. Bu sayede tam istediğimiz gibi yazabiliriz:

```scala
tanım yinele3(ys: Sayı)(dk: => Her): Birim = {
    için (i <- 1 |-| ys) dk
}
sil
yinele3(4) { ileri(160); sağ() }
```

Biraz önce soldanKatla örneğinde de bu tekniği kullanmıştık. Daha okunur hale geldi değil mi?

Yazılıma hobi ya da işin olarak devam edersen işlevlerin nesne gibi kullanılabilmesinin daha pek çok yazılım problemini çözmede faydalı olduğunu göreceksin. Başka yolları da var elbet ama buradaki örneklerde de gördüğümüz gibi bu teknikle kodumuz epey kısa ve okunur hale geldi. Daha kapsamlı ve tasarlaması, yazması ve idare etmesi esaslı bir iş olan bazı örnekleri de şimdiden duymuş olman için kısa bir liste vereyim. Merak ettikçe, yeri geldikçe incelersin. Kusura bakma bunlar önce İngilizce olsun: (1) passing callback functions in event driven IO, (2) passing tasks to Akka Actors in concurrent processing environments, (3) in scheduling work loads. Yani, (1) olgu güdümlü girdi/çıktı idaresinde geriçağırım yapan işlevleri dağıtmak (2) eşzamanlı işlemler olan ortamlarda, yani aynı anda birden fazla işlemin paralel yani yanyana çalışması durumunda bağımsız aktörlere yeni işlevler verilmesi, (3) uzun işlerin sıralamasını düzenlerken işlevleri dağıtmak. Bilgisayar bilimi ve mühendisliği artık o kadar gerekli bir hale geldi ki, bu temeli iyi bilmek ilerde çok helal kazanç kazanmana faydalı olacaktır. Tabii eğer gönlünü bu işe verirsen.

#### Bir Tür Girdiyi Yinelemek

Bazen işlevimize kaç tane girdi gireceğini bilemeyiz. Daha doğrusu kaç tane girilirse girilsin çalışmasını isteriz. Örneğin bu kılavuzda çok kullandığımız `satıryaz` komutu masaüstünde öyledir. Bir, iki, üç... ne sayıda girdi girersek girelim işini yapar. Bunun yolu da çok kolay:

```scala
tanım topla(s: Sayı*) = s.indirge(_ + _)
satıryaz(topla(1, 2, 3))
satıryaz(topla(4, 5, 6, 7, 8, 9, 10))
// Pek toplamaya gerek olmasa da yine de tek değerle de çalışması güzel!
satıryaz(topla(99))
```

Girdinin türünden sonra gelen yıldız imi, yani `*` sayesinde `s` girdisi tek bir sayı değil bir dizi sayı oluveriyor. Onun için de `indirge` (İngilizcesi reduce) yöntemini kullandık. Bu soldanKatla yöntemine benzer ama daha basittir. Bakın girilen bütün sayıları toplamak bu kadar kolay. Bir istisnaya da bakalım, ne olacak?

```scala
tanım topla(s: Sayı*) = s.indirge(_ + _)
satıryaz(topla()) // Bak ne oldu? Bunu onarabilir misin?
```

Bu yıldızlı girdiden önce yıldızsız yani normal girdiler de tanımlayabiliriz. Ama yıldızlı yani yinelenen girdi en son gelmelidir.
