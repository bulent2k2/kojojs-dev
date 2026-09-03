// Klavyeyle bir resmi gezdirmek
// Ok tuşlarını kullan. tuşBasılıMı her karede tuşun durumuna bakar.

artalanıKur(siyah)
yakınlaştırmayıKapat()

dez top = Resim.daire(15).boyalı(sarı)
çiz(top)

dez adım = 4.0

canlandır {
  eğer (tuşBasılıMı(tuşlar.sağ))    top.kaydır(adım, 0)
  eğer (tuşBasılıMı(tuşlar.sol))    top.kaydır(-adım, 0)
  eğer (tuşBasılıMı(tuşlar.yukarı)) top.kaydır(0, adım)
  eğer (tuşBasılıMı(tuşlar.aşağı))  top.kaydır(0, -adım)
}
