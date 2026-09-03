package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// Guards the Turkish (Koco) seam. The import stack below is a verbatim copy of
// `scalafiddle.defaultSource` in kojojs-editor's application.conf (with
// TestKojoWorld standing in for KojoWorldImpl, which needs a real canvas).
//
// The point is that this FILE COMPILES: that is what proves the four wildcard
// imports -- builtins, turtle, svTurtle, trTurtle -- coexist without ambiguity
// and that the Turkish names typecheck against the real runtime. That is
// exactly the check the compiler-server performs on every fiddle.
class TurkishPreludeTest extends AnyFunSuite with Matchers {

  test("a Turkish script compiles and runs against the editor prelude") {
    import kojo.{SwedishTurtle, TurkishTurtle, Turtle, Vector2D, Picture}
    import kojo.doodle.Color._
    import kojo.Speed._
    import kojo.RepeatCommands._
    import kojo.syntax.Builtins
    implicit val kojoWorld = new TestKojoWorld()
    val builtins = new Builtins()
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._

    // the Phase 1 success criterion
    yinele(4) {
      ileri(100)
      sağ()
    }

    // the commands Phase 0 added to Turtle for this layer
    ev()
    noktayaDön(50, 50)
    zıpla(10)
    biçimleriBelleğeYaz()
    kalemRenginiKur(kırmızı)
    biçimleriGeriYükle()
    konumVeYönüBelleğeYaz()
    konumVeYönüGeriYükle()

    // the rest of the vocabulary
    kare(50)
    üçgen(50)
    daire(20)
    kalemKalınlığınıKur(3)
    boyamaRenginiKur(saydam)
    hızıKur(çokHızlı)
    yazı("merhaba")
    yineleDizinli(3) { i => yaz(i) }
    yineleİlktenSona(1, 3) { i => yaz(i) }

    // ---- Faz 3: grafik sözcükleri ----
    // renkler (RenkYöntemleri)
    kalemRenginiKur(Renkler.turkuaz)
    kalemRenginiKur(renkKur(10, 20, 30))
    artalanıKur(Renkler.gökMavisi)
    val r0: Renk = Renkler.mercan
    r0.kırmızısı

    // klavye
    tuşBasılıMı(tuşlar.sol)
    tuşBasılıMı(tuşlar.boşluk)

    // resimler
    // blok biçimi: Türkçe komutlarla resim
    val rBlok = Resim { yinele(4) { ileri(20); sağ() } }
    çiz(rBlok.boyalı(kırmızı))

    val r1 = Resim.dikdörtgen(50, 30)
    val r2 = Resim.daire(20).boyalı(kırmızı).kalemRenkli(mavi)
    val r3 = Resim.yazı("merhaba", 20).döndürülmüş(45).saydamlıklı(0.5)
    val r4 = Resim.elips(30, 15).büyütülmüş(2).konumlu(10, 10)
    çiz(r1, r2)
    çizMerkezde(r3)
    çizSahne(siyah)
    çizMerkezdeYazı("selam", beyaz, 24)
    r2.çarpışıyorMu(r1)
    r4.döndür(10); r4.taşı(5, 5); r4.kaydır(5, 5); r4.kaydır(Yöney2B(1, 1)); r4.gizle(); r4.göster(); r4.sil()
    r1.sınırları
    Resim.yatayÇizgi(50); Resim.kare(20); Resim.dikeyBoşluk(5)

    // ---- kuyruktan okuma: konum / yön / değme ----
    konumuOku { n => yaz(n.x + ", " + n.y) }
    yönüOku { açı => yaz(açı) }
    dokunuyorMu(r1) { değdi => if (değdi) yaz("değdi") }

    // ---- Faz 3 kalanı: kumanda kolu + gelecek ----
    val kol = kumandaKolu(50)
    kol.çiz()
    kol.konumuKur(-200, -150)
    kol.kondur(-200, -150)
    kol.çevreRenginiKur(Renkler.gri)
    kol.kolRenginiKur(kırmızı)
    kol.çevreKalemRenginiKur(siyah)
    val kolYöney: Yöney2B = kol.yöney
    kol.oynat(r1)
    kol.oynat(r1, 2.0)
    kol.oynat(r1, 2.0, Yöney2B(1, 0))
    kol.oynatSahneİçinde(r1)
    kol.oynatSahneİçinde(r1, 2.0, Yöney2B(0, 1))

    // örtük işletim bağlamı trTurtle'dan geliyor -- kullanıcı tanımlamıyor
    val ib: İşletimBağlamı = İşletimBağlamı.küresel
    val g: Gelecek[Sayı] = Gelecek.başarılı(5)
    g.işle(_ * 2).düzİşle(n => Gelecek.başarılı(n + 1)).ele(_ > 0)
    val gh: Gelecek[Sayı] = Gelecek.başarısız(new KuralDışı("olmadı"))

    // oyun/etkileşim
    tuvalSınırları
    yakınlaştırmayıKapat()
    kareSüresi

    // ---- Nokta / Dikdörtgen (PIXI türleri, bu yüzden burada) ----
    val n = Nokta(3, 4)
    n.uzaklığı(Nokta(0, 0)) should be(5.0)
    n.açısı(Nokta(4, 5)) should be(45.0 +- 1e-9)
    n.taşınmış(1, 1).x should be(4.0)
    Nokta.sıfır.x should be(0.0)
    val Nokta(nx, ny) = Nokta(7, 8)
    nx should be(7.0); ny should be(8.0)
    // PIXI.Point'in x/y'si var -- kopyası bağımsız olmalı
    val kopya = n.kopyası
    n.x = 99
    kopya.x should be(3.0)
    uzaklık(Nokta(0, 0), Nokta(3, 4)) should be(5.0)
    açı(Nokta(0, 0), Nokta(1, 1)) should be(45.0 +- 1e-9)

    val dd = Dikdörtgen(0, 0, 10, 20)
    dd.eni should be(10.0)
    dd.merkezi.x should be(5.0)
    dd.içeriyorMu(Nokta(5, 5)) should be(doğru)
    dd.içeriyorMu(Nokta(50, 5)) should be(yanlış)

    // Türkçe API'den artık çevrilmemiş tür sızmıyor
    val k: Nokta = Resim.daire(5).konum
    val s2: Dikdörtgen = Resim.daire(5).sınırları
    val tb: Dikdörtgen = tuvalSınırları

    // ---- Yöney2B (oyun fiziği) ----
    // Nokta gibi burada: kojo.Vector2D JTS'e devrediyor (vec.rotate/normalize),
    // stdlib süiti ise JTS'siz düz Node'da koşuyor.
    val h = Yöney2B(3, 4)
    h.boyu should be(5.0 +- 1e-9)
    h.boyunKaresi should be(25.0 +- 1e-9)
    h.boyunuBirYap.boyu should be(1.0 +- 1e-9)
    h.içÇarpım(Yöney2B(1, 0)) should be(3.0 +- 1e-9)
    h.uzaklığı(Yöney2B(0, 0)) should be(5.0 +- 1e-9)
    h.sınırla(1).boyu should be(1.0 +- 1e-9)
    (h + Yöney2B(1, 1)).x should be(4.0 +- 1e-9)
    (h * 2).x should be(6.0 +- 1e-9)
    (-h).x should be(-3.0 +- 1e-9)
    Yöney2B.sıfır.boyu should be(0.0)
    Yöney2B.açıdan(0).x should be(1.0 +- 1e-9)
    Yöney2B.açıdan(90).y should be(1.0 +- 1e-9)
    val Yöney2B(hx, hy) = Yöney2B(7, 8)
    hx should be(7.0); hy should be(8.0)

    // sekme: KojoJS'in gerçek imzalarıyla uyumlu mu
    val top = Resim.daire(10)
    val yeniHız: Yöney2B = sahnedenSek(top, Yöney2B(2, 3))
    val yeniHız2: Yöney2B = resimdenSek(top, Yöney2B(2, 3), sahneKenarı)
    val kenar: Resim = sahneÜstü

    yuvarla(3.14159, 2) should be(3.14)
    yuvarla(2.5) should be(3.0)

    // ---- Faz 2: stdlib sözcük dağarcığı ----
    // matematik
    karekökü(16.0) should be(4.0)
    enİriOrtakPayda(12, 18) should be(6)
    enUfakOrtakKat(4, 6) should be(12)
    ortalama(Array(1.0, 2.0, 3.0)) should be(2.0)
    açı(0.0, 0.0, 1.0, 1.0) should be(45.0)

    // sayı uzantıları
    (5).yazıya should be("5")
    (5).kesire should be(5.0)
    (-3).mutlakDeğer should be(3)
    (2).enİrisi(7) should be(7)
    (1 |-| 3).toList should be(List(1, 2, 3))
    (1 |- 3).toList should be(List(1, 2))
    Sayılar(3, 1, 2).sorted should be(Vector(1, 2, 3))
    Sayı.Enİrisi should be(Int.MaxValue)

    // belki (Option)
    val b: Belki[Sayı] = Biri(5)
    b.varMı should be(doğru)
    b.al should be(5)
    b.işle(_ * 2).alYoksa(0) should be(10)
    (Hiçbiri: Belki[Sayı]).yokMu should be(doğru)
    varMı(b) should be(doğru)

    // bölümsel işlev
    val bi: Bölümselİşlev[Sayı, Yazı] = { case 1 => "bir" }
    bi.tanımlıMı(1) should be(doğru)
    bi.tanımlıMı(2) should be(yanlış)
  }
}
