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
// 250x7 ölçeğinde ölçüm (kojojs-dev#130, düzeltme sonrası):
//
//     251 nokta -> 35 ms
//
// Yani tablodakinin dört katından fazla. Sayı tam 251 çünkü kalem inince bir
// başlangıç noktası konuyor, sonra 250 kenar ekleniyor -- betiğin kendi
// geometrisi. (Bu örnek eskiden "146 nokta" gibi betikte karşılığı olmayan
// sayılar yazıyordu: dolgu şekil bitmeden de yayınlanıyor ve not her yayını
// ayrı ayrı bildiriyordu. Artık not ŞEKİL BAŞINA toplamı veriyor.)
//
// SOĞUK / SICAK farkı HENÜZ ÖLÇÜLMEDİ -- burada bir savım vardı, yanlıştı
// (#133 incelemesi §2). Şöyleydi: "15-mesh-olcumu.kojo'nun döngüsünde hiç
// not düşmüyor, demek ki sıcak süre <= 16.7 ms; yani soğuk/sıcak ~7 kat."
//
// Çıkarım geçersiz, çünkü o çıkarım şeklin TAMAMLANMIŞ olmasını gerektiriyor
// -- tamamlanmamış şekil ancak 50.1 ms'yi aşarsa konuşuyor. Ve aletin gülü
// hiç tamamlanmıyor: `sil()` boyamaRenginiKur'dan ÖNCE geliyor, yani
// boyamayıİşle boş çokgen buluyor. (Tam da bu dosyanın yukarıdaki
// "şekli tamamla" satırıyla düzelttiği durum, orada hâlâ duruyor.)
//
// Doğru üst sınır 16.7 değil 50.1 ms. 35 ms soğuk ile <= 50.1 ms sıcak,
// HİÇ FARK OLMAMASIYLA da uyumlu. Tablonun "iyimser" olduğu hâlâ makul bir
// hipotez (#68'in sayıları ısıtılmış ortancalar) ama bu koşudan çıkmıyor.
//
// Buradan çıkan kural: tabloyu BÜYÜK ÖLÇEK farkları için oku (250 ile 4000
// arasındaki fark gerçek), yakın sayıları karşılaştırmak ya da mutlak bir
// eşik çıkarmak için değil. Düzeltme öncesinde alınmış üç ölçüm de aynı
// şeyi söylüyordu ve sıralamayı İZLEMİYORDU (146 nokta 44 ms ama 236 nokta
// 27 ms): o sayılar tek tek yarım yayınlardı, yani bu ölçekte koşudan
// koşuya değişim nokta sayısının etkisini bastırıyor.
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

// ŞEKLİ TAMAMLA -- bu satır olmadan örnek SESSİZ kalıyordu.
//
// Bir dolgu şeklini tamamlayan tek şey kalem kalkık taşınma ya da boya
// değişimi. İkisi de gelmezse şekil "bitmemiş" sayılıyor, ve bitmemiş bir
// şekil ancak ERKEN EŞİĞİ (3 x bütçe = 50.1 ms) aşarsa not düşürüyor.
// Yukarıdaki gül gerçek donanımda 35 ms (ölçüldü, 251 nokta) -- yani
// eşiğin altında, ve örneğin bütün amacı olan not hiç çıkmıyordu.
// Ölçülmeden görülmedi, çünkü bu yalnız gerçek tarayıcıda oluyor.
kalemiKaldır(); noktayaGit(0, -220)

gizle()

// DENEYECEKLERİN:
//
// HER DENEY DOSYANIN ÖZGÜN HÂLİNDEN BAŞLAR: bir önceki değişikliği GERİ AL,
// sonra sıradakini yap. Üst üste bindirirsen ne ölçtüğün belirsizleşir --
// ve 2b üst üste binince büsbütün yanlış şey öğretir (orada yazılı).
//
// 1. İkinci çağrıdaki 250'yi 1000 yap. Nokta dört katına çıkıyor ama süre
//    çok daha fazla artıyor -- karesele yakın büyüme bu demek.
//
// 2. (Önce 1'i geri al: ikinci çağrı yine 250 olsun.) İkinci çağrıdaki kat'ı
//    7 yerine 1 yap. Artık iki gül de kesişmiyor ve not tümüyle kayboluyor.
//    Pahalı olanın kesişme olduğunu buradan görebilirsin.
//
//    1'i geri almazsan not yine kaybolur -- kesişmeyen yol 1000 noktada da
//    ucuz -- ama o zaman iki şeyi birden değiştirmiş olursun ve "nokta sayısı
//    aynı, yalnız kesişme değişti" karşılaştırması elinden gider. Bu deneyin
//    bütün gücü o karşılaştırmada.
//
// 2b. Tersini de dene: (önce 2'yi GERİ AL -- ikinci çağrının kat'ı yine 7
//    olmalı) BİRİNCİ çağrının kat'ını 7 yap. Bu kez iki not birden beklersin
//    ama TEK not görürsün -- ikinci not, iki not arasındaki en az süreye
//    (2 saniye) takılır. Uyarı bilerek böyle: tekrar eden uyarı, yanlış uyarı
//    kadar hızlı öğretir ki uyarılar okunmasın.
//
//    2'yi geri almazsan deney ÇALIŞMAZ ama bozulduğu belli olmaz: ikinci gül
//    kat = 1 kalır, yani ucuzdur ve zaten not düşürmez. Yine tek not
//    görürsün, ama "iki not beklersin" öncülü hiç kurulmamıştır -- yani
//    zaman kapısını değil, kendi kurulumunu gözlemlemiş olursun.
//
//    DİKKAT: bu deney yukarıdaki "şekli tamamla" satırına da BAĞLI. O satır
//    olmasaydı ikinci not zaten düşmezdi -- ama zaman kapısı yüzünden değil,
//    ikinci gül hiç tamamlanmadığı için. Aynı gözlem, yanlış sebep.
//
// 3. boyamaRenginiKur satırını sil. Dolgu hiç hesaplanmıyor, yalnız kalem izi
//    kalıyor -- şekil hâlâ görünür, çizim anında biter.
