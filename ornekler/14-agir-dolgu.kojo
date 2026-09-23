// AĞIR DOLGU: kendini kesen şekiller neden yavaşLARDI -- ve artık neden değil
//
// Kendini kesen bir yolun içini boyamak için iKojo şekli ÜÇGENLERE AYIRIYORDU
// (NON_ZERO sarım kuralı, libtess kütüphanesi; masaüstü Kojo'nun Java ile
// yaptığının aynısı). O hesap nokta sayısıyla KARESELE YAKIN büyüyordu.
// Ölçüldü (kojojs-dev#68, ısıtılmış, yazılımsal çizici):
//
//      250 nokta  ->    ~8 ms
//     1000 nokta  ->   ~95 ms
//     2000 nokta  ->  ~440 ms
//     4000 nokta  -> ~1840 ms
//
// Gerçek tarayıcıda, soğuk (tek atışlık betik), 251 noktada 18-38 ms
// (kojojs-dev#130/#134/#143): bir karelik bütçenin (16.7 ms) üstü. Bu
// örneğin ilk amacı o bedeli GÖSTERMEKTİ: iki gül, aynı nokta sayısı,
// kesişeni çizilince çıktı paneline "N ms sürdü (M nokta)" diye bir not
// düşüyordu.
//
// ARTIK DÜŞMÜYOR (kojojs-dev#147). 64 noktadan büyük dolgular hiç
// üçgenlenmiyor: şekil ekran kartına bir "yelpaze" olarak veriliyor, sarım
// sayısı stencil tamponunda sayılıyor, sıfır olmayan pikseller boyanıyor.
// Bedel nokta sayısıyla DOĞRUSAL ve ekran kartında; 1000 noktalı gül 1
// ms'nin altında (ölçüldü, kojojs-dev#152: 250'de 0.1-0.4 ms, 1000'de
// 0.3-0.8 ms -- eski yolun 100-400'de biri). Sonuç piksel piksel aynı:
// silüet kenarında %0.06 fark, ters sarımlı delikler dâhil.
//
// Bu dosya bu yüzden bir GERİLEME GÖSTERİMİ: çalıştır, iki gül de anında
// çizilmeli ve panel SESSİZ kalmalı. Not görürsen kitaplık eski yola düşmüş
// demektir (aşağıda: hangi durumlarda düşer) -- yaz.
//
// 64 NEDEN: küçük dolgular tek partide çiziliyor (PIXI Graphics), stencil
// ise şekil başına iki çizim çağrısı -- 2000 küçük kareyle ölçüldü, stencil
// orada kaybediyor (20 ms'ye 0.8 ms). 64 ve altındaki nokta sayısında
// libtess'in en kötü bedeli 4 ms; o yüzden küçük şekil eski yolda, büyük
// şekil yenisinde. Sınır kitaplıkta (StencilDolgu.Eşik), betikten
// görünmüyor -- 60 noktalı kesişen gül de sessiz, çünkü ucuz.
//
// ESKİ YOL NE ZAMAN ÇALIŞIR: PIXI 4 (eski tarayıcı), stencil tamponu
// vermeyen bir WebGL bağlamı, ya da LİBTESS SEÇENEĞİ elle açılırsa (tarayıcı
// konsolunda `localStorage.kojoDolgu = "libtess"` -- 2. deney). O yolda
// üçgenleme ve not aynen duruyor; not makinesinin kuralları (şekil başına
// bir kez, "sürdü" ile "şu ana dek" farkı, kuyruk boşalması) o yolun
// belgesinde: ornekler/README.

sil()
artalanıKur(beyaz)
hızıKur(çokHızlı)
kalemKalınlığınıKur(0)

// {nokta/kat} yıldızı: her adımda aynı açı kadar dönen kapalı yol. kat kaç
// kez sarıldığını söylüyor; kat > 1 ise yol kendini keser. kat ile nokta
// ARALARINDA ASAL olmalı, yoksa yol kendini tekrarlar ve şekil beklenenden
// seyrek çıkar.
tanım gül(nokta: Sayı, kat: Sayı, yarıçap: Kesir, renk: Renk): Birim = {
  // Çevrel yarıçaptan kenar uzunluğu: s = 2 * R * sin(kat * pi / nokta)
  dez kenar = 2 * yarıçap * sinüs(radyana(kat * 180.0 / nokta))
  dez dönüş = kat * 360.0 / nokta
  boyamaRenginiKur(renk)
  yinele(nokta) { ileri(kenar); sağ(dönüş) }
}

// 1) 250 nokta, kat = 1: yol kendini KESMİYOR, düz bir çokgen. Her zaman
//    ucuzdu.
kalemiKaldır(); noktayaGit(-170, 0); kalemiİndir()
gül(250, 1, 140, mavi)

// 2) yine 250 nokta, ama kat = 7: yol kendini kesiyor. Eskiden burada not
//    çıkardı; şimdi ikisi arasında fark yok.
kalemiKaldır(); noktayaGit(170, 0); kalemiİndir()
gül(250, 7, 140, kırmızı)

gizle()

// DENEYECEKLERİN (her deney dosyanın özgün hâlinden başlar):
//
// 1. İkinci çağrıdaki 250'yi 1000, sonra 4000 yap. Eski yolda 1000'de
//    ~100 ms, 4000'de ~2 saniyeydi (yukarıdaki tablo); şimdi ikisi de bir
//    karede çizilmeli ve panel sessiz kalmalı. Görmen gereken tek fark
//    gülün sıklaşması.
//
// 2. LİBTESS SEÇENEĞİ: tarayıcının geliştirici konsolunu aç (macOS:
//    Cmd+Option+J, Windows: Ctrl+Shift+J), şunu yaz ve sayfayı yenile:
//
//      localStorage.kojoDolgu = "libtess"
//
//    Kitaplık ESKİ yola döner: panelin ilk satırı "Eski dolgu yolu (libtess)
//    elle açık ..." olur, kırmızı gül üçgenlenir ve panelde
//    "... hesaplamak N ms SÜRDÜ (251 nokta)" notu çıkar. 1000 noktada
//    "ŞU ANA DEK N ms aldı (şimdilik M nokta; şekil büyüdükçe artacak)"
//    biçimini görürsün -- o yolun kurallarını README anlatıyor. Bu deney
//    eski bedelin ne olduğunu kendi makinende görmek için. Bitince seçeneği
//    GERİ AL ve yenile, yoksa sayfa yenilense de açık kalır:
//
//      delete localStorage.kojoDolgu
//
//    (Adrese `?dolgu=libtess` eklemek editörde güvenilmez: yönlendirici
//    sorguyu düşürüyor. Yalın tuval sayfasında çalışır.)
//
// 3. boyamaRenginiKur satırını sil. Dolgu hiç kurulmuyor, yalnız kalem izi
//    kalıyor -- şekil hâlâ görünür.
//
// 4. (Önce 3'ü geri al.) İkinci gülü bir TUŞA bağla: `gül(250, 7, 140,
//    kırmızı)` satırının yerine şu ikisini koy,
//
//      boyamaRenginiKur(kırmızı)
//      tuşaBasınca { t => yinele(40) { ileri(140); sağ(7 * 360.0 / 250) } }
//
//    çalıştır, boşluk tuşuna art arda bas. Şekil her basışta 40 nokta
//    büyüyor ve her karede baştan boyanıyor -- eskiden 7-8. basışta not
//    düşerdi (57 ms, "şimdilik 251 nokta"); şimdi kaç basarsan bas sessiz,
//    çünkü büyüyen şeklin her yayını da aynı ucuz yoldan gidiyor.
