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
 *   önce : [Turtle Fill: ff] [Turtle Path: ] [Turtle Fill (in progress): ff0000]
 *          [Turtle Path: ff]        <- İKİNCİ kalem yolu MAVİ dolguluydu, üstteydi
 *   sonra: [Turtle Path: ]          <- kalem yolunda dolgu yok
 *
 * BURADA İKİ AYRI KATMAN SINANIYOR (incelemenin 2. bulgusu):
 *   - mekanizma: kalem yolları dolgu taşımıyor (aşağıdaki ilk iki sınama),
 *   - belirti  : ikinci şekil GERÇEKTEN kendi renginde görünüyor (piksel
 *     sınaması). Belirti, örtmenin başka bir yoldan (katman sırası, `öneAl`,
 *     `boyamayıİşle`nin ekleme noktası) geri gelmesine de kızarır.
 *
 * Kırma sınaması (iki `boyamayaBaşla(turtlePath, fillBoya)` çağrısı geri
 * konarak ölçüldü): mekanizma sınaması KIZARIYOR --
 *   "kalem yolları dolgusuz olmalı -- Vector("ff") was not empty".
 *
 * ÖNKOŞUL NEDEN VAR (incelemenin 1. bulgusu): kusur ancak İKİNCİ
 * `setFillColor`'da doğuyor; birinci kalem yolu kusurlu kodda BİLE dolgusuz
 * (yukarıdaki "önce" satırı). Sabit bir kare sayısı beklemek, o ana varıldığını
 * TUTMUYOR: kuyruk yavaş boşalırsa dökümde yalnız birinci kalem yolu olur ve
 * sınama boş yere yeşil yanar. Bu yüzden her sınama "iki rengin de sahnede
 * olması" koşulunu bekliyor ve varılamazsa AÇIKÇA kızarıyor.
 */
class KalemDolgusuTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private val Mavi = kojo.doodle.Color.blue.toRGBDouble.toInt.toHexString
  private val Kırmızı = kojo.doodle.Color.red.toRGBDouble.toInt.toHexString

  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
    new KojoWorldImpl()
  }

  /** Koşul sağlanana kadar kare koştur; sağlandı mı diye döner (azami kare sonra false). */
  private def koşulaKadar(azami: Int)(koşul: () => Boolean): Future[Boolean] = {
    val söz = Promise[Boolean](); var i = 0
    def d(): Unit = {
      i += 1
      if (koşul()) söz.success(true)
      else if (i >= azami) söz.success(false)
      else window.requestAnimationFrame(_ => d())
    }
    window.requestAnimationFrame(_ => d())
    söz.future
  }

  /**
   * turtleLayer'daki her Graphics için (ad, görünür dolgu renkleri).
   *
   * DİKKAT: salt-okunur DEĞİL -- `finishPoly()` çağırıyor, yani bekleyen
   * çokgeni kapatıp `graphicsData`'ya yazıyor; sahneyi `_render`'ın yaptığı
   * gibi ilerletiyor. Kusur tam da bu kapanmayla görünür olduğu için bilerek
   * öyle. Zaten kapanmış bir çokgende `finishPoly` etkisiz.
   */
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

  private def dolguRenkleri(t: Turtle) =
    katmanDökümü(t).filter(_._1.startsWith("Turtle Fill")).flatMap(_._2).distinct

  /** İki rengin de sahnede olması = ikinci setFillColor koştu = kusurun doğduğu an. */
  private def ikiRenkDeVarMı(t: Turtle): Boolean = {
    val r = dolguRenkleri(t)
    r.contains(Mavi) && r.contains(Kırmızı)
  }

  private def dökümYazısı(t: Turtle) =
    katmanDökümü(t).map { case (a, r) => s"[$a: ${r.mkString(",")}]" }.mkString(" ")

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
    koşulaKadar(180)(() => ikiRenkDeVarMı(t)).map { varıldı =>
      withClue(s"\n${dökümYazısı(t)}\n") {
        withClue(s"kusurun doğduğu ana (ikinci setFillColor) varılmış olmalı, yoksa sav boş -- ") {
          varıldı shouldBe true
        }
        val kalemler = katmanDökümü(t).filter(_._1 == "Turtle Path")
        kalemler should not be empty
        withClue("kalem yolları dolgusuz olmalı -- ") {
          kalemler.flatMap(_._2) shouldBe empty
        }
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
    val t = ikiGül()
    koşulaKadar(180)(() => ikiRenkDeVarMı(t)).map { varıldı =>
      withClue(s"\nmavi=$Mavi kırmızı=$Kırmızı\n${dökümYazısı(t)}\n") {
        varıldı shouldBe true
      }
    }
  }

  // --- belirti: ekranda ne görünüyor ------------------------------------------
  // Piksel okuma kalıbı SolukTest'ten: sahneyi kendi RenderTexture'ımıza
  // çizmek, çünkü kare birleştikten sonra hem extract.pixels(düğüm) hem de
  // varsayılan tampon 0 okuyor (orada ölçülüp yazılmış).
  //
  // Gül yerine KARE: bir karenin ortası kuşkusuz dolgunun içi, ve ikinci
  // karenin kalem yolu (kusurlu kodda mavi dolgulu) tam o dolgunun üstüne
  // oturuyor -- belirti en yalın burada.

  /** İki kare, mavi sonra kırmızı, çokHızlı. Köşeler: sol [-100,-20], sağ [20,100]. */
  private def ikiKare()(implicit w: KojoWorldImpl): Turtle = {
    val t = new Turtle(0, 0)
    def kare(x0: Double, kenar: Double, renk: kojo.doodle.Color): Unit = {
      t.penUp(); t.setPosition(x0, -kenar / 2); t.setHeading(90); t.penDown()
      t.setFillColor(renk)
      var i = 0; while (i < 4) { t.forward(kenar); t.right(90); i += 1 }
    }
    t.setAnimationDelay(0)
    t.setPenThickness(0)
    kare(-100, 80, kojo.doodle.Color.blue)
    kare(20, 80, kojo.doodle.Color.red)
    t.invisible()
    t
  }

  /** Sahneyi kendi dokumuza çizip dünya (x,y) noktasının RGB'sini okur. */
  private def noktanınRengi(w: KojoWorldImpl, t: Turtle, dx: Double, dy: Double): (Int, Int, Int) = {
    val d = w.renderer.asInstanceOf[js.Dynamic]
    val sahne = t.turtleLayer.asInstanceOf[js.Dynamic].parent
    val en = d.width.asInstanceOf[Double].toInt
    val yük = d.height.asInstanceOf[Double].toInt
    // sahne dönüşümü: konum (en/2, yük/2), ölçek (1, -1) -> y ters
    val sx = (sahne.position.x.asInstanceOf[Double] + dx).toInt
    val sy = (sahne.position.y.asInstanceOf[Double] - dy).toInt
    val rt = js.Dynamic.global.PIXI.RenderTexture.create(js.Dictionary("width" -> en, "height" -> yük))
    d.render(sahne, rt, true)
    val px = d.plugins.extract.pixels(rt).asInstanceOf[js.typedarray.Uint8Array]
    val i = ((sy * en) + sx) * 4
    (px(i).toInt, px(i + 1).toInt, px(i + 2).toInt)
  }

  test("BELİRTİ: ikinci şekil ekranda gerçekten KIRMIZI görünüyor (#126)") {
    implicit val w: KojoWorldImpl =
      try dünyaKur() catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }
    val t = ikiKare()
    koşulaKadar(180)(() => ikiRenkDeVarMı(t)).map { varıldı =>
      withClue(s"\n${dökümYazısı(t)}\n") { varıldı shouldBe true }
      w.boyalarıBoşalt()
      val (sr, sg, sb) = noktanınRengi(w, t, -60, 0) // birinci karenin ortası
      val (kr, kg, kb) = noktanınRengi(w, t, 60, 0)  // ikinci karenin ortası
      withClue(s"\nsol(mavi olmalı)=($sr,$sg,$sb) sağ(kırmızı olmalı)=($kr,$kg,$kb)\n${dökümYazısı(t)}\n") {
        withClue("birinci kare mavi kalmalı -- ") { sb should be > (sr + 60) }
        withClue("ikinci kare KIRMIZI görünmeli (kusurda mavi görünüyordu) -- ") {
          kr should be > (kb + 60)
        }
      }
    }
  }
}
