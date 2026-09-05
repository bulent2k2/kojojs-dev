# Yazılım Akışı: Eğer, Yoksa ve İçin

Şu ana kadar yazdığımız yazılımcıkların komutları, baştan sona kadar, sırayla, satır satır ve teker teker çalıştılar. Ama pek çok durumda program akışını değiştirmek isteriz. Yani komutların çalışma sırasını duruma göre değiştiririz. Bu sayede, bazı komutları yineler, bazılarını atlarız.

İlk önce bir komut dizisi (ya da komut bloğu da denir) oluştururuz. Bunun için komutları kıvrık parantezler, yani `{}` içine alıyoruz. Bu komut dizisi istediğimiz kadar satır ve hatta başka komut dizileri de içerebilir. Dizinin içindeki son komut dizinin değerini belirler.

Program akışını değiştirmek için kullandığımız bir kaç değişik yöntem var. Gelin en önemlilerinden biri olan, eğer/yoksa yapısıyla başlayalım. Bu iki anahtar sözcük bir koşul kullanarak bir karar çatalı oluşturmaya yarar.

### eğer

Genel olarak şöyle yazarız: `eğer (koşul) dizi1/deyiş1 yoksa dizi2/deyiş2`. Eğer koşul doğruysa ilk kısım yani dizi1/deyiş1 çalışır. Yoksa 'yoksa' sözcüğünden sonra gelenler çalışır.

```scala
eğer(doğru) satıryaz("Doğru") yoksa satıryaz("Doğru değil")
```

Bunu daha kısa da yazabiliriz. 'eğer/yoksa' deyişi hep bir değer verir. Bakın bunu da satıryaz komutuyla yazıyoruz.

```scala
satıryaz(eğer(doğru) "Doğru" yoksa "Doğru değil")
```

Kullandığımız koşulun geçerli olması için 'doğru' ya da 'yanlış' (İngilizcede 'true' veya 'false') değerlerinden birini vermesi gerekir. Bu değerlerin türüne biz İkil deriz (İngilizcesi Boolean). Bu koşulu sağlayan işlemlere karşılaştırma işlemi deriz. Bunlar matematiksel işlemler ya da benzerleri olabilir. İlk önce matematiksel olanları, yani sayıları karşılaştıran işlemleri görelim. Başka tür değerleri (örneğin sözcükleri) karşılaştırmayı sonraya bırakalım.

```scala
satıryaz(1 > 2)  // büyüktür
satıryaz(1 < 2)  // küçüktür
satıryaz(1 == 2) // eşittir
satıryaz(1 >= 2) // büyük ya da eşittir
satıryaz(1 != 2) // eşit değil
satıryaz(1 <= 2) // küçük ya da eşittir
```

eğer/yoksa yapısının nasıl çalıştığını anlamak çok kolay. Koşul doğruysa 'yoksa' sözcüğünden önceki komutlar çalıştırılırlar. Yok, eğer koşul doğru değilse, o halde 'yoksa' sözcüğünden sonraki komutlar çalıştırılır. Scala ve diğer işlevsel dillere benzemeyen yazılım dilleri (örneğin, C, Java, Python), eğer/yoksa yapısını sadece akışı belirlemek için kullanırlar. Ama Scala, Haskell ve diğer işlevsel diller gibi, eğer/yoksa yapısından bir değer beklerler. Onun için 'eğer' genelde tek başına kullanılamaz. Arkasından hemen 'yoksa' gelir ve iki durumda da bir değer geri bildirilir. Bunun bir istisnası da vardır, ama onu da sonraya bırakalım. Bakın bu örneklerde koşul olarak hep yalın sayıları karşılaştırıyoruz. Ama elbette başka `dez` değerler ve komutlar da kullanabiliriz. Yeter ki bir İkil, yani doğru ya da yanlış değeri olsun.

```scala
satıryaz(eğer (1>2) 4 yoksa 5)
satıryaz(eğer (1<2) 6 yoksa 7)
dez deneme1 = eğer (1==2) 8 yoksa 9
satıryaz(deneme1)
```

```scala
dez kitapsa = 6 >= 3 // bu doğru
dez değer = 16
dez sayı = 10
dez satış = eğer (kitapsa) değer * sayı yoksa {
  dez birim = değer / sayı
  birim * 3
}
satıryaz(satış) // doğru mu?
```

`{` ve `}` içinde bir dizi deyiş ya da bir dizi komut yazabiliyoruz. satış değeri dizi içindeki en son deyişin değeri olur.

Gerçek hayatta da olduğu gibi bazen bir kaç koşul bir araya gelir. Bu durumda iki tane mantıksal işlem kullanırız. Bunlardan birincisi `&&` mantıksal 'Ve' anlamına gelir. İkincisi de `||` mantıksal 'Veya' anlamına gelir. Bunları sakın parçacıkları işleyen `&` ve `|` işlemleriyle karıştırmayalım. Aslında ilişkili ve benzer kavramlar. Ama dikkat edelim. Çok uzatmadan birkaç örnek görelim, göreceksin çok doğal gelecek. Ne de olsa mantık hepimizde var.

```scala
dez kitapsa = 6 >= 3
dez değer = 16
dez sayı = 10
dez satış =
    eğer ( (kitapsa && değer > 5) || ( sayı > 30) ) değer * sayı
    yoksa değer / sayı
satıryaz(satış)
```

## yineleDoğruKaldıkça

Bu yapı bilhassa eski yazılım dillerinde çok kullanılır. İngilizce'de pek çok farklı anlama gelen 'while' sözcüğü komut olarak kullanılır. Türkçemize 'o halde' ya da 'o sırada' diye de çevirebiliriz. Genel olarak, `yineleDoğruKaldıkça (koşul) dizi/deyiş` yapısı kullanarak komut dizisini tekrar tekrar çalıştırabiliriz. Koşul sağlandığı sürece yineleme devam eder. Koşul değişince, yani artık doğru olmadığında yineleme son bulur. Bir örnekle anlamak çok daha kolay olacak. Ama ilk önce bir değişkenle başlayalım:

```scala
den toplam = 18
yineleDoğruKaldıkça (toplam < 15) toplam += 5
satıryaz(toplam)
```

Kolay değil mi? Sizce kaç kere yinelenecek komut? Sonunda toplam kaç olacak? Bu biraz hileli bir soru oldu. Aslında yinelenecek mi, toplam değişecek mi diye mi sormalıydık? Toplamın ilk değerini ya da koşulu değiştirip tekrar çalıştırabiliriz elbet. Bu 'yineleDoğruKaldıkça' yapısını 'yap' adlı anahtar sözcüğü kullanarak tersine de çevirebiliriz: `yap dizi/deyiş yineleDoğruKaldıkça (koşul)`. Bakın burada ilk önce komut dizisi çalıştırılır sonra koşula bakılır. Doğruysa komut dizisi yinelenir:

```scala
den toplam = 18
yap toplam += 5
yineleDoğruKaldıkça (toplam < 15)
satıryaz(toplam)
```

Gördük ki bu sefer toplam 23 oldu. Bir önceki örnekteki halbuki 18 olmuştu. Bakın bu yapıyı iki sayının ortak paydalarının en büyüğünü hesaplamak için kullanalım:

```scala
// en büyük ortak paydayı bulalım
den x = 36
den y = 99
yineleDoğruKaldıkça (x != 0) {
    dez yardımcı = x
    x = y % x
    y = yardımcı
}
satıryaz("ortak paydaların en büyüğü: " + y)
```

## için

Komut dizilerini kolayca yinelemek için kullanabileceğimiz bir yöntem daha var ki belki de en faydalısı. `için (aralık) dizi/deyiş` sayesinde verilen aralıktaki her bir değer için dizi/deyiş yinelenir. Aralık da nedir mi? Hemen bir örnek görelim:

```scala
için ( i <- 1 |-| 4 ) yaz("merhaba!")
```

Bunun İngilizcesi de şöyle:

```scala
için ( i <- 1 to 4 ) yaz("merhaba!")
```

'to' sözcüğü de bizim yapım/çekim ekimiz gibi, birden dörde kadar derken dörde sözcüğündeki '-e' anlamında. İngilizceyle Türkçe ne kadar farklı sanki, değil mi? Bir de bana sorun. 22 yaşında yaşamak için Amerika'ya gittiğimde, ki daha önce yurtdışına çıktığım günler sayılıdır, o kadar zorluk çektim ki! Güya iyi biliyordum hem de İngilizceyi! İşimize dönelim: Aralık burada birden dörde kadar olan sayılar elbet. `i` değişkeni 1 değeriyle başlıyor ve her tekrarda bir artıyor. Son sayı burada 4. Ama sonuncu sayıya gelmeden hemen önce durmak istersek `|-|` yerine `|-` imgesini kullanıyoruz:

```scala
için ( i <- 1 |- 12 )  {
  dez kare = i*i
  satıryaz(i, kare)
}
```

Bunun İngilizcesi de şöyle:

```scala
için (i <- 1 until 12) {
  dez kare = i*i
  satıryaz(i, kare)
}
```

İngilizce'de 'until 12', 12'ye kadar anlamına geliyor.

Biliyor musun, bu yineleme işlemlerini birden çok boyutta yapmak bilgisayarla çok kolay. Birden fazla aralık vereceğiz ve her aralık için de bir değişken (aslında değişiyor gibi göründüğüne bakma, bunlar da değişmez). Tek dikkat etmemiz gereken ikisi arasına bir noktalı virgül koymak. Bakın ne kolay!

```scala
için (i <- 1 |- 5 ; j <- "abc") satıryaz(i, j)
```

Bakın şu işe! Sayı yerine harfler kullandık! `için` yapısı içinde kullandığımız kümeler illa da sayılardan oluşmak zorunda değil yani. Genel olarak biz bunlara küme tekerleme diyebiliriz (İngilizcesi: iterating through a set or collection) yani teker teker her küme elemanını ele alıyoruz. "abc" yazısı da aslında bir harf kümesi ya da kolleksiyonu. Bakın hep küme ya da kolleksiyon dedim. Bu kavramlar yakın ama ufak farklılıkları var. Daha sonra bunlara verilen anlamı daha iyi anlayacağız. Şu anda çok da önemli değil gerçekten. Neyse. Harflerle tekerlemeye bir örnek daha verelim ve devam edelim:

```scala
için(c<-"merhaba!") satıryaz(c)
```

Bu `için` komudu aslında çok faydalı. Sadece çıktı için kullanılmıyor. Anahtar sözcük `ver` ile birlikte kullanarak bir dizi değer oluşturabiliriz. Bir küçük örnekle yetinelim şimdilik:

```scala
dez ikililer = için(i <- 1 |- 5 ; j <- "abc") ver(i, j)
yaz(ikililer)
```

Şimdi de matematik, bilhassa kartezyen geometrisi sevenlere bir sürprizimiz var. Kaplumbağacığı kullanarak bir eğri çizelim. Neyin eğrisi? İki boyutlu bir polinom, yani çok terimli bir matematiksel deyiş. Genel olarak `a*x^2 + b*x + c` diye yazabiliriz. Yine bu çok faydalı olan `için` yapısıyla:

```scala
sil
yaklaşXY(0.9, 0.5, 60, 400)
tanım eğri(x: Kesir) = 0.01 * x * x - 0.5 * x - 10
ızgarayıGöster(); eksenleriGöster()
dez aralık = 200
atla(-aralık,eğri(-aralık))
hızıKur(orta)
için(x <- -aralık+10 |-| aralık+100; eğer (x % 10 == 0)) noktayaGit(x, eğri(x))
```

`eğri` adında yeni bir işlev tanımlayıverdik. Bunu daha sonra daha iyi anlayacağız. (ikojo'da ızgara ve eksen komutları henüz yok; o satırı silip çalıştır, parabol yine çizilir.)

Şimdilik x ve y eksenlerini ve kare çizgileri silelim. Ve [bir sonraki bölümle](#b06) devam edelim!

```scala
eksenleriGizle(); ızgarayıGizle()
```
