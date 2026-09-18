// AĞIR DOLGU: kendini kesen şekiller neden yavaşlar?
//
// Kendini kesen bir yolun içini boyamak için ikojo şekli üçgenlere ayırıyor
// (NON_ZERO sarım kuralı, libtess kütüphanesi). Masaüstü Kojo'nun Java ile
// yaptığı şeyin aynısı -- ama bu hesap nokta sayısıyla KARESELE YAKIN
// büyüyor. Ölçüldü (bkz. kojojs-dev#68):
//
//      250 nokta  ->    ~8 ms      sorunsuz
//     1000 nokta  ->   ~95 ms      bir karelik bütçe 17 ms
//     2000 nokta  ->  ~440 ms
//     4000 nokta  -> ~1840 ms
//
// Kesişmeyen bir yolda aynı nokta sayısı bedavaya yakın: 4000 noktalı bir
// çemberin dolgusu 6 ms'den az. Yani pahalı olan nokta sayısı DEĞİL,
// kesişmeyle BİRLİKTE nokta sayısı.
//
// Aşağıdaki iki gül aynı şekil; yalnız nokta sayıları farklı. İkincisini
// çizerken çıktı panelinde bir not göreceksin -- dolgu bir karelik bütçeyi
// aştığında ikojo bunu söylüyor, sessizce yavaşlamıyor.

sil()
artalanıKur(beyaz)
hızıKur(çokHızlı)
kalemKalınlığınıKur(0)

// {nokta/kat} yıldızı: her adımda aynı açı kadar dönen kapalı yol. kat kaç
// kez sarıldığını söylüyor; kat > 1 ise yol kendini keser ve dolgu pahalıya
// gider. kat ile nokta ARALARINDA ASAL olmalı, yoksa yol kendini tekrarlar
// ve şekil beklenenden seyrek çıkar.
tanım gül(nokta: Sayı, kat: Sayı, yarıçap: Kesir, renk: Renk): Birim = {
  // Çevrel yarıçaptan kenar uzunluğu: s = 2 * R * sin(kat * pi / nokta)
  dez kenar = 2 * yarıçap * sinüs(radyana(kat * 180.0 / nokta))
  dez dönüş = kat * 360.0 / nokta
  boyamaRenginiKur(renk)
  yinele(nokta) { ileri(kenar); sağ(dönüş) }
}

// 1) HAFİF -- 250 nokta. Panel sessiz kalır.
kalemiKaldır(); noktayaGit(-170, 0); kalemiİndir()
gül(250, 7, 140, mavi)

// 2) AĞIR -- 1000 nokta. Aynı şekil, dört katı nokta. Ölçülen süre ~95 ms,
//    yani bir karelik bütçenin altı katı: panelde not çıkar.
kalemiKaldır(); noktayaGit(170, 0); kalemiİndir()
gül(1000, 7, 140, kırmızı)

gizle()

// DENEYECEKLERİN:
//
// 1. İkinci çağrıdaki 1000'i 2000 yap. Nokta iki katına çıkıyor ama süre
//    DÖRDE katlanıyor (~440 ms) -- karesel büyüme bu demek.
//
// 2. İkinci çağrıdaki kat'ı 7 yerine 1 yap. Nokta sayısı aynı kalıyor ama yol
//    artık kendini kesmiyor (düz bir çokgen): dolgu bedavaya iniyor ve not
//    kayboluyor. Pahalı olanın kesişme olduğunu buradan görebilirsin.
//
// 3. boyamaRenginiKur satırını sil. Dolgu hiç hesaplanmıyor, yalnız kalem izi
//    kalıyor -- şekil hâlâ görünür, çizim anında biter.
