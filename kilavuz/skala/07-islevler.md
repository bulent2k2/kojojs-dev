# İşlevler

İşlevlerin başka bir adı da fonksiyon. Matematikte de kullanırız bu terimi. Yazılım yazarken biraz farklı bir anlamı var. İşlevler, komut ve deyiş dizileri gibidirler. Tekrar tekrar kullanmak istediğimizde de çok faydalı olurlar. Onun için ilk önce bir ad vermek gerekir. Sonra da bir ya da daha fazla parametre girişi yapabiliriz. `tanım` özel sözcüğüyle başlarız bir işlev tanımlamaya. Arkasından da işlevin adını veririz. Yine en iyisi bir güzel örnekle başlayalım. Bakın işimiz şu. İki sayı arasından hangisinin daha büyük olduğunu bulmak.

```scala
tanım büyük(x: Sayı, y: Sayı): Sayı = {
    eğer (x > y) x
    yoksa y
}
satıryaz(büyük(6, 7))
```

İşlevin adını "büyük" koyduk. Hemen `tanım` anahtar sözcüğünden sonra geldi. Arkadan parantez içinde gerekli iki sayının adlarını tanımladık. Bunlara genelde işlevin değiştirge ya da parametreleri denir. Her değiştirge için türünü de belirtmemiz gerekir. Değiştirgenin adıyla türü arasına da iki nokta üstüste konur. Bu işlevimizin iki değiştirgesi var ve ikisi de Sayı türünde. Parantezden sonra da işlevimizin çıktısının türünü bildiriyoruz ki o da bir sayı. En son da bir eşittir işareti ve sonra kıvrık parantezler içinde işlevin bedeni geliyor. Bu şekilde tanımladığımız anda artık onu çağırabilir ve kullanabiliriz: `büyük(6,7)`.

Genelde her işlevin bir çıktısı vardır. Ama bazen çıktısız işlevler de olur. O tür işlevlerin sadece yan etkisi olacaktır. Örneğin ekrana yazı yazar ya da diske bir dosya yazar. O durumda çıktının türü Birim olur (İngilizcesi Unit) ve değer yok ya da boşluk (void) anlamına gelir. Bu tür işlevlere yöntem, metod, prosedür ya da altprogram da denir bazen (procedure ve subroutine İngilizce'de iki ilişkili sözcük). Daha önce birkaç örnek yöntem görmüştük:

```scala
sil
ileri(100)
satıryaz("bunu yazan işleve yöntem de deriz")
```

Yan etkisi olmayan işlevlere ise arı işlevler deriz (pure functions). Bunlar matematikte kullandığımız işlevlere benzerler. Aynı girdi değerleri hep aynı çıktıyı verir. Arı işlevler, adını işlevsel yazılım koyduğumuz bir yazılım tarzının temel yapıtaşıdır.

### Özyineli (recursive) İşlevler

İşlevler bazen kendilerini çağırabilirler (recursion). Özyineli işlevlerle daha önce gördüğümüz küme tekerlemesi, yani kümenin her elemanını teker teker ele alma işlemini daha kısa şekilde ifade edebiliriz. Yine bir örnekle anlamak daha kolay olacak. Bakın hiç yardımcı değişken kullanmadan iki sayının bölenlerinin en büyüğünü bulabiliriz:

```scala
// en büyük ortak payda
tanım enbop(x: Uzun, y: Uzun): Uzun = eğer (y == 0) x yoksa enbop(y, x % y)
satıryaz(enbop(96, 128))
```

Bu tanımı daha önce `yineleDoğruKaldıkça` komutuyla yaptığımız tanımla karşılaştırmanda fayda var ([bir önceki bölüm](#b05)).

Şimdi de daha renkli bir özyineleme görelim. Bu işlev kendini iki kere çağırarak kaplumbağacığa bir ağacın dallarını çizdiriyor. Bu tür ağaçlara ikil ağaç (binary tree) deriz. Sen de beğendin mi? Özyineleme nasıl duruyor? Yukarıdaki `enbop` işlevinde y'nin değeri sıfır olunca. Aşağıda ise uzaklık dört ya da daha küçük olduğunda.

```scala
tanım ağaç(boy: Kesir): Birim = {
    // sayıya yöntemi kesirli sayıyı tam sayıya çeviriyor
    // yani boy 1.75 olursa boy.sayıya 1 oluyor
    tanım renk = Renk(
        boy.sayıya % 255,
        mutlakDeğer(255 - boy * 3).sayıya % 255,
        125)
    eğer(boy > 4) {
        kalemKalınlığınıKur(boy / 7)
        kalemRenginiKur(renk)
        ileri(boy)
        sağ(25)
        ağaç(boy * 0.8 - 2)
        sol(45)
        ağaç(boy - 10)
        sağ(20)
        ileri(-boy)
    }
}
sil()
hızıKur(hızlı)
konumuKur(100, -200)
ağaç(90) // 100, 120 ve 150 gibi boyları da dene!
```
