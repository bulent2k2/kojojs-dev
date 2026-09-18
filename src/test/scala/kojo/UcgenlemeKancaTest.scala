/*
 * Copyright (C) 2026 Bülent Başaran <bulent2k2@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package kojo

import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Ölçüm kancası `Turtle.üçgenleriÇiz` içinde GERÇEKTEN bağlı mı (#68).
 *
 * NEDEN AYRI BİR SINAMA: `UcgenlemeUyarisiTest`in savlarının hepsi
 * `ÜçgenlemeUyarısı`yı DOĞRUDAN çağırıyor, yani nesnenin doğru çalıştığını
 * gösteriyor ama kancanın kurulu olduğunu göstermiyor -- kancayı söksem
 * oradaki yedi sav da yeşil kalırdı. Burası o boşluğu kapatıyor.
 *
 * NEDEN ASENKRON: kaplumbağa komutları KUYRUĞA giriyor; çizim bitmeden
 * ölçmek boş bir şekil ölçmek demek. İlk yazdığımda senkron bir savdı ve
 * panel boş çıktığı için kırmızı yandı -- kanca bağlıydı, ölçüm erkendi.
 * (Aynı tuzak #114 ve #116'da da çıkmıştı.)
 *
 * NEDEN SAHTE SAAT: gerçek süreye baksaydı sav makinenin hızına bağlı olurdu
 * -- yavaş bir makinede tesadüfen yeşil, hızlıda kırmızı. Her çağrıda 100 ms
 * ilerleyen sayaç, üçgenleme gerçekte ne kadar sürerse sürsün "bütçeyi aştı"
 * demek; sınanan şey sürenin büyüklüğü değil, ÖLÇÜMÜN BAĞLI OLMASI.
 */
class UcgenlemeKancaTest extends AsyncFunSuite with Matchers {
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

  test("kanca bağlı: kaplumbağa dolgusu çizilince not düşüyor") {
    panelKur()
    ÜçgenlemeUyarısı.hepsiniUnut()
    val gerçekSaat = ÜçgenlemeUyarısı.saat
    var tik = 0.0
    ÜçgenlemeUyarısı.saat = () => { tik += 100.0; tik }

    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible(); setAnimationDelay(0); setFillColor(blue)
      kaplumbağa = t
      var i = 0
      while (i < 4) { forward(50); right(90); i += 1 }
    }
    p.draw()
    for (_ <- p.ready) yield {
      kojoWorld.boyalarıBoşalt()
      val not = panelMetni
      // Saati hemen geri ver: küresel durum, sonraki takımlara sızmasın.
      ÜçgenlemeUyarısı.saat = gerçekSaat
      ÜçgenlemeUyarısı.hepsiniUnut()
      withClue(s"panel: '$not' -- ") {
        not should include("nokta")
        not should include("ms")
      }
    }
  }
}
