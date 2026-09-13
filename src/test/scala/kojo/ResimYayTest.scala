package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * `Resim.yay` masaüstündeki `ArcPic` ile AYNI YERE çizmeli (#75).
 *
 * Masaüstü kaynağı (net.kogics.kojo.picture.picshapes):
 *   ArcPic.initGeom  -> noktalar (r·cos t, r·sin t), yani MERKEZ (0,0)
 *   KPath.createArc  -> Arc2D.setArc(-r, -r, 2r, 2r, 0, -açı), yine merkez (0,0)
 * Yayın başlangıcı (r, 0), yönü saat yönünün tersi.
 *
 * ikojo'nun kaplumbağa yayı (Turtle.realArc2) merkezi (-r,0)'a koyuyor; aradaki
 * fark `tr/resim.scala`'daki +r ötelemeyle kapatılıyor. Bu sınama o ötelemenin
 * kaybolmasını yakalar -- kaybolursa hiçbir derleyici uyarısı çıkmaz, yalnız
 * bütün yazılımcıklar yayı r kadar solda çizmeye başlar.
 */
class ResimYayTest extends AsyncFunSuite with Matchers {
  // Kurulum kojojs-editor'ün prelude'ünün aynısı (bkz. TurkishPreludeTest).
  implicit val kojoWorld: KojoWorld = new TestKojoWorld()
  val builtins = new kojo.syntax.Builtins()
  import builtins._
  import trTurtle._
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  // Yay kaplumbağa komut kuyruğundan çiziliyor: sınırları kuyruk boşalmadan
  // okumak x=[0,0] döndürür. `ready` beklemek şart.
  // `executionContext` açıkça veriliyor: trTurtle'ın küreselİşletimBağlamı'yla
  // örtük seçim belirsiz kalıyor.
  def sınırlar(r: Resim) = { r.çiz(); r.ready.map(_ => r.bounds)(executionContext) }

  val kalem = 1.0 // kalem kalınlığı payı

  test("Resim.yay(100, 90): çeyrek çember, merkezi (0,0)") {
    sınırlar(Resim.yay(100, 90)).map { b =>
      // masaüstü ArcPic(100, 90): x=[0, 100], y=[0, 100]
      b.x should be(0.0 +- kalem)
      b.y should be(0.0 +- kalem)
      (b.x + b.width) should be(100.0 +- kalem)
      (b.y + b.height) should be(100.0 +- kalem)
    }
  }

  test("Resim.yay(100, 360): tam çember, merkezi (0,0)") {
    sınırlar(Resim.yay(100, 360)).map { b =>
      // masaüstü ArcPic(100, 360): x=[-100, 100], y=[-100, 100]
      b.x should be(-100.0 +- kalem)
      b.y should be(-100.0 +- kalem)
      (b.x + b.width) should be(100.0 +- kalem)
      (b.y + b.height) should be(100.0 +- kalem)
    }
  }

  test("Resim.yay başlangıcı (r, 0) -- yarıçapın ucuna değiyor") {
    // 1 derecelik yay neredeyse bir nokta: yayın BAŞLADIĞI yer.
    sınırlar(Resim.yay(200, 1)).map { b =>
      b.x should be(199.0 +- 2 * kalem)        // (r, 0) çevresinde
      b.y should be(0.0 +- 2 * kalem)
      b.width should be < 5.0
      b.height should be < 10.0
    }
  }
}
