# Desen Eşleme: Eşle ve Durum Komutları

## Desen Eşleme

Program akışını değiştirmenin bazı yollarını görmüştük. Bilhassa eğer/yoksa yapısıyla akış nasıl dallandırılır biliyorsun. Şimdi de eşle/durum yapısıyla akışı aynı anda birden fazla dala ayırmayı görelim. C ve Java gibi daha eski dillerde de vardır benzeri yapılar. Scala bu kavramı daha da geneller ve cebirsel desen eşleme yapmayı sağlar! Bunu böyle anlamak mümkün değil elbet. Desene İngilizcede 'pattern' deniyor. 'Design patterns' ve 'pattern matching' gibi terimler çok yaygındır bilgisayar mühendisliğinde. Türkçeye 'tasarım desenleri' ve 'desen eşleme' diye çevirebiliriz. Desen yerine örüntü sözcüğü de var, ama onu pek duyan ve kullanan yok. Neyse biz gelin birkaç örnekle gizemi çözüverelim. İlk önce geleneksel ve daha basit kullanışıyla başlayalım:

```scala
tanım sayıdanYazıya(s: Sayı): Birim = {
  s eşle {
    durum 1 => satıryaz("Bir")
    durum 2 => satıryaz("İki")
    durum 5 => satıryaz("Beş")
    durum _ => satıryaz("Hata")
  }
}
sayıdanYazıya(5)
```

Burada her `durum` satırında kullandığımız `=>` iminin adı kalın ok imi olsun. Bu imden önce gelen desen ondan sonra gelen komut dizisi ya da deyişle eşleşiyor. Son örneğimizde 1 sayısı "Bir" yazısıyla eşleşiyor. Son desen olarak kullandığımız `_` imi herşey demek. Yani `s` sayısı ne olursa olsun artık eşini buluyor. Bu yapıda kullandığımız `eşle` anahtar sözcüğü hemen hemen bütün işlevler gibi bir değere sahip. Onun için son örneği daha da kısa ve öz bir şekilde yazalım:

```scala
tanım sayıdanYazıya(s: Sayı): Birim = {
  satıryaz(s eşle {
    durum 1 => "Bir"
    durum 2 => "İki"
    durum 5 => "Beş"
    durum _ => "Hata"
  })
}
sayıdanYazıya(3)
```

İşin güzel tarafı yukarıdaki eşleşmeyi tam tersine çevirmek de mümkün (Java ve C'deyse yapamazdık bunu):

```scala
tanım yazıdanSayıya(y: Yazı): Birim = {
  satıryaz(y eşle {
    durum "Bir" => 1
    durum "İki" => 2
    durum "Beş" => 5
    durum _ => 0
    }
  )
}
yazıdanSayıya("Beş")
```

İngilizce'de açık açık okuyabildiğimiz bir yazıyı sayıya çevirmeye "encoding", tersine de "decoding" derler. Son örnekle encoding, ondan öncekiyle de decoding yapmış olduk. Haberin olsun. Desen eşlemeyle bir nesnenin türüne göre de farklı işlemler yapmak kolaylaşır. Hemen bir örnek görelim:

```scala
tanım nedir(a: Her): Yazı = {
    a eşle {
        durum x: Sayı  => "Bir Sayı"
        durum x: Yazı  => "Bir Yazı"
        durum x: Kesir => "Bir Kesir"
        durum _        => "Kim bilir ne türdür?"
    }
}

/*
 * Aşağıda üç örnek var. Son örnek kesir
 * olsun diye sonuna F koyduk.
 * Anımsadın mı? Float UfakKesir demek.
 */
satıryaz(nedir("text"), nedir(2), nedir(2F))
```

Bakın bu yöntem `durum sınıf` kullanarak yarattığımız türleri işlemekte o kadar faydalıdır ki, [bir sonraki bölümü](#b10) tamamen ona ayırdık.
