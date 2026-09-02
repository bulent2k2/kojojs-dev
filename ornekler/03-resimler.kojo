// Resimler: çizimi bir NESNE gibi tutup dönüştürmek
// Dönüşümler zincirlenebilir ve her biri YENİ bir resim döndürür.

artalanıKur(siyah)

// Türkçe komutlarla resim yapmak
val kare = Picture { yinele(4) { ileri(60); sağ() } }
çiz(kare.boyalı(kırmızı).döndürülmüş(30))

// hazır şekiller
çizMerkezde(
  Resim.daire(50)
    .boyalı(Renkler.turkuaz)
    .kalemRenkli(beyaz)
    .saydamlıklı(0.6)
)

çiz(Resim.yazı("Merhaba Koco", 24).boyalı(sarı).konumlu(-90, 120))

// üçgenlerden çiçek
yineleDizinli(12) { i =>
  çiz(
    Resim.dikdörtgen(80, 8)
      .boyalı(Renkler.gökMavisi)
      .saydamlıklı(0.5)
      .döndürülmüş(i * 30)
  )
}
