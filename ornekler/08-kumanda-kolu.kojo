// Kumanda kolu: fareyle/parmakla sürüklenen bir top
//
// Kolu ekranın sol altına koyuyoruz. Ortasındaki küçük daireyi tutup
// çektiğin yöne top da o yönde gidiyor -- ne kadar uzağa çekersen o kadar hızlı.
// "Sahneİçinde" olduğu için top ekrandan dışarı kaçamıyor.

çizSahne(siyah)
yakınlaştırmayıKapat()

val top = Resim.daire(15).boyalı(sarı).kalemRenkli(turuncu)
çizMerkezde(top)

val kol = kumandaKolu(60)
kol.çevreRenginiKur(saydam)
kol.çevreKalemRenginiKur(gri)
kol.kolRenginiKur(Renkler.gökMavisi)
kol.konumuKur(-250, -180)
kol.çiz()

canlandır {
  kol.oynatSahneİçinde(top, 3)
}
