# Başlayalım

## Başlayalım

Kılavuzumuzda pek çok yazılımcık örneği bulacaksın. Onları buradan kolaylıkla çalıştırabilirsin. Herhangi birinin altındaki **Editörde aç** bağlantısına tıkladığında o örnek düzenleyiciye taşınır; **Çalıştır**'a bas. Yani hepsini yazmana gerek yok. Ayrıca işin bir güzel yanı da şu: istediğin değişiklikleri orada yapıp tekrar çalıştırabilirsin. Haydi şimdi bunu deneyelim. Aşağıdaki mesajı Bülent yerine kendi adını yazıp tekrar çalıştırıver. Bu arada benim adım Bülent. Bu satırları siz Türkçe severler için severek çevirdim.

```scala
satıryaz("merhaba dünya ve kaplumbağacık")
satıryaz("Bülent'ten hepinize selamlar, sevgiler")
```

Yaptığın değişikliğin sonucunu çıktı alanında ya da çizim tuvalinde hemen göreceksin.

Bu kılavuz [ilk bölümde](#b01) gördüğün bölümlerden oluşuyor. Bir sonraki bölüme geçmek için her bölümün sonundaki bağlantıyı kullan; ilgini çeken bölüme de soldaki listeden kolaylıkla atlayabilirsin. Kılavuz ayrı bir sekmede açıldığı için düzenleyicideki yazılımcığın kapanmaz; onun üzerinde değişiklikler yapıp çalıştırmaya odaklanabilirsin.

Her örneği deneyebilir, istediğine geri dönüp değişiklikler yapıp tekrar çalıştırabilirsin. Değişik fikirler dener, yazılım dilini daha iyi tanıyıp yazım, yazılım gramer (syntax) kurallarını daha çabuk öğrenebilirsin. Düzenleyicideki programları **Kaydet** ile kalıcı bir adrese kaydedebilir, bağlantısını paylaşabilirsin (giriş yapman gerekir).

Şu anda kullandığın Kojo öğrenim ortamı, çok gelişmiş ve uzman bilgisayar mühendislerinin en sevdikleri dillerin önde gelenlerinden Scala yazılım dilini öğrenmene yardımcı olmak için hem çok faydalı hem de eğlendirici özellikler ve beceriler içeriyor. İleri matematik kavramlarını resimler ve grafikler çizerek inceleyip daha iyi öğrenir, değişik tür oyunları hem oynayabilir hem de nasıl yazıldıklarını kolayca öğrenebilir, hatta fizik deneyleri bile yapabilirsin! En güzeli Kojo'nun kaplumbağaları var! Onlara yol göstererek çizimler yaptırabilirsin. Küçük, hemen anlayıp seveceğin tek kaplumbağalı bir örnekle başlayalım mı? Kaplumbağa yürüsün ve bir taraftan da bir üçgen çizsin istersen, bu yazılımcığı çalıştırman yeter:

```scala
sil
ileri(100)
sağ(120)
ileri(100)
sağ(120)
ileri(100)
sağ(120)
```

Bu kılavuzun [ikinci bölümünde](#b02) daha pek çok örnek yazılımcık ve kaplumbağanın anladığı komutların bir listesini bulabilirsin.

Scala dili tam anlamıyla genel ve güçlü bir bilgisayar programlama dilidir. Özelliklerinin çoğunluğu uzman programcılara ve bilgisayar mühendislerine tanıdık gelecektir. Bu kılavuzdan faydalanmak için yazılım dili bilmesen de olur, ama elbette daha önce başka bir yazılım diliyle deneyimin olduysa daha hızlı ilerleyebilirsin. Onun için anladığın yerleri ve bölümleri hızlı geçmen hatta atlaman bile doğal olur.

Öyleyse, başlayalım mı artık?

## Deyişler

Basit matematiksel deyişler pek şaşırtıcı gelmeyecektir. Tanıdık matematik işlemleri ve öncelikleri Scala'da da geçerlidir. İşlemleri daha açık açık sıralamak için parantez kullanılır.

> Masaüstü Kojo bir deyişin değerini kendiliğinden çıktı gözüne yazar. Tarayıcıda görmek için `satıryaz` içine alıyoruz.

```scala
satıryaz(1 + 2)
satıryaz(3 + 4 * (2 - 3))
satıryaz(23 % 5)
```

Son satırda tabanlı aritmetik işlemi yaptık. 5 tabanında. Yani 5'e bölünce ne kalıyorsa onu bulduk.

```scala
satıryaz(6 / 4)
```

Bu son işlemde önemli bir nokta var. Tam sayılar bölününce sonuç yine bir tam sayı olur ve kalan dikkate alınmaz. Ufak bir değişiklikle kesirli ve daha doğru bir işlem yapabiliriz:

```scala
satıryaz(6 / 4.0)
```

Biraz daha uzun bir işlem yapalım:

```scala
satıryaz(3.5 * 9.4 + 7 / 5)
```

Scala tam ve kesirli gibi değişik bir kaç sayı tipini tanır. Hepsinin bir tür adı var. Örneğin Sayı, Uzun, İriSayı, Kesir, vb. Bunları ileride detaylı olarak inceleyeceğiz. Yukarıdaki iki örnekten 4.0 ve 3.5 kesir (İngilizcesi Double), 4 ve 6 ise birer tam sayıdır (İngilizcede Integer, kısaca Int). Eğer bir matematiksel deyiş bir kaç tür sayı içeriyorsa Scala derleyicisi mümkünse sayının türünü biraz zorlayarak değiştirebilir.

Bir deyişin sonucunu bir değişken (aslında çoğunlukla bir değişmez değer) kullanarak kaydedip daha sonra yine kullanabiliriz. Değişken ve değişmez değer isimleri (ve sonra göreceğimiz başka tür isimler) harf, sayı ve `* / + - : = ! < > & ^ |` gibi semboller kullanarak yazılır. Örneğin, "FutbolTopu", "BilardoTopuBeyaz1", "yardımHattı", "*+" ve "res4" (result yani sonuç)...

Bunun için iki yöntem vardır: `den` ve `dez` anahtar sözcükleri. `dez` sözcüğüyle sabit ve hiç değişmeyecek değerleri ve sonuçları saklayabiliriz. Bunlara değişmez değer, ya da kısaca değişmez diyelim. Ya neden bir de `den` anahtar sözcüğü var? Aşağıda bir örnekle ikisinin farkını hemen anlayacağız. `dez` ile tanımlanan değerlerin sabit olması (İngilizcede 'immutable value') aslında çok önemli bir işlevsel yazılım (functional programming) kavramıdır, ama bunu daha sonra yeri gelince daha iyi anlayacağız. Şimdilik mümkün oldukça `den` yerine `dez` komutunu kullanmaya dikkat edelim. Bu sayede yazılımın başka bir yerindeki değişkenleri yanlışlıkla bozamayız.

```scala
dez noktaSayısı = 34 + 5
satıryaz(noktaSayısı)
```

Bir ya da daha fazla sayıda işlemin sonucunu çıktı alanına `satıryaz(deyiş)` komutunu kullanarak yazabiliriz. Birden çok deyişi yan yana yazmak istersek `satıryaz(deyiş1, deyiş2, deyişn)` da olur; masaüstü Kojo bunları aralarına boşluk koyarak yazar, ikojo ise `(d1,d2,dn)` biçiminde bir sıralama olarak gösterir. Deyişler arasına virgül koymayı unutmayalım.

```scala
dez noktaSayısı = 34 + 5
satıryaz(noktaSayısı, 3 + 2, noktaSayısı / 2, 3.9 / 2.3)
den boy = noktaSayısı + 4
satıryaz(boy)
```

Masaüstü Kojo'nun çıktı gözü her değerin adını ve değerini yazmakla kalmaz, ikisinin arasında o değerin türünü de yazar: Int (Sayı), Double (Kesir) vb. Şimdi deneyelim:

```scala
dez noktaSayısı = 34 + 5
noktaSayısı = 10
```

Bu hata verdi, değil mi? `dez` ile tanımlanan değişkenlerin değerleri değiştirilemez. Scala derleyicisi (compiler), 'Error: reassignment to val' yani sabit bir değeri değiştirmek hata olur diyor ve izin vermiyor. 'error' hata demek.

```scala
dez x = 3.14
x = 3.1415
```

Şimdi boyunu 4 nokta uzatalım:

```scala
dez noktaSayısı = 34 + 5
den boy = noktaSayısı + 4
boy += 4
satıryaz(boy)
```

Okuyanları bilgilendirmek ve kendimize anımsatmak için satır sonlarına `//` yani iki taksim ya da bölüm işaretinden sonra bir açıklama yazabiliriz. Bir satıra sığmıyorsa `/*` ile başlayıp `*/` ile biten daha uzun açıklamalar ekleyebiliriz. Scala derleyicisi bunları göz ardı eder ve bu sayede bilgisayarın kafası karışmaz 8-).

```scala
/*
  Çok satırlı bir açıklama örneği
  Fahrenayttan santigrata çevirelim
*/
dez dereceF = 98.4 // vücut termometresi bunu göstersin
satıryaz(dereceF, "derece Fahrenayt",
         (dereceF-32)*5/9, "derece Santigrat")
```

`satıryaz` komutuyla yazı yazdık, yukarıda gördüğün gibi. Böyle çift tırnaklar içine alınan yazıların türüne Yazı (İngilizcesi String) diyoruz. Bu tür, sadece yazı yazmak için değil, yazılarla işlemler yapmak için de kullanılabilir:

```scala
dez adım = "Mustafa Kemal"
dez mesaj = "Merhaba " + adım
satıryaz(mesaj)
```

Toplama işareti sanki toplama yaparmış gibi yazıları birbirine ekleyiveriyor. Mantıklı değil mi? Yazı türüyle daha neler yazılabilir neler! [İki bölüm sonra](#b06) başka örnekler de göreceğiz.

## İki tabanlı sayma ve parçacık işlemleri

Duymuşsundur eminim, bilgisayar devreleri aslında 2, 3, 4 gibi sayıları bile bilmez. Onun yerine sadece 0 ve 1 sayılarını tanır. Hatta tanımak dedik de aslında sadece voltaj değerlerini ve elektrik akımlarını tanır onlar. Bu uzun, ilginç ve çok keyifli bir öyküdür. Benim gibi elektrik mühendisi olmak istersen, bana emaille selam ve sorularını yollayabilirsin. Neyse, konumuza dönelim. Daha büyük sayılarla işlemler yapmak için bilgisayar onları bir 0 ve 1 dizisi olarak ele alır ve içindeki sayısız mantık devreleri sayesinde toplama, çıkarma, çarpma, bölme ve hatta türev ve integral alma gibi daha ileri matematik işlemlerini kolayca ve hiç üşenmeden halleder. Bunların detayı bilgisayar uzmanlarının işi. Biz 0 ve 1 dizilerine dönelim, çünkü herşey onlarla başlıyor! İngilizcede 'binary arithmetic' denir. Biz iki tabanlı sayma diyelim. Her sayı, 0 ve 1'lerden oluşan bir dizi olduğu için, direk onun parçacıkları üzerinde de işlemler yapabiliriz. Bu işlemlere 'bitwise' yani parçacıksal işlem denir. Böyle 0 ve 1 dizilerinin her bir elemanına İngilizcede 'bit' denir. Saçlarımızda yaşayan ve zararsız küçük böcekcikler değil elbet! İngilizcede azıcık, küçücük anlamlarına geliyor. Biz parçacık diyelim istersen. Çok uzattık. Kusura kalma. Şimdi parçacıkları teker teker nasıl işleme sokarız bir kaç örnek görelim:

```scala
satıryaz(3 & 2)
```

Bu `&` imiyle mantıksal 've' işlemi yapıyoruz (İngilizcesi 'logic and'). Sadece 1 ve 1 sonuç olarak 1 verir. Girdilerden biri 0 olursa sonucu da 0 olur. `|` da mantıksal veya (or) işlemi. Sadece 0 veya 0 sonuç olarak 0 verir. Biri 1 olursa sonuç da 1 olur:

```scala
satıryaz(1 | 2)
```

Mantıksal dışlayan veya işlemi (xor yani exclusive or) aşağıda. Sadece biri 1 öbürü 0 olunca 1 verir:

```scala
satıryaz(1 ^ 2)
```

Parçacıkları sola kaydırmak ikiyle çarpmaya denk! İki kere kaydırsak ne olur? Ya 10, 20 ya da 30 kere kaydırsak?

```scala
satıryaz(1 << 2)
satıryaz(1 << 10)
satıryaz(1 << 20)
satıryaz(1 << 30)
```

Nasıl da hızlı büyüdü, değil mi? 10 kere kaydırmak, 2'yi on kere kendisiyle çarpmak demek, yani iki üssü ona denk. O da 1024 ediyor. Binden birazcık fazla. 2 üssü 20, bir milyonu; 2 üssü 30 da bir milyarı geçti...

Sağa kaydır ama eksiyse eksi kalsın:

```scala
satıryaz(-24 >> 2)
satıryaz(-14 >>> 2)
```

Sağa kaydırıyor ama sonuca bakın! Ben anlamadım vallahi. Ya sen? Bakın bu çok ilginç. Parçacıkları sola kaydırmak ikiyle çarpmaya denk! Sağa kaydırmaksa ikiye bölmeye benzemiyor mu? Bu daha önce de dediğim gibi uzmanlık konusu. Üzerinde yazılmış pek çok bilimsel makale ve ders kitapları var. Hatta bazıları çok azımızın anlayabileceği yüksek ihtisas kitapları! Bugünlük bu kadarı fazla bile. Ama sen istersen bu yazılımcığı kurcala. -14 yerine 14'ü dene. Çalıştır ki sonucu hemen göresin. Bakarsın uzman olmak istersin. Neden olmasın?

Farkında mısınız? Bu kadarcık bilgiyle bile artık çok güçlü bir hesap makinemiz oldu. Ama dahası var! [Bir sonraki bölümde](#b05) program akışı nasıl düzenlenir öğrenecek ve yazılımcıklarımızı çok daha becerikli hale getireceğiz.
