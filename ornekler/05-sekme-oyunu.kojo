// Sekme: Yöney2B ile basit fizik
// Hız bir yöney; her karede resmi o kadar taşıyoruz ve kenara
// çarpınca sahnedenSek yeni hızı veriyor.

// çizSahne: sahneyi boyar VE kenarlarını kurar. sahnedenSek bu kenarlara
// bakıyor -- artalanıKur yalnızca rengi değiştirdiği için yetmiyor.
çizSahne(siyah)
yakınlaştırmayıKapat()

// konumlu(...) ile merkezden uzakta başlıyoruz: merkezde kaplumbağa
// simgesinin altında kalıyor ve hareket ettiği ilk anda fark edilmiyor.
dez top = Resim.daire(12).boyalı(Renkler.mercan).konumlu(-100, 60)
çiz(top)

den hız = Yöney2B(3, 2)

canlandır {
  // kaydır ve taşı aynı şey: dünya koordinatlarında hareket
  top.kaydır(hız)
  hız = sahnedenSek(top, hız)
}
