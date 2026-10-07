package kojo

import org.scalajs.dom.document
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * `isDrawn` / `çizili` (#182).
 *
 * Eskiden Türkçe `çizili` `r.made` okuyordu. `made` "resim hazır" demek ve
 * VectorGraphicsPic ile TextPic onu KURUCUDA doğru yapıyor: resim daha
 * çizilmeden `çizili` `doğru` dönüyordu. Masaüstünde (`RedrawStopper.isDrawn`)
 * yalnız `draw()` doğru yapar, `erase()` yanlışa döndürmez. Ölçülen tablo:
 *
 *   resim                    çizmeden önce   çizince   silince
 *   eski iKojo `çizili`      true            true      true
 *   masaüstü `isDrawn`       false           true      true
 *
 * Bu dosya her resim türünde üç anı sınıyor; `çizili = r.made`'e geri
 * dönülünce kırmızı yanar (bkz. PR'daki mutasyon).
 */
class CizildiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  private implicit val w: TestKojoWorld = new TestKojoWorld()
  private val b = new kojo.syntax.Builtins()

  private def kare(): TurtlePicture = b.PictureT { t =>
    t.invisible(); t.setAnimationDelay(0)
    var i = 0; while (i < 4) { t.forward(20); t.right(90); i += 1 }
  }

  /** (çizmeden önce, çizince, silince) */
  private def üçAn(p: Picture): (Boolean, Boolean, Boolean) = {
    val önce = p.isDrawn
    p.draw()
    val çizince = p.isDrawn
    p.erase()
    (önce, çizince, p.isDrawn)
  }

  private def beklenen = (false, true, true)

  test("her resim türünde: çizmeden önce false, çizince true, silince true") {
    withClue("daire: ") { üçAn(b.Picture.circle(10)) shouldBe beklenen }
    withClue("dikdörtgen: ") { üçAn(b.Picture.rectangle(10, 20)) shouldBe beklenen }
    withClue("yazı: ") { üçAn(b.Picture.text("a", 12)) shouldBe beklenen }
    withClue("kaplumbağa resmi: ") { üçAn(kare()) shouldBe beklenen }
    succeed
  }

  test("sarmalayıcılar: Transform ve PreDraw/PostDraw doğru cevap veriyor") {
    // Transform: kendi draw()'ını çalıştırıyor. PreDraw/PostDraw: draw()'ı ezip
    // içerideki resme `tpic.draw()` diyor, kendi imini koymuyor -- isDrawn ikisini birleştirmeli.
    withClue("trans: ") { üçAn(b.trans(5, 5) -> b.Picture.circle(10)) shouldBe beklenen }
    withClue("preDraw: ") { üçAn(b.preDrawTransform(_ => ()) -> b.Picture.circle(10)) shouldBe beklenen }
    withClue("postDraw: ") { üçAn(b.postDrawTransform(_ => ()) -> b.Picture.circle(10)) shouldBe beklenen }
    succeed
  }

  test("grup çizilince çocukları da çizili; çizilmeden önce hiçbiri değil") {
    val a = b.Picture.circle(10)
    val c = b.Picture.rectangle(10, 10)
    val g = new HPics(Seq(a, c))
    (g.isDrawn, a.isDrawn, c.isDrawn) shouldBe ((false, false, false))
    g.draw()
    (g.isDrawn, a.isDrawn, c.isDrawn) shouldBe ((true, true, true))
  }

  test("çizilen kopya değil asıl çizili: copy yeni, çizilmemiş bir resim") {
    val p = b.Picture.circle(10)
    p.draw()
    p.copy.isDrawn shouldBe false
    p.isDrawn shouldBe true
  }

  test("imge: yüklenmiş (made) ama çizilmemiş olabilir, ikisi ayrı") {
    val tuval = document.createElement("canvas").asInstanceOf[org.scalajs.dom.html.Canvas]
    tuval.width = 10; tuval.height = 10
    val c = tuval.getContext("2d").asInstanceOf[org.scalajs.dom.CanvasRenderingContext2D]
    c.fillStyle = "red"; c.fillRect(0, 0, 10, 10)
    val p = new ImagePic(tuval.toDataURL("image/png"), None)
    p.ready.map { _ =>
      withClue("yüklendi, çizilmedi: ") { (p.made, p.isDrawn) shouldBe ((true, false)) }
      p.draw()
      p.isDrawn shouldBe true
      p.erase()
      p.isDrawn shouldBe true
    }
  }

  test("Türkçe çizili: Resim.* ile aynı üç an, ve sarmalayıcıda da") {
    val builtins = b
    import builtins._
    import builtins.trTurtle._
    def üç(r: Resim): (Boolean, Boolean, Boolean) = {
      val önce = r.çizili
      çiz(r)
      val çizince = r.çizili
      r.sil()
      (önce, çizince, r.çizili)
    }
    withClue("Resim.daire: ") { üç(Resim.daire(10)) shouldBe beklenen }
    withClue("Resim.dikdörtgen: ") { üç(Resim.dikdörtgen(10, 20)) shouldBe beklenen }
    withClue("Resim.yazı: ") { üç(Resim.yazı("a")) shouldBe beklenen }
    // Türkçe ile İngilizce aynı resim için aynı şeyi söylüyor
    val p = Resim.daire(10)
    (p.çizili, p.isDrawn) shouldBe ((false, false))
    çiz(p)
    (p.çizili, p.isDrawn) shouldBe ((true, true))
  }
}
