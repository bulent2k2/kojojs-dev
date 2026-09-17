package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js

/**
 * `soluk(n)` masaüstündeki `fade(n)` gibi davranmalı: resmi ÜSTTEN AŞAĞI n
 * piksel boyunca söndürüp altını hiç çizmemeli.
 *
 * NEDEN PİKSEL OKUYOR: bu bir GÖRÜNTÜ etkisi; "süzgeç takıldı mı" diye bakmak
 * hiçbir şey kanıtlamaz -- yanlış eksende ya da ters yönde sönen bir kabuk da
 * o savı geçerdi. Bu yüzden resim gerçekten çizilip `extract.pixels` ile
 * okunuyor ve alfa profili satır satır sınanıyor.
 *
 * WebGL'siz bir ortamda sınamalar PATLAMAK yerine İPTAL oluyor
 * (KaynakSizintisiTest'teki ile aynı gerekçe: başsız Chrome WebGL'i
 * SwiftShader'la veriyor ama bunu CI ortamına şart koşmuyoruz).
 */
class SolukTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try dünyaKur()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def dünyaKur(): KojoWorldImpl = {
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

  /** Resmin kendi sınırları kadar RGBA pikselleri; (en, boy, piksel). */
  private def pikseller(w: KojoWorldImpl, p: Picture): (Int, Int, js.typedarray.Uint8Array) = {
    val ext = w.renderer.asInstanceOf[js.Dynamic].plugins.extract
    val px = ext.pixels(p.tnode.asInstanceOf[js.Any]).asInstanceOf[js.typedarray.Uint8Array]
    val en = p.tnode.getBounds().width.toInt
    (en, (px.length / 4) / math.max(en, 1), px)
  }

  private def alfa(en: Int, px: js.typedarray.Uint8Array, satır: Int): Int =
    px((satır * en + en / 2) * 4 + 3)

  private def dikdörtgen(b: kojo.syntax.Builtins): Picture = {
    import b._
    fillColor(kojo.doodle.Color.blue) -> Picture.rectangle(60, 200)
  }

  test("alfa üstten aşağı doğrusal sönüyor ve n'den sonra bitiyor") {
    val w = dünyaKurYaDaİptal()
    implicit val kd: KojoWorld = w
    val b = new kojo.syntax.Builtins()
    val p = b.fade(100) -> dikdörtgen(b)
    p.draw()
    p.ready.map { _ =>
      val (en, boy, px) = pikseller(w, p)
      boy should be > 150 // resim n'den (100) uzun olmalı ki "altı silinmiş" görülsün
      // Doğrusal rampa: alfa(y) ~= 255 * (1 - y/100). Pay 4: kenar örneklemesi
      // yarım piksellik bir kayma bırakıyor (ölçüldü: sabit +1.5).
      Seq(10 -> 229, 25 -> 191, 50 -> 128, 75 -> 64).foreach { case (satır, beklenen) =>
        withClue(s"$satır. satır: ") { alfa(en, px, satır) shouldBe beklenen +- 4 }
      }
      withClue("n'den (100) sonrası silinmiş olmalı: ") { alfa(en, px, 120) shouldBe 0 }
      withClue("n'den (100) sonrası silinmiş olmalı: ") { alfa(en, px, boy - 2) shouldBe 0 }
    }
  }

  test("soluk olmadan resim baştan sona opak -- sav boş değil") {
    val w = dünyaKurYaDaİptal()
    implicit val kd: KojoWorld = w
    val b = new kojo.syntax.Builtins()
    val p = dikdörtgen(b)
    p.draw()
    p.ready.map { _ =>
      val (en, boy, px) = pikseller(w, p)
      Seq(10, 50, 120, boy - 2).foreach { satır =>
        withClue(s"$satır. satır: ") { alfa(en, px, satır) should be > 250 }
      }
      succeed
    }
  }

  test("Türkçe `soluk` İngilizce `fade` ile aynı profili veriyor") {
    val w = dünyaKurYaDaİptal()
    implicit val kd: KojoWorld = w
    val b = new kojo.syntax.Builtins()
    import b.trTurtle._
    val tr = (soluk(80) -> dikdörtgen(b))
    val en = (b.fade(80) -> dikdörtgen(b))
    tr.draw(); en.draw()
    for {
      _ <- tr.ready
      _ <- en.ready
    } yield {
      val (e1, b1, p1) = pikseller(w, tr)
      val (e2, _, p2) = pikseller(w, en)
      Seq(5, 20, 40, 60, 79, 90, b1 - 2).foreach { satır =>
        withClue(s"$satır. satır: ") { alfa(e1, p1, satır) shouldBe alfa(e2, p2, satır) +- 1 }
      }
      succeed
    }
  }

  test("n <= 0 resmi tümüyle siler (masaüstündeki gibi)") {
    val w = dünyaKurYaDaİptal()
    implicit val kd: KojoWorld = w
    val b = new kojo.syntax.Builtins()
    val p = b.fade(0) -> dikdörtgen(b)
    p.draw()
    p.ready.map { _ =>
      val (en, boy, px) = pikseller(w, p)
      Seq(1, 10, boy / 2, boy - 2).foreach { satır =>
        withClue(s"$satır. satır: ") { alfa(en, px, satır) shouldBe 0 }
      }
      succeed
    }
  }
}
