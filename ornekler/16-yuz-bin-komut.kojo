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
// NE ÖLÇMÜYOR: eski dolgu yolunun (libtess) en kötü hâlini. 200 tur aynı
// 250 x 7 gülü ÜST ÜSTE çiziyor; çakışık kenar libtess'te yeni kesişme
// değil, o yüzden 50 001 nokta eski yolda bile dakikalar değil yüzlerce
// milisaniye alıyor (MacBook, kojojs-dev#147 §7: 719 ms, notu "76 ms
// (7 993 nokta)"). Kesişme sayısını artırmak istiyorsan tur başına yarıçapı
// değiştir -- o zaman gül başka bir betik olur, bu dosya değil.
//
// ÖLÇÜLDÜ (MacBook, kojojs-dev#147 §7):
//
//     eski yol (libtess seçeneği açık)   719 ms, not düşüyor
//     stencil (bugünkü varsayılan)       (yayın sonrası; buraya yazılacak)
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

dez t0 = buAn
üstÜsteGül(200, 250)
// konumuOku kuyruğun sonuna giriyor: geri çağrım, önündeki 100 000 komut
// işlenince tetikleniyor -- bitiş damgası bu.
konumuOku { _ => satıryaz("100 000 komut, dolgulu: " + (buAn - t0) + " ms") }
