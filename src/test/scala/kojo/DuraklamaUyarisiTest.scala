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

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement

/**
 * Sorun #73: `durakla` resim çizimini geciktirmiyor, ve bunu kimse söylemiyor.
 *
 * Davranış bilerek böyle (tarayıcıda ana iş parçacığı bloklanamaz); düzeltilen
 * şey SESSİZLİĞİ. Buradaki savlar notun DOĞRU BETİKTE düştüğünü çiviliyor --
 * asıl risk yanlış alarm, çünkü not düşen her yerde öğrenci onu okuyacak.
 *
 * Ölçüt üç gerçek masaüstü betiğine göre ayarlandı:
 *   angles.kojo, robosim/robot.kojo   resim + durakla   -> not düşmeli
 *   sprite-animation.kojo             durakla, resim YOK -> susmalı
 *     (orada konumuDeğiştir/giysiyiBüyült/birsonrakiGiysi kuyruğa giriyor,
 *      yani durakla gerçekten çalışıyor)
 */
class DuraklamaUyarisiTest extends AsyncFunSuite with Matchers {
  implicit val kojoWorld: TestKojoWorld = new TestKojoWorld()
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   * Her sınama temiz başlasın: nesne küresel durum tutuyor. Saat de sahte --
   * zaman kapısı (bkz. enAzAralıkMs) duvar saatine bağlı olsaydı sınamalar
   * makinenin hızına göre değişirdi.
   */
  private var şimdi = 0.0
  private def sıfırla(): Unit = {
    DuraklamaUyarısı.hepsiniUnut()
    şimdi = 0.0
    DuraklamaUyarısı.saat = () => şimdi
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
  }

  private def panelKur(): HTMLElement = {
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
    d
  }

  private def resim() = TurtlePicture { t => t.setAnimationDelay(0); t.forward(10) }

  test("resim + durakla: not düşüyor") {
    sıfırla()
    val panel = panelKur()
    val k = new Turtle(0, 0)
    resim().draw()
    k.pause(0.01)
    DuraklamaUyarısı.düşenNotSayısı shouldBe 1
    withClue("not çıktı panelinde görünmeli -- ") {
      panel.textContent should include("durakla resim çizimini geciktirmez")
    }
  }

  test("sıra önemli değil: önce durakla, sonra resim de sayılıyor") {
    sıfırla()
    panelKur()
    val k = new Turtle(0, 0)
    k.pause(0.01)
    withClue("tek başına durakla yetmemeli -- ") {
      DuraklamaUyarısı.düşenNotSayısı shouldBe 0
    }
    resim().draw()
    DuraklamaUyarısı.düşenNotSayısı shouldBe 1
  }

  test("yalnız resim: susuyor") {
    sıfırla()
    panelKur()
    resim().draw()
    resim().draw()
    DuraklamaUyarısı.düşenNotSayısı shouldBe 0
  }

  test("yalnız durakla (sprite-animation.kojo kalıbı): susuyor") {
    sıfırla()
    panelKur()
    val k = new Turtle(0, 0)
    k.pause(0.01)
    k.pause(0.01)
    DuraklamaUyarısı.düşenNotSayısı shouldBe 0
  }

  test("Resim{} gövdesindeki durakla sayılmıyor: orada gerçekten geciktiriyor") {
    sıfırla()
    panelKur()
    // TurtlePicture'ın kendi kaplumbağası forPic=true ile kuruluyor.
    val p = TurtlePicture { t =>
      t.setAnimationDelay(0)
      t.pause(0.01)
      t.forward(10)
    }
    p.draw()
    withClue("resim çizildi ama duraklama forPic kaplumbağasınındı -- ") {
      DuraklamaUyarısı.düşenNotSayısı shouldBe 0
    }
  }

  test("not bir kez düşüyor, her çiz'de değil") {
    sıfırla()
    val panel = panelKur()
    val k = new Turtle(0, 0)
    k.pause(0.01)
    resim().draw()
    resim().draw()
    k.pause(0.01)
    resim().draw()
    panel.childNodes.length shouldBe 1
  }

  test("sil() yeni koşum sayılıyor: not yeniden düşebiliyor") {
    sıfırla()
    val panel = panelKur()
    val k = new Turtle(0, 0)
    k.pause(0.01)
    resim().draw()
    DuraklamaUyarısı.düşenNotSayısı shouldBe 1
    k.clear() // silVeSakla'nın eşzamanlı kolu
    şimdi += DuraklamaUyarısı.enAzAralıkMs // gerçek yeniden koşum: kullanıcı Çalıştır'a bastı
    k.pause(0.01)
    resim().draw()
    withClue("yeni koşumda not yeniden düşmeli -- ") {
      DuraklamaUyarısı.düşenNotSayısı shouldBe 2
    }
    panel.childNodes.length shouldBe 2
  }

  test("döngü içinde sil(): not TEK satır kalıyor (zaman kapısı)") {
    // #98 incelemesinin bulgusu. Bu kalıp rastgele değil -- durakla ile adım
    // adım ilerlemeye çalışan öğrencinin yazacağı ilk şey bu, yani gürültü tam
    // notun hedef kitlesine düşüyordu. Kapıdan önce: 20 satır.
    sıfırla()
    val panel = panelKur()
    val k = new Turtle(0, 0)
    var i = 0
    while (i < 20) {
      k.clear()
      resim().draw()
      k.pause(0.1)
      şimdi += 1.0 // döngü gövdesi tek eşzamanlı blok: turlar arası milisaniyeler
      i += 1
    }
    withClue("20 yinelemede tek not olmalı -- ") {
      DuraklamaUyarısı.düşenNotSayısı shouldBe 1
      panel.childNodes.length shouldBe 1
    }
  }

  test("kapı süresi geçince yeniden düşüyor: gerçek yeniden koşum susturulmuyor") {
    sıfırla()
    val panel = panelKur()
    val k = new Turtle(0, 0)
    k.pause(0.01); resim().draw()
    k.clear()
    şimdi += DuraklamaUyarısı.enAzAralıkMs - 1 // kılpayı içeride: susmalı
    k.pause(0.01); resim().draw()
    withClue("kapı süresi dolmadan susmalı -- ") {
      panel.childNodes.length shouldBe 1
    }
    k.clear()
    şimdi += 1 // tam sınırda: düşmeli
    k.pause(0.01); resim().draw()
    panel.childNodes.length shouldBe 2
  }
}
