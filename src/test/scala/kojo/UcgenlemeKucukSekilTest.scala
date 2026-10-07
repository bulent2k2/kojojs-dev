package kojo

import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Küçük şekilde "dolgu hesabı yavaş" notu çıkmıyor (#180), UÇTAN UCA.
 *
 * Kaydın betiği: `boyamaRenginiKur(rastgeleRenk); yinele(4) { ileri(100); sağ(90) }`. Gerçek
 * kaplumbağa ve gerçek `Turtle.üçgenleriÇiz` kancası; saat sahte ve HER çağrıda 100 ms ilerliyor,
 * yani üçgenleme ne kadar sürerse sürsün "bütçeyi aştı" der. Sınanan şey sürenin büyüklüğü
 * değil, TABANIN (`ÜçgenlemeUyarısı.enAzNokta`) kancanın önünde durması.
 *
 * Karşıt sınama `UcgenlemeKancaTest`: aynı sahte saatle 120 kenarlı çokgen not DÜŞÜRÜYOR; yani
 * buradaki sessizlik kancanın bağlı OLMAMASINDAN değil, tabandan.
 *
 * Ayrı sınıf: `UcgenlemeKancaTest` tek sınamalık ve sonunda dünyayı kapatıyor; paylaşılan dünya
 * ikinci sınamada kapalı olurdu.
 */
class UcgenlemeKucukSekilTest extends AsyncFunSuite with Matchers {
  import kojo.syntax.Builtins
  implicit val kojoWorld: TestKojoWorld = new TestKojoWorld()
  val builtins = new Builtins()
  import builtins._
  import builtins.Color._
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def panelKur(): HTMLElement = {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
    d
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  test("4 kenarlı kare (5 nokta): saat 100 ms/çağrı olsa da panele not düşmüyor") {
    panelKur()
    var tik = 0.0
    kojoWorld.üçgenlemeRaporu.saat = () => { tik += 100.0; tik }
    val p = PictureT { t =>
      import t._
      invisible(); setAnimationDelay(0); setFillColor(blue)
      var i = 0
      while (i < 4) { forward(100); right(90); i += 1 }
    }
    p.draw()
    for (_ <- p.ready) yield {
      kojoWorld.boyalarıBoşalt()
      val metin = panelMetni
      val düşen = kojoWorld.üçgenlemeRaporu.düşenNotSayısı
      kojoWorld.kapat()
      withClue(s"panel: '$metin' -- ") {
        düşen shouldBe 0
        metin should not include "nokta"
      }
    }
  }
}
