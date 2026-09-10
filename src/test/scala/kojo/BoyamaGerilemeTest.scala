package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * `boyamaRenginiKur`ün ASIL kusurunu çivileyen gerileme sınaması.
 *
 * NEDEN VAR: kusur master'da fark edilmeden durdu ve HİÇBİR sınama kırmızı
 * yanmadı -- ölçüldü: kusurlu kodda tarayıcı takımının tamamı 92/92 yeşil.
 * Hiçbir sınama bu davranışı kapsamıyordu. BoyamaYolu'nun kendi sınaması saf
 * çekirdeği doğruluyor, ama çekirdek silinir ya da Turtle'daki bağlantısı
 * koparılırsa hiçbir şey söylemez. Buradaki sav Turtle'ın DAVRANIŞINA
 * bakıyor: iki durumda da düşüyor.
 *
 * KUSUR NEYDİ: PIXI 5'te `Graphics._render` her çizimde `finishPoly()` çağırıp
 * yarım kalan çokgeni olduğu yerde kapatıyor. Kaplumbağa şekli kenar kenar
 * kuruyor; canlandırma açıkken (varsayılan `animationDelay = 1000`) her kenar
 * ayrı bir kareye düşüyor, yani her kenardan sonra bir render giriyor ve
 * dolgu hiç oluşmuyordu. `hızıKur(çokHızlı)` çalışıyordu, çünkü orada bütün
 * şekil tek blokta kuruluyor ve araya render girmiyor.
 *
 * NASIL SINANIYOR: canlandırmayı taklit etmek gerekmiyor -- ve etmemeli,
 * requestAnimationFrame'e dayanan bir sınama zamanlamaya bağlı olurdu.
 * Renderın dolguya YAPTIĞI ŞEY tek bir çağrı: `finishPoly()`. Onu komut
 * kuyruğunun içinden (`sync`) her kenardan sonra çağırınca kusur birebir,
 * kare beklemeden ve zamanlamadan bağımsız olarak üretiliyor.
 */
class BoyamaGerilemeTest extends AsyncFunSuite with Matchers with RepeatCommands {
  import kojo.syntax.Builtins
  implicit val kojoWorld = new TestKojoWorld()
  val builtins = new Builtins()
  import builtins._
  import builtins.Color._
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /** Katmandaki Graphics'ler. PIXI küçültülmüş olduğu için sınıf ADINA değil,
    * `finishPoly` yeteneğine bakıyoruz (minifier adları değiştirebiliyor). */
  private def grafikler(kap: pixiscalajs.PIXI.Container): Seq[js.Dynamic] =
    kap.children.toSeq
      .map(_.asInstanceOf[js.Dynamic])
      .filter(d => js.typeOf(d.finishPoly) == "function")

  /** Renderın dolguya yaptığı şey: yarım çokgeni olduğu yerde kapat. */
  private def renderTaklidi(t: Turtle): Unit =
    grafikler(t.turtleLayer).foreach(_.finishPoly())

  /** Katmandaki DOLGULU parçaların köşe sayıları. */
  private def dolguKöşeSayıları(t: Turtle): Seq[Int] =
    grafikler(t.turtleLayer).flatMap { g =>
      g.finishPoly()
      g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].toSeq
    }
      .filter(gd => gd.fillStyle.visible.asInstanceOf[Boolean])
      .filter(gd => js.typeOf(gd.shape.points) != "undefined")
      .map(gd => gd.shape.points.asInstanceOf[js.Array[Double]].length / 2)

  /** Alan kaplayan (en az üç köşeli) dolgu parçası var mı. */
  private def alanKaplayanDolgu(t: Turtle): Seq[Int] = dolguKöşeSayıları(t).filter(_ >= 3)

  private def kare(araRender: Boolean): (TurtlePicture, () => Turtle) = {
    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible()
      setAnimationDelay(0) // canlandırmayı taklit etmiyoruz; renderı elle koyuyoruz
      setFillColor(blue)
      kaplumbağa = t
      repeat(4) {
        forward(100)
        right()
        // Kuyruğun İÇİNDEN: gerçek renderın kenarlar arasına düştüğü yer tam burası.
        if (araRender) t.sync(() => renderTaklidi(t))
      }
    }
    (p, () => kaplumbağa)
  }

  test("kenarlar arasında render OLMADAN dolgu alanı oluşuyor (çokHızlı hâli)") {
    val (p, t) = kare(araRender = false)
    p.draw()
    for (_ <- p.ready) yield {
      // Bu yol kusurluyken de çalışıyordu; savı simetrik tutmak için burada.
      alanKaplayanDolgu(t()) should not be empty
    }
  }

  test("kenarlar arasında render OLSA DA dolgu alanı oluşuyor (varsayılan hız hâli)") {
    val (p, t) = kare(araRender = true)
    p.draw()
    for (_ <- p.ready) yield {
      val köşeler = alanKaplayanDolgu(t())
      withClue(
        "Her kenardan sonra render girince dolgu kayboluyor: bütün dolgu " +
          s"parçaları ikişer köşeli. Katmandaki dolgu köşe sayıları: ${dolguKöşeSayıları(t())}. " +
          "boyamaRenginiKur varsayılan hızda hiçbir şeyi doldurmuyor demektir. -- "
      ) {
        köşeler should not be empty
      }
      // Kapalı kare: beş köşe (başlangıç noktası sonda yineleniyor).
      köşeler.max should be(5)
    }
  }
}
