// Sekme: Yöney2B ile basit fizik
// Hız bir yöney; her karede resmi o kadar taşıyoruz ve kenara
// çarpınca sahnedenSek yeni hızı veriyor.

artalanıKur(siyah)
yakınlaştırmayıKapat()

val top = Resim.daire(12).boyalı(Renkler.mercan)
çiz(top)

var hız = Yöney2B(3, 2)

canlandır {
  top.taşı(hız.x, hız.y)
  hız = sahnedenSek(top, hız)
}
