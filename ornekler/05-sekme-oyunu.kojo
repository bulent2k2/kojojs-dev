// Sekme: Yöney2B ile basit fizik
// Hız bir yöney; her karede resmi o kadar taşıyoruz ve kenara
// çarpınca sahnedenSek yeni hızı veriyor.

artalanıKur(siyah)
yakınlaştırmayıKapat()

// konumlu(...) ile merkezden uzakta başlıyoruz: merkezde kaplumbağa
// simgesinin altında kalıyor ve hareket ettiği ilk anda fark edilmiyor.
val top = Resim.daire(12).boyalı(Renkler.mercan).konumlu(-100, 60)
çiz(top)

var hız = Yöney2B(3, 2)

canlandır {
  // kaydır: dünya koordinatlarında hareket (taşı, resmin KENDİ çerçevesinde
  // taşır; dönmeyen bir top için ikisi aynı ama kaydır daha doğrudan)
  top.kaydır(hız)
  hız = sahnedenSek(top, hız)
}
