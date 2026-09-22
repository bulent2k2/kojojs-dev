// AĞIR DOLGU: kendini kesen şekiller neden yavaşlar?
//
// Kendini kesen bir yolun içini boyamak için iKojo şekli üçgenlere ayırıyor
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
//     251 nokta -> 22 / 32 / 35 ms    (üç koşu, elle tamamlama satırıyla)
//     251 nokta -> 18 ms              (tek koşu, satırsız -- #134, aşağıda)
//     251 nokta -> 21 ms              (tek koşu, satırsız, yeni pompa -- #131)
//     251 nokta -> 23 / 31 / 38 ms    (üç koşu, Resim{} içinde -- #143)
//
// Yani tablodaki 8 ms'nin 2.8 ile 4.4 katı arası. Sayı tam 251 çünkü kalem inince bir
// başlangıç noktası konuyor, sonra 250 kenar ekleniyor -- betiğin kendi
// geometrisi.
//
// ÜÇ SAYININ YAYILMASINA BAK: en büyüğü en küçüğün 1.6 KATI (35 ve 22;
// yüzde vermiyoruz, çünkü hangi tabana bölündüğüne göre %37 ile %59 arası
// değişiyor ve o seçim sayıyı istediğin yere taşır). Üçü de aynı
// makinede, aynı şekil, tamamlanmış hâlde ölçüldü -- yani bu fark işin
// kendisinden değil, koşudan koşuya değişimden geliyor. Buradan çıkan kural
// aşağıda; tek bir koşunun sayısına dayanıp "şu kadar hızlandı" demeyin.
//
// (O üçü örneğin eski hâlinde, gülden sonra elle eklenmiş bir "şekli
// tamamla" satırıyla alındı. O satır tam çokgeni bir kez daha üçgenlettiği
// için toplama fazladan bir üçgenleme katıyordu; satırsız sayının küçük
// çıkması beklenir. 18 ms gerçekten bandın ALTINDA -- ama TEK koşu, ve
// koşudan koşuya yayılma 1.6 kat. Farkı bir üçgenlemeye yazmadan önce
// satırsız hâlin de yayılmasını ölç.)
//
// (Bu örnek eskiden "146 nokta" gibi betikte karşılığı olmayan sayılar
// yazıyordu: dolgu şekil bitmeden de yayınlanıyor ve not her yayını ayrı
// ayrı bildiriyordu. Artık not ŞEKİL BAŞINA toplamı veriyor.)
//
// SOĞUK / SICAK farkı ÖLÇÜLDÜ (#143, canlı, gerçek donanım): her karede
// `resimleriSil(); çiz(Resim { gül })` yapan bir `canlandır` döngüsü 300
// kareyi 5.0 saniyede bitiriyor -- 60 kare/s, yani ısınmış gülün dolgusu
// 16.7 ms'lik bütçeye SIĞIYOR. Aynı koşuda düşen tek not ilk, soğuk gülün:
// 20 / 22 / 27 ms (üç koşu). Tek atışlık koşulardaki 18-38 ms'nin hepsi
// soğuk sayı. Yani tablonun "iyimser" olması artık hipotez değil: soğuk
// 20-38, sıcak <= 16.7 -- en az 1.2, en çok 2.3 kat. (Üst sınır ölçülmedi;
// 60 kare/s yalnız "bütçeye sığıyor" diyor, kaç ms olduğunu değil.) 400
// noktada da aynı tablo: 300 kare 5.0 s, yine tek not ve soğuk gülün, 36 ms
// -- yani ısınmış 400 noktalı gül de bütçeye sığıyor, soğuk/sıcak orada en
// az 2.2 kat. 700 noktada da: 300 kare 5.0 s, tek not, soğuk 49 ms -- en az
// 2.9 kat. Sıcak tarafın nerede bütçeyi aştığı hâlâ ölçülmedi; 700'de
// aşmıyor, 1000'de ölçü aleti aynı makinede 7-8 gül/s okuyor (iki karelik
// el sıkışma dâhil; daha hızlı bir makinede 15-17), yani orada aşıyor. Sınır
// 700 ile 1000 arasında bir yerde.
//
// Bu ölçüm bir süre yapılamadı, çünkü buradaki eski sav yanlıştı (#133
// incelemesi §2): "15-mesh-olcumu.kojo'nun döngüsünde not düşmüyor, demek
// ki sıcak <= 16.7 ms" çıkarımı şeklin TAMAMLANMIŞ olmasını gerektiriyordu,
// aletin gülü ise `canlandır` içinde hiç tamamlanmıyor ve kaplumbağanın
// kendisi için kuyruk boşalması canlandırma dönerken sayılmıyor (kapı
// bilerek böyle: boşalma canlandırmada kare başına 1.63 kez oluyor, ve bir
// sonraki kare şekle nokta ekleyebilir). Sınır orada hâlâ 50.1 ms.
//
// `Resim{}` İÇİN KAPI YOK (#143): resmin gövdesi bitince şekli de bitmiştir
// -- `çiz`/`sil` yeniden çizer, nokta eklemez. O yüzden canlandırma içinde
// kurulan bir Resim{} bütçeyi aşarsa konuşur; yukarıdaki soğuk/sıcak
// ölçümü tam bu yoldan geldi. Kaplumbağa ile resim arasındaki bu fark
// bilinçli: birinde belirsizlik var, ötekinde yok.
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
// göreceksin -- dolgu bir karelik bütçeyi aştığında iKojo bunu söylüyor,
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

// Burada eskiden elle bir "şekli tamamla" satırı vardı: `kalemiKaldır();
// noktayaGit(0, -220)`. Onsuz örnek SESSİZ kalıyordu (#133) -- bir dolgu
// şeklini "bitmiş" sayan yalnız iki yol vardı (kalem kalkık taşınma, boya
// değişimi), betiğin SON şekli ikisini de görmüyordu, ve bitmemiş bir şekil
// ancak ERKEN EŞİĞİ (3 x bütçe = 50.1 ms) aşarsa konuşuyordu. Yukarıdaki
// gül 22-35 ms, yani eşiğin altında: örneğin bütün amacı olan not hiç
// çıkmıyordu. Ölçülmeden görülmedi -- bu yalnız gerçek tarayıcıda oluyor.
//
// Boşluk kitaplık tarafında kapatıldı (#134): komut kuyruğu boşalmış ve
// betiği uyandırabilecek hiçbir şey kalmamışsa şekil bitmiştir, biriken
// süre bildirilir. Satır önce canlıda gereksizliği gösterilene dek yerinde
// tutuldu, sonra kaldırıldı: satırsız koşu "18 ms sürdü (251 nokta)" verdi.
// (İlk denemede "17 ms sürdü (193 nokta)" çıkmıştı -- o öykü 4. deneyde.)

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
//    AMA İKİ SAYIYI BÖLME. Notun iki biçimi var ve 1000'de ÖTEKİ biçimi
//    görürsün:
//
//      250'de    "... hesaplamak 32 ms SÜRDÜ (251 nokta)"
//      1000'de   "... ŞU ANA DEK 53 ms aldı (ŞİMDİLİK 643 nokta;
//                  şekil büyüdükçe artacak)"
//
//    İkincisi şeklin TOPLAMI değil: şekil daha 643 noktadayken, erken eşiği
//    (50.1 ms) aştığı anda düşen bir ara toplam. Not şekil başına en çok bir
//    kez düştüğü için nihai toplam HİÇ yazılmıyor -- şekil bitse bile, çünkü
//    o şekil bir kez konuştu diye imleniyor. Yani 53/32 gibi bir oran
//    büyümeyi ÇOK EKSİK gösterir.
//
//    Gerçek toplam ne kadar? Bu araçla BİLİNEMEZ, yukarıdaki sebeple. Elde
//    iki ayrı ölçüm var ve BİRBİRİNE BÖLÜNMEZ -- farklı şeyleri sayıyorlar:
//
//      ~95 ms   TEK bir üçgenleme, tamamlanmış 1000 noktalı çokgen.
//               Isıtılmış, yazılımsal çizici (#68; UcgenlemeUyarisi.scala'nın
//               "1000x7 (~95 ms) konuşuyor" satırı da bu).
//      230-240  Bir gülün ŞEKİL BAŞINA TOPLAMI: yarım yayınların hepsinin
//      ms       üst üste toplamı (1000 noktada gül başına ~10 yayın ölçüldü).
//               Sınama harness'inde, üçgenleme süresini toplayan geçici bir
//               sayaçla; aynı yazılımsal çizici, başka makine. (#125/#131
//               tartışması, kojojs-dev.)
//
//    İkisi çelişmiyor: sonuncu yayın zaten o ~95 ms'lik tam çokgen, öncekiler
//    büyüyen önekler, ve üst üste gelince ~2.5 katı çıkıyor. Ama bu cümle
//    yazılmadan iki sayı yan yana konsaydı, okuyan onları bölmeye davet
//    edilmiş olurdu -- bu deneyin tam da uyardığı hata.
//
//    Öğrenilecek şey burada: "sürdü" ile "şu ana dek" aynı şey değil.
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
//    İkinci gülün notu üçüncü yoldan geliyor -- kuyruk boşalması (#134).
//    Bu deney eskiden gülden sonraki elle eklenmiş "şekli tamamla" satırına
//    bağlıydı: o satır olmasaydı ikinci not zaten düşmezdi, zaman kapısı
//    yüzünden değil, ikinci gül hiç tamamlanmadığı için -- aynı gözlem,
//    yanlış sebep. O bağımlılık kalktı; satır da.
//
// 3. boyamaRenginiKur satırını sil. Dolgu hiç hesaplanmıyor, yalnız kalem izi
//    kalıyor -- şekil hâlâ görünür, çizim anında biter.
//
// 4. (Önce 3'ü geri al.) İkinci gülü bir TUŞA bağla: `gül(250, 7, 140, kırmızı)`
//    satırının yerine şu ikisini koy,
//
//      boyamaRenginiKur(kırmızı)
//      tuşaBasınca { t => yinele(40) { ileri(140); sağ(7 * 360.0 / 250) } }
//
//    çalıştır, boşluk tuşuna art arda bas. (Kenar 140 olunca gül tuvali
//    taşar; önemi yok, ölçülen şey dolgu hesabı.) İlk basışlarda not YOK.
//    7-8. basışta tek not, ve biçimi "şu ana dek ... aldı (şimdilik N nokta;
//    şekil büyüdükçe artacak)" -- "SÜRDÜ" değil. Ölçüldü: 7. basış, 57 ms,
//    "şimdilik 251 nokta". 251 burada tesadüf, eşiğin aşıldığı andaki ara
//    sayı (6 basış 241 nokta eder); aynı deney fareyle 8. tıkta 59 ms / 291.
//
//    NE ÖLÇÜYOR: üçüncü yolun NEREDE ÇALIŞMADIĞINI. "Kuyruk boşaldı" ile
//    "betik bitti" aynı şey değil: tuşlar arasında kuyruk boşalıyor ama bir
//    sonraki tuş şekle nokta ekleyecek. Orada "şu kadar SÜRDÜ (N nokta)"
//    demek yanlış sayıyı kesin diye söylemek olurdu. O yüzden üçüncü yol
//    ancak betiği uyandırabilecek hiçbir şey kalmadığında sayıyor:
//    `canlandır`, `yineleSayaçla`, `tuşaBasınca` ya da bir resim fare
//    işleyicisi varsa susuyor, ve eski yol (erken eşik, 50.1 ms) dürüst
//    biçimiyle konuşuyor. `ornekler/11-acilar-ve-radyan.kojo` böyle bir
//    betik.
//
//    193 ÖYKÜSÜ, çünkü bu yolun bir tuzağı daha vardı. İlk canlı koşuda
//    (satırsız, tuşsuz -- yani bu dosyanın bugünkü hâli) not "17 ms sürdü
//    (193 nokta)" dedi: kesin cümle, ama şeklin yalnız bir öneki. Sebebi
//    ölçüldü. Komut kuyruğu bir karede 8 ms'ye kadar iş yapıp kareye teslim
//    ediyor (#131; bu ölçüm alındığında 100'lük partiler ve 4 ms'lik
//    setTimeout aralarıyla çalışıyordu, sonuç aynı): bir kareye yüzlerce,
//    artık binlerce komut sığıyor. Yani kuyruk boşaldığı anda son kenarlar
//    henüz YAYINLANMAMIŞ olabiliyor; elimizdeki süre
//    de nokta sayısı da eksik. Düzeltildi: bekleyen yayın varsa not o yayını
//    bekliyor. Bu dosyayı olduğu gibi koşunca "18 ms sürdü (251 nokta)"
//    görüyorsan o düzeltme çalışıyor; 251'den küçük bir sayı görürsen yaz.
