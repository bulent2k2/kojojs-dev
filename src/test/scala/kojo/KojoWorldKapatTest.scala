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
 * `kapat()`tan ÖNCE zamanlanmış rAF geri çağrıları kullanıcının fn'ini
 * kapanmış dünyada koşturmuyor mu? (#149, core #44 incelemesi §1.)
 *
 * `setup` ve `animateHelper` kareyi kurup dönüyor; `kapat` o kareyi iptal
 * etmiyor. Koruma geri çağrının SONUNDA olsaydı (animateHelper'da öyleydi) ya
 * da hiç olmasaydı (setup'ta öyleydi), kare gelince fn bir kez daha koşardı.
 * Mutasyon: iki korumadan birini sökünce ilgili sav kırmızı.
 *
 * WebGL yoksa İPTAL (SolukTest ile aynı gerekçe).
 */
class KojoWorldKapatTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private var sonDünya: Option[KojoWorld] = None

  override def afterAll(): Unit = {
    sonDünya.foreach(_.kapat())
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
  }

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      sonDünya.foreach(_.kapat())
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"
      kap.style.width = "400px"
      kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"
      kap.appendChild(tuval)
      document.body.appendChild(kap)
      val w = new KojoWorldImpl()
      sonDünya = Some(w)
      w
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def bekle(ms: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    window.setTimeout(() => söz.success(()), ms)
    söz.future
  }

  test("#149: kapat()tan önce kurulan setup karesi fn'i koşturmuyor") {
    val w = dünyaKurYaDaİptal()
    // Önce koruma olmadan da koşacağını gör: setup fn'i bir karede çağırıyor.
    var açıkta = 0
    w.setup { açıkta += 1 }
    bekle(200).flatMap { _ =>
      var kapalıda = 0
      w.setup { kapalıda += 1 }
      w.kapat()
      bekle(200).map { _ =>
        withClue("düzenek: açık dünyada setup fn'i koşmalı -- ") { açıkta shouldBe 1 }
        kapalıda shouldBe 0
      }
    }
  }

  test("#149: kapat()tan önce kurulan canlandırma karesi fn'i koşturmuyor") {
    val w = dünyaKurYaDaİptal()
    var açıkta = 0
    w.animate { açıkta += 1 }
    bekle(200).flatMap { _ =>
      w.stopAnimation()
      bekle(50).flatMap { _ =>
        var kapalıda = 0
        w.animate { kapalıda += 1 }
        w.kapat()
        bekle(200).map { _ =>
          withClue("düzenek: açık dünyada canlandırma koşmalı -- ") { açıkta should be > 0 }
          kapalıda shouldBe 0
        }
      }
    }
  }
}
