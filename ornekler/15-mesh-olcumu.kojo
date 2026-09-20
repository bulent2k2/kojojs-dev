// DOLGU ÇİZİM MALİYETİ: sabit bir ölçüm düzeneği (kojojs-dev#125)
//
// Bu dosya bir ÖRNEK değil, bir ÖLÇÜ ALETİ. Öğretmek için değil, bir
// değişikliğin ÖNCESİ ve SONRASI aynı şeyle ölçülsün diye var.
//
// ÖLÇTÜĞÜ ŞEY: kesişen bir şekli HER KAREDE yeniden çizen bir döngünün
// saniyede kaç kare verdiği. Bugün ikojo bu dolguyu üçgenlere ayırıp PIXI'ye
// ÜÇGEN BAŞINA BİR drawPolygon ile veriyor: aşağıdaki 250 noktalı gülde
// yayın başına 2 998 çağrı, 1000 noktalıda 11 998. #125 bunun yerine tek bir
// mesh vermeyi tartışıyor; ölçülmüş kazanç 8-14 kat ama mutlak olarak küçük.
//
// BU BETİK O DEĞİŞİKLİĞİ YAPAMAZ -- hangi PIXI nesnesinin kullanıldığı
// kitaplığın içinde. Betiğin işi kareyi SAYMAK: #125 uygulanmadan önce bir
// kez, uygulandıktan sonra bir kez koşturulur, iki çıktı karşılaştırılır.
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
//   4. TAMAMLANMIŞ GÜL sayılır, canlandırma tiki değil. İlk sürüm `canlandır`
//      tikini sayıyordu ve YANLIŞTI: kaplumbağa komutları kuyruğa giriyor,
//      `scheduleLater` ilk 100 komutu eşzamanlı koşturup gerisini erteliyor,
//      yani 250 komutluk bir gül birkaç kareye yayılıyor. Kanıtı gerçek
//      tarayıcı çıktısında görüldü (#125): not "146 nokta" dedi, oysa betik
//      250 çiziyor -- yani sayılan tik, biten gül değildi. Şimdi her gülün
//      ardına `konumuOku` konuyor; o geri çağrım kuyruk oraya varınca, yani
//      gül GERÇEKTEN bitince çalışıyor.
//
// ALET BOYAMAYLA EŞLEŞİYOR MU (#130 incelemesi §2): sayılan şey biten gül,
// ama biten bir gül BOYANMAMIŞ olabilir -- `scheduleLater`in `setTimeout(0)`
// hoplamaları rAF'i beklemiyor, ve bir gülün dolgu düğümü sonraki `sil()` ile
// kalkıyor. İki rAF arasında tamamlanıp kaldırılan bir gülün üçgenlemesi
// ödenir ama GPU'ya hiç gitmez -- ve #125'in kazancı en çok render tarafında,
// yani böyle bir alet mesh'i kendi aleyhine ölçer.
//
// Ölçüldü (kojo.MeshAletiOlcumTest, SwiftShader, gül başına BOYAMA dağılımı --
// ortalama değil, çünkü ortalama boyanmamış gülü saklar):
//
//   nokta =  250 ->  5,4,3,3,3,3,3,2,2,2   en az 2   (üç koşuda da en az 2)
//   nokta = 1000 ->  9,10,13,11,10         en az 9
//   nokta =    4 ->  0,0,0,0,0,0,0,0,0,0   en az 0
//
// Yani bu aletin iki ölçeğinde (250 ve 1000) endişe ISIRMIYOR: her gül birkaç
// kareye yayılıyor, EN AZ boyanan gül bile iki kez boyanıyor, sayılan gül ile
// boyanan gül aynı. Mekanizma yine de gerçek -- 4 noktalı gülde on gülün onu
// da ilk rAF ateşlenmeden bitiyor ve hiç boyanmıyor. O yüzden aleti çok daha
// ucuz bir şekle çevirirsen ya da çok hızlı bir makineye taşırsan önce o
// dağılımı yeniden ölç; sınama savı gül başına EN KÜÇÜK boyama sayısı,
// kırılırsa haber verir.
//
// UYARI: nokta = 1000 yaparsan çıktı panelinde "dolgu bir karelik bütçeyi
// aştı" notu da görürsün (#68/#124). Beklenen -- burada tam da o pahalı
// durumu ölçüyoruz.

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

tanım tur(): Birim = {
  sil()
  gülÇiz(nokta, kat, yarıçap)
  // Kuyruk buraya varınca gül BİTMİŞTİR: sayımın tek doğru yeri burası.
  konumuOku { _ =>
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
    eğer (!bitti) tur()
  }
}

tur()

// NASIL KULLANILIR
//
// 1. Olduğu gibi koştur, çıkan 10 satırı not et (nokta = 250).
// 2. nokta = 1000 yap, yine koştur ve not et.
// 3. #125'in değişikliğinden sonra ikisini de tekrarla.
// 4. Karşılaştırmayı aralık olarak yaz: "250'de 12-15 -> 18-22 gül/s".
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
