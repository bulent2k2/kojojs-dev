// Kaplumbağa nerede? Bir şeye değiyor mu?
//
// KojoJS'te kaplumbağa komutları KUYRUĞA giriyor: ileri(100) yazdığın anda
// kaplumbağa henüz kıpırdamamış oluyor. Bu yüzden konumu düz bir değer olarak
// okuyamıyoruz -- okusaydık, henüz çalışmamış komutlardan ÖNCEKİ yeri verirdi.
//
// konumuOku / yönüOku / dokunuyorMu okumayı kuyruğa sokuyor: verdiğin işlev,
// kendisinden önce yazdığın bütün komutlar bittikten sonra çalışıyor.

çizSahne(siyah)
yakınlaştırmayıKapat()
hızıKur(hızlı)

val duvar = Resim.kare(80).boyalı(Renkler.mercan).konumlu(120, -40)
çiz(duvar)

// bir kare çiz, sonra nerede bittiğimizi sor
yinele(4) { ileri(100); sağ() }
konumuOku { n => satıryaz("kare bitti, buradayım: " + n.x + ", " + n.y) }
yönüOku   { a => satıryaz("yönüm: " + a + " derece") }

// duvara doğru yürü ve her adımda değip değmediğine bak
noktayaDön(160, 0)
yinele(40) {
  ileri(5)
  dokunuyorMu(duvar) { değdi =>
    if (değdi) satıryaz("duvara değdim!")
  }
}
