// Renkli çiçek: aynı şekli döndüre döndüre çizmek
// yineleDizinli sayaç verir; onunla renk listesinde dolaşıyoruz.

hızıKur(çokHızlı)      // animasyonsuz, anında çizer
artalanıKur(siyah)
kalemKalınlığınıKur(2)

dez renkler = Dizi(kırmızı, turuncu, sarı, yeşil, mavi, mor, pembe)

yineleDizinli(72) { i =>
  kalemRenginiKur(renkler(i % renkler.boyu))
  yinele(4) {
    ileri(120)
    sağ(90)
  }
  sağ(5)
}
gizle()
