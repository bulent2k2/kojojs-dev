package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * KALEM YOLU DOLGU TAŞIMAZ (#126).
 *
 * Kusur: `boyamaRenginiKur` ile iki ayrı renkte iki şekil çizildiğinde, hızlı
 * modda (`hızıKur(çokHızlı)`) ikinci şekil de BİRİNCİ renkte görünüyordu.
 *
 * Sebep renk seçiminde değil, Z-SIRASINDA: `kalemYolunuDondur` yeni kalem
 * yoluna `fillBoya` ile açık bir `beginFill` koyuyordu. PIXI 5 her çizimde
 * `finishPoly()` çağırıp yarım çokgeni kapatıyor, yani kalem izi DOLU bir
 * çokgen olarak boyanıp altındaki gerçek dolgunun üstünü örtüyordu. Üstelik o
 * çağrı `realSetFillPaint` içinde `fillBoya = boya`dan ÖNCE koştuğu için renk
 * hâlâ ESKİ renkti: ikinci şeklin kalem yolu birinci şeklin rengiyle doğuyor.
 *
 * Neden yalnız hızlı modda: yavaş modda her kenardan sonra bir render giriyor,
 * `finishPoly` çokgeni 2 noktada kapatıyor ve alan kaplamıyor. Hızlı modda
 * bütün kenarlar tek blokta geliyor, çokgen kapanınca kocaman bir dolgu oluyor.
 *
 * Ölçüldü (#126 reprosunun kalıbı, turtleLayer çocukları):
 *   önce : [Turtle Fill: renk=ff] [Turtle Fill (in progress): renk=ff0000]
 *          [Turtle Path: renk=ff]        <- kalem yolu MAVİ dolguluydu, üstteydi
 *   sonra: [Turtle Path: renk=]          <- kalem yolunda dolgu yok
 *
 * Kırma sınaması (iki `boyamayaBaşla(turtlePath, fillBoya)` çağrısı geri
 * konarak ölçüldü): birinci sınama KIZARIYOR --
 *   "kalem yolları dolgusuz olmalı -- Vector("ff") was not empty".
 * İkinci sınama kırma altında da YEŞİL kalıyor; onu aşağıda anlatıyorum.
 */
class KalemDolgusuTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
    new KojoWorldImpl()
  }

  private def kareler(n: Int)(adım: Int => Unit): Future[Unit] = {
    val söz = Promise[Unit](); var i = 0
    def d(): Unit = { i += 1; adım(i); if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => d()) }
    window.requestAnimationFrame(_ => d()); söz.future
  }

  /** turtleLayer'daki her Graphics için (ad, görünür dolgu renkleri). */
  private def katmanDökümü(t: Turtle): Seq[(String, Seq[String])] = {
    val kap = t.turtleLayer.asInstanceOf[js.Dynamic]
    kap.children.asInstanceOf[js.Array[js.Dynamic]].toSeq.flatMap { g =>
      if (js.typeOf(g.finishPoly) != "function") None
      else {
        g.finishPoly()
        val renkler = g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].toSeq
          .filter(d => d.fillStyle.visible.asInstanceOf[Boolean])
          .map(d => d.fillStyle.color.asInstanceOf[Double].toInt.toHexString)
          .distinct
        Some((g.name.asInstanceOf[String], renkler))
      }
    }
  }

  /** #126'nın reprosunun küçültülmüşü: iki gül, iki renk, çokHızlı. */
  private def ikiGül()(implicit w: KojoWorldImpl): Turtle = {
    val t = new Turtle(0, 0)
    def gül(nokta: Int, kat: Int, yarıçap: Double, renk: kojo.doodle.Color): Unit = {
      val kenar = 2 * yarıçap * math.sin(math.toRadians(kat * 180.0 / nokta))
      val dönüş = kat * 360.0 / nokta
      t.setFillColor(renk)
      var i = 0; while (i < nokta) { t.forward(kenar); t.right(dönüş); i += 1 }
    }
    t.setAnimationDelay(0) // çokHızlı: bütün kenarlar tek blokta
    t.setPenThickness(0)
    t.penUp(); t.setPosition(-170, 0); t.penDown()
    gül(60, 7, 140, kojo.doodle.Color.blue)
    t.penUp(); t.setPosition(170, 0); t.penDown()
    gül(60, 7, 140, kojo.doodle.Color.red)
    t.invisible()
    t
  }

  test("kalem yolu DOLGU TAŞIMIYOR: ikinci şekil birinci rengin altında kalmıyor (#126)") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    val t = ikiGül()
    kareler(40) { _ => () }.map { _ =>
      val döküm = katmanDökümü(t)
      val kalemler = döküm.filter(_._1 == "Turtle Path")
      withClue(s"\n${döküm.map { case (a, r) => s"[$a: ${r.mkString(",")}]" }.mkString(" ")}\n") {
        withClue("kalem yolları dolgusuz olmalı -- ") {
          kalemler.flatMap(_._2) shouldBe empty
        }
        kalemler should not be empty // sondanın gerçekten kalem yolu gördüğünü doğrula
      }
    }
  }

  // Bu sınama kusuru YAKALAMIYOR: #126'da renk seçimi zaten doğruydu (ölçüldü --
  // kırma altında da yeşil kalıyor), kusur Z-sırasındaydı. Yine de duruyor,
  // çünkü çözüm kalem yolundan dolguyu kaldırıyor: gerçek dolgunun iki ayrı
  // renkte var olmayı SÜRDÜRDÜĞÜNÜ bu sınama bekliyor. Dolguyu yanlışlıkla
  // birlikte kaldıran bir değişiklik burada kızarır.
  test("iki şeklin dolgusu İKİ AYRI renkte (#126)") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    val mavi = kojo.doodle.Color.blue.toRGBDouble.toInt.toHexString
    val kırmızı = kojo.doodle.Color.red.toRGBDouble.toInt.toHexString
    val t = ikiGül()
    kareler(40) { _ => () }.map { _ =>
      val döküm = katmanDökümü(t)
      // Biten şekil kalıcı "Turtle Fill"e, süren şekil "Turtle Fill (in progress)"e gidiyor.
      val dolguRenkleri = döküm.filter(_._1.startsWith("Turtle Fill")).flatMap(_._2).distinct
      withClue(s"\nmavi=$mavi kırmızı=$kırmızı\n" +
        s"${döküm.map { case (a, r) => s"[$a: ${r.mkString(",")}]" }.mkString(" ")}\n") {
        dolguRenkleri should contain(mavi)
        dolguRenkleri should contain(kırmızı)
      }
    }
  }
}
