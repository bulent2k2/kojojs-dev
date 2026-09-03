// Türkçe ANAHTAR KELİMELER: tanım, dez, den, eğer/yoksa, için, eşle/durum,
// doğru/yanlış... Bunlar kütüphane adları değil, yamalı (scala-tr) derleyicinin
// sözcükleri -- bkz. kojo/scala-tr/turkish-keywords.patch. Stok Scala
// derleyicisi bu dosyayı DERLEYEMEZ; ikojo-tr'nin derleyicisi derler.
sil()
artalanıKur(siyah)
hızıKur(çokHızlı)

tanım çokgen(kenarSayısı: Sayı, en: Kesir): Birim = {
  yinele(kenarSayısı) { ileri(en); sağ(360.0 / kenarSayısı) }
}

dez renkler = Dizi(kırmızı, sarı, yeşil, mavi, mor)
den sıra = 0
için (renk <- renkler) {
  kalemRenginiKur(renk)
  eğer (sıra % 2 == 0) { çokgen(6, 60) } yoksa { çokgen(4, 80) }
  sıra += 1
  sağ(72)
}

dez bitişMesajı = sıra eşle {
  durum 5 => "beş çokgen çizildi"
  durum _ => "beklenmedik sayı"
}
eğer (doğru) println(bitişMesajı)
gizle()
