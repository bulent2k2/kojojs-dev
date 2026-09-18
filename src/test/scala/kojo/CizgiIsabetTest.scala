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
 * AÇIK yollar -- çizgiler -- tıklanabiliyor mu (#118).
 *
 * #116 dolgusu görünmeyen KAPALI şekilleri kurtardı; alanı olmayan yollar
 * (Resim.çizgi, Resim.yatayÇizgi, kaplumbağa çizgisi) kapsam dışındaydı ve
 * sessizce ölü kalıyordu: PIXI'nin isabet sınaması yalnız dolguya bakıyor,
 * kalem çizgisini hiç sınamıyor.
 *
 * ŞERİDİN ÖLÇÜSÜ MASAÜSTÜNDEN GELİYOR, uydurulmadı: Piccolo'nun
 * PPath.intersects'i dolgu tutmayınca STROKE'LANMIŞ şekli sınıyor ve
 * PInputManager isabeti PCamera.pick(x, y, 1) ile, yani 1 birimlik payla
 * arıyor. Buradaki şerit: kalem kalınlığının yarısı + 1 birim.
 *
 * AYRICA bu sınama #116'nın bir AŞIRILIĞINI de tutuyor: PIXI açık bir yolun
 * dolgusunu yolu örtük kapatarak kuruyor, yani L biçimli bir çizimin
 * "dolgusu" ÇİZİLMEMİŞ bir kenarla kapanan üçgen. #116 onu tıklanabilir
 * yapıyordu (ölçüldü). Artık görünmez dolgu yalnız KAPALI parçalar için
 * çevriliyor.
 */
class CizgiIsabetTest extends AsyncFunSuite with Matchers {
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

  private def isabetAlıyorMu(w: KojoWorldImpl, p: Picture, yx: Double, yy: Double): Boolean = {
    val d = p.tnode.asInstanceOf[js.Dynamic]
    val sahne = d.parent
    w.renderer.asInstanceOf[js.Dynamic].render(sahne)
    val küresel = d.toGlobal(js.Dynamic.newInstance(js.Dynamic.global.PIXI.Point)(yx, yy))
    val bulunan = w.renderer.asInstanceOf[js.Dynamic].plugins.interaction.hitTest(küresel, sahne)
    !js.isUndefined(bulunan) && bulunan != null &&
    (bulunan.asInstanceOf[js.Any] eq p.tnode.asInstanceOf[js.Any])
  }

  test("Resim.çizgi tıklanabiliyor (#118)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val ç = penColor(kojo.doodle.Color.black) -> Picture.line(120, 0)
    ç.draw(); ç.onMouseClick((_, _) => ())
    kareler(4).map(_ => isabetAlıyorMu(w, ç, 60, 0) shouldBe true)
  }

  test("Resim.yatayÇizgi tıklanabiliyor (#118)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val ç = penColor(kojo.doodle.Color.black) -> Picture.hline(120)
    ç.draw(); ç.onMouseClick((_, _) => ())
    kareler(4).map(_ => isabetAlıyorMu(w, ç, 60, 0) shouldBe true)
  }

  test("KAPLUMBAĞA çizgisi tıklanabiliyor (#118)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val p = Picture {
      val t = TurtlePicture.turtle
      t.invisible(); t.setAnimationDelay(0); t.setPenColor(kojo.doodle.Color.black)
      t.right(90); t.forward(120)
    }
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(12).map(_ => isabetAlıyorMu(w, p, 60, 0) shouldBe true)
  }

  test("şerit çizginin UZAĞINA taşmıyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    // ÇAPRAZ çizgi bilerek seçildi: sınır kutusu 100x100, şerit ise ince bir
    // köşegen. Yatay bir çizgide kutunun kendisi 122x2 olduğu için "uzak"
    // noktalar ucuz eleme tarafından zaten atılıyor ve sav ŞERİDİ değil
    // KUTUYU sınamış olurdu -- mutasyonla yakalandı: şerit 40 kat
    // şişirildiğinde yatay sürüm yeşil kalıyordu.
    val ç = penColor(kojo.doodle.Color.black) -> Picture.line(100, 100)
    ç.draw(); ç.onMouseClick((_, _) => ())
    kareler(4).map { _ =>
      isabetAlıyorMu(w, ç, 50, 50) shouldBe true   // köşegenin üstü
      // 10 birim DİK uzaklık: şerit ~2 birim olduğu için isabet olmamalı.
      // Sınırı gerçekten daraltan sav bu; 56 birimlik uzak noktalar
      // şeridi 40 kat şişirsem bile yeşil kalıyordu (mutasyonla ölçüldü).
      isabetAlıyorMu(w, ç, 57.07, 42.93) shouldBe false
      isabetAlıyorMu(w, ç, 90, 10) shouldBe false  // kutunun İÇİ, şeridin uzağı
      isabetAlıyorMu(w, ç, 10, 90) shouldBe false  // öteki köşe
      isabetAlıyorMu(w, ç, 200, 200) shouldBe false // ucundan ötesi
    }
  }

  test("AÇIK yolun ima ettiği alan tıklanmıyor, çizili kenarı tıklanıyor (#116 aşırılığı)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    // (0,0) -> (100,0) -> (100,100): iki kenar ÇİZİLİ, kapanış kenarı DEĞİL
    val l = Picture {
      val t = TurtlePicture.turtle
      t.invisible(); t.setAnimationDelay(0); t.setPenColor(kojo.doodle.Color.black)
      t.right(90); t.forward(100); t.left(90); t.forward(100)
    }
    l.draw(); l.onMouseClick((_, _) => ())
    kareler(12).map { _ =>
      isabetAlıyorMu(w, l, 50, 0) shouldBe true    // çizili kenarın üstü
      isabetAlıyorMu(w, l, 70, 20) shouldBe false  // ima edilen üçgenin içi: ÇİZİLİ DEĞİL
    }
  }

  test("KAPALI kaplumbağa şeklinin içi hâlâ tıklanıyor (#116 bozulmadı)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val kare = Picture {
      val t = TurtlePicture.turtle
      t.invisible(); t.setAnimationDelay(0); t.setPenColor(kojo.doodle.Color.black)
      var i = 0
      while (i < 4) { t.forward(100); t.right(90); i += 1 }
    }
    kare.draw(); kare.onMouseClick((_, _) => ())
    kareler(12).map(_ => isabetAlıyorMu(w, kare, 50, 50) shouldBe true)
  }

  test("ÖLÇEKLENMİŞ çizgi de tıklanabiliyor: pay ekran biriminde") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val b = new kojo.syntax.Builtins()(w)
    import b._
    val ç = penColor(kojo.doodle.Color.black) -> Picture.line(120, 0)
    ç.draw(); ç.onMouseClick((_, _) => ())
    ç.scale(0.25)
    kareler(4).map(_ => isabetAlıyorMu(w, ç, 60, 0) shouldBe true)
  }
}
