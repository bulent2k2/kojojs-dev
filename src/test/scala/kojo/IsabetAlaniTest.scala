/*
 * Copyright (C) 2026 Lalit Pant <pant.lalit@gmail.com>
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
 */
package kojo

import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * Dolgusu GÖRÜNMEYEN resimler de tıklanabiliyor mu (#114).
 *
 * NEDEN VAR: bir resmin tıklanabilir olup olmaması dolgusunun GÖRÜNÜRLÜĞÜNE
 * bağlıydı. Dolgu hiç kurulmamışsa ya da saydamsa `fareyeTıklayınca`
 * bağlanıyor, `interactive` doğru kuruluyor, ama resim hiçbir fare olayı
 * almıyordu -- hata sessiz: ne uyarı var ne de görünür bir belirti, yalnız
 * hiçbir şey olmuyor. En yaygın hâli en kötüsüydü: kullanıcı dolgu kurmadıysa
 * (yalnız kalem rengi verdiyse) resim otomatik olarak ölü.
 *
 * ÖLÇÜM PIXI'NİN KENDİ MANTIĞIYLA: renderer.plugins.interaction.hitTest.
 * Elle yazılmış bir taklit yanıltırdı, çünkü sıra önemli -- PIXI önce
 * hitArea'ya, sonra containsPoint'e bakıyor.
 *
 * İKİ TUZAK (ikisine de düşüldü, kayda geçsin):
 *   1. hitTest'e KÖK açıkça verilmeli. Varsayılan kök
 *      renderer._lastObjectRendered ve o ancak bir render'dan sonra doluyor;
 *      vermeden ölçünce SAĞLAM durum da false çıkıyor, yani sonda her şeye
 *      "bozuk" diyor.
 *   2. Kaplumbağa çizimi ASENKRON. Komutlar kuyrukta; çizim bitmeden ölçülürse
 *      şeklin graphicsData'sı BOŞ olur ve "ölü" sonucu çizimin yokluğundan
 *      gelir, dolgudan değil. Onun için savlar kare bekliyor.
 */
class IsabetAlaniTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"
      kap.style.width = "800px"
      kap.style.height = "600px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"
      kap.appendChild(tuval)
      document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def kareler(n: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def adım(): Unit = { i += 1; if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => adım()) }
    window.requestAnimationFrame(_ => adım())
    söz.future
  }

  /** Resmin YERELDE (yx, yy) noktasında PIXI'nin bulduğu hedef bu resim mi? */
  private def isabetAlıyorMu(w: KojoWorldImpl, p: Picture, yx: Double = 0, yy: Double = 0): Boolean = {
    val d = p.tnode.asInstanceOf[js.Dynamic]
    val sahne = d.parent
    w.renderer.asInstanceOf[js.Dynamic].render(sahne) // kök ancak render'dan sonra doluyor
    val küresel = d.toGlobal(js.Dynamic.newInstance(js.Dynamic.global.PIXI.Point)(yx, yy))
    val bulunan = w.renderer.asInstanceOf[js.Dynamic].plugins.interaction.hitTest(küresel, sahne)
    !js.isUndefined(bulunan) && bulunan != null &&
    (bulunan.asInstanceOf[js.Any] eq p.tnode.asInstanceOf[js.Any])
  }

  private val saydam = kojo.doodle.Color(0, 0, 0, 0)

  private def daire(w: KojoWorldImpl, dolgu: Option[kojo.doodle.Color]): Picture = {
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val temel = penColor(kojo.doodle.Color.black) -> Picture.circle(50)
    dolgu.foreach(c => temel.setFillColor(c))
    temel
  }

  test("GÖRÜNÜR dolgulu resim tıklanabiliyor (ön koşul)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = daire(w, Some(kojo.doodle.Color.blue))
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(4).map(_ => isabetAlıyorMu(w, p) shouldBe true)
  }

  test("dolgusu HİÇ KURULMAMIŞ resim de tıklanabiliyor (#114, en yaygın hâl)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = daire(w, None)
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(4).map(_ => isabetAlıyorMu(w, p) shouldBe true)
  }

  test("SAYDAM dolgulu resim de tıklanabiliyor (#114)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = daire(w, Some(saydam))
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(4).map(_ => isabetAlıyorMu(w, p) shouldBe true)
  }

  test("KAPLUMBAĞA çizimli dolgusuz resim de tıklanabiliyor (#114)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val p = Picture {
      val t = TurtlePicture.turtle
      t.invisible(); t.setAnimationDelay(0); t.setPenColor(kojo.doodle.Color.black)
      var i = 0
      while (i < 4) { t.forward(60); t.right(90); i += 1 }
    }
    p.draw(); p.onMouseClick((_, _) => ())
    // kaplumbağa kuyruğu boşalsın: çizim bitmeden ölçmek YANLIŞ "ölü" verir
    // kare (0,0)-(60,60) arasında; (30,30) tam ortası, (-30,-30) dışı
    kareler(12).map { _ =>
      isabetAlıyorMu(w, p, 30, 30) shouldBe true
      isabetAlıyorMu(w, p, -30, -30) shouldBe false
    }
  }

  test("isabet alanı şeklin DIŞINA taşmıyor: kutunun içi, dairenin dışı") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = daire(w, None)
    p.draw(); p.onMouseClick((_, _) => ())
    // (40,40) merkeze 56.6 uzakta: r=50'nin DIŞI ama sınır kutusunun İÇİ.
    // Sınır kutusu isabet alanı olarak kullanılsaydı bu sav kırmızı olurdu.
    kareler(4).map { _ =>
      isabetAlıyorMu(w, p, 40, 40) shouldBe false
      isabetAlıyorMu(w, p, 0, 0) shouldBe true
    }
  }

  test("fare olayı BAĞLANMAMIŞ resme isabet alanı kurulmuyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = daire(w, None)
    p.draw()
    kareler(4).map { _ =>
      val d = p.tnode.asInstanceOf[js.Dynamic]
      (js.isUndefined(d.hitArea) || d.hitArea == null) shouldBe true
    }
  }

  test("ELLE kurulmuş isabet alanı korunuyor (kumanda kolu, #113)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kol = new JoyStick(60)(new kojo.syntax.Builtins()(w))
    kol.draw()
    kareler(4).map { _ =>
      val alan = kol.perimeter.tnode.asInstanceOf[js.Dynamic].hitArea
      // #113'ün koyduğu PIXI.Circle duruyor: yarıçapı okunabiliyor olmalı
      alan.radius.asInstanceOf[Double] shouldBe 60.0
    }
  }

  test("üst üste iki resimde ÜSTTEKİ isabet alıyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val alt = daire(w, None)
    val üst = daire(w, None)
    alt.draw(); üst.draw()
    alt.onMouseClick((_, _) => ()); üst.onMouseClick((_, _) => ())
    kareler(4).map { _ =>
      isabetAlıyorMu(w, üst) shouldBe true
      isabetAlıyorMu(w, alt) shouldBe false // üstteki kapıyor
    }
  }

  /** Sınır kutusunun ortası ve kutunun 10 birim solu: içi isabet, dışı değil. */
  private def ortadaVeDışta(w: KojoWorldImpl, p: Picture): (Boolean, Boolean) = {
    val b = p.tnode.getLocalBounds()
    (isabetAlıyorMu(w, p, b.x + b.width / 2, b.y + b.height / 2), isabetAlıyorMu(w, p, b.x - 10, b.y + b.height / 2))
  }

  // İmge ve yazı resmi Graphics değil SPRITE taşıyor. isabetAlanınıKur'un
  // yoklaması yalnız Graphics geometrisine bakıyordu; sprite'ta geometri yok,
  // yani fareyeTıklayınca bağlanmış bir imge ya da yazı hiç isabet almıyor ve
  // işleyici hiç koşmuyordu (canlıda Resim.imge + fareyeTıklayınca, 2026-09).
  test("İMGE resmi (sprite) tıklanabiliyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val tuval = document.createElement("canvas").asInstanceOf[org.scalajs.dom.html.Canvas]
    tuval.width = 40; tuval.height = 30
    val c = tuval.getContext("2d").asInstanceOf[org.scalajs.dom.CanvasRenderingContext2D]
    c.fillStyle = "red"; c.fillRect(0, 0, 40, 30)
    val p = new ImagePic(tuval.toDataURL("image/png"), None)
    p.draw(); p.onMouseClick((_, _) => ())
    p.ready.flatMap(_ => kareler(4)).map { _ =>
      val b = p.tnode.getLocalBounds()
      withClue(s"imge yüklenmeli (sınır ${b.width}x${b.height}) -- ") { b.width shouldBe 40.0 }
      ortadaVeDışta(w, p) shouldBe ((true, false))
    }
  }

  test("YAZI resmi (sprite) tıklanabiliyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = new TextPic("Merhaba", 30, kojo.doodle.Color.black)
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(4).map(_ => ortadaVeDışta(w, p) shouldBe ((true, false)))
  }
}
