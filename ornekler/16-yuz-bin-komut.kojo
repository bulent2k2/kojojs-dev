// YÜZ BİN KOMUT: komut pompasının hızı, dolguyla
//
// Bu dosya da bir ÖLÇÜ ALETİ (15 gibi), öğretmek için değil. 100 000
// kaplumbağa komutu (200 tur x 250 x [ileri + sağ]) TEK bir dolgulu şekil
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
// çiziyor ama kenarlar ÇAKIŞIK DEĞİL -- her `ileri(24.6)` kayan noktada
// biraz kayıyor, 200 kopya birbirine neredeyse paralel, ve libtess'in
// kesişme sayısı kopya sayısının karesiyle patlıyor (10 000 komutluk
// 200 x 25 bile 59 saniye; 4001 noktalı TEK gül 3-4 saniye); üstüne büyüyen
// şekil her karede baştan üçgenleniyor. Eski yolu ölçmek istiyorsan
// aşağıdaki (tur, nokta)'yı küçült.
//
// ÖLÇÜLDÜ (MacBook, kojojs-dev#147 §7):
//
//     (tur, nokta)   komut     eski yol (libtess seçeneği)   stencil (varsayılan)
//     (200, 250)     100 000   sekme kilitleniyor            497 / 544 / 546 ms
//     (20, 250)       10 000   17 569 ms (*)                 77 ms
//     (200, 25)       10 000   59 090 ms                     --
//     (20, 25)         1 000   194 ms                        --
//
//     (*) Yazılan süre kuyruğun boşalmasına kadar; sekme ondan sonra da bir
//         süre kilitli kalıyor -- tamamlanmış şeklin son üçgenlemeleri
//         konumuOku'nun ARDINDAN geliyor, sayıya girmiyor.
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
// dönüş = 7 * 360 / 250 = 10.08. 14 ve 15'teki gül işlevinin sabitleri.
tanım üstÜsteGül(tur: Sayı, nokta: Sayı): Birim = {
  yinele(tur) {
    yinele(nokta) { ileri(24.6); sağ(10.08) }
  }
}

// tur x nokta x 2 komut: (200, 250) = 100 000. Eski yol için küçült (yukarıdaki tablo).
dez (tur, nokta) = (200, 250)
dez t0 = buAn
üstÜsteGül(tur, nokta)
// konumuOku kuyruğun sonuna giriyor: geri çağrım, önündeki komutların hepsi
// işlenince tetikleniyor -- bitiş damgası bu.
konumuOku { _ => satıryaz(s"${2 * tur * nokta} komut, dolgulu: " + (buAn - t0) + " ms") }
