// DOLGU ÇİZİM MALİYETİ: sabit bir ölçüm düzeneği (kojojs-dev#125 -> #147)
//
// Bu dosya bir ÖRNEK değil, bir ÖLÇÜ ALETİ. Öğretmek için değil, bir
// değişikliğin ÖNCESİ ve SONRASI aynı şeyle ölçülsün diye var.
//
// ÖLÇTÜĞÜ ŞEY: kesişen bir şekli HER KAREDE yeniden çizen bir döngünün
// saniyede kaç kare verdiği. Bugün iKojo bu dolguyu üçgenlere ayırıp PIXI'ye
// ÜÇGEN BAŞINA BİR drawPolygon ile veriyor: aşağıdaki 250 noktalı gülde
// yayın başına 2 998 çağrı, 1000 noktalıda 11 998.
//
// #125 (tek mesh) BU ALET İÇİN YAZILDI VE "YAPILMAYACAK" DİYE KAPANDI: mesh
// kendi diliminde 5-14 kat ucuz, ama dilim gülün <= %5-10'u (1000 noktada
// libtess %58) ve uçtan uca kazanç <= %8 -- bu aletin çözünürlüğünün altında.
// Aşağıdaki #125 göndermeleri o ölçümlerin tarihi; alet duruyor, çünkü bir
// sonraki kaldıraç (#147: stencil tamponuyla dolgu, hiç üçgenlemeden) aynı
// şeyle ölçülecek. Öncesi 1000 noktada 15-17 gül/s -- AMA MAKİNEYE GÖRE:
// ikinci bir makinede aynı yayın 7-8 okudu (notlar 105-112 ms, ötekinde
// 58-81). Sayı taşınmaz; öncesi ve sonrası AYNI makinede alınmalı (2. adım).
//
// BU BETİK O DEĞİŞİKLİKLERİ YAPAMAZ -- dolgunun nasıl çizildiği kitaplığın
// içinde. Betiğin işi kareyi SAYMAK: değişiklik uygulanmadan önce bir kez,
// uygulandıktan sonra bir kez koşturulur, iki çıktı karşılaştırılır.
//
// #68'DE ÜÇ KEZ YANLIŞ ÖLÇÜLDÜ. Buradaki üç kural o üç hatanın karşılığı:
//
//   1. TEK KAPLUMBAĞA, sil() ile yeniden kullanılır. Her karede yeni bir
//      kaplumbağa/resim yaratıp eskisini bırakmak sahneyi biriktiriyor ve
//      ölçüm senaryoyu değil büyüyen sahneyi ölçüyor (298 ms/kare diye
//      okunan sayı buydu; gerçeği ~20 ms'ydi).
//   2. ISINMA kareleri sayılmaz. Isıtılmamış çizici ilk kareleri şişiriyor;
//      #125'teki çelişkili iki ölçümün farkı da buradan geliyor olabilir.
//   3. TEK SAYI DEĞİL, birkaç saniyelik dizi okunur. Ölçümler koşudan koşuya
//      %25 oynuyor; tek sayı yanıltır.
//   4. BOYANMIŞ GÜL sayılır: her karede en fazla bir gül başlar ve bir gül
//      ancak öncekinin kuyruğu bittikten (`konumuOku`) SONRAKİ karede
//      başlar. Bu kuralın iki tarihi var, ikisi de ölçümle:
//
//      İlk sürüm `canlandır` tikini sayıyordu ve YANLIŞTI: eski komut pompası
//      100 komutta bir 4 ms'lik `setTimeout` arası veriyor, 250 komutluk bir
//      gül birkaç kareye yayılıyordu; not "146 nokta" dedi, oysa betik 250
//      çiziyor -- sayılan tik, biten gül değildi. İkinci sürüm her gülün
//      ardına `konumuOku` koyup oradan bir sonraki gülü başlatıyordu.
//
//      Sonra pompa değişti (#131): kuyruk artık bir karede 8 ms iş yapıp
//      kareye teslim ediyor, ve 250 noktalı gül ~0.3 ms'de bitiyor. İkinci
//      sürümün zinciri o rejimde gülleri BOYANMADAN siliyordu -- ölçüldü,
//      on gülün sekizi hiç yayınlanmadan bitti; sayılan şey gül/s değil
//      KUYRUK hızı olurdu, ve #125'in kazancı en çok render tarafında olduğu
//      için alet mesh'i kendi aleyhine ölçerdi. Şimdi gül `canlandır` karesinde
//      başlıyor, o karede bitiyor (kuyruk boşken verilen komut eşzamanlı
//      koşuyor) ve karenin sonunda boyanıyor; bir sonraki gül bir sonraki
//      karede -- ama İKİ kare sonra, bir kare değil. Çünkü gül karenin
//      canlandır gövdesinde bitmezse (dilim dolup pompa sonraki kareye
//      teslim ettiyse) sonraki karede pompanın devamında bitiyor, ve o
//      karenin canlandır gövdesi hemen ardından `sil()` derse gül BOYANMADAN
//      gidiyor. Ölçüldü: tam takımda, yük altında, aletin savı böyle kırmızıya
//      döndü. O yüzden el sıkışma iki karelik: gül biter, bir kare boyanır,
//      ondan sonraki kare yenisini başlatır. Sayılan gül = boyanan gül,
//      yapısal olarak. Sınama savı hâlâ "sayılan her gülün en az bir
//      boyaması var" (kojo.MeshAletiOlcumTest).
//
// BU YÜZDEN ÜST SINIR KARE HIZININ YARISI: gül başına en az iki kare, yani
// ucuz gülde en çok ~30 gül/s. 30 okumak "üçgenleme + çizim bir kareye
// sığıyor" demek, ötesi bu aletle görülmez; #125'in asıl yeri olan ağır
// uçta (nokta = 1000, gül başına yüz milisaniyeler) iki karelik el sıkışma
// gül başına ~16 ms, yani sayının küçük bir payı. Okunan sayı artık
// üçgenleme + çizim maliyetinin haberi -- tam #125'in dokunduğu yer. Eski
// pompayla alınan sayılarla (250x7 -> 38-41 gül/s) KARŞILAŞTIRILAMAZ: o
// sayı %80 oranında pompanın bekleme süresiydi (#131).
//
// UYARI: nokta = 1000 yaparsan çıktı panelinde dolgu notu da görürsün
// (#68/#124). Beklenen -- burada tam da o pahalı durumu ölçüyoruz.
//
// AMA EŞİK SANDIĞIN YERDE DEĞİL. Bu aletin gülleri hiç TAMAMLANMIYOR:
// `sil()` boyamaRenginiKur'dan önce geldiği için boyamayıİşle boş çokgen
// buluyor. Tamamlanmamış bir şekil ise bir karelik bütçeyi (16.7 ms) aşınca
// değil, ERKEN EŞİĞİ (3 x bütçe = 50.1 ms) aşınca konuşuyor. Yani buradaki
// not 16.7'nin değil 50.1'in haberi.
//
// Ölçüldü (gerçek donanım, v59): nokta = 1000'de not gerçekten düşüyor ve
// "şu ana dek 53 / 57 / 59 / 66 / 75 / 91 / 97 ms" diye okunuyor -- yani
// TAMAMLANMIŞ biçim ("... sürdü") değil, sürmekte olan biçim. nokta = 250'de
// ise hiç not düşmüyor, ve bu "250 ucuz" demek DEĞİL: yalnız 50.1'in altında
// kaldığını söylüyor. (kojojs-dev#133, #134.)

dez nokta = 250       // 250: bugünkü örneklerin ölçeği. 1000 dene: fark büyür.
dez kat = 7           // nokta ile ARALARINDA ASAL olmalı, yoksa yol tekrar eder
dez yarıçap = 140.0
dez ısınmaKare = 5    // bu güller sayılmıyor (kural 2)
dez kaçSaniye = 10    // kaç satır rapor basılacak (kural 3)

silVeSakla()
artalanıKur(beyaz)
hızıKur(çokHızlı)
gizle()

// Kalem ve dolgu HER KAREDE yeniden kuruluyor: sil() kaplumbağayı eve
// göndermiyor ama açık dolguyu da sürdürmüyor. Dışarıda bir kez kurmak
// ikinci kareden sonra sessizce boş şekil ölçmeye yol açardı.
tanım gülÇiz(nokta: Sayı, kat: Sayı, yarıçap: Kesir): Birim = {
  kalemKalınlığınıKur(0)
  boyamaRenginiKur(mavi)
  dez kenar = 2 * yarıçap * sinüs(radyana(kat * 180.0 / nokta))
  dez dönüş = kat * 360.0 / nokta
  yinele(nokta) { ileri(kenar); sağ(dönüş) }
}

den kare = 0   // tamamlanmış gül sayısı
den sayaç = 0
den satır = 0
den sonSaniye = 0
den tabanKuruldu = yanlış
den ilkSınırGeçildi = yanlış

satıryaz("ölçüm başlıyor: " + nokta + " nokta x " + kat + " kat, " + ısınmaKare + " gül ısınma")

// HER RAPOR SATIRI TAM BİR SANİYEYİ KAPSAMALI. İki bayrak bunun için, ve
// ikisi de gerekli (#127 incelemesi §1):
//
//   tabanKuruldu    -- saniye tabanı ısınma BİTİNCE alınıyor. Isınmadan önce
//                      alınsaydı taban ısınma boyunca bayatlar, sayılan ilk
//                      kare hemen bir "sınır" sayılır ve panele tek kareli
//                      bir satır düşerdi.
//   ilkSınırGeçildi -- taban alındığı an saniyenin ORTASINDAYIZ, yani ilk
//                      sınıra kadar geçen süre kısmi. O satır BASILMIYOR,
//                      yalnız sayacı hizalıyor.
//
// İkisi olmadan raporun ilk (bazen ilk iki) satırı eksik saniye sayıyordu:
// 30 kare/s'lik bir koşuda "29-31" yerine "1-31" okunuyordu -- yani aletin
// verdiği aralık ölçtüğü şeyi değil kendi kurulum artığını gösteriyordu.
// Bedeli 1-2 saniyelik gecikme (taban tam bir saniye sınırına düşerse 1,
// düşmezse 2 -- ölçüldü); ölçümün dürüstlüğü ona değer.
den bitti = yanlış
den gülBitti = doğru   // bir sonraki gül ancak bu doğruyken
den boyandı = doğru    // ... ve gülün bittiği kareden sonra bir kare geçince

tanım tur(): Birim = {
  gülBitti = yanlış
  sil()
  gülÇiz(nokta, kat, yarıçap)
  // Kuyruk buraya varınca gül BİTMİŞTİR: sayımın tek doğru yeri burası.
  // Buradan yeni gül BAŞLATILMIYOR (kural 4): bayrak kalkar, canlandır alır.
  konumuOku { _ =>
    gülBitti = doğru
    kare += 1
    eğer (kare > ısınmaKare) {
      dez şuAn = BuAn().saniye
      eğer (!tabanKuruldu) {
        sonSaniye = şuAn
        sayaç = 0
        tabanKuruldu = doğru
      }
      eğer (şuAn != sonSaniye) {
        sonSaniye = şuAn
        eğer (ilkSınırGeçildi) {
          satır += 1
          satıryaz(satır + ". saniye: " + sayaç + " gül")
          eğer (satır >= kaçSaniye) {
            bitti = doğru
            satıryaz("bitti. En düşük ve en yüksek satırı birlikte yazın --")
            satıryaz("tek sayı vermeyin, bu ölçüm koşudan koşuya oynuyor.")
          }
        }
        yoksa {
          ilkSınırGeçildi = doğru
        }
        sayaç = 0
      }
      sayaç += 1
    }
    eğer (bitti) durdur()
  }
}

// Her karede en fazla bir gül, ve iki karelik el sıkışma (kural 4): gülün
// bittiği ilk karede yalnız `boyandı` kalkar (o kare gülü boyar), bir
// sonraki kare yenisini başlatır. Önceki bitmediyse kare boş geçer.
canlandır {
  eğer (gülBitti && !bitti) {
    eğer (boyandı) { boyandı = yanlış; tur() }
    yoksa boyandı = doğru
  }
}

// NASIL KULLANILIR
//
// 1. Olduğu gibi koştur, çıkan 10 satırı not et (nokta = 250). DİKKAT, BU
//    ADIM DOYUYOR: 250 noktalı gülün dolgusu bir kareye sığıyor ve iki
//    karelik el sıkışma yüzünden alet kare hızının yarısını okur -- ölçüldü
//    (harness, SwiftShader): 26.5 gül/s, 54 kare/s. #125 o ölçekte dolguyu
//    ~3 ms'den ~0.3 ms'ye indirse de ikisi bir kareye sığdığı için alet
//    öncesi de sonrası da ~27 okur. Yani 250'de "değişmedi" görürsen bu
//    #125'in değil aletin haberi. Bu adım sağlık denetimi: ~27-30 okunuyorsa
//    alet ve makine beklendiği gibi. Canlıda ölçüldü (gerçek donanım,
//    #131): eski pompada da yeni pompada da 30 -- tavanın ta kendisi.
// 2. nokta = 1000 yap, yine koştur ve not et. SİNYAL BURADA: kare hızı işin
//    kendisi yüzünden düşüyor (ölçüldü, aynı harness: 6.6 gül/s, 13 kare/s),
//    yani sayı dolgu maliyetini izliyor. Canlıda (gerçek donanım, #131):
//    eski pompa 4-5, yeni pompa 15-17 gül/s -- fark yalnız pompa payının
//    gitmesi değil: gül tek karede bitince TEK kez üçgenleniyor, eskiden
//    ~10 karede büyüyen önek her seferinde yeniden üçgenleniyordu (notlar
//    145-155 ms'den 80 ms'ye indi). Öncesi bu 15-17 -- o makinede. Aynı
//    yayın ikinci bir makinede 7-8 okudu (notlar 105-112 ms; ölçüldü,
//    #148 öncesi/sonrası harness'te aynı, yani gerileme değil makine).
//    Karşılaştırmayı hep aynı makinede yap.
// 3. Ölçülen değişiklikten (bugün #147) sonra ikisini de tekrarla.
// 4. Karşılaştırmayı aralık olarak, 1000 üstünden yaz: "1000'de 6-7 -> ?
//    gül/s". (Eski pompadan kalan "250'de 12-15 -> 18-22" gibi sayılar bu
//    rejimde İMKÂNSIZ, 250 ~27'de doyuyor; öyle bir örnek yanıltır.)
//
// SINIRI: saniye çözünürlüğü. BuAn() saniyeden ince ölçmüyor, o yüzden
// buradaki sayı gül SÜRESİ değil, saniyedeki GÜL sayısı. Kare süresinin
// dağılımı (ortanca, en kötü kare) bu düzenekle görülmez; onun için
// tarayıcının kendi profilleyicisi ya da #68'deki sınama harness'i gerekir.
//
// YAVAŞ UÇTA ÇÖZÜNÜRLÜK: satırlar düzeltmeden sonra da +-1 gül oynuyor --
// tam sayı saniyeye bölmenin kaçınılmaz artığı, hata değil. Ama bu artık
// ORANSAL: 60 kare/s'te %3, 12 kare/s'te %17. Yani 2. adım (nokta = 1000)
// yavaş bir makinede 12 kare/s verirse aletin kendi çözünürlüğü tek başına
// %17'lik bir aralık üretir; #125'in kazancı bunun altındaysa öncesi/sonrası
// aralıkları örtüşür ve karşılaştırma sonuç vermez. Öyle bir durumda kaçSaniye
// artırılmalı ya da tarayıcının profilleyicisine geçilmeli.
//
// SINIRIN ÖTEKİ YÜZÜ: bir gül 1 saniyeyi AŞARSA her gül bir saniye sınırı
// sayılır ve panele "1 gül" satırları düşer. O satırlar "saniyede 1 gül"
// DEĞİL, "gül başına en az 1 saniye" diye okunmalı -- ve orada bu aletin
// çözünürlüğü bitmiştir, sayılar arasındaki farkı göstermez. 2. adımda
// (nokta = 1000) yavaş bir makinede bu bölgeye girilebilir.
