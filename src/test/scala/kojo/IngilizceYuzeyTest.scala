package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * Masaüstü Kojo'nun İngilizce adları iKojo'da da olmalı: Koco -> Kojo çevirmeni
 * (bulent2k2/kojo, lite/i18n/tr/cevirmen.scala) Türkçe bir betiği bu adlara
 * çeviriyor ve çıkan betik derlenmeli (kojojs-dev#183 Aşama 3). Bu adlar
 * `araclar/cevir-derle.py` kapısında "çeviri açığı" olarak görünüyordu.
 *
 * Her ad Türkçe karşılığıyla AYNI işi yapmalı; çevirmenin seçtiği çift buraya
 * tek tek yazıldı, biri kayarsa iki dil ayrışır.
 */
class IngilizceYuzeyTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  import kojo.syntax.Builtins

  private def yeni() = {
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    b.turtle0.setAnimationDelay(0)
    b
  }

  private def oku(t: TurtleAPI): Future[(Double, Double)] = {
    val s = Promise[(Double, Double)]()
    t.readPosition(p => s.success((p.x, p.y)))
    s.future
  }

  test("jumpTo = setPosition = atla: çizmeden konuma gider") {
    val b = yeni()
    b.turtle0.jumpTo(30, 40)
    oku(b.turtle0).map { p => p shouldBe ((30.0, 40.0)) }
  }

  test("newTurtle: konum ve giysi, yeniKaplumbağa ile aynı; jumpTo bu kaplumbağada da çalışıyor") {
    val b = yeni()
    val t0 = b.newTurtle()
    val t1 = b.newTurtle(10, 20)
    val t2 = b.newTurtle(-5, 7, b.Costume.car)
    t2.jumpTo(1, 2)
    for { p0 <- oku(t0); p1 <- oku(t1); p2 <- oku(t2) } yield {
      p0 shouldBe ((0.0, 0.0)); p1 shouldBe ((10.0, 20.0)); p2 shouldBe ((1.0, 2.0))
    }
  }

  test("mousePosition = fareKonumu: aynı nokta, Point türünde") {
    val b = yeni()
    val p: b.Point = b.mousePosition
    val q = b.trTurtle.fareKonumu
    (p.x, p.y) shouldBe ((q.x, q.y))
  }

  test("gridOn/gridOff/axesOn/axesOff ve Türkçe karşılıkları çağrılabiliyor (etkisi KojoWorld sınamalarında)") {
    val b = yeni()
    // TestKojoWorld'de bu dört sabit işlev; burada yalnız patlamadıkları ve Türkçe karşılıklarının
    // aynı çağrıya gittiği sınanabiliyor (görüntü etkisi KojoWorld sınamalarında).
    b.gridOn(); b.gridOff(); b.axesOn(); b.axesOff()
    b.trTurtle.gridiGöster(); b.trTurtle.gridiGizle(); b.trTurtle.eksenleriGöster(); b.trTurtle.eksenleriGizle()
    succeed
  }

  test("Costume / Background: masaüstündeki değerler; Türkçe Görünüş / Artalan'la aynı yollar") {
    val b = yeni()
    val g = b.trTurtle.Görünüş
    b.Costume.car shouldBe g.araba
    b.Costume.pencil shouldBe g.kalem
    b.Costume.bat1 shouldBe g.yarasa1a
    b.Costume.bat2 shouldBe g.yarasa1b
    b.Costume.womanWaving shouldBe g.kadınElSallarken
    b.Background.trainTrack shouldBe b.trTurtle.Artalan.demiryolu
    // masaüstü Tw.Costume'daki yollar (TurtleWorldAPI.scala:139-143)
    b.Costume.car shouldBe "/media/costumes/car.png"
    b.Costume.womanWaving shouldBe "/media/costumes/womanwaving.png"
  }
}
