package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.scalajs.js

/**
 * #179: `notaÇal` / `playNote` masaüstündeki gibi SIRAYA girmeli. Masaüstünün
 * RealtimeNotePlayer'ı notayı MIDI izine bir imleçte ekler ve imleci süre kadar
 * ilerletir; arka arkaya üç çağrı üç notalık bir ezgidir. Eski iKojo'da her nota
 * `currentTime`'da başlıyordu: üçü aynı anda, akor gibi çalıyordu.
 *
 * İki katman:
 *  - sahte bağlam: start/stop anlarını ve dalga biçimini kaydeder -- imlecin
 *    kuralları (boşta hemen, çalarken öncekinin sonunda, çalgı yalnız sonrakine)
 *    tam sayılarla sınanır;
 *  - gerçek OfflineAudioContext: aynı çalıcı gerçekten çaldırılıp her zaman
 *    penceresinde HANGİ notanın duyulduğu Goertzel ile ölçülür. Sahte bağlamın
 *    göremeyeceği şeyi (örn. zarfın notayı erken susturması) bu yakalar.
 */
class NotaCalarTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /** start/stop/tür/frekans kaydeden, saati elle kurulan sahte AudioContext. */
  class SahteBağlam {
    val osilatörler = mutable.ArrayBuffer.empty[js.Dynamic]
    var susturulan = 0
    private def düğüm(): js.Dynamic = js.Dynamic.literal(
      connect = (_: js.Any) => (),
      disconnect = () => { susturulan += 1 }
    )
    val ctx: js.Dynamic = js.Dynamic.literal(
      currentTime = 0.0,
      destination = düğüm()
    )
    ctx.createOscillator = () => {
      val o = düğüm()
      o.frequency = js.Dynamic.literal(value = 0.0)
      o.start = (t: Double) => { o.başla = t }
      o.stop = (t: Double) => { o.bitiş = t }
      o.connect = (hedef: js.Dynamic) => { o.kazanç = hedef }
      osilatörler += o
      o
    }
    ctx.createGain = () => {
      val g = düğüm()
      g.gain = js.Dynamic.literal(
        setValueAtTime = (düzey: Double, _: Double) => { g.düzey = düzey },
        exponentialRampToValueAtTime = (_: Double, _: Double) => ()
      )
      g
    }
    def saat(t: Double): Unit = ctx.currentTime = t
    def başlar: Seq[Double] = osilatörler.map(_.başla.asInstanceOf[Double]).toSeq
    def bitişler: Seq[Double] = osilatörler.map(_.bitiş.asInstanceOf[Double]).toSeq
    def türler: Seq[String] = osilatörler.map(_.`type`.asInstanceOf[String]).toSeq
    def frekanslar: Seq[Double] = osilatörler.map(_.frequency.value.asInstanceOf[Double]).toSeq
    /** Her osilatörün bağlandığı kazanç düğümünün başlangıç düzeyi. */
    def düzeyler: Seq[Double] = osilatörler.map(_.kazanç.düzey.asInstanceOf[Double]).toSeq
  }

  private def çalıcı(): (NotaÇalar, SahteBağlam) = {
    val s = new SahteBağlam
    val n = new NotaÇalar
    n.bağlam = s.ctx
    (n, s)
  }

  private def yakın(a: Seq[Double], b: Seq[Double]) = {
    a.size shouldBe b.size
    a.zip(b).foreach { case (x, y) => x shouldBe y +- 1e-9 }
    succeed
  }

  test("arka arkaya notalar sıraya girer: ezgi, akor değil (#179)") {
    val (n, s) = çalıcı()
    n.çal(60, 300, 80); n.çal(64, 600, 80); n.çal(67, 300, 80)
    yakın(s.başlar, Seq(0.0, 0.3, 0.9))
    yakın(s.bitişler, Seq(0.3, 0.9, 1.2))
  }

  test("çalıcı boştaysa nota hemen, çalıyorsa öncekinin sonunda başlar") {
    val (n, s) = çalıcı()
    n.çal(60, 300, 80)            // 0.0 - 0.3
    s.saat(0.1)
    n.çal(62, 100, 80)            // çalarken: öncekinin sonunda, 0.3 - 0.4
    s.saat(5.0)
    n.çal(64, 100, 80)            // uzun sessizlikten sonra: hemen, 5.0 - 5.1
    yakın(s.başlar, Seq(0.0, 0.3, 5.0))
    yakın(s.bitişler, Seq(0.3, 0.4, 5.1))
  }

  test("çalgı değişikliği yalnız sonraki notaları etkiler") {
    val (n, s) = çalıcı()
    n.çal(60, 100, 80)
    n.çalgıyıKur(Instrument.ACOUSTIC_BASS)
    n.çal(40, 100, 80)
    s.türler shouldBe Seq("triangle", "sine")
    s.osilatörler(1).frequency.value.asInstanceOf[Double] shouldBe NotaÇalar.frekans(40) +- 1e-9
  }

  test("durdur sıradakileri susturur ve imleci sıfırlar (stopNotePlayer)") {
    val (n, s) = çalıcı()
    n.çal(60, 1000, 80); n.çal(62, 1000, 80)
    n.durdur()
    s.susturulan shouldBe 2
    s.saat(0.5)
    n.çal(64, 100, 80)            // eski sıranın (2.0) arkasına değil, hemen
    s.başlar.last shouldBe 0.5 +- 1e-9
  }

  test("sınırlar masaüstüyle aynı; süre eksiyse imleç geri gitmez") {
    val (n, s) = çalıcı()
    an[IllegalArgumentException] should be thrownBy n.çal(128, 10, 80)
    an[IllegalArgumentException] should be thrownBy n.çal(60, 10, -1)
    an[IllegalArgumentException] should be thrownBy n.çalgıyıKur(128)
    n.çal(60, 200, 80); n.çal(60, -50, 80); n.çal(62, 100, 80)
    yakın(s.başlar, Seq(0.0, 0.2, 0.2))
  }

  test("notaÇal ile playNote aynı imleci paylaşır; Çalgı = Instrument") {
    import kojo.syntax.Builtins
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    val s = new SahteBağlam
    b.notaÇalar.bağlam = s.ctx
    val tr = b.trTurtle
    tr.notaÇalgısınıKur(tr.Çalgı.AkustikBas)
    tr.notaÇal(50, 150)
    b.playNote(45, 200)
    b.setNoteInstrument(b.Instrument.PIANO)
    tr.notaÇal(60, 100)
    yakın(s.başlar, Seq(0.0, 0.15, 0.35))
    s.türler shouldBe Seq("sine", "sine", "triangle")
    an[IllegalArgumentException] should be thrownBy tr.notaÇalgısınıKur(200)

    // Koco -> Kojo çevirmeninin (kojo: ceviri-sozlugu.tsv) seçtiği karşılıklar
    val ç = tr.Çalgı
    val I = b.Instrument
    Seq(
      ç.Piyano -> I.PIANO, ç.AkustikKuyruklu -> I.ACOUSTIC_GRAND,
      ç.ParlakAkustik -> I.BRIGHT_ACOUSTIC, ç.ElektroKuyruklu -> I.ELECTRIC_GRAND,
      ç.HonkyTonkPiyano -> I.HONKYTONK_PIANO, ç.ElektroPiyano -> I.EPIANO,
      ç.AkustikBas -> I.ACOUSTIC_BASS, ç.Kuş -> I.BIRD, ç.Telefon -> I.TELEPHONE,
      ç.Helikopter -> I.HELICOPTER, ç.Alkış -> I.APPLAUSE, ç.Tabanca -> I.GUNSHOT
    ).foreach { case (türkçe, ingilizce) => türkçe shouldBe ingilizce }
    succeed
  }

  test("beraberÇal: hepsi aynı anda başlar, her nota kendi süresince; sonraki nota en uzununun sonunda") {
    val (n, s) = çalıcı()
    n.beraberÇal(Seq((60, 300), (64, 1000), (67, 600)), 80)
    n.çal(72, 100, 80)
    yakın(s.başlar, Seq(0.0, 0.0, 0.0, 1.0))
    yakın(s.bitişler, Seq(0.3, 1.0, 0.6, 1.1))
    yakın(s.frekanslar, Seq(60, 64, 67, 72).map(NotaÇalar.frekans))
  }

  test("akorÇal: hepsi aynı süre; beraberÇal'ın özel hâli") {
    val (n, s) = çalıcı()
    n.çal(40, 200, 80)               // 0.0 - 0.2
    s.saat(0.05)
    n.akorÇal(Seq(48, 52, 55), 400, 80) // çalarken: öncekinin sonunda, 0.2 - 0.6, üçü birden
    n.çal(60, 100, 80)               // 0.6 - 0.7
    yakın(s.başlar, Seq(0.0, 0.2, 0.2, 0.2, 0.6))
    yakın(s.bitişler, Seq(0.2, 0.6, 0.6, 0.6, 0.7))
  }

  test("akor ses düzeyi nota sayısının karekökü kadar bölünür; tek nota eskisi gibi") {
    val (n, s) = çalıcı()
    n.çal(60, 100, 127)                           // tek nota: bölen 1
    n.akorÇal(Seq(60, 64, 67, 71), 100, 127)      // dört nota: bölen 2
    val tek = s.düzeyler.head
    tek shouldBe 0.3 +- 1e-9
    s.düzeyler.tail.foreach(d => d shouldBe (tek / 2) +- 1e-9)
    // güç korunuyor: Σ düzey² = tek nota gücü
    s.düzeyler.tail.map(d => d * d).sum shouldBe (tek * tek) +- 1e-9
  }

  test("beraberÇal önce HEPSİNİ doğrular: biri sınır dışıysa hiçbir nota sıraya girmez; boş dizi bir şey yapmaz") {
    val (n, s) = çalıcı()
    an[IllegalArgumentException] should be thrownBy n.beraberÇal(Seq((60, 100), (128, 100)), 80)
    an[IllegalArgumentException] should be thrownBy n.akorÇal(Seq(60, -1), 100, 80)
    an[IllegalArgumentException] should be thrownBy n.akorÇal(Seq(60, 64), 100, 128)
    s.osilatörler shouldBe empty
    n.beraberÇal(Seq.empty, 80)
    n.akorÇal(Seq.empty, 100, 80)
    s.osilatörler shouldBe empty
    n.çal(62, 100, 80)               // imleç kıpırdamamış: hemen
    yakın(s.başlar, Seq(0.0))
  }

  test("sus: sessizce bekletir; boştayken şimdiden, çalarken öncekinin sonundan sayar") {
    val (n, s) = çalıcı()
    n.çal(60, 100, 80)               // 0.0 - 0.1
    n.sus(200)                       // imleç 0.3
    n.çal(62, 100, 80)               // 0.3 - 0.4
    s.saat(5.0)
    n.sus(100)                       // boşta: 5.0'dan, imleç 5.1
    n.çal(64, 100, 80)               // 5.1 - 5.2
    n.sus(-50)                       // eksi süre imleci geri götürmez
    n.çal(65, 100, 80)               // 5.2 - 5.3
    yakın(s.başlar, Seq(0.0, 0.3, 5.1, 5.2))
    s.osilatörler.size shouldBe 4    // es osilatör yaratmadı
  }

  test("kalanMiliSaniye: sıradaki notaların bitmesine kalan süre; boşta 0, hiç çalınmadıysa 0") {
    val (n, s) = çalıcı()
    n.kalanMiliSaniye shouldBe 0
    n.çal(60, 300, 80); n.çal(64, 600, 80); n.çal(67, 300, 80) // toplam 1200 ms
    n.kalanMiliSaniye shouldBe 1200
    s.saat(0.5)
    n.kalanMiliSaniye shouldBe 700
    n.sus(300)
    n.kalanMiliSaniye shouldBe 1000
    s.saat(9.0)
    n.kalanMiliSaniye shouldBe 0     // eksiye düşmüyor
    n.durdur()
    n.kalanMiliSaniye shouldBe 0
    // bağlam hiç kurulmamışsa (Web Audio yok) 0
    new NotaÇalar().kalanMiliSaniye shouldBe 0
  }

  test("İngilizce ve Türkçe yüzey aynı çalıcıya gidiyor: playChord/playTogether/playRest/noteTimeLeftMillis = akorÇal/beraberÇal/notaSus/kalanNotaSüresi") {
    import kojo.syntax.Builtins
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    val s = new SahteBağlam
    b.notaÇalar.bağlam = s.ctx
    val tr = b.trTurtle
    b.playChord(Seq(60, 64), 100)                 // 0.0 - 0.1
    tr.akorÇal(Seq(67, 71), 100)                  // 0.1 - 0.2
    b.playTogether(Seq((72, 100), (76, 300)))     // 0.2 - 0.5
    tr.beraberÇal(Seq((79, 100)))                 // 0.5 - 0.6
    b.playRest(100)                               // 0.7
    tr.notaSus(100)                               // 0.8
    tr.notaÇal(60, 100)                           // 0.8 - 0.9
    yakın(s.başlar, Seq(0.0, 0.0, 0.1, 0.1, 0.2, 0.2, 0.5, 0.8))
    // Notaların SIRASI ve her birinin kendi süresi de doğru yüzeye bağlı: playTogether'ın
    // (72, 100), (76, 300) çifti ters çevrilirse yalnız frekans/bitiş sırası değişir.
    yakın(s.frekanslar, Seq(60, 64, 67, 71, 72, 76, 79, 60).map(NotaÇalar.frekans))
    yakın(s.bitişler, Seq(0.1, 0.1, 0.2, 0.2, 0.3, 0.5, 0.6, 0.9))
    b.noteTimeLeftMillis shouldBe 900
    tr.kalanNotaSüresi shouldBe 900
    an[IllegalArgumentException] should be thrownBy tr.akorÇal(Seq(60, 200), 100)
    an[IllegalArgumentException] should be thrownBy b.playTogether(Seq((60, 100), (-1, 100)))
  }

  test("gerçek ses motorunda (OfflineAudioContext) akorun üç notası birlikte duyulur, sonraki nota akor bitince") {
    val Çevrimdışı = js.Dynamic.global.OfflineAudioContext
    if (js.isUndefined(Çevrimdışı)) cancel("OfflineAudioContext yok")
    val örnekHızı = 44100
    val ctx = js.Dynamic.newInstance(Çevrimdışı)(1, (örnekHızı * 1.2).toInt, örnekHızı)
    val n = new NotaÇalar
    n.bağlam = ctx
    val akor = Seq(60, 64, 67)
    n.akorÇal(akor, 400, 80)   // 0.0 - 0.4, üçü birden
    n.sus(200)                 // 0.4 - 0.6 sessiz
    n.çal(72, 300, 80)         // 0.6 - 0.9

    val söz = ctx.startRendering().asInstanceOf[js.Promise[js.Dynamic]]
    söz.toFuture.map { tampon =>
      val a = tampon.getChannelData(0).asInstanceOf[js.typedarray.Float32Array]
      def güç(b0: Double, b1: Double, nota: Int): Double = {
        val f = NotaÇalar.frekans(nota)
        val k = 2 * math.cos(2 * math.Pi * f / örnekHızı)
        var s1 = 0.0; var s2 = 0.0; var i = (b0 * örnekHızı).toInt
        val son = (b1 * örnekHızı).toInt
        while (i < son) { val x = a(i) + k * s1 - s2; s2 = s1; s1 = x; i += 1 }
        math.sqrt(s1 * s1 + s2 * s2 - k * s1 * s2) / (son - (b0 * örnekHızı).toInt)
      }
      val tümü = akor :+ 72
      def duyulan(b0: Double, b1: Double): Seq[Int] = {
        val güçler = tümü.map(nt => nt -> güç(b0, b1, nt))
        val enGüçlü = güçler.map(_._2).max
        if (enGüçlü < 1e-6) Seq.empty else güçler.collect { case (nt, p) if p > enGüçlü * 0.2 => nt }
      }
      val ölçüm = Seq(
        "akor 0.02-0.30" -> duyulan(0.02, 0.30),
        "es 0.45-0.58" -> duyulan(0.45, 0.58),
        "72 0.65-0.85" -> duyulan(0.65, 0.85),
        "son 0.95-1.15" -> duyulan(0.95, 1.15)
      )
      withClue(ölçüm.mkString("\n", "\n", "\n")) {
        ölçüm(0)._2 shouldBe akor          // üçü birlikte, 72 yok
        ölçüm(1)._2 shouldBe empty         // es: sessiz
        ölçüm(2)._2 shouldBe Seq(72)       // yalnız sonraki nota
        ölçüm(3)._2 shouldBe empty         // toplam süre 0.9
      }
    }
  }

  test("gerçek ses motorunda (OfflineAudioContext) her pencerede tek nota duyulur") {
    val Çevrimdışı = js.Dynamic.global.OfflineAudioContext
    if (js.isUndefined(Çevrimdışı)) cancel("OfflineAudioContext yok")
    val örnekHızı = 44100
    val ctx = js.Dynamic.newInstance(Çevrimdışı)(1, (örnekHızı * 1.6).toInt, örnekHızı)
    val n = new NotaÇalar
    n.bağlam = ctx
    val notalar = Seq(60 -> 300, 64 -> 600, 67 -> 300) // 0-0.3, 0.3-0.9, 0.9-1.2 sn
    notalar.foreach { case (nota, ms) => n.çal(nota, ms, 80) }

    val söz = ctx.startRendering().asInstanceOf[js.Promise[js.Dynamic]]
    söz.toFuture.map { tampon =>
      val a = tampon.getChannelData(0).asInstanceOf[js.typedarray.Float32Array]
      // Goertzel: [b0, b1) örnekleri içinde f frekansının gücü
      def güç(b0: Int, b1: Int, f: Double): Double = {
        val k = 2 * math.cos(2 * math.Pi * f / örnekHızı)
        var s1 = 0.0; var s2 = 0.0; var i = b0
        while (i < b1) { val x = a(i) + k * s1 - s2; s2 = s1; s1 = x; i += 1 }
        math.sqrt(s1 * s1 + s2 * s2 - k * s1 * s2) / (b1 - b0)
      }
      val pencereler = Seq((0.00, 0.25, 60), (0.35, 0.85, 64), (0.95, 1.15, 67))
      val ölçüm = pencereler.map { case (t0, t1, beklenen) =>
        val güçler = notalar.map { case (nota, _) =>
          nota -> güç((t0 * örnekHızı).toInt, (t1 * örnekHızı).toInt, NotaÇalar.frekans(nota))
        }
        val enGüçlü = güçler.map(_._2).max
        val duyulan = güçler.collect { case (nota, p) if p > enGüçlü * 0.2 => nota }
        (f"$t0%.2f-$t1%.2f sn", beklenen, duyulan, güçler)
      }
      withClue(ölçüm.mkString("\n", "\n", "\n")) {
        ölçüm.foreach { case (_, beklenen, duyulan, _) => duyulan shouldBe Seq(beklenen) }
        // 1.2 sn'den sonra sessizlik: toplam süre masaüstündeki gibi
        güç((1.3 * örnekHızı).toInt, (1.55 * örnekHızı).toInt, NotaÇalar.frekans(67)) shouldBe 0.0 +- 1e-6
      }
    }
  }
}
