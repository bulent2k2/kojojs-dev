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

  /** Her sınama temiz başlasın: nesne küresel durum tutuyor. */
  private def sıfırla(): Unit = {
    DuraklamaUyarısı.unut()
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
    DuraklamaUyarısı.uyarıldıMı shouldBe true
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
      DuraklamaUyarısı.uyarıldıMı shouldBe false
    }
    resim().draw()
    DuraklamaUyarısı.uyarıldıMı shouldBe true
  }

  test("yalnız resim: susuyor") {
    sıfırla()
    panelKur()
    resim().draw()
    resim().draw()
    DuraklamaUyarısı.uyarıldıMı shouldBe false
  }

  test("yalnız durakla (sprite-animation.kojo kalıbı): susuyor") {
    sıfırla()
    panelKur()
    val k = new Turtle(0, 0)
    k.pause(0.01)
    k.pause(0.01)
    DuraklamaUyarısı.uyarıldıMı shouldBe false
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
      DuraklamaUyarısı.uyarıldıMı shouldBe false
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
    DuraklamaUyarısı.uyarıldıMı shouldBe true
    k.clear() // silVeSakla'nın eşzamanlı kolu
    withClue("sil() bayrakları sıfırlamalı -- ") {
      DuraklamaUyarısı.uyarıldıMı shouldBe false
    }
    k.pause(0.01)
    resim().draw()
    DuraklamaUyarısı.uyarıldıMı shouldBe true
    panel.childNodes.length shouldBe 2
  }
}
