package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * `silVeÇizimBiriminiKur(santim)` / `clearWithUL(Cm)` ve `yansıtX` / `yansıtY` (flipX / flipY).
 * Örnek: masaüstü `hunted.kojo`. Masaüstü anlamı (SpriteCanvas.camScale): sahne birim ölçeğiyle
 * büyür; kalem kalınlığı, yazı ve imge EKRAN boyutunu korur (2 / camScale, 1 / camScale).
 */
class CizimBirimiTest extends AnyFunSuite with Matchers {
  private def yeni() = {
    implicit val w: TestKojoWorld = new TestKojoWorld()
    val b = new kojo.syntax.Builtins()
    b.turtle0.setAnimationDelay(0)
    (w, b)
  }

  test("santim: 1 birim = 96/2.54 ekran pikseli; inç 96; noktaSayısı 1") {
    val (w, b) = yeni()
    w.birimÖlçeği shouldBe 1.0
    b.clearWithUL(b.Cm)
    w.birimÖlçeği shouldBe (96.0 / 2.54) +- 1e-9
    b.clearWithUL(b.Inch)
    w.birimÖlçeği shouldBe 96.0
    b.clearWithUL(b.Pixel)
    w.birimÖlçeği shouldBe 1.0
  }

  test("Türkçe adlar: santim, inç, noktaSayısı ve silVeÇizimBiriminiKur") {
    val (w, b) = yeni()
    import b.trTurtle._
    silVeÇizimBiriminiKur(santim)
    w.birimÖlçeği shouldBe (96.0 / 2.54) +- 1e-9
    silVeÇizimBiriminiKur(inç)
    w.birimÖlçeği shouldBe 96.0
    silVeÇizimBiriminiKur(noktaSayısı)
    w.birimÖlçeği shouldBe 1.0
  }

  test("sahne birim ölçeğinde büyür ve resetView birimi korur (zoomXY(1,1,0,0) birimi bozmaz)") {
    // gerçek çizici kap ister (bkz. KapSifirBoyutTest.kapKur); WebGL yoksa iptal
    Option(org.scalajs.dom.document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = org.scalajs.dom.document.createElement("div").asInstanceOf[org.scalajs.dom.raw.HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = org.scalajs.dom.document.createElement("div").asInstanceOf[org.scalajs.dom.raw.HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval)
    org.scalajs.dom.document.body.appendChild(kap)
    val w =
      try new KojoWorldImpl()
      catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }
    w.birimiKur(Cm)
    w.stage.scale.x shouldBe (96.0 / 2.54) +- 1e-9
    w.stage.scale.y shouldBe -(96.0 / 2.54) +- 1e-9
    w.resetView()
    w.stage.scale.x shouldBe (96.0 / 2.54) +- 1e-9
    w.kapat()
  }

  test("kalem kalınlığı ekranda 2 piksel kalır: santimde varsayılan 2 / ölçek") {
    val (w, b) = yeni()
    b.clearWithUL(b.Cm)
    val r = b.Picture.rectangle(1, 1).asInstanceOf[VectorGraphicsPic]
    r.path.asInstanceOf[scala.scalajs.js.Dynamic]._lineStyle.width.asInstanceOf[Double] shouldBe (2 / (96.0 / 2.54)) +- 1e-9
  }

  test("yazı resmi ekran boyunu korur: ölçek 1/birimÖlçeği") {
    val (w, b) = yeni()
    b.clearWithUL(b.Cm)
    val t = b.Picture.text("a").asInstanceOf[TextPic]
    t.textNode.scale.x shouldBe (1 / (96.0 / 2.54)) +- 1e-9
    t.textNode.scale.y shouldBe -(1 / (96.0 / 2.54)) +- 1e-9
  }

  test("yansıtY / flipY X'i, yansıtX / flipX Y'yi ters çevirir (masaüstü Picture.flipX = Y ters)") {
    val (w, b) = yeni()
    import b.trTurtle._
    val y = yansıtY -> Resim.dikdörtgen(10, 10)
    val x = yansıtX -> Resim.dikdörtgen(10, 10)
    y.draw(); x.draw()
    y.tnode.scale.x shouldBe -1.0 +- 1e-12
    y.tnode.scale.y shouldBe 1.0 +- 1e-12
    x.tnode.scale.x shouldBe 1.0 +- 1e-12
    x.tnode.scale.y shouldBe -1.0 +- 1e-12
    val e = b.flipY -> b.Picture.rectangle(10, 10)
    e.draw()
    e.tnode.scale.x shouldBe -1.0 +- 1e-12
  }

  test("yansıtY zincirde döndür ve götür ile birleşir (hunted.kojo kalıbı)") {
    val (w, b) = yeni()
    import b.trTurtle._
    val r = yansıtY * döndür(120) * götür(1.5, 0) -> Resim.dikdörtgen(2, 1)
    r.draw()
    r.tnode.scale.x should be < 0.0
  }
}
