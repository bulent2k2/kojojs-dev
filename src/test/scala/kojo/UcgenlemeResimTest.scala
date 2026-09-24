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

import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * `Resim{}` içindeki kaplumbağa için "kuyruk boşaldı" yolu ne yapıyor (#143).
 *
 * #134'ün sözü "betiğin son şekli artık sessiz değil"di; `Resim{}` ile çizen
 * betiklerde o söz ölçülmemişti. Kaydın üç hipotezi vardı, üçü de "not
 * kaçırma" yönünde. Ölçüm (bu sınamalar, düzeltmeden önce koşturuldu):
 *
 *   1. hipotez  DOĞRUYDU: `canlandır` içinde kurulan ağır bir Resim{}
 *               susuyordu -- kapı dünya düzeyindeydi (`komutGelebilir`),
 *               oysa resmin gövdesi bittiğinde şekli de bitmiştir: `çiz` /
 *               `sil` yeniden çizer, nokta eklemez. Çare: forPic çizerde
 *               kapı yok (Turtle.şekilDurmuş).
 *   2. hipotez  YANLIŞTI: boşalma anında yayın BEKLİYOR. Çizer sahnede
 *               olmasa da (`çiz` sonra geliyor) `boyaKirlendi` onu sıraya
 *               alıyor -- "silindi" imi yalnız silme yollarında konuyor,
 *               hiç sahneye konmamış katmanda yok. Not `çiz`den bağımsız
 *               düşüyor; bedel ödendi.
 *   3. hipotez  (yeniden çizim) ayrı bir kusur değil: `bildirildi` ilk
 *               yayında konulmuşsa ikinci yayın susar -- tasarım bu.
 *
 * SAHTE SAAT ve BANT `UcgenlemeTamamlamaTest` ile aynı: 30 ms, bütçe üstü,
 * erken eşik altı -- yani konuşan şey boşalma yolu, erkenÇarpan değil.
 */
class UcgenlemeResimTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   * Rapor durumu dünya başına (#149): sahte saat her sınamanın KENDİ
   * dünyasında kalıyor; geri verilecek küresel saat yok. Önceki dünya
   * yenisi kurulmadan, sonuncusu takım sonunda kapatılıyor.
   */
  private var sonDünya: Option[KojoWorld] = None

  override def afterAll(): Unit = {
    sonDünya.foreach(_.kapat())
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
  }

  private def panelKur(): Unit = {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  private def saatiKur()(implicit w: KojoWorld): Unit = {
    var tik = 0.0
    w.üçgenlemeRaporu.saat = () => { tik += 30.0; tik }
  }

  private def dünyaKurYaDaİptal(): TestKojoWorld =
    try { sonDünya.foreach(_.kapat()); val w = new TestKojoWorld(); sonDünya = Some(w); w }
    catch { case t: Throwable => cancel(s"dünya kurulamadı: $t") }

  /** Dolgulu bir kare çizen Resim{}. Gövde `make()` içinde kuyruğa giriyor. */
  private def kareResmi()(implicit w: TestKojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 4) { t.forward(60); t.right(90); i += 1 }
  }

  /**
   * Resmin kuyruğu boşalana dek bekle, sonra kareyi ver (yayın + durma
   * denetimi) ve düşen not sayısını dön. `sync` make()'in kendi sync'inden
   * ve `görün`den sonra sıraya giriyor; 50 ms sonra kuyruk boş.
   */
  private def boşalınca(w: TestKojoWorld, r: TurtlePicture): Future[Int] = {
    val söz = Promise[Int]()
    r.turtle.sync { () =>
      window.setTimeout(
        () => {
          w.boyalarıBoşalt()
          söz.success(w.üçgenlemeRaporu.düşenNotSayısı)
        },
        50
      )
    }
    söz.future
  }

  test("Resim{} + çiz: betiğin son şekli resimde de konuşuyor, sayı tam (#143)") {
    implicit val w: TestKojoWorld = dünyaKurYaDaİptal()
    panelKur()
    saatiKur()
    val r = kareResmi()
    r.draw()
    boşalınca(w, r).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        panelMetni should include("sürdü")
        panelMetni should include("(5 nokta)")
      }
    }
  }

  test("Resim{} çizilmeden de konuşuyor: bedel çiz'den bağımsız ödendi (#143, 2. hipotez)") {
    implicit val w: TestKojoWorld = dünyaKurYaDaİptal()
    panelKur()
    saatiKur()
    val r = kareResmi()
    boşalınca(w, r).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        panelMetni should include("(5 nokta)")
      }
    }
  }

  /**
   * 1. hipotez: dünya düzeyindeki kapı resmi de susturuyordu. Kaplumbağanın
   * kendisi için kapı doğru (bir sonraki kare nokta ekleyebilir -- bkz.
   * UcgenlemeTamamlamaTest "CANLANDIRMADA"); resim için yanlış.
   */
  test("CANLANDIRMADA kurulan Resim{} yine konuşuyor: gövdesi bitti, kare nokta eklemez (#143, 1. hipotez)") {
    implicit val w: TestKojoWorld = dünyaKurYaDaİptal()
    panelKur()
    saatiKur()
    w.canlandırmaDönüyorMu = true
    val r = kareResmi()
    r.draw()
    boşalınca(w, r).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        panelMetni should include("sürdü")
        panelMetni should not include "şimdilik"
      }
    }
  }
}
