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
import org.scalajs.dom.raw.{HTMLElement, MessageEvent}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}

/**
 * Tuş dinleyen program, editörden çerçeveye odak istiyor mu (#168)?
 *
 * Çerçeve opak kökende (kojojs-editor#45); odağı kendisi alamıyor, üst
 * pencereye "klavyeOdagi" iletisini yolluyor ve editör odak veriyor. Sınama
 * sayfası çerçevede değil, o yüzden ileti `KojoWorld.üstPencereyeYaz` kancası
 * değiştirilerek yakalanıyor.
 *
 * İki yönlü: tuş kullanan program iletiyi BİR kez yolluyor (`isKeyPressed` her
 * karede çağrılabiliyor), tuş kullanmayan program HİÇ yollamıyor (odağı kod
 * düzenleyiciden çalmamalı).
 */
class KlavyeOdagiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

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

  /** Kancayı kaydediciyle değiştirip gövdeyi koşar; iletileri döndürür, kancayı geri koyar. */
  private def iletiler(gövde: KojoWorldImpl => Unit): Seq[String] = {
    val eski = KojoWorld.üstPencereyeYaz
    val kayıt = ArrayBuffer.empty[String]
    KojoWorld.üstPencereyeYaz = kayıt += _
    try {
      val w = dünyaKurYaDaİptal()
      try gövde(w) finally w.kapat()
      kayıt.toList
    }
    finally KojoWorld.üstPencereyeYaz = eski
  }

  test("editörle sözleşme: ileti dizgisi \"klavyeOdagi\" (editör aynı dizgiyi elle bekliyor)") {
    KojoWorld.KlavyeOdağıİletisi shouldBe "klavyeOdagi"
  }

  test("yalnız tuşaBasınca (onKeyPress) kullanan program odak istiyor") {
    iletiler { w => w.onKeyPress(_ => ()) } shouldBe Seq(KojoWorld.KlavyeOdağıİletisi)
  }

  test("yalnız tuşBasılıMı (isKeyPressed) kullanan program da odak istiyor") {
    iletiler { w => (1 to 10).foreach(_ => w.isKeyPressed(38)) } shouldBe Seq(KojoWorld.KlavyeOdağıİletisi)
  }

  test("yalnız tuşuBırakınca (onKeyRelease) kullanan program da odak istiyor") {
    iletiler { w => w.onKeyRelease(_ => ()) } shouldBe Seq(KojoWorld.KlavyeOdağıİletisi)
  }

  test("tuvaliEtkinleştir (activateCanvas) de odak istiyor") {
    iletiler { w => new kojo.syntax.Builtins()(w).activateCanvas() } shouldBe Seq(KojoWorld.KlavyeOdağıİletisi)
  }

  test("bütün tuş yolları birlikte ve her karede isKeyPressed: yine tek ileti") {
    iletiler { w =>
      w.onKeyPress(_ => ())
      w.onKeyRelease(_ => ())
      (1 to 100).foreach(_ => w.isKeyPressed(37))
      new kojo.syntax.Builtins()(w).activateCanvas()
    } shouldBe Seq(KojoWorld.KlavyeOdağıİletisi)
  }

  test("varsayılan kanca: çerçevede değilken (üst düzey sayfa) ileti yollamıyor") {
    // Öteki savlar kancayı değiştiriyor; bu, VARSAYILAN gövdeyi koşuyor. Sınama
    // sayfası üst düzeyde (window.parent === window): ileti gitseydi bu
    // pencerenin kendisine düşerdi.
    val gelenler = ArrayBuffer.empty[Any]
    val dinleyici: scala.scalajs.js.Function1[MessageEvent, Unit] = e => gelenler += e.data
    window.addEventListener("message", dinleyici)
    KojoWorld.üstPencereyeYaz("klavyeOdagi-varsayilan-sinama")
    val söz = Promise[Unit]()
    window.setTimeout(() => söz.success(()), 100)
    söz.future.map { _ =>
      window.removeEventListener("message", dinleyici)
      gelenler.filter(_ == "klavyeOdagi-varsayilan-sinama") shouldBe empty
    }
  }

  test("tuş kullanmayan program odak istemiyor (fare, çizim, canlandırma)") {
    iletiler { w =>
      implicit val kw: KojoWorld = w
      val p = new TextPic("merhaba", 20, kojo.doodle.Color.black)
      p.draw()
      p.onMouseClick((_, _) => ())
      w.animate(())
      w.stopAnimation()
    } shouldBe empty
  }

  test("her yeni dünya (her çalıştırma) yeniden odak istiyor") {
    (iletiler { w => w.onKeyPress(_ => ()) } ++ iletiler { w => w.onKeyPress(_ => ()) }) shouldBe
      Seq(KojoWorld.KlavyeOdağıİletisi, KojoWorld.KlavyeOdağıİletisi)
  }
}
