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
 * GERÇEK render döngüsüyle: boşalmada bekletilen not sahiden geliyor mu
 * (#142 incelemesi §1).
 *
 * NEDEN AYRI BİR SINAMA. `UcgenlemeTamamlamaTest` düzeltmenin yalnız bir
 * yarısını tutuyor -- "bekleyen yayın varsa sus, imi kur; yayın gelince
 * doğru sayıyla konuş". Öteki yarısı bir DEĞİŞMEZ: o yayın gerçekten
 * gelecek. `TestKojoWorld.render()` boş olduğu için orada yayını sınamanın
 * kendisi elle yapıyor, yani değişmez sınanmıyor.
 *
 * Değişmez bugün tutuyor: `boyaKirlendi` -> `render()` -> (renderPending ile
 * tek bir rAF) -> `flushRender` -> `boyalarıBoşalt`. Ama tam da sessizce
 * bozulabilecek cinsten: `render`a ya da `flushRender`a bir kapı eklenirse
 * (sekme görünmezken atla, canlandırma dışında ertele) not artık HİÇ düşmez
 * -- düzeltme öncesindeki bayat sayı bile gelmez -- ve öteki sınamaların
 * hepsi yeşil kalır.
 *
 * KURULUM `ornekler/14-agir-dolgu.kojo`'nun 4. deneyinin küçüğü: şekli çiz,
 * ELLE YAYIN YAPMA, notu bekle. Düzeltme olmadan burada hiç not düşmez:
 * boşalma ilk rAF'tan önce oluyor, yani o anda `toplamMs` sıfır.
 *
 * WebGL yoksa İPTAL (SolukTest ile aynı gerekçe).
 */
class UcgenlemeGercekRenderTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private val gerçekSaat = ÜçgenlemeUyarısı.saat

  override def afterAll(): Unit = {
    ÜçgenlemeUyarısı.saat = gerçekSaat
    ÜçgenlemeUyarısı.hepsiniUnut()
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
  }

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"
      kap.style.width = "400px"
      kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"
      kap.appendChild(tuval)
      document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def panelKur(): Unit = {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
  }

  private def bekle(ms: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    window.setTimeout(() => söz.success(()), ms)
    söz.future
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  /** Her üçgenleme 30 ms: bütçe (16.7) üstü, erken eşik (50.1) altı. */
  private def saatiKur(): Unit = {
    var tik = 0.0
    ÜçgenlemeUyarısı.saat = () => { tik += 30.0; tik }
  }

  /** Not düşene dek bekle; `sınırMs` dolarsa olduğu gibi dön. */
  private def notuBekle(sınırMs: Int): Future[Int] = {
    val söz = Promise[Int]()
    var kalan = sınırMs
    def bak(): Unit =
      if (ÜçgenlemeUyarısı.düşenNotSayısı > 0 || kalan <= 0) söz.success(ÜçgenlemeUyarısı.düşenNotSayısı)
      else { kalan -= 50; window.setTimeout(() => bak(), 50) }
    bak()
    söz.future
  }

  test("GERÇEK rAF: boşalmada bekletilen not sahiden geliyor, ve sayı tam (#142 §1)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    ÜçgenlemeUyarısı.hepsiniUnut()
    panelKur()
    saatiKur()

    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()
    t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 4) { t.forward(60); t.right(90); i += 1 }

    notuBekle(3000).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        panelMetni should include("sürdü") // durmuş şekil: kesin biçim
        panelMetni should include("(5 nokta)") // ve şeklin TAMAMI
      }
    }
  }

  /**
   * STENCİL YOLUNDA NOT YOK (#147 §7 canlı bulgusu): 40 000 noktalı gül
   * stencil'de saniyede 3-4 kez çizilirken not düşüyordu -- tampon kurulumu
   * büyüyen şeklin her yayınında baştan yapılıyor, toplamı 190 ms'ye çıkıyor
   * ve süre ÜçgenlemeUyarısı'na gidiyordu. Not üçgenlemenin bedeli için var;
   * metni ("karesele yakın büyüyor") stencil'de yanlış. Sav: aynı sahte saat
   * (her okuma +30 ms) altında stencil yolu SUSAR, libtess yolu (anahtar
   * kapalı) KONUŞUR -- yani sessizlik saatin bozukluğundan değil, yolun
   * süreyi hiç yazmamasından geliyor.
   */
  test("STENCİL yolunda not düşmüyor; aynı saatle libtess yolu düşürüyor (#147 §7)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (!w.stencilDolgu) cancel("bağlamda stencil tamponu yok")
    ÜçgenlemeUyarısı.hepsiniUnut()
    panelKur()
    saatiKur()

    def gül(): Unit = {
      val t = new Turtle(0, 0)
      t.setAnimationDelay(0)
      t.invisible()
      t.setFillColor(kojo.doodle.Color.blue)
      var i = 0
      while (i < 100) { t.forward(9); t.right(25.2); i += 1 } // 101 nokta > Eşik, kendini kesiyor
    }
    w.stencilDolgu = true
    gül()
    notuBekle(1500).flatMap { stencilNot =>
      val stencilPanel = panelMetni
      ÜçgenlemeUyarısı.hepsiniUnut()
      w.stencilDolgu = false
      gül()
      notuBekle(3000).map { libtessNot =>
        w.stencilDolgu = true
        withClue(s"stencil: $stencilNot not, panel '$stencilPanel'; libtess: $libtessNot not, panel '$panelMetni' -- ") {
          stencilNot shouldBe 0
          stencilPanel shouldBe ""
          libtessNot shouldBe 1 // denetim: aynı saat, aynı şekil, öteki yol konuşuyor
        }
      }
    }
  }

  /**
   * EŞİĞİ GEÇEN ŞEKİL LİBTESS EVRESİNİ UNUTUYOR (#154 incelemesi §2): şekil
   * Eşik'in (64) altındayken libtess'te yayınlanıyor ve süre birikiyor; sonra
   * stencil'e geçiyor. Birikim unutulmazsa kuyruk boşalma yolu (şekilDurdu)
   * onu ESKİ nokta sayısıyla kesin biçimde bildiriyor: "24 ms sürdü (64
   * nokta)", 201 noktalı bir şekil için. Düzenek: yavaş makine benzetimi
   * (DilimMs küçük, pompa kare başına birkaç komut) ki libtess evresi
   * gerçekten birkaç kez yayınlansın; sahte saat okuma başına +1.5 ms ki o
   * evrenin birikimi bütçe (16.7) üstünde, erken eşik (50.1) altında kalsın.
   * Sav: not 0. (`stencilKur`daki `şekilBirikimi.unut()` sökülünce kırmızı --
   * denendi.)
   */
  test("Eşiği geçen şekil libtess evresinin birikimini unutuyor: bayat nokta sayısıyla not yok (#154 §2)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (!w.stencilDolgu) cancel("bağlamda stencil tamponu yok")
    val gerçekDilim = w.DilimMs
    bekle(300).flatMap { _ =>
      ÜçgenlemeUyarısı.hepsiniUnut()
      panelKur()
      var tik = 0.0
      ÜçgenlemeUyarısı.saat = () => { tik += 1.5; tik }
      w.yayınSayısı = 0
      val t = new Turtle(0, 0)
      t.setAnimationDelay(0)
      t.invisible()
      t.setPenThickness(0)
      t.setFillColor(kojo.doodle.Color.blue)
      w.DilimMs = 0.05
      var i = 0
      while (i < 200) { t.forward(9); t.right(25.2); i += 1 } // 201 nokta, kendini kesiyor
      // Kuyruk boşalsın, sonra durma kararı için birkaç kare daha.
      def boşalsın(kalan: Int): Future[Unit] =
        if (t.commandQs.head.size == 0 || kalan <= 0) bekle(400) else bekle(50).flatMap(_ => boşalsın(kalan - 1))
      boşalsın(160).map { _ =>
        w.DilimMs = gerçekDilim
        withClue(s"yayın: ${w.yayınSayısı}, kuyruk: ${t.commandQs.head.size}, panel: '$panelMetni' -- ") {
          t.commandQs.head.size shouldBe 0
          w.yayınSayısı should be > 3L // düzenek: şekil birkaç yayında büyüdü
          ÜçgenlemeUyarısı.düşenNotSayısı shouldBe 0
          panelMetni shouldBe ""
        }
      }
    }.andThen { case _ => w.DilimMs = gerçekDilim }
  }
}
