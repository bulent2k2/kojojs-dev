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
//     251 nokta -> 22 / 32 / 35 ms    (üç koşu)
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
// (Bu örnek eskiden "146 nokta" gibi betikte karşılığı olmayan sayılar
// yazıyordu: dolgu şekil bitmeden de yayınlanıyor ve not her yayını ayrı
// ayrı bildiriyordu. Artık not ŞEKİL BAŞINA toplamı veriyor.)
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
// #134 BU SINIRI DEĞİŞTİRMİYOR: yeni üçüncü yol (kuyruk boşalması) yalnız
// canlandırma DÖNMÜYORKEN sayılıyor, alet ise gülünü `canlandır` döngüsünde
// çiziyor. Orada şekil hâlâ tamamlanmıyor, ve sınır hâlâ 50.1 ms. (Kapı
// bilerek böyle: boşalma canlandırmada kare başına 1.63 kez oluyor.)
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

// ŞEKLİ TAMAMLA -- bu satır olmadan örnek SESSİZ kalıyordu.
//
// Bir dolgu şeklini tamamlayan tek şey kalem kalkık taşınma ya da boya
// değişimi. İkisi de gelmezse şekil "bitmemiş" sayılıyor, ve bitmemiş bir
// şekil ancak ERKEN EŞİĞİ (3 x bütçe = 50.1 ms) aşarsa not düşürüyor.
// Yukarıdaki gül gerçek donanımda 35 ms (ölçüldü, 251 nokta) -- yani
// eşiğin altında, ve örneğin bütün amacı olan not hiç çıkmıyordu.
// Ölçülmeden görülmedi, çünkü bu yalnız gerçek tarayıcıda oluyor.
//
// SATIR ARTIK GEREKSİZ OLMALI: kitaplık tarafındaki boşluk kapatıldı (#134 --
// komut kuyruğu boşalıp canlandırma da dönmüyorsa betik bitmiştir, şekil
// büyüyemez, ve biriken süre bildirilir). Ama bunu hâlâ SATIR DURURKEN
// söyleyemeyiz: satır varken not zaten eski yoldan düşüyor. Ölçen deney
// aşağıda, 4. sırada. O deney notun geldiğini gösterene dek satır kalıyor --
// örneğin bütün amacı o not, ve bir kez sessizliğe düşürüldü.
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
//    DİKKAT: bu deney yukarıdaki "şekli tamamla" satırına da BAĞLI. O satır
//    olmasaydı ikinci not zaten düşmezdi -- ama zaman kapısı yüzünden değil,
//    ikinci gül hiç tamamlanmadığı için. Aynı gözlem, yanlış sebep. (#134'ten
//    sonra bu bağımlılık kalkmış OLMALI: satır olmasa da kuyruk boşalınca not
//    düşer. 4. deney bunu ölçüyor; ölçülene dek satırı yerinde bırak.)
//
// 3. boyamaRenginiKur satırını sil. Dolgu hiç hesaplanmıyor, yalnız kalem izi
//    kalıyor -- şekil hâlâ görünür, çizim anında biter.
//
// 4. (Önce 3'ü geri al.) "ŞEKLİ TAMAMLA" başlıklı satırı -- yani
//    `kalemiKaldır(); noktayaGit(0, -220)` -- SİL. Not yine de düşmeli.
//
//    NE ÖLÇÜYOR: bir şeklin "bittiğini" anlamanın üçüncü yolunu (#134).
//    Eskiden yalnız iki yol vardı (kalem kalkık taşınma, boya değişimi) ve
//    betiğin SON şekli çoğu zaman ikisini de görmüyordu; o yüzden bu örnek
//    sessiz kalmış, ve o satır elle eklenmişti. Artık üçüncü yol var: komut
//    kuyruğu boşalıyor ve canlandırma da dönmüyorsa betik bitmiştir.
//
//    NOT BİÇİMİ de değişmeli: "şu ana dek ... aldı (şimdilik N nokta)" değil,
//    "hesaplamak ... SÜRDÜ (N nokta)" -- çünkü şekil artık büyüyemez.
//
//    SAYI 251 OLMALI, ve buna ayrıca bak. İlk canlı koşuda "17 ms sürdü
//    (193 nokta)" çıkmıştı: kesin cümle, ama şeklin yalnız bir öneki. Sebebi
//    ölçüldü -- kuyruk iki kare arasında yüzlerce komut işleyebiliyor. Komut
//    kuyruğu 100'lük partiler hâlinde koşuyor: 99 komut eşzamanlı, 100.'de
//    tarayıcıya dönülüyor ve o dönüş ~4.2 ms'ye kelepçeleniyor. Yani kelepçe
//    komut başına DEĞİL, parti başına; bir kareye (16.7 ms) dört parti,
//    yani ~400 komut sığıyor. Sonuç: boşalma anında son onlarca kenar henüz
//    YAYINLANMAMIŞ oluyor ve elimizdeki süre de nokta sayısı da eksik.
//    Düzeltildi: bekleyen yayın varsa not o yayını bekliyor. 251'den küçük
//    bir sayı görürsen düzeltme çalışmıyor demektir, yaz.
//
//    NEREDE ÇALIŞMAZ, bilerek: bu üçüncü yol ancak betiği UYANDIRABİLECEK
//    hiçbir şey kalmadığında sayıyor. `canlandır`, `yineleSayaçla`,
//    `tuşaBasınca` ve
//    resim fare işleyicilerinden biri varsa kuyruk boşalsa da şekle nokta
//    gelebilir -- orada susuyoruz, çünkü "şu kadar SÜRDÜ (N nokta)" demek
//    yanlış sayıyı kesin diye söylemek olurdu. O betiklerde eski yol
//    (erken eşik, 50.1 ms) hâlâ geçerli ve dürüst biçimiyle konuşuyor.
//    `ornekler/11-acilar-ve-radyan.kojo` böyle bir betik.
//
//    Not ÇIKMAZSA satırı geri koy ve söyle: #134 canlıda çalışmıyor demektir,
//    ve bunu ancak gerçek tarayıcı gösterir -- birim sınamaları yeşil.
