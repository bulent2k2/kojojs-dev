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
 *   - `resimleriSil()` (erasePictures): GERÇEK kaplumbağaların katmanlarını
 *     BİLEREK silmiyor, yani hayatta kalan katmanlar tam olarak duran
 *     Boyacıların katmanları -- düşürülen dolgular da tam onlarınki.
 *     (Ölçüt bir ara ADA bakıyordu ve Resim{} katmanları da o adı taşıdığı için
 *     hiç silinmiyordu; bkz. sorun #91 / KaynakSizintisiTest.)
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

  /** Katman hâlâ bir ebeveyne bağlı mı (yani sahnede mi). */
  private def sahnedeMi(t: Turtle): Boolean = {
    val p = t.turtleLayer.asInstanceOf[js.Dynamic].parent
    !js.isUndefined(p) && p != null
  }

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
    // KojoWorldImpl.erasePictures gerçek kaplumbağa katmanlarını silmiyor, yani
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

  // --- Silinen resim: TERS yön --------------------------------------------
  //
  // Yukarısı dolgunun KAYBOLMAMASINI çiviliyor. Buradan aşağısı tersini:
  // sahneden ÇIKARILMIŞ bir resmin dolgusu yeniden YAYINLANMAMALI.
  //
  // TurtlePicture.picLayer kaplumbağanın KENDİ turtleLayer'ı, yani erase()
  // katmanı sahneden çıkarıyor. removeLayer çizeri bilmiyor (bekleyenBoyayıUnut
  // bir Boyacı istiyor, Boyacı katmanına gönderme taşımıyor), o yüzden düşürme
  // erase()'in kendi işi. Düşürülmezse sahnede olmayan bir şekil bir kez daha
  // üçgenleniyor -- n büyük ve kesişen şekillerde pahalı (bkz. #68).

  test("silinen resim gerçekten sahneden çıkıyor (aşağıdaki sınamanın ön koşulu)") {
    val (p, t) = doluKare()
    p.draw()
    for {
      _ <- p.ready
      önce = sahnedeMi(t())
      _ = p.erase()
      _ <- p.ready
    } yield {
      withClue("çizilince sahnede olmalı -- ") { önce shouldBe true }
      withClue("erase sonrası sahnede olmamalı -- ") { sahnedeMi(t()) shouldBe false }
    }
  }

  test("silinen resmin dolgusu boşaltmada YENİDEN yayınlanmıyor") {
    val (p, t) = doluKare()
    p.draw()
    for {
      _ <- p.ready
      _ = p.erase()
      _ <- p.ready
    } yield {
      withClue("erase sonrası, boşaltmadan önce dolgu olmamalı -- ") {
        dolguKöşeleri(t()) shouldBe empty
      }
      kojoWorld.boyalarıBoşalt()
      withClue("sahneden çıkmış resim yeniden üçgenlenip yayınlandı -- ") {
        dolguKöşeleri(t()) shouldBe empty
      }
    }
  }

  test("silme BİR resmin bekleyenini düşürüyor, ÖTEKİNİNKİNİ değil") {
    val (pA, tA) = doluKare()
    val (pB, tB) = doluKare()
    pA.draw()
    for {
      _ <- pA.ready
      _ = pB.draw()
      _ <- pB.ready
      _ = pB.erase()
      _ <- pB.ready
    } yield {
      kojoWorld.boyalarıBoşalt()
      withClue("B'nin silinmesi A'nın bekleyen dolgusunu düşürmemeli -- ") {
        dolguKöşeleri(tA()) should not be empty
      }
      withClue("B silindi, onda dolgu olmamalı -- ") {
        dolguKöşeleri(tB()) shouldBe empty
      }
    }
  }

  // --- Silinen resim yeniden çizilirse ------------------------------------
  //
  // Düşürülen yayın BİLGİ taşıyor. Yukarıdaki düşürme sahne dışına boşa
  // üçgenlemeyi önlüyor, ama hiç yayınlanmamış bir dolgu öyle kaybolursa
  // resim yeniden çizilince dolgusuz görünür. Bu iki sınama o sınırı
  // çiviliyor; ikincisi bir ara gerçekten kırıktı (bkz. TurtlePicture'daki
  // dolguDüşürüldü notu).

  test("yayınlanmış resim silinip yeniden çizilince dolgusu duruyor") {
    val (p, t) = doluKare()
    p.draw()
    for {
      _ <- p.ready
      _ = kojoWorld.boyalarıBoşalt() // ilk yayın olsun
      önce = dolguKöşeleri(t())
      _ = p.erase()
      _ <- p.ready
      _ = p.draw()
      _ <- p.ready
      _ = kojoWorld.boyalarıBoşalt()
    } yield {
      withClue("ilk yayın olmuş olmalı (sınavın ön koşulu) -- ") { önce should not be empty }
      withClue("silip yeniden çizince dolgu durmalı -- ") {
        dolguKöşeleri(t()) should not be empty
      }
    }
  }

  test("HİÇ yayınlanmamış resim silinip yeniden çizilince dolgusu geri geliyor") {
    val (p, t) = doluKare()
    p.draw()
    for {
      _ <- p.ready
      hiç = dolguKöşeleri(t()) // boşaltma YOK: dolgu hâlâ bekliyor
      _ = p.erase()
      _ <- p.ready
      _ = p.draw()
      _ <- p.ready
      _ = kojoWorld.boyalarıBoşalt()
    } yield {
      withClue("boşaltmadan önce dolgu olmamalı (yayın tembel) -- ") { hiç shouldBe empty }
      withClue("silme bekleyeni düşürdü; yeniden çizim onu geri istemeli -- ") {
        dolguKöşeleri(t()) should not be empty
      }
    }
  }
}
