// AĞIR DOLGU: kendini kesen şekiller neden yavaşlar?
//
// Kendini kesen bir yolun içini boyamak için ikojo şekli üçgenlere ayırıyor
// (NON_ZERO sarım kuralı, libtess kütüphanesi). Masaüstü Kojo'nun Java ile
// yaptığı şeyin aynısı -- ama bu hesap nokta sayısıyla KARESELE YAKIN
// büyüyor. Ölçüldü (bkz. kojojs-dev#68):
//
//      250 nokta  ->    ~8 ms
//     1000 nokta  ->   ~95 ms
//     2000 nokta  ->  ~440 ms
//     4000 nokta  -> ~1840 ms
//
// DİKKAT, BU TABLO İYİMSER: sayılar aynı girdiyle tekrarlanan çağrıların
// ortancası (JIT ısınmış) ve yazılımsal bir çizici üstünde alındı. Tek
// atışlık gerçek bir betikte libtess SOĞUK koşuyor. Gerçek bir tarayıcıda
// 250x7 ölçeğinde üç ölçüm (kojojs-dev#130):
//
//     146 nokta -> 44 ms      193 nokta -> 25 ms      236 nokta -> 27 ms
//
// BU ÜÇ SAYI DÜZELTME ÖNCESİ KODDAN (#130 incelemesi §3): o sırada not her
// YAYINI ayrı ayrı bildiriyordu, yani her sayı şeklin TEK BİR yarım
// yayınının süresi. Düzeltmeden sonra not şekil başına TOPLAMI yazıyor --
// aynı örneği bugün koşturan kişi bu üçünü değil, daha büyük TEK bir sayı
// görecek. Üçü burada duruyor çünkü aşağıdaki iki dersi hâlâ veriyorlar;
// dağıtımdan sonra yeniden ölçülüp değiştirilmeli.
//
// Yani tablodakinin birkaç katı. İki şey daha var, ikisi de öğretici:
//   - Bu nokta sayıları 250'den KÜÇÜK, çünkü dolgu şekil bitmeden de
//     yayınlanıyor (kaplumbağa komutları kuyrukta işleniyor).
//   - Sıralama nokta sayısını İZLEMİYOR: 146 nokta 44 ms, 236 nokta 27 ms.
//     Bu ölçekte koşudan koşuya değişim, nokta sayısının etkisini bastırıyor.
//
// Buradan çıkan kural: tabloyu BÜYÜK ÖLÇEK farkları için oku (250 ile 4000
// arasındaki fark gerçek), yakın sayıları karşılaştırmak ya da mutlak bir
// eşik çıkarmak için değil.
//
// Kesişmeyen bir yolda aynı nokta sayısı bedavaya yakın: 4000 noktalı bir
// çemberin dolgusu 6 ms'den az. Yani pahalı olan nokta sayısı DEĞİL,
// kesişmeyle BİRLİKTE nokta sayısı.
//
// Aşağıdaki iki gülün NOKTA SAYISI AYNI; farkları kendilerini kesip
// kesmemeleri. Karşılaştırmayı nokta sayısı üstünden değil kesişme üstünden
// kurduk, çünkü "kaç noktada yavaşlar" makineye göre değişiyor ama "kesişme
// pahalıdır" her makinede aynı. İkincisini çizerken çıktı panelinde bir not
// göreceksin -- dolgu bir karelik bütçeyi aştığında ikojo bunu söylüyor,
// sessizce yavaşlamıyor.

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

// 1) HAFİF -- 250 nokta, kat = 1: yol kendini KESMİYOR, düz bir çokgen.
//    Dolgu bedavaya yakın, panel sessiz kalır.
kalemiKaldır(); noktayaGit(-170, 0); kalemiİndir()
gül(250, 1, 140, mavi)

// 2) AĞIR -- yine 250 nokta, ama kat = 7: yol kendini kesiyor. Aynı nokta
//    sayısı, panelde not çıkar. Aradaki tek fark kesişme.
kalemiKaldır(); noktayaGit(170, 0); kalemiİndir()
gül(250, 7, 140, kırmızı)

gizle()

// DENEYECEKLERİN:
//
// 1. İkinci çağrıdaki 250'yi 1000 yap. Nokta dört katına çıkıyor ama süre
//    çok daha fazla artıyor -- karesele yakın büyüme bu demek.
//
// 2. İkinci çağrıdaki kat'ı 7 yerine 1 yap. Artık iki gül de kesişmiyor ve
//    not tümüyle kayboluyor. Pahalı olanın kesişme olduğunu buradan
//    görebilirsin.
//
// 2b. Tersini de dene: BİRİNCİ çağrının kat'ını 7 yap. Bu kez iki not birden
//    beklersin ama TEK not görürsün -- ikinci not, iki not arasındaki en az
//    süreye (2 saniye) takılır. Uyarı bilerek böyle: tekrar eden uyarı,
//    yanlış uyarı kadar hızlı öğretir ki uyarılar okunmasın.
//
// 3. boyamaRenginiKur satırını sil. Dolgu hiç hesaplanmıyor, yalnız kalem izi
//    kalıyor -- şekil hâlâ görünür, çizim anında biter.
