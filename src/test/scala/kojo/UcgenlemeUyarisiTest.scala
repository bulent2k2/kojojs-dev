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
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Ağır dolgu hesabı kullanıcıya bildiriliyor mu (#68).
 *
 * Kayıt "eşik nerede" sorusunu cevapsız bırakmıştı. Karar: DAVRANIŞI
 * değiştirmiyoruz (dolgu yine hesaplanıyor), SESSİZLİĞİ düzeltiyoruz.
 *
 * Buradaki asıl risk YANLIŞ ALARM: not düşen her yerde öğrenci onu okuyacak,
 * ve #68'in ölçümü sıradan ölçekte (250x7 ~8 ms) dolgunun ucuz olduğunu
 * söylüyor. O yüzden savların yarısı notun SUSMASI üstüne.
 *
 * Saat sahte: eşik ve zaman kapısı duvar saatine bağlı olsaydı sınamalar
 * makinenin hızına göre değişirdi -- DuraklamaUyarisiTest'teki aynı sebep.
 */
class UcgenlemeUyarisiTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private var şimdi = 0.0

  /**
   * Sahte saat KÜRESEL: geri vermezsek takım bitince `ÜçgenlemeUyarısı.saat`
   * ölmüş bir takım nesnesinin son `şimdi` değerinde DONMUŞ kalıyor, ve o saat
   * artık sıcak yolda (`Turtle.üçgenleriÇiz` her dolguda iki kez çağırıyor):
   * `saat() - t0` hep 0 -> sonraki takımlarda uyarı bir daha hiç tetiklenmez.
   *
   * Bugün yeşil olması yalnız takım sırasına (K < U) bağlıydı, ve ScalaTest
   * takım sırası bir sözleşme değil: sıra tersine dönseydi UcgenlemeKancaTest
   * kendi `gerçekSaat`ini SIZMIŞ sahte saatten okur ve sonunda onu "gerçek
   * saat" diye geri verirdi -- yani oradaki özenli geri verme sızıntıyı
   * aklardı. (#124 incelemesi, §1.)
   */
  private val gerçekSaat = ÜçgenlemeUyarısı.saat

  override def afterAll(): Unit = {
    ÜçgenlemeUyarısı.saat = gerçekSaat
    ÜçgenlemeUyarısı.hepsiniUnut()
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
  }

  private def sıfırla(): Unit = {
    ÜçgenlemeUyarısı.hepsiniUnut()
    şimdi = 0.0
    ÜçgenlemeUyarısı.saat = () => şimdi
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
  }

  private def panelKur(): HTMLElement = {
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
    d
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  test("bütçeyi AŞAN dolgu not düşürüyor") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(95.0, 1000)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
    panelMetni should include("95 ms")
    panelMetni should include("1000 nokta")
  }

  test("bütçenin ALTINDAKİ dolgu susuyor: sıradan ölçekte yanlış alarm yok") {
    sıfırla(); panelKur()
    // #68'in ölçümü: 250 nokta x 7 kat ~8 ms. Bugünkü örneklerin ölçeği bu.
    ÜçgenlemeUyarısı.üçgenlemeBitti(8.0, 250)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    panelMetni shouldBe ""
  }

  test("eşik tam bir karelik bütçe: 16.7 ms'nin iki yanı") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(16.0, 400)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    ÜçgenlemeUyarısı.üçgenlemeBitti(17.0, 400)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
  }

  test("zaman kapısı: canlandırma döngüsü paneli doldurmuyor") {
    sıfırla(); panelKur()
    // Her karede yeniden çizilen ağır bir şekil: 50 kare, kare başına 20 ms.
    var kare = 0
    while (kare < 50) {
      ÜçgenlemeUyarısı.üçgenlemeBitti(95.0, 1000)
      şimdi += 20.0
      kare += 1
    }
    // 50 kare x 20 ms = 1000 ms, yani 2000 ms'lik kapı bir kez bile açılmadı.
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
  }

  test("kapı süresi geçince yeniden düşüyor: sorun sürüyorsa haber de sürüyor") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(95.0, 1000)
    şimdi += ÜçgenlemeUyarısı.enAzAralıkMs + 1
    ÜçgenlemeUyarısı.üçgenlemeBitti(95.0, 1000)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 2
  }

  test("not okunabilir: ne oldu, neden, ne yapılabilir") {
    val m = ÜçgenlemeUyarısı.metin(95.0, 1000)
    withClue(s"not: $m\n") {
      m should include("95 ms")            // NE oldu -- ölçülmüş sayı
      m should include("1000 nokta")
      m should include("16.7 ms")          // neye göre ağır
      m should include("karesel")          // NEDEN
      m should include("boyamaRenginiKur") // NE yapılabilir -- gerçek komut adı
    }
  }

  test("eşiğin hemen üstünde not kendi gerekçesini yalanlamıyor") {
    // `bütçeMs.round` 17 basarken süresi 17.0 olan bir dolgu için not
    // "17 ms sürdü -- bir karelik bütçe 17 ms" diye okunuyordu: eşik
    // aşıldığı için düşmüş bir not, bütçede kalındığını söylüyordu.
    // Ölçekte dar bir bant ama YAVAŞ MAKİNELERİN bandı (#124 incelemesi, §2).
    val m = ÜçgenlemeUyarısı.metin(17.0, 400)
    withClue(s"not: $m\n") {
      m should include("17 ms sürdü")
      m should include("16.7 ms")
      m should not include "bütçe 17 ms"
    }
  }

  test("panel yoksa çökmüyor: konsola düşüyor") {
    sıfırla() // panel kurulmadı
    ÜçgenlemeUyarısı.üçgenlemeBitti(95.0, 1000)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
  }
}
