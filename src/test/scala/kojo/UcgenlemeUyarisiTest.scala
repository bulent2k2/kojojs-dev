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
   * Rapor durumu DÜNYA BAŞINA (#149): her sınama kendi `ÜçgenlemeRaporu`nu
   * kuruyor, sahte saat o nesnede kalıyor. Eskiden saat küreseldi ve bu
   * takım onu `afterAll`da geri vermek zorundaydı -- geri vermezse ölmüş bir
   * takım nesnesinin donmuş saati sonraki takımlarda uyarıyı susturuyordu
   * (#124 incelemesi §1). Artık geri verilecek bir şey yok; yalnız panel
   * (DOM'da tek) temizleniyor.
   */
  override def afterAll(): Unit =
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))

  /**
   * Her sınama KENDİ birikimini kullanıyor, çünkü birikim artık küresel değil:
   * bir çizerin o anki şekline ait (aşağıdaki iki sızıntı sınamasına bak).
   */
  private var birikim: ŞekilBirikimi = _
  private var rapor: ÜçgenlemeRaporu = _

  private def sıfırla(): Unit = {
    rapor = new ÜçgenlemeRaporu()
    birikim = new ŞekilBirikimi
    şimdi = 0.0
    rapor.saat = () => şimdi
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
    rapor.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    rapor.düşenNotSayısı shouldBe 1
    panelMetni should include("95 ms")
    panelMetni should include("1000 nokta")
  }

  test("bütçenin ALTINDAKİ dolgu susuyor: sıradan ölçekte yanlış alarm yok") {
    sıfırla(); panelKur()
    // #68'in ölçümü: 250 nokta x 7 kat ~8 ms. Bugünkü örneklerin ölçeği bu.
    rapor.üçgenlemeBitti(birikim, 8.0, 250, bitti = true)
    rapor.düşenNotSayısı shouldBe 0
    panelMetni shouldBe ""
  }

  test("eşik tam bir karelik bütçe: 16.7 ms'nin iki yanı") {
    sıfırla(); panelKur()
    rapor.üçgenlemeBitti(birikim, 16.0, 400, bitti = true)
    rapor.düşenNotSayısı shouldBe 0
    rapor.üçgenlemeBitti(birikim, 17.0, 400, bitti = true)
    rapor.düşenNotSayısı shouldBe 1
  }

  test("zaman kapısı: canlandırma döngüsü paneli doldurmuyor") {
    sıfırla(); panelKur()
    // Her karede yeniden çizilen ağır bir şekil: 50 kare, kare başına 20 ms.
    var kare = 0
    while (kare < 50) {
      rapor.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
      şimdi += 20.0
      kare += 1
    }
    // 50 kare x 20 ms = 1000 ms, yani 2000 ms'lik kapı bir kez bile açılmadı.
    rapor.düşenNotSayısı shouldBe 1
  }

  test("kapı süresi geçince yeniden düşüyor: sorun sürüyorsa haber de sürüyor") {
    sıfırla(); panelKur()
    rapor.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    şimdi += ÜçgenlemeUyarısı.enAzAralıkMs + 1
    rapor.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    rapor.düşenNotSayısı shouldBe 2
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
    rapor.üçgenlemeBitti(birikim, 20.0, 100, bitti = false)
    rapor.üçgenlemeBitti(birikim, 25.0, 180, bitti = false)
    rapor.üçgenlemeBitti(birikim, 30.0, 250, bitti = true)
    withClue(s"panel: '$panelMetni' -- ") {
      rapor.düşenNotSayısı shouldBe 1
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
    rapor.üçgenlemeBitti(birikim, 30.0, 120, bitti = false)
    withClue("30 ms bütçeyi aşıyor ama erken eşiğin (50.1) altında -- ") {
      rapor.düşenNotSayısı shouldBe 0
    }
    rapor.üçgenlemeBitti(birikim, 30.0, 240, bitti = false)
    withClue("toplam 60 ms erken eşiği aştı, şekil bitmese de konuşmalı -- ") {
      rapor.düşenNotSayısı shouldBe 1
    }
    panelMetni should include("şimdilik 240 nokta")
    panelMetni should include("büyüdükçe artacak")
  }

  test("şekil bitince birikim sıfırlanıyor: sonraki şekil temiz başlıyor") {
    sıfırla(); panelKur()
    rapor.üçgenlemeBitti(birikim, 16.0, 200, bitti = true) // bütçe altı, sessiz
    rapor.düşenNotSayısı shouldBe 0
    rapor.üçgenlemeBitti(birikim, 16.0, 200, bitti = true) // birikseydi 32 ms olurdu
    withClue("iki ayrı şeklin süresi TOPLANMAMALI -- ") {
      rapor.düşenNotSayısı shouldBe 0
    }
  }

  test("sil() yarım şeklin birikimini sonraki şekle taşımıyor") {
    sıfırla(); panelKur()
    rapor.üçgenlemeBitti(birikim, 30.0, 120, bitti = false)
    birikim.unut() // Turtle.realClear'ın yaptığı
    rapor.üçgenlemeBitti(birikim, 30.0, 120, bitti = false)
    withClue("taşınsaydı toplam 60 ms olup erken eşiği aşardı -- ") {
      rapor.düşenNotSayısı shouldBe 0
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
    rapor.üçgenlemeBitti(a, 30.0, 120, bitti = false)
    rapor.üçgenlemeBitti(b, 30.0, 120, bitti = false)
    withClue(s"panel: '$panelMetni' -- ") {
      // Küresel birikimde toplam 60 ms erken eşiği (50.1) aşıp not düşerdi.
      rapor.düşenNotSayısı shouldBe 0
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
    rapor.üçgenlemeBitti(ağır, 45.0, 800, bitti = false) // erken eşiğin altında
    rapor.üçgenlemeBitti(kare, 2.0, 4, bitti = true)
    withClue(s"panel: '$panelMetni' -- ") {
      // Küresel birikimde 45 + 2 = 47 ms > 16.7 ve bitti=true, yani panele
      // "47 ms sürdü (4 nokta)" düşerdi.
      rapor.düşenNotSayısı shouldBe 0
      panelMetni should not include "4 nokta"
    }
  }

  test("panel yoksa çökmüyor: konsola düşüyor") {
    sıfırla() // panel kurulmadı
    rapor.üçgenlemeBitti(birikim, 95.0, 1000, bitti = true)
    rapor.düşenNotSayısı shouldBe 1
  }

  // ---- #149: dünya başına rapor durumu ----

  /**
   * #149'un 1. ölçütü: bir dünyanın kirletmesi ötekini etkilemiyor. A'ya
   * sahte saat takılıyor, erken yol kapatılıyor, bir not düşürülüyor ve zaman
   * kapısı kapatılıyor; B'nin saati, erken çarpanı, sayacı ve kapısı el
   * değmemiş. Eskiden bu dört şeyin hepsi `object ÜçgenlemeUyarısı`ndaydı ve
   * aynı sınama tam tersini gösterirdi.
   */
  test("#149: bir dünyanın sahte saati, erken çarpanı, sayacı ve zaman kapısı ötekine ulaşmıyor") {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e)); panelKur()
    val a = new TestKojoWorld(); val b = new TestKojoWorld()
    var t = 0.0
    a.üçgenlemeRaporu.saat = () => t
    a.üçgenlemeRaporu.erkenÇarpan = Double.PositiveInfinity
    a.üçgenlemeRaporu.üçgenlemeBitti(new ŞekilBirikimi, 95.0, 1000, bitti = true)
    a.üçgenlemeRaporu.düşenNotSayısı shouldBe 1
    // B: kirlenmemiş -- kendi saati tarayıcı saati, çarpan varsayılan, sayaç 0,
    // ve A'nın AZ ÖNCE kapattığı zaman kapısı B için açık.
    (b.üçgenlemeRaporu.saat eq ÜçgenlemeUyarısı.tarayıcıSaati) shouldBe true
    b.üçgenlemeRaporu.erkenÇarpan shouldBe ÜçgenlemeUyarısı.varsayılanErkenÇarpan
    b.üçgenlemeRaporu.düşenNotSayısı shouldBe 0
    b.üçgenlemeRaporu.saat = () => t // aynı anda
    b.üçgenlemeRaporu.üçgenlemeBitti(new ŞekilBirikimi, 95.0, 1000, bitti = true)
    b.üçgenlemeRaporu.düşenNotSayısı shouldBe 1 // A'nın kapısı B'yi bekletmedi
    a.üçgenlemeRaporu.düşenNotSayısı shouldBe 1 // B'nin notu A'ya sayılmadı
    a.kapat(); b.kapat()
  }

  /**
   * `kapat()` sonrası dünya panele yazmıyor: kapanmış bir takımın geç kalan
   * işi (giysisi geç yüklenen kaplumbağa, bekleyen boya) paylaşılan panele
   * satır düşmesin -- #148'deki UcgenlemeDilimTest kırmızısının biçimi.
   */
  test("#149: kapat() sonrası rapor susuyor -- kapanmış dünya paylaşılan panele yazmıyor") {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e)); panelKur()
    val w = new TestKojoWorld()
    w.üçgenlemeRaporu.saat = () => 0.0
    w.kapat()
    w.üçgenlemeRaporu.üçgenlemeBitti(new ŞekilBirikimi, 95.0, 1000, bitti = true)
    val durmuş = new ŞekilBirikimi; durmuş.toplamMs = 95.0; durmuş.sonNoktaSayısı = 1000
    w.üçgenlemeRaporu.şekilDurdu(durmuş)
    w.üçgenlemeRaporu.düşenNotSayısı shouldBe 0
    panelMetni shouldBe ""
    w.kapat() // idempotent
  }
}
