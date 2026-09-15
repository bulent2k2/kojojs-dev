package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * Sorun #86'nın gerileme savı: ÖNCE çizilen bir şeklin kenarlığı, SONRA
 * çizilen bir şeklin dolgusunun ÜSTÜNDE kalmamalı.
 *
 * KUSUR NEYDİ: tamamlanmış dolguların tamamı tek bir `boyamaBitmiş`te, kalem
 * izinin tamamı tek bir `turtlePath`te birikiyordu ve katman sırası bir kez,
 * init'te kuruluyordu -- dolgular altta, kalem üstte. Yani kaç şekil çizilirse
 * çizilsin BÜTÜN kenarlıklar BÜTÜN dolguların üstünde kalıyordu. On kare çizen
 * betikte ölçüldü: turtleLayer'ın çocukları
 *   ["Turtle Fill (done)"(27 parça), "Turtle Fill (in progress)",
 *    "Turtle Path"(40 parça), "Turtle Icon"]
 *
 * DÜZELTME: her tamamlanan şekil kendi dolgu düğümünü alıyor ve o şeklin kalem
 * izinin hemen ALTINA konuyor; kalem izi orada donup üstüne yeni bir canlı yol
 * açılıyor. Katman sırası çizilme sırasıyla örtüşüyor.
 *
 * NEDEN SIRAYA BAKIYORUZ: sav tam olarak Z-SIRASI hakkında, piksel hakkında
 * değil; WebGL geri okuması bu takımda güvenilir değil. Sıra yanlışsa piksel de
 * yanlış olur -- kusurlu kodda bu sıra hiç oluşmuyordu.
 */
class KatmanSirasiTest extends AsyncFunSuite with Matchers with RepeatCommands {
  import kojo.syntax.Builtins
  implicit val kojoWorld = new TestKojoWorld()
  val builtins = new Builtins()
  import builtins._
  import builtins.Color._
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def adlar(t: Turtle): Seq[String] =
    t.turtleLayer.children.toSeq.map(_.asInstanceOf[js.Dynamic].name.asInstanceOf[String])

  /** İki dolgulu kare. Her `setFillColor` bir öncekini kalıcıya yazıyor, yani
    * üçüncü çağrı ikinci kareyi de kapatıyor. */
  private def ikiKare(): (TurtlePicture, () => Turtle) = {
    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible()
      setAnimationDelay(0)
      kaplumbağa = t
      setFillColor(blue)
      repeat(4) { forward(100); right() }
      setFillColor(red) // 1. kareyi kapatır, 2.yi açar
      repeat(4) { forward(100); right() }
      setFillColor(green) // 2. kareyi kapatır
    }
    (p, () => kaplumbağa)
  }

  test("her tamamlanan şekil kendi dolgu düğümünü alıyor") {
    val (p, t) = ikiKare()
    p.draw()
    for (_ <- p.ready) yield {
      adlar(t()).count(_ == "Turtle Fill") shouldBe 2
    }
  }

  test("sonraki şeklin dolgusu, önceki şeklin kalem izinin ÜSTÜNDE") {
    val (p, t) = ikiKare()
    p.draw()
    for (_ <- p.ready) yield {
      val a = adlar(t())
      val ilkDolgu = a.indexOf("Turtle Fill")
      val ilkKalem = a.indexOf("Turtle Path")
      val ikinciDolgu = a.indexOf("Turtle Fill", ilkDolgu + 1)
      withClue(s"katman sırası: ${a.mkString(" | ")} -- ") {
        ilkDolgu should be >= 0
        ikinciDolgu should be > ilkDolgu
        // 1. kare: kenarlık kendi dolgusunun ÜSTÜNDE
        ilkKalem should be > ilkDolgu
        // ASIL SAV: 2. karenin dolgusu 1. karenin kenarlığını örtebilmeli
        ikinciDolgu should be > ilkKalem
      }
    }
  }

  test("tamamlanan parçalar dolgu/kalem diye SIRAYLA diziliyor") {
    val (p, t) = ikiKare()
    p.draw()
    for (_ <- p.ready) yield {
      // "(in progress)" ve simge kuyrukta; onlardan öncesi tamamlanmış çift(ler)
      val tamamlanan = adlar(t()).takeWhile(ad => ad == "Turtle Fill" || ad == "Turtle Path")
      withClue(s"tamamlanan parçalar: ${tamamlanan.mkString(" | ")} -- ") {
        tamamlanan shouldBe Seq("Turtle Fill", "Turtle Path", "Turtle Fill", "Turtle Path")
      }
    }
  }

  test("sil() donmuş parçaları da katmandan çıkarıyor") {
    val (p, t) = ikiKare()
    p.draw()
    p.ready.flatMap { _ =>
      adlar(t()).count(_ == "Turtle Fill") shouldBe 2
      // clear KOMUT KUYRUĞUNA giriyor; sync ile tam arkasına düşüyoruz, yoksa
      // sav silme işlenmeden koşar ve hiçbir şey sınamaz.
      val söz = scala.concurrent.Promise[Seq[String]]()
      t().clear()
      t().sync(() => söz.success(adlar(t())))
      söz.future.map { sonra =>
        withClue(s"sil() sonrası katman: ${sonra.mkString(" | ")} -- ") {
          sonra.count(_ == "Turtle Fill") shouldBe 0
        }
      }
    }
  }
}
