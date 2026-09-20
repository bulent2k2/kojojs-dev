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

  /**
   * Her sınama KENDİ birikimini kullanıyor, çünkü birikim artık küresel değil:
   * bir çizerin o anki şekline ait (aşağıdaki iki sızıntı sınamasına bak).
   */
  private var birikim: ŞekilBirikimi = _

  private def sıfırla(): Unit = {
    ÜçgenlemeUyarısı.hepsiniUnut()
    birikim = new ŞekilBirikimi
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
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
    panelMetni should include("95 ms")
    panelMetni should include("1000 nokta")
  }

  test("bütçenin ALTINDAKİ dolgu susuyor: sıradan ölçekte yanlış alarm yok") {
    sıfırla(); panelKur()
    // #68'in ölçümü: 250 nokta x 7 kat ~8 ms. Bugünkü örneklerin ölçeği bu.
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 8.0, 250, bitti = true)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    panelMetni shouldBe ""
  }

  test("eşik tam bir karelik bütçe: 16.7 ms'nin iki yanı") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 16.0, 400, bitti = true)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 17.0, 400, bitti = true)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
  }

  test("zaman kapısı: canlandırma döngüsü paneli doldurmuyor") {
    sıfırla(); panelKur()
    // Her karede yeniden çizilen ağır bir şekil: 50 kare, kare başına 20 ms.
    var kare = 0
    while (kare < 50) {
      ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
      şimdi += 20.0
      kare += 1
    }
    // 50 kare x 20 ms = 1000 ms, yani 2000 ms'lik kapı bir kez bile açılmadı.
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
  }

  test("kapı süresi geçince yeniden düşüyor: sorun sürüyorsa haber de sürüyor") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    şimdi += ÜçgenlemeUyarısı.enAzAralıkMs + 1
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 2
  }

  test("not okunabilir: ne oldu, neden, ne yapılabilir") {
    val m = ÜçgenlemeUyarısı.metin(95.0, 1000, bitti = true)
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
    val m = ÜçgenlemeUyarısı.metin(17.0, 400, bitti = true)
    withClue(s"not: $m\n") {
      m should include("17 ms sürdü")
      m should include("16.7 ms")
      m should not include "bütçe 17 ms"
    }
  }

  test("aynı şeklin yarım yayınları TEK not veriyor, toplam süreyle") {
    // Gerçek tarayıcı çıktısı (#125): 250 noktalık bir gül için not "146 nokta"
    // diyordu -- kullanıcının betiğinde olmayan bir sayı. Sebebi, şeklin
    // bitmeden birkaç kez yayınlanması. Artık ölçüm şekil başına birikiyor.
    sıfırla(); val panel = panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 20.0, 100, bitti = false)
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 25.0, 180, bitti = false)
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 30.0, 250, bitti = true)
    withClue(s"panel: '$panelMetni' -- ") {
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
      panel.childNodes.length shouldBe 1
      // 20 + 25 = 45 ms, erken eşiği (3 x 16.7 = 50.1) aşmıyor, yani not
      // ancak ÜÇÜNCÜ yayında (şekil bitince) düşüyor -- ve toplamı söylüyor.
      panelMetni should include("75 ms")
      panelMetni should include("250 nokta")
    }
  }

  test("şekil bitmeden de konuşuyor: betiğin SON şekli hiç bitmeyebilir") {
    // 14-agir-dolgu.kojo'nun 1000 noktalık gülü hiç tamamlanmıyor (ardından
    // kalem kalkık taşınma ya da boya değişimi gelmiyor). Yalnız tamamlanmış
    // şekle bakan bir uyarı, uyarılması gereken şekli susturuyordu.
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 30.0, 120, bitti = false)
    withClue("30 ms bütçeyi aşıyor ama erken eşiğin (50.1) altında -- ") {
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    }
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 30.0, 240, bitti = false)
    withClue("toplam 60 ms erken eşiği aştı, şekil bitmese de konuşmalı -- ") {
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
    }
    panelMetni should include("şimdilik 240 nokta")
    panelMetni should include("büyüdükçe artacak")
  }

  test("şekil bitince birikim sıfırlanıyor: sonraki şekil temiz başlıyor") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 16.0, 200, bitti = true) // bütçe altı, sessiz
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 16.0, 200, bitti = true) // birikseydi 32 ms olurdu
    withClue("iki ayrı şeklin süresi TOPLANMAMALI -- ") {
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    }
  }

  test("sil() yarım şeklin birikimini sonraki şekle taşımıyor") {
    sıfırla(); panelKur()
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 30.0, 120, bitti = false)
    birikim.unut() // Turtle.realClear'ın yaptığı
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 30.0, 120, bitti = false)
    withClue("taşınsaydı toplam 60 ms olup erken eşiği aşardı -- ") {
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    }
  }

  test("iki ÇİZERİN süresi toplanmıyor: not sahibi olmayan şekli suçlamıyor") {
    // #130 incelemesinin ölçümü. `KojoWorld.boyalarıBoşalt` tek turda BİRDEN
    // ÇOK çizerin boyasını yayınlıyor; birikim küresel olduğu sürece iki ayrı
    // `Resim{}`in süresi aynı kovaya akıyordu. Ölçülen: iki resim, her biri
    // 30 ms, panele "şu ana dek 60 ms" düşüyordu -- oysa hiçbir şekil 60 ms
    // harcamamıştı.
    sıfırla(); panelKur()
    val a = new ŞekilBirikimi
    val b = new ŞekilBirikimi
    ÜçgenlemeUyarısı.üçgenlemeBitti(a, 30.0, 120, bitti = false)
    ÜçgenlemeUyarısı.üçgenlemeBitti(b, 30.0, 120, bitti = false)
    withClue(s"panel: '$panelMetni' -- ") {
      // Küresel birikimde toplam 60 ms erken eşiği (50.1) aşıp not düşerdi.
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
    }
  }

  test("ucuz ve BİTMİŞ bir şekle başkasının süresi fatura edilmiyor") {
    // Aynı kusurun daha kötü biçimi (#130 incelemesi): orada sayı yanlıştı ama
    // ŞEKİL doğruydu; burada ikisi de yanlış. Ağır bir şekil daha bitmeden,
    // araya giren 4 noktalık bitmiş bir kare onun birikimini üstleniyor ve
    // kullanıcıya O KARENİN nokta sayısını yarıya indirmesi öğütleniyordu.
    sıfırla(); panelKur()
    val ağır = new ŞekilBirikimi
    val kare = new ŞekilBirikimi
    ÜçgenlemeUyarısı.üçgenlemeBitti(ağır, 45.0, 800, bitti = false) // erken eşiğin altında
    ÜçgenlemeUyarısı.üçgenlemeBitti(kare, 2.0, 4, bitti = true)
    withClue(s"panel: '$panelMetni' -- ") {
      // Küresel birikimde 45 + 2 = 47 ms > 16.7 ve bitti=true, yani panele
      // "47 ms sürdü (4 nokta)" düşerdi.
      ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
      panelMetni should not include "4 nokta"
    }
  }

  test("panel yoksa çökmüyor: konsola düşüyor") {
    sıfırla() // panel kurulmadı
    ÜçgenlemeUyarısı.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 1
  }
}
