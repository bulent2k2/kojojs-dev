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
//
// UYARI: nokta = 1000 yaparsan çıktı panelinde "dolgu bir karelik bütçeyi
// aştı" notu da görürsün (#68/#124). Beklenen -- burada tam da o pahalı
// durumu ölçüyoruz.

dez nokta = 250       // 250: bugünkü örneklerin ölçeği. 1000 dene: fark büyür.
dez kat = 7           // nokta ile ARALARINDA ASAL olmalı, yoksa yol tekrar eder
dez yarıçap = 140.0
dez ısınmaKare = 30   // bu kareler sayılmıyor (kural 2)
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

den kare = 0
den sayaç = 0
den satır = 0
den sonSaniye = BuAn().saniye

satıryaz("ölçüm başlıyor: " + nokta + " nokta x " + kat + " kat, " + ısınmaKare + " kare ısınma")

canlandır {
  sil()
  gülÇiz(nokta, kat, yarıçap)
  kare += 1
  eğer (kare > ısınmaKare) {
    sayaç += 1
    dez şuAn = BuAn().saniye
    eğer (şuAn != sonSaniye) {
      satır += 1
      satıryaz(satır + ". saniye: " + sayaç + " kare")
      sayaç = 0
      sonSaniye = şuAn
      eğer (satır >= kaçSaniye) {
        canlandırmayıDurdur()
        satıryaz("bitti. En düşük ve en yüksek satırı birlikte yazın --")
        satıryaz("tek sayı vermeyin, bu ölçüm koşudan koşuya oynuyor.")
      }
    }
  }
}

// NASIL KULLANILIR
//
// 1. Olduğu gibi koştur, çıkan 10 satırı not et (nokta = 250).
// 2. nokta = 1000 yap, yine koştur ve not et.
// 3. #125'in değişikliğinden sonra ikisini de tekrarla.
// 4. Karşılaştırmayı aralık olarak yaz: "250'de 44-52 -> 48-57 kare/s".
//
// SINIRI: saniye çözünürlüğü. BuAn() saniyeden ince ölçmüyor, o yüzden
// buradaki sayı kare SÜRESİ değil, saniyedeki kare SAYISI. Kare süresinin
// dağılımı (ortanca, en kötü kare) bu düzenekle görülmez; onun için
// tarayıcının kendi profilleyicisi ya da #68'deki sınama harness'i gerekir.
