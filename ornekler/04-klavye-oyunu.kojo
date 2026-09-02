// Klavyeyle bir resmi gezdirmek
// Ok tuşlarını kullan. tuşBasılıMı her karede tuşun durumuna bakar.

artalanıKur(siyah)
yakınlaştırmayıKapat()

val top = Resim.daire(15).boyalı(sarı)
çiz(top)

val adım = 4.0

canlandır {
  if (tuşBasılıMı(tuşlar.sağ))    top.taşı(adım, 0)
  if (tuşBasılıMı(tuşlar.sol))    top.taşı(-adım, 0)
  if (tuşBasılıMı(tuşlar.yukarı)) top.taşı(0, adım)
  if (tuşBasılıMı(tuşlar.aşağı))  top.taşı(0, -adım)
}
