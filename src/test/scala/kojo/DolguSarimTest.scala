package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * Kaplumbağanın dolgusunun NON_ZERO sarım kuralıyla yayınlandığını çiviliyor.
 *
 * NEDEN AYRI BİR TAKIM: bu savı yazana kadar hiçbir sınama BAĞLANTIYI
 * korumuyordu -- ölçüldü: Turtle'ı earcut'a geri döndürünce takımın tamamı
 * yeşil kalıyordu.
 *   - UcgenleyiciTest Üçgenleyici'yi DOĞRUDAN sınıyor, Turtle'ın onu kullanıp
 *     kullanmadığını bilmiyor.
 *   - BoyamaGerilemeTest KARE çiziyor; kare basit bir çokgen, earcut ile
 *     libtess orada zaten aynı sonucu veriyor.
 *
 * Yani ayırt edici sav, kaplumbağanın KENDİNİ KESEN bir şekil çizmesi ve
 * dolgunun alanının kapalı formülle karşılaştırılması olmalı. earcut aynı
 * girdide 1.894 kat fazla alan dolduruyor, yani bu sav bağlantı koparsa
 * kırmızı yanıyor.
 */
class DolguSarimTest extends AsyncFunSuite with Matchers {
  import kojo.syntax.Builtins
  implicit val kojoWorld = new TestKojoWorld()
  val builtins = new Builtins()
  import builtins._
  import builtins.Color._
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def grafikler(kap: pixiscalajs.PIXI.Container): Seq[js.Dynamic] =
    kap.children.toSeq
      .map(_.asInstanceOf[js.Dynamic])
      .filter(d => js.typeOf(d.finishPoly) == "function")

  /** Katmandaki dolgu parçalarının toplam alanı (ayakkabı bağı formülü). */
  private def dolguAlanı(t: Turtle): Double = {
    kojoWorld.boyalarıBoşalt()
    grafikler(t.turtleLayer).flatMap { g =>
      g.finishPoly()
      g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].toSeq
    }
      .filter(gd => gd.fillStyle.visible.asInstanceOf[Boolean])
      .filter(gd => js.typeOf(gd.shape.points) != "undefined")
      .map { gd =>
        val p = gd.shape.points.asInstanceOf[js.Array[Double]]
        var a = 0.0
        var i = 0
        while (i < p.length) {
          val j = (i + 2) % p.length
          a += p(i) * p(j + 1) - p(j) * p(i + 1)
          i += 2
        }
        math.abs(a) / 2
      }
      .sum
  }

  /** Beş köşeli yıldızın NON_ZERO alanı, kenar uzunluğundan kapalı formülle. */
  private def yıldızAlanı(kenar: Double): Double = {
    val R = kenar / (2 * math.sin(math.toRadians(72))) // çevrel yarıçap
    val r = R * math.cos(math.toRadians(72)) / math.cos(math.toRadians(36))
    val beşgen = 2.5 * r * r * math.sin(math.toRadians(72))
    val uç = 0.5 * (2 * r * math.sin(math.toRadians(36))) * (R - r * math.cos(math.toRadians(36)))
    beşgen + 5 * uç
  }

  private def yıldızÇiz(kenar: Double): (TurtlePicture, () => Turtle) = {
    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible()
      setAnimationDelay(0)
      setFillColor(blue)
      kaplumbağa = t
      var i = 0
      while (i < 5) { forward(kenar); right(144); i += 1 } // 144 derece -> kendini kesen yıldız
    }
    (p, () => kaplumbağa)
  }

  test("kaplumbağanın çizdiği kendini kesen yıldız NON_ZERO alanı kadar doluyor") {
    val kenar = 200.0
    val (p, t) = yıldızÇiz(kenar)
    p.draw()
    for (_ <- p.ready) yield {
      val beklenen = yıldızAlanı(kenar)
      // kenar 200 -> çevrel yarıçap 105.146 -> alan 12410.9
      beklenen shouldBe 12410.9 +- 1.0
      withClue(
        s"Dolgunun alanı NON_ZERO değil: ölçülen ${dolguAlanı(t()).round}, beklenen " +
          s"${beklenen.round}. Turtle earcut'a döndüyse burada 16246 çıkıyor (ölçüldü). " +
          "Üçgenleyici bağlantısı kopmuş olabilir. -- "
      ) {
        dolguAlanı(t()) shouldBe beklenen +- (beklenen * 0.02)
      }
    }
  }

  test("basit kare hem earcut hem NON_ZERO ile aynı: bu sav ayırt EDİCİ değil") {
    // Bilerek burada: kareyle sınamanın neden yetmediğini belgeliyor.
    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible(); setAnimationDelay(0); setFillColor(blue)
      kaplumbağa = t
      var i = 0
      while (i < 4) { forward(100); right(90); i += 1 }
    }
    p.draw()
    for (_ <- p.ready) yield {
      dolguAlanı(kaplumbağa) shouldBe 10000.0 +- 1.0
    }
  }
}
