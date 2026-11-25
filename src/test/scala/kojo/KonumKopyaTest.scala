package kojo

import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * Konum döndüren her yol masaüstü Kojo gibi KOPYA vermeli, canlı nesne değil.
 *
 * Masaüstünde (bulent2k2/kojo, picture/pics.scala):
 * {{{
 * def position = ... { val o = tnode.getOffset; new core.Point(o.getX, o.getY) }
 * }}}
 * yani `resim.konum` her çağrıda yeni bir nokta. iKoco ise PIXI'nin CANLI
 * `ObservablePoint`'ini (tnode.position) veriyordu. İkisi şu kalıpta ayrışıyor:
 * {{{
 * dez p = r.konum
 * r.konumuKur(p.x + 10, p.y)
 * r.konumuKur(p.x - 10, p.y)  // masaüstü: eski yerin 10 solu; canlı p ile: eski yer
 * }}}
 * Bir kullanıcı betiğinde (dörtgenleri kutuya sığdıran aceleci arama) tam bu
 * oldu: "geri alma" iKoco'da tutuyor, masaüstünde tutmuyordu -- aynı betik iki
 * yerde iki ayrı sonuç.
 *
 * İkinci kusur sessiz: canlı noktaya yazmak (`r.konum.x = 999`) resmi gerçekten
 * taşıyordu ama `transformDone`'u atladığı için çarpışma geometrisi eski yerde
 * kalıyordu. Masaüstünde nokta değişmez (`core.Point` bir case class).
 *
 * Kaplumbağa ve fare de aynı soruyla ölçüldü (ayrı sınamalar aşağıda); ikisi
 * zaten kopya veriyordu, sınama bunu çiviliyor.
 */
class KonumKopyaTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  import kojo.syntax.Builtins

  private def yeni(): Builtins = {
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    b.turtle0.setAnimationDelay(0)
    b
  }

  test("Resim.konum kopya: saklanan nokta resim taşınınca değişmez (dene/geri al kalıbı)") {
    val b = yeni()
    import b.trTurtle._
    val r = Resim.dikdörtgen(20, 10)
    r.çiz()
    r.ready.map { _ =>
      r.konumuKur(30, 40)
      val p = r.konum
      r.konumuKur(p.x + 10, p.y) // dene
      withClue("konumuKur'dan sonra saklanan nokta: ") { (p.x, p.y) shouldBe ((30.0, 40.0)) }
      r.konumuKur(p.x - 10, p.y) // geri al: masaüstünde eski yerin 10 solu
      withClue("resmin yeri (masaüstünde 20, 40): ") { (r.konum.x, r.konum.y) shouldBe ((20.0, 40.0)) }

      // Konumu değiştiren öbür yollar da saklanan noktaya dokunmamalı
      val q = r.konum
      r.kaydır(5, 6)
      withClue("kaydır'dan sonra: ") { (q.x, q.y) shouldBe ((20.0, 40.0)) }
      r.götür(1, 1)
      withClue("götür'den sonra: ") { (q.x, q.y) shouldBe ((20.0, 40.0)) }
      (r.konum.x, r.konum.y) shouldBe ((26.0, 47.0))
    }(executionContext)
  }

  test("Resim.konum'a yazmak resmi sessizce taşımaz; sınırlar aynı yeri söyler") {
    val b = yeni()
    import b.trTurtle._
    val r = Resim.dikdörtgen(20, 10)
    r.çiz()
    r.ready.map { _ =>
      r.konumuKur(30, 40)
      val q = r.konum
      q.x = 999
      q.y = -999
      withClue("resmin yeri: ") { (r.konum.x, r.konum.y) shouldBe ((30.0, 40.0)) }
      // Sınırlar da aynı yeri söylemeli (canlı noktaya yazmak transformDone'u atlıyordu)
      val s = r.sınırları
      s.x shouldBe 29.0 +- 0.5 // 1 piksellik kalem her kenardan 1 taşırır
      s.y shouldBe 39.0 +- 0.5
    }(executionContext)
  }

  test("Picture.position (İngilizce ad) da kopya: konum ile aynı anlam") {
    val b = yeni()
    val r = b.Picture.rectangle(20, 10)
    r.draw()
    r.ready.map { _ =>
      r.setPosition(30, 40)
      val p = r.position
      r.setPosition(p.x + 10, p.y)
      r.setPosition(p.x - 10, p.y)
      withClue("saklanan nokta: ") { (p.x, p.y) shouldBe ((30.0, 40.0)) }
      withClue("resmin yeri: ") { (r.position.x, r.position.y) shouldBe ((20.0, 40.0)) }
      r.offset(1, 2)
      (p.x, p.y) shouldBe ((30.0, 40.0))
      (r.position.x, r.position.y) shouldBe ((21.0, 42.0))
    }(executionContext)
  }

  // --- aynı soru, öbür konum yolları ---------------------------------------

  private def söz[T](kaydet: (T => Unit) => Unit): Future[T] = {
    val s = Promise[T]()
    kaydet(v => s.success(v))
    s.future
  }

  test("kaplumbağa: konumuOku kopya verir; noktaya yazmak kaplumbağayı taşımaz") {
    val b = yeni()
    val tr = b.trTurtle
    tr.ileri(100)
    // Okunan noktayı hemen bozuyoruz: kaplumbağa etkilenmemeli
    val bozulan = söz[(Double, Double)] { k => tr.konumuOku { n => n.x = 999; n.y = -999; k((n.x, n.y)) } }
    val sonra = söz[(Double, Double)](k => tr.konumuOku(n => k((n.x, n.y))))
    val saklanan = söz[pixiscalajs.PIXI.Point](k => tr.konumuOku(k))
    tr.sağ(); tr.ileri(50)
    val son = söz[(Double, Double)](k => tr.konumuOku(n => k((n.x, n.y))))
    for { bz <- bozulan; s <- sonra; sk <- saklanan; z <- son } yield {
      bz shouldBe ((999.0, -999.0))
      withClue("bozulan noktadan sonra kaplumbağa: ") { s._1 shouldBe 0.0 +- 1e-6; s._2 shouldBe 100.0 +- 1e-6 }
      withClue("yürüyüşten sonra saklanan nokta: ") { sk.x shouldBe 0.0 +- 1e-6; sk.y shouldBe 100.0 +- 1e-6 }
      z._1 shouldBe 50.0 +- 1e-6; z._2 shouldBe 100.0 +- 1e-6
    }
  }

  test("fareKonumu / mousePosition: her okuma yeni nokta; yazmak fareyi etkilemez (gerçek dünya)") {
    val w =
      try {
        Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
        val kap = document.createElement("div").asInstanceOf[HTMLElement]
        kap.id = "fiddle-container"
        kap.style.width = "400px"
        kap.style.height = "300px"
        val tuval = document.createElement("div").asInstanceOf[HTMLElement]
        tuval.id = "canvas-holder"
        kap.appendChild(tuval)
        document.body.appendChild(kap)
        new KojoWorldImpl()
      }
      catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }
    try {
      val a = w.mouseXY
      val (ax, ay) = (a.x, a.y)
      (a eq w.mouseXY) shouldBe false
      a.x = ax + 999
      a.y = ay - 999
      val c = w.mouseXY
      (c.x, c.y) shouldBe ((ax, ay))
    }
    finally w.kapat()
  }
}
