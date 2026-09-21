// Kaplumbağa nerede? Bir şeye değiyor mu?
//
// iKojo'da kaplumbağa komutları KUYRUĞA giriyor: 
// ileri(100) yazdığın anda kaplumbağa henüz kıpırdamamış oluyor.
// Bu yüzden konumu düz bir değer olarak okuyamıyoruz.
// Okusaydık, henüz çalışmamış komutlardan ÖNCEKİ yeri verirdi.
// konumuOku / yönüOku / dokunuyorMu komutları okumayı kuyruğa 
// sokuyor: verdiğin işlev, kendisinden önce yazdığın bütün 
// komutlar bittikten sonra çalışıyor.

çizSahne(açıkGri)
yakınlaştırmayıKapat()
hızıKur(hızlı)

dez duvar = Resim.dikdörtgen(20,80).boyalı(Renkler.mercan).konumlu(120, -40)
çiz(duvar)

// bir kare çiz, sonra nerede bittiğimizi sor
yinele(4) { ileri(100); sağ() }
konumuOku { n => satıryaz("kare çizildi, buradayım: " + n.x.sayıya + ", " + n.y.sayıya) }
yönüOku   { a => satıryaz("yönüm: " + a + " derece") }

// duvara doğru yürü ve her adımda değip değmediğine bak
noktayaDön(160, 0)
yinele(40) {
  ileri(6)
  dokunuyorMu(duvar) { değdi =>
    eğer (değdi) satıryaz("duvara değdim!")
  }
}

konumuOku { n => satıryaz("şimdi de buradayım: " + n.x.sayıya + ", " + n.y.sayıya) }
yönüOku   { a => satıryaz("yönüm: " + a + " derece") }
