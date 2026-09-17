package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.{Future, Promise}
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

  /** n kare koştur. */
  private def kareler(n: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def döngü(): Unit = {
      i += 1
      if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => döngü())
    }
    window.requestAnimationFrame(_ => döngü())
    söz.future
  }

  /**
   * SAHNEYİ kendi RenderTexture'ımıza çizip belirli bir sütunu okur.
   *
   * NEDEN bu dolambaç -- iki ölçüm aracı da kare koştuktan sonra YALAN söylüyor,
   * ölçüldü (2026-09, inceleme §5):
   *  - `extract.pixels(düğüm)`: birkaç kareden sonra her satır 0 okuyor. Süzgeçle
   *    İLGİSİ YOK; süzgeçsiz bir kontrol resmi de aynı şekilde 0 okuyor
   *    (düğüm hâlâ Stage'e bağlı, görünür ve alfası 1).
   *  - varsayılan tamponu doğrudan okumak: `preserveDrawingBuffer` kapalı
   *    olduğu için kare birleştikten sonra tuval sıfır dönüyor.
   * Sahneyi kendi dokumuza çizince ikisi de devre dışı kalıyor ve resim
   * gerçekten oradaysa görünüyor.
   */
  private def sahnedenSütun(w: KojoWorldImpl, p: Picture, x: Int, ySatırları: Seq[Int]): Seq[Int] = {
    val d = w.renderer.asInstanceOf[js.Dynamic]
    val sahne = p.tnode.asInstanceOf[js.Dynamic].parent
    val en = d.width.asInstanceOf[Double].toInt
    val yük = d.height.asInstanceOf[Double].toInt
    val rt = js.Dynamic.global.PIXI.RenderTexture.create(js.Dictionary("width" -> en, "height" -> yük))
    d.render(sahne, rt, true)
    val px = d.plugins.extract.pixels(rt).asInstanceOf[js.typedarray.Uint8Array]
    ySatırları.map(y => px(((y * en) + x) * 4 + 3).toInt)
  }

  test("kareler geçtikten sonra resim hâlâ orada ve profili değişmiyor (inceleme §5)") {
    val w = dünyaKurYaDaİptal()
    implicit val kd: KojoWorld = w
    val b = new kojo.syntax.Builtins()
    // Tuvalin içine tam otursun: dünya x -150..-90, y -100..100 ->
    // ekran x 50..110, y 50..250 (sahne merkezi 200,150; y ters).
    val p = b.trans(-150, -100) -> (b.fade(100) -> dikdörtgen(b))
    val satırlar = Seq(55, 70, 100, 130, 160, 200, 240)
    p.draw()
    for {
      _ <- p.ready
      _ <- kareler(3)
      ilk = sahnedenSütun(w, p, 80, satırlar)
      _ <- kareler(30)
      son = { w.boyalarıBoşalt(); sahnedenSütun(w, p, 80, satırlar) }
    } yield {
      withClue(s"ilk=$ilk: ") {
        ilk.head should be > 200 // üstte hâlâ boyalı
        ilk.drop(4).foreach(_ shouldBe 0) // n'den (100 piksel) aşağısı silinmiş
        ilk.take(4).sliding(2).foreach { case Seq(a, b2) => a should be > b2 } // sönüyor
      }
      withClue(s"ilk=$ilk son=$son: ") { son shouldBe ilk } // 30 kare sonra AYNI
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
