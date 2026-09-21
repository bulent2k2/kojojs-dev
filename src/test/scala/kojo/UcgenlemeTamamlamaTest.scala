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
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * TAMAMLANMAYAN şekil susuyor, tamamlanan konuşuyor -- GERÇEK çizim yolundan
 * (#133 incelemesi §1).
 *
 * NEDEN AYRI BİR SINAMA. `UcgenlemeUyarisiTest`in "şekil bitmeden de
 * konuşuyor" savı aynı mekanizmayı DOĞRUDAN çağrıyla tutuyor ve yeşildi --
 * ama `14-agir-dolgu.kojo` yine de sessiz kaldı ve bunu ancak örneği gerçek
 * tarayıcıda koşturunca gördük. Aradaki boşluk şu: o sav bir şeklin
 * tamamlanıp tamamlanmadığını PARAMETRE olarak alıyor; burada şekli gerçekten
 * `Turtle` tamamlıyor (ya da tamamlamıyor).
 *
 * SINANAN BANT bilerek 16.7 ile 50.1 arasında: bütçenin üstünde ama erken
 * eşiğin altında. Gerçek gülün ölçülen süresi (35 ms, 251 nokta) tam bu
 * bantta, ve #133'ün gerilemesi tam burada yaşıyordu.
 *
 * SAHTE SAAT çünkü sınanan şey sürenin büyüklüğü değil, tamamlamanın olup
 * olmaması. Saat KÜRESEL, o yüzden `afterAll` geri veriyor -- geri
 * vermemenin bedeli ölçülmüştü (#124 incelemesi §1: sızan sahte saat sonraki
 * takımlarda uyarıyı tümüyle susturuyor).
 */
class UcgenlemeTamamlamaTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private val gerçekSaat = ÜçgenlemeUyarısı.saat

  override def afterAll(): Unit = {
    ÜçgenlemeUyarısı.saat = gerçekSaat
    ÜçgenlemeUyarısı.hepsiniUnut()
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
  }

  private def panelKur(): HTMLElement = {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
    d
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  /** Her çağrıda 30 ms: üçgenleme başına delta 30 -- bütçe üstü, erken eşik altı. */
  private def saatiKur(): Unit = {
    var tik = 0.0
    ÜçgenlemeUyarısı.saat = () => { tik += 30.0; tik }
  }

  private def dünyaKurYaDaİptal(): TestKojoWorld =
    try new TestKojoWorld()
    catch { case t: Throwable => cancel(s"dünya kurulamadı: $t") }

  /**
   * Dolgulu bir kare çizer, tek bir yayın yaptırır, sonra `tamamla` verilmişse
   * şekli tamamlayacak komutları kuyruğa koyar. Dönen sayı düşen not sayısı.
   */
  private def koştur(tamamla: Boolean): Future[Int] = {
    implicit val w: TestKojoWorld = dünyaKurYaDaİptal()
    ÜçgenlemeUyarısı.hepsiniUnut()
    panelKur()
    saatiKur()

    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()
    t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 4) { t.forward(60); t.right(90); i += 1 }

    val söz = Promise[Int]()
    t.sync { () =>
      // Şekil çizildi ama HENÜZ tamamlanmadı: yarım yayın.
      w.boyalarıBoşalt()
      if (!tamamla) söz.success(ÜçgenlemeUyarısı.düşenNotSayısı)
      else {
        // 14-agir-dolgu.kojo'daki düzeltmenin aynısı: kalem kalkık taşınma.
        t.penUp()
        t.setPosition(0, -220)
        t.sync { () => söz.success(ÜçgenlemeUyarısı.düşenNotSayısı) }
      }
    }
    söz.future
  }

  test("TAMAMLANMAYAN şekil susuyor: bütçe üstü ama erken eşik altı (#133)") {
    koştur(tamamla = false).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        // Kusurun kendisi. 30 ms > 16.7 ama < 50.1, ve şekli tamamlayan
        // hiçbir şey gelmiyor -> uyarı hiç çıkmıyor.
        n shouldBe 0
      }
    }
  }

  test("TAMAMLANAN şekil konuşuyor: kalem kalkık taşınma yetiyor (#133)") {
    koştur(tamamla = true).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        panelMetni should include("sürdü") // bitti = true biçimi
      }
    }
  }
}
