// YÜZ BİN KOMUT: komut pompasının hızı, dolguyla
//
// Bu dosya da bir ÖLÇÜ ALETİ (15 gibi), öğretmek için değil. 100 000
// kaplumbağa komutu (50 000 adım x [ileri + sağ], yani 200 tur) TEK bir dolgulu şekil
// olarak çiziliyor ve baştan sona kaç milisaniye sürdüğü yazılıyor.
//
// NE ÖLÇÜYOR: komut pompasının (kojojs-dev#131) kuyruğu ne hızla
// boşalttığı, üstüne dolgunun bedeli. Kalemli (dolgusuz) sürümü
// ornekler/README'de: aynı döngü, boyamaRenginiKur satırı olmadan --
// MacBook'ta 108-165 ms (kojojs-dev#131). Buradaki sürüm o döngüye dolgu
// ekliyor: 50 001 noktalı, kendini kesen TEK çokgen.
//
// ESKİ YOLDA (libtess) BU BETİĞİ 100 000'DE KOŞTURMA: tarayıcı sekmesi
// kilitleniyor (MacBook, kojojs-dev#147 §7). 200 tur aynı gülü üst üste
// çiziyor ve kopyalar neredeyse ÇAKIŞIK: tur k'nin köşesi tur 0'ınkinden en
// çok 6e-7 birim uzakta (140 yarıçapta göreli 4e-9, pikselin milyarda biri;
// #157 incelemesi ölçtü). libtess'i pahalıya getiren tam bu: çakışıklığın
// son basamaklarda bozulması, yani dejenereye en yakın kesişimler. Aynı nokta
// sayısında bu yol tek güle göre 2.4-9.7 kat pahalı ve üssü kareselin üstünde
// (2.2-2.4; tek gül 1.7-1.9); 50 001 noktada TEK üçgenleme dakikalar eder,
// üstüne büyüyen şekil her karede baştan üçgenleniyor. Eski yolu ölçmek
// istiyorsan aşağıdaki `adım`ı küçült. Kopyaların gerçekten farklı olmasını
// istiyorsan tek kaldıraç tur başına kenarı/yarıçapı değiştirmek -- o zaman
// başka bir şekil olur, bu dosya değil.
//
// ÖLÇÜLDÜ (MacBook, kojojs-dev#147 §7; komut = 2 x adım):
//
//     adım     komut     eski yol (libtess seçeneği)   stencil (varsayılan)
//     50 000   100 000   sekme kilitleniyor            >= 497 ms (yedi koşu: 497-1145)
//      5 000    10 000   17 569 ms (*) / 59 090 ms     77 ms / 103-413 ms (beş koşu)
//        500     1 000   194 ms                        29-138 ms (dört koşu)
//
//     (*) Yazılan süre kuyruğun boşalmasına kadar; sekme ondan sonra da bir
//         süre kilitli kalıyor -- tamamlanmış şeklin son üçgenlemeleri
//         konumuOku'nun ARDINDAN geliyor, sayıya girmiyor.
//
//     10 000'in iki libtess sayısı AYNI işin iki koşusu (3.4 kat); iki
//     sütunda da yayılma paylaşılan makinenin (ölçüm sırasında başka iş
//     koşuyordu), dolgu yolunun değil -- en küçük değer yola en yakını,
//     15'in kuralı. Oran yine mertebe: 10 000'de 140-570 kat.
//
// (Bu dosyanın ilk sürümü "eski yol 719 ms, not 76 ms (7 993 nokta)" diyordu.
// O koşu aslında STENCİL'di: not, #154'ten önce stencil yolunun da düşürdüğü
// yanlış nottu, libtess'in değil.)
//
// LİBTESS SEÇENEĞİ eski yolu elle açar. Tarayıcı konsolunda
// `localStorage.kojoDolgu = "libtess"` yaz, sayfayı yenile; bitince
// `delete localStorage.kojoDolgu`, yine yenile (ornekler/14-agir-dolgu.kojo,
// 2. deney). Seçenek açıkken panelin ilk satırı "Eski dolgu yolu (libtess)
// elle açık ..." olur -- hangi yolu ölçtüğünü oradan bil.
//
// KURALLAR 15'inkiyle aynı: tek kaplumbağa, tek sayı değil birkaç koşu (bu
// ölçüm koşudan koşuya oynuyor), ve öncesi/sonrası AYNI makinede.

sil()
hızıKur(çokHızlı)
gizle()
kalemKalınlığınıKur(0)
boyamaRenginiKur(mavi)   // bu satırı silersen kalemli (dolgusuz) sürüm olur

// 250 x 7 gül, yarıçap 140: kenar = 2 * R * sin(kat * pi / nokta) = 24.6,
// dönüş = 7 * 360 / 250 = 10.08 -- 14 ve 15'teki gül işlevinin sabitleri.
// Her 250 adım bir tur; sabitler değişmediği için adım sayısından başka
// hiçbir şey şekli belirlemiyor (tur ve nokta ayrı parametre olsaydı yalnız
// çarpımları sayardı -- #157 incelemesi).
tanım üstÜsteGül(adım: Sayı): Birim = {
  yinele(adım) { ileri(24.6); sağ(10.08) }
}

// komut = 2 x adım: 50 000 adım = 200 tur = 100 000 komut. Eski yol için küçült.
dez t0 = buAn
dez adım = 50000
üstÜsteGül(adım)
// konumuOku kuyruğun sonuna giriyor: geri çağrım, önündeki komutların hepsi
// işlenince tetikleniyor -- bitiş damgası bu.
konumuOku { _ => satıryaz(s"adım=$adım (${adım / 250} tur) -> ${2 * adım} komut, dolgulu: " + (buAn - t0) + " ms") }
