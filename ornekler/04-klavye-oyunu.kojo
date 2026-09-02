// Klavyeyle bir resmi gezdirmek
// Ok tuşlarını kullan. tuşBasılıMı her karede tuşun durumuna bakar.

artalanıKur(siyah)
yakınlaştırmayıKapat()

val top = Resim.daire(15).boyalı(sarı)
çiz(top)

val adım = 4.0

canlandır {
  if (tuşBasılıMı(tuşlar.sağ))    top.kaydır(adım, 0)
  if (tuşBasılıMı(tuşlar.sol))    top.kaydır(-adım, 0)
  if (tuşBasılıMı(tuşlar.yukarı)) top.kaydır(0, adım)
  if (tuşBasılıMı(tuşlar.aşağı))  top.kaydır(0, -adım)
}
