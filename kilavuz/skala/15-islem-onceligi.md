# İşlem Önceliği ve Birleşmeliği

### İşlem Önceliği

Scala'nın işlemleri de birer nesne dersem artık hiç şaşırmazsın değil mi? Belki de şaşırman doğal aslında. Ben de hala hayret ediyorum biraz. Şöyle anımsayalım: Scala'nın sunduğu bütün işlemler de aslında birer yöntem, onun için de birer işlev, onun için de birer nesne! Dolayısıyla işlemlerin adı olarak da her hangi bir ad kullanabiliriz. Yeter ki geçerli bir ad olsun. Geçersiz adlar nasıl olur?

```scala
dez 0abc = "olur mu canım?"
```

```scala
dez *olmaz = "bu da yanlış!"
```

Yani harf ya da özel bir karakterle başlamaması gerek. Ama bu kuralın da istisnaları var.

```scala
dez _0abc = "bak bu oldu"
dez _a$b__c_ = "bak bunlar da oldu"
satıryaz(_0abc, _a$b__c_)
```

```scala
dez a*b = "yok yok bu hala problem"
```

Gördüğümüz gibi bazı özel karakterleri değişken ve değişmez adlarında kullanamıyoruz. Matematik işlemlerinin imleri hiç olmuyor. Ama aşağıdaki yazılımda bir istisna daha göreceğiz.

Peki geçerli ve geçersiz bazı adları görmemiz iyi oldu da esas konumuza dönelim, yani öncelik meselesi. Bakın aşağıdaki tablo öncelik sırasını gösteriyor. Üsttekiler önce geliyor. Eğer bir işlem iminde birden fazla karakter varsa o durumda da ilk karakter önem kazanıyor. Örneğin `+*` diye bir işlem tanımlarsak, onun önceliği `+` imininkiyle aynı olur.

| İşlem imleri | Açıklama |
|---|---|
| diğer bütün özel karakterler | Örneğin `@ { } ( )` vb... |
| `* / % + - : = ! < > & ^ \|` | Soldakiler sağdakilerden önce gelir. |
| bütün harfler | O halde a, b'den önce gelir. Ya büyük A? |
| eşitlik işlemleri | Örneğin `= += -= *= /=` vb... |

### İşlem Birleşmeliği

Birkaç örnekle daha kolay olacak bu konuyu işlemek:

```scala
satıryaz(3 * 5 * 7)
satıryaz((3 * 5) * 7)
satıryaz(3 :: 5 :: Boş)
satıryaz(3 :: (5 :: Boş))
```

```scala
satıryaz((3 :: 5) :: Boş)
```

Ne oldu? Burada iki değişik işlemle karşı karşıyayız. `*` imi ile `::` imi farklı çalışıyor. Son örneğimiz çalışmadı çünkü `::` yani bir dizinin kuyruğuyla başını birleştirip yeni bir dizin oluşturan birleştirme işlemi sağdaki değeri temel alıp onun yöntemi olan `::` işlemine soldaki değeri girdi olarak giriyor ve sonunda yeni bir dizin çıktısı veriyor. Son örnekteki hata nereden kaynaklandı şimdi daha iyi anladık. Ama `*` işlemi öbür taraftan çalışıyor, yani soldaki değer temel alınıyor onun yöntemi olarak `*` çağrılıyor ve ona sağdaki değer girdi oluyor. Scala bu durumu düzenlemek ve programcıya iki seçenek de sunmak için şunu yapıyor: işlemin adına bakıyor ve son karakteri seçiyor. Eğer son karakter `:` ise, yani iki nokta üstüste ise, sağdan birleşme yapıyor ve sağdaki değerin üzerindeki yöntemi çağırıyor ve soldaki değeri giriyor o yönteme. Adlarının son karakteri `:` olmayan yöntemlerse öbür türlü çalışıyor, yani yukarıda gördüğümüz `*` örneğindeki gibi soldaki değerin yöntemi çağrılıyor ve sağdaki değer girdi oluyor. Yani `a * b` yazarsak derleyici `a.*(b)` görmüş gibi çalışıyor, ama `a *: b` yazarsak `b.*:(a)` gibi çalışıyor. Bir örnekle alıştırma yapalım ki tam pekişsin. Diyelim ki iki sayıyı önce toplayıp sonra toplamını ikinci sayıyla çarpmak istiyoruz. Bunu da çok yapacağız. O zaman bir nesne türü yani sınıf tanımlayıverelim ve bütün seçenekleri deneyelim:

```scala
durum sınıf Deneme(s: Sayı) {
    tanım +*(x: Deneme) = (s + x.s) * x.s
    tanım +:(x: Deneme) = (s + x.s) * x.s
}
dez (a,b) = (Deneme(5), Deneme(3))
satıryaz((a +* b, b +* a, a +: b, b +: a))
```

Umarım faydalı olmuştur. Sen de birşeyler dene, hem parmakların hem de beynin daha iyi öğrensin!

Bu vesileyle daha büyük bir örnek görelim ve bilgisayarın temeli olan mantığa giriş yapalım. Mantık nedir bilir misin? Belki iyi tanımazsın ama bildiğinden eminim çünkü mantık bizim altıncı hissimiz gibidir. Mantıksızlık hiç hoşumuza gitmez. Bak şöyle yazabiliriz mantık işlemlerinin temelini. Bunu okurken `+ * x` ve `!` gibi imlerin tanımlarını nasıl yaptığımıza da dikkat!

```scala
durum sınıf Önerge(doğruMu: İkil) yayar BaskınYazıyaYöntemiyle {
    tanım tersi = Önerge(!doğruMu)
    tanım ve(öbürü: Önerge) = eğer (doğruMu) öbürü yoksa bu
    tanım veya(öbürü: Önerge) = eğer (doğruMu) bu yoksa öbürü
    tanım yada(öbürü: Önerge) = (bu veya öbürü) ve (bu ve öbürü).tersi
    tanım eşittir(öbürü: Önerge) = doğruMu == öbürü.doğruMu

    tanım unary_!(): Önerge = bu.tersi  // ! imi nesnenin önüne gelsin ve girdisiz yöntem olsun istiyoruz. Onun için bu 'unary_' gerekli.
    tanım *(öbürü: Önerge) = bu ve öbürü
    tanım +(öbürü: Önerge) = bu veya öbürü
    tanım x(öbürü: Önerge) = bu yada öbürü
    tanım ==(öbürü: Önerge) = doğruMu == öbürü.doğruMu
    tanım ==>(öbürü: Önerge) = bu.tersi veya öbürü
    tanım <=>(öbürü: Önerge) = (bu ==> öbürü) ve (öbürü ==> bu)

    baskın tanım yazıya = eğer (doğruMu) "doğru" yoksa "yanlış"
    tanım to01 = eğer (doğruMu) "1" yoksa "0"
}

tanım tersi(x: Önerge) = x.tersi
tanım ve(x: Önerge, y: Önerge) = x ve y
tanım veya(x: Önerge, y: Önerge) = x veya y
tanım yada(x: Önerge, y: Önerge) = x yada y
tanım eşittir(x: Önerge, y: Önerge) = x eşittir y
tanım ise(x: Önerge, y: Önerge) = x ==> y
tanım gvy(x: Önerge, y: Önerge) = x <=> y // gerek ve yeter

tanım deneme() = {
    tanım çizgi = satıryaz("-" * 34)
    tanım çift = {çizgi; çizgi}
    dez ara = " " * 10
    çift
    dez (d, y) = (Önerge(doğru), Önerge(yanlış))
    dez (dt, yt) = (tersi(d), tersi(y))
    satıryaz(s"  $d'nun tersi == $dt")
    satıryaz(s"  $y'ın tersi == $yt")
    çizgi
    satıryaz(s"$ara !${d.to01} == ${(!d).to01}")
    satıryaz(s"$ara !${y.to01} == ${(!y).to01}")
    çift
    dez seçenek = Dizin(d, y)
    için ((bağlam, adı) <- Dizin((ve _, "ve"), (veya _, "veya"), (yada _, "ya da"))) {
        için (a <- seçenek; b <- seçenek) {
            satıryaz(f"  $a%6s $adı%6s $b%6s == ${bağlam(a, b)}")
        }
        çizgi
    }
    çizgi
    için ((bağlam, adı) <- Dizin((ve _, "*"), (veya _, "+"), (yada _, "x"), (ise _, "==>"), (gvy _, "<=>"))) {
        için (a <- seçenek; b <- seçenek) {
            dez c = bağlam(a, b)
            satıryaz(f"$ara ${a.to01}%s $adı%s ${b.to01}%s == ${c.to01}")
        }
        çizgi
    }
    çizgi
    için (a <- seçenek; b <- seçenek) {
        dez (ab, a_b, axb) = (a ve b, a veya b, a yada b)
        satıryaz(f"  $a%6s ve    $b%6s == $ab")
        satıryaz(f"  $a%6s veya  $b%6s == $a_b")
        satıryaz(f"  $a%6s ya da $b%6s == $axb")
        çizgi
    }
    çizgi
    için (a <- seçenek; b <- seçenek) {
        dez (ab, a_b, axb) = (a * b, a + b, a x b)
        satıryaz(s"$ara ${a.to01} * ${b.to01} == ${ab.to01}")
        satıryaz(s"$ara ${a.to01} + ${b.to01} == ${a_b.to01}")
        satıryaz(s"$ara ${a.to01} x ${b.to01} == ${axb.to01}")
        çizgi
    }
    çizgi
}
deneme
```
<!-- ikojo: bağlam, adı -->
