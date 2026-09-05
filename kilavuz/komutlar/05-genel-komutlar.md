# Genel yazılım komutları
<!-- hücreler: çalıştır -->

Bazı basit komutlar çok faydalıdır. Sık kullanılanları burada görelim.

| Komut | Örnekler | | |
|---|---|---|---|
| `yaz` | `yaz("Selam!")` | `yaz('a')` | `yaz(kaplumbağa0)` |
| `satıryaz` | `satıryaz("Merhaba!")` | | |
| `satıroku` | (aşağıdaki ilk örnek) | | |
| `belirt(belit, mesaj)` | (ikinci örnek) | | |
| `gerekli(koşul, mesaj)` | (üçüncü örnek) | | |

```scala
dez gülerYüz: Yazı = "☺"
dez girdi = satıroku("Adınız nedir? ")
satıryaz("Merhaba " + girdi + s" $gülerYüz!" + " Ne var ne yok?")
```

```scala
belirt(1 < 2, "bir genelde ikiden küçük olmalı")
// 1 yerine 3 yazıp tekrar çalıştır ki belirtmek istediğimiz durum yanlış olunca ne olduğunu görelim
```

```scala
durum sınıf Kişi(ad: Yazı, yaş: Sayı) {
    gerekli(yaş > 0 && yaş < 1000, "kişinin yaşı yanlış girildi")
    satıryaz(s"$ad $yaş yaşında")
}
dez k1 = Kişi("Mustafa Kemal", 143)
 // bir de 1 yerine -1 girip çalıştır
dez k2 = Kişi("Garip Durum", 1)
```

`gerekli` ikojo'da yok; yerine Scala'nın kendi `require` komutu aynı işi görür. Masaüstünde bu örnek `çıktıyıSil` ile de başlıyordu; ikojo her çalıştırmada çıktıyı zaten temizler.

İngilizce karşılıkları da burada:

| Türkçe | İngilizce |
|---|---|
| `yaz(nesne)` | `print(object)` |
| `satıryaz(nesne)` | `println(object)` |
| `satıroku(istem)` | `readln(prompt)` |
| `belirt(belit, mesaj)` | `assert(assumption, message)` |
| `gerekli(koşul, mesaj)` | `require(condition, message)` |

Daha çok komut var, inşallah bir ara hepsini ekleriz buraya. ☺
