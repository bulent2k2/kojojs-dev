package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Future
import scala.scalajs.js
import pixiscalajs.PIXI

/**
 * Sorun #115: İÇ İÇE resimlerde (GPics ve öteki BasePicSequence'lar) silinen
 * dolgu yayınlanmaya devam ediyordu.
 *
 * #109'un `resimleriSil()` kolu #112 ile kapanmıştı, ama yalnız SAHNE ÇOCUĞU
 * olan katmanlar için. Bir resim bir grubun içindeyse sahneden çıkan düğüm
 * grubun kabı; çizerin kendi `turtleLayer`'ı onun ALTINDA kalıyordu, yani
 * `katmanınBoyasınıUnut`un `ne katman` ölçütüyle eşleşmiyor ve "silindi" imini
 * de almıyordu. Sonuç: kaplumbağanın komut kuyruğu boşaldıkça çizer
 * `boyaKirlendi` ile sıraya geri giriyor ve görünmeyen bir şekil yeniden
 * yeniden üçgenleniyordu.
 *
 * Ölçüldü (40 kare, her karede resimleriSil + yeniden çizim, gerçek
 * KojoWorldImpl, başsız Chromium/SwiftShader; üçer koşu):
 *
 *   çıplak iki gül     düzeltmeden önce  74 / 74 / 74     sonra  74 / 76 / 74
 *   GPics(gül, gül)    düzeltmeden önce 512 / 533 / 512   sonra 114 / 114 / 114
 *
 * Kalan 114-74 = 40 ÖLÜ İŞ DEĞİL: kare sayısıyla doğrusal ve belirlenimci
 * (40 kare 114, 80 kare 234; çıplak 74 ve 156). Yani grup sarmalının canlı
 * maliyeti kare başına +1 yayın; bozuk hâlde ise kare başına ~13 idi ve
 * zamanlamaya göre oynuyordu.
 */
class IcIceSilmeTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  // ---- B/C. Düşürmenin doğruluğu: TestKojoWorld + doğrudan çağrı ----------
  //
  // TestKojoWorld.erasePictures BOŞ bir saplama (bkz. TembelSilmeTest'in aynı
  // uyarısı), o yüzden erasePictures'ın YAPTIĞI düşürmeyi doğrudan sınıyoruz.

  private implicit val testDünyası: TestKojoWorld = new TestKojoWorld()
  private val builtins = new kojo.syntax.Builtins()

  private def grafikler(kap: PIXI.Container): Seq[js.Dynamic] =
    kap.children.toSeq.map(_.asInstanceOf[js.Dynamic]).filter(d => js.typeOf(d.finishPoly) == "function")

  /** Katmandaki alan kaplayan dolgu parçalarının köşe sayıları. */
  private def dolguKöşeleri(t: Turtle): Seq[Int] =
    grafikler(t.turtleLayer).flatMap { g =>
      g.finishPoly()
      g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].toSeq
    }.filter(gd => gd.fillStyle.visible.asInstanceOf[Boolean])
      .filter(gd => js.typeOf(gd.shape.points) != "undefined")
      .map(gd => gd.shape.points.asInstanceOf[js.Array[Double]].length / 2)
      .filter(_ >= 3)

  private def doluKare(): (TurtlePicture, () => Turtle) = {
    var kaplumbağa: Turtle = null
    val p = builtins.PictureT { t =>
      t.invisible()
      t.setAnimationDelay(0)
      t.setFillColor(kojo.doodle.Color.blue)
      kaplumbağa = t
      var i = 0
      while (i < 4) { t.forward(100); t.right(90); i += 1 }
    }
    (p, () => kaplumbağa)
  }

  test("grup silinince İÇİNDEKİ çizerlerin bekleyen dolgusu da düşüyor (#115)") {
    val (a, ta) = doluKare()
    val (b, tb) = doluKare()
    val g = builtins.GPics(a, b)
    g.draw()
    for {
      _ <- g.ready
    } yield {
      withClue("ön koşul: boşaltmadan önce dolgu bekliyor olmalı -- ") {
        testDünyası.bekleyenBoyaSayısı should be > 0
      }
      testDünyası.katmanınBoyasınıUnut(g.tnode.asInstanceOf[PIXI.Container])
      testDünyası.boyalarıBoşalt()
      withClue("grup silindi: A'nın dolgusu yayınlanmamalı -- ") { dolguKöşeleri(ta()) shouldBe empty }
      withClue("grup silindi: B'nin dolgusu yayınlanmamalı -- ") { dolguKöşeleri(tb()) shouldBe empty }
    }
  }

  test("düşürülen dolgu ÇOCUK yeniden çizilince geri geliyor -- bilgi kaybı yok (#115)") {
    // Grubun KENDİSİ yeniden çizilemiyor: BasePicSequence.layout -> makeDone
    // ikinci kez çağrılınca readyPromise zaten tamamlanmış oluyor (bu #115'in
    // konusu değil, ReadyPromise'ın yerleşik davranışı). Gerçekçi ve sınanabilir
    // olan yol, betiğin elinde tuttuğu ÇOCUK resmi yeniden çizmesi.
    val (a, ta) = doluKare()
    val (b, _) = doluKare()
    val g = builtins.GPics(a, b)
    g.draw()
    for {
      _ <- g.ready
      _ = {
        testDünyası.katmanınBoyasınıUnut(g.tnode.asInstanceOf[PIXI.Container])
        testDünyası.boyalarıBoşalt()
      }
      // İm ÇOCUĞUN kendi katmanında olmalı; grup kabına yazılsaydı realDraw
      // onu göremezdi.
      imÇocukta = PixiUyum.düşenBoyaVarMı(a.tnode)
      _ = a.draw() // çocuğun kendi realDraw'ı: düşen-boya imini KENDİ tnode'unda okumalı
      _ <- Future(())
    } yield {
      testDünyası.boyalarıBoşalt()
      withClue("düşen-boya imi çocuğun KENDİ katmanına yazılmalı -- ") { imÇocukta shouldBe true }
      // İm grup KABINA yazılsaydı buradan dolgusuz çıkılırdı: TurtlePicture.realDraw
      // çocuğun kendi tnode'una bakıyor, kaba değil.
      withClue("yeniden çizilen çocuğun dolgusu geri gelmeli -- ") {
        dolguKöşeleri(ta()) should not be empty
      }
    }
  }

  test("bir grubun silinmesi BAŞKA bir grubun bekleyenini düşürmüyor (#115)") {
    val (a, ta) = doluKare()
    val (b, tb) = doluKare()
    val gA = builtins.GPics(a)
    val gB = builtins.GPics(b)
    gA.draw(); gB.draw()
    for {
      _ <- gA.ready
      _ <- gB.ready
    } yield {
      testDünyası.katmanınBoyasınıUnut(gB.tnode.asInstanceOf[PIXI.Container])
      testDünyası.boyalarıBoşalt()
      withClue("B'nin silinmesi A'nın bekleyen dolgusunu düşürmemeli -- ") {
        dolguKöşeleri(ta()) should not be empty
      }
      withClue("B silindi, onda dolgu olmamalı -- ") { dolguKöşeleri(tb()) shouldBe empty }
    }
  }
}
