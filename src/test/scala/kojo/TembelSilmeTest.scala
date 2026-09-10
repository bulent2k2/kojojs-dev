package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * Tembel üçgenlemenin açtığı pencerede dolgunun KAYBOLMAMASINI çiviliyor.
 *
 * NEDEN VAR: yayın tembelleşince "kirlenme" ile "yayın" arasına bir pencere
 * girdi. İlk uygulamada o pencerede bekleyenler KÜRESEL olarak düşürülüyordu
 * ve iki yerden dolgu sessizce yok oluyordu:
 *
 *   - `resimleriSil()` (erasePictures): "Turtle Layer" adlı çocukları BİLEREK
 *     silmiyor, yani hayatta kalan katmanlar tam olarak Boyacıların katmanları
 *     -- düşürülen dolgular da tam onlarınki.
 *   - `sil()` (Turtle.realClear): A kaplumbağasının bekleyeni, B kaplumbağası
 *     sil() deyince düşüyordu.
 *
 * Bu PR'dan önce ikisi de sorun değildi çünkü yayın istekliydi.
 *
 * DİKKAT (bu sınamayı yazarken iki kez yanıldım):
 *   - `TestKojoWorld.erasePictures` BOŞ bir saplama, yani onu çağırmak hiçbir
 *     şey sınamıyor. erasePictures'ın yaptığı düşürmeyi doğrudan sınıyoruz.
 *   - `clear()` yalnız KUYRUĞA koyuyor; `realClear` sonra koşuyor. Kuyruk
 *     boşalmadan bakmak yanlış yeşil veriyor.
 */
class TembelSilmeTest extends AsyncFunSuite with Matchers {
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

  /** Katmandaki alan kaplayan dolgu parçalarının köşe sayıları. */
  private def dolguKöşeleri(t: Turtle): Seq[Int] =
    grafikler(t.turtleLayer).flatMap { g =>
      g.finishPoly()
      g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].toSeq
    }
      .filter(gd => gd.fillStyle.visible.asInstanceOf[Boolean])
      .filter(gd => js.typeOf(gd.shape.points) != "undefined")
      .map(gd => gd.shape.points.asInstanceOf[js.Array[Double]].length / 2)
      .filter(_ >= 3)

  private def doluKare(sonda: Turtle => Unit = _ => ()): (TurtlePicture, () => Turtle) = {
    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible()
      setAnimationDelay(0) // çokHızlı: bütün kenarlar tek kareye düşüyor
      setFillColor(blue)
      kaplumbağa = t
      var i = 0
      while (i < 4) { forward(100); right(90); i += 1 }
      sonda(t)
    }
    (p, () => kaplumbağa)
  }

  test("kaplumbağanın dolgusu boşaltmadan ÖNCE bekliyor (mekanizma çalışıyor)") {
    val (p, t) = doluKare()
    p.draw()
    for (_ <- p.ready) yield {
      withClue("yayın tembel olmalı: boşaltmadan önce dolgu görünmemeli -- ") {
        dolguKöşeleri(t()) shouldBe empty
      }
      kojoWorld.boyalarıBoşalt()
      dolguKöşeleri(t()) should not be empty
    }
  }

  test("bir çizerin bekleyeni düşünce ÖTEKİNİN bekleyeni ayakta kalıyor") {
    val (pA, tA) = doluKare()
    // B'nin gövdesi sil() ile bitiyor; sil() kuyruğa giriyor, realClear
    // p.ready'den önce koşuyor -- yani gerçek yolu sınıyoruz.
    val (pB, tB) = doluKare(_.clear())
    pA.draw()
    for {
      _ <- pA.ready
      _ = pB.draw()
      _ <- pB.ready
    } yield {
      kojoWorld.boyalarıBoşalt()
      withClue("B'nin sil()'i A'nın bekleyen dolgusunu düşürmemeli -- ") {
        dolguKöşeleri(tA()) should not be empty
      }
      withClue("B kendi dolgusunu sildi, onda kalmamalı -- ") {
        dolguKöşeleri(tB()) shouldBe empty
      }
    }
  }

  test("resimleriSil'in düşürmesi: hayatta kalan katmanın dolgusu düşmemeli") {
    // TestKojoWorld.erasePictures boş bir saplama olduğu için gerçek çağrı
    // yerini sınayamıyoruz; erasePictures'ın YAPTIĞI düşürmeyi sınıyoruz.
    // KojoWorldImpl.erasePictures 'Turtle Layer' çocuklarını silmiyor, yani
    // burada düşen dolgu orada da düşerdi.
    val (p, t) = doluKare()
    p.draw()
    for (_ <- p.ready) yield {
      kojoWorld.bekleyenBoyayıUnut(t()) // erasePictures artık BUNU yapmıyor
      kojoWorld.boyalarıBoşalt()
      withClue("çizer başına düşürme doğru çalışmalı -- ") {
        dolguKöşeleri(t()) shouldBe empty
      }
    }
  }
}
