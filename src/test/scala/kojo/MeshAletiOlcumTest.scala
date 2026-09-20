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
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * `ornekler/15-mesh-olcumu.kojo` ALETİNİN kendisinin denetimi (#130 incelemesi
 * §2).
 *
 * Alet `konumuOku` zinciriyle BİTEN GÜLLERİ sayıyor -- doğru sayılan şey. Ama
 * `canlandır`dan çıkmanın ikinci bir sonucu var: sayılan ritim artık boyama
 * ritmine bağlı değil.
 *
 *   KojoWorld.scala:741-751  scheduleLater -> her 100 komutta bir setTimeout(0)
 *   KojoWorld.scala:776-790  render() bir rAF kaydeder; flushRender o karede
 *                            boyalarıBoşalt() + renderer.render(stage) koşar
 *
 * `setTimeout(0)` boyamayı beklemiyor. Bir gül iki rAF ARASINDA tamamlanıp
 * sonraki `sil()` ile kaldırılırsa, üçgenlemesi ödenmiş ama GPU'ya hiç
 * gitmemiş olur. Bu tam da aletin ölçmek için var olduğu yere çarpıyor:
 * #125'in kazancı en çok render tarafında, yani render'ı eksik sayan bir alet
 * mesh'i KENDİ ALEYHİNE ölçer.
 *
 * Bu takım o oranı ölçüyor: aletin gülü başına kaç yayın, kaç kare düşüyor.
 * Yayın sayısı (KojoWorld.yayınSayısı) render'ın vekili -- yalnız flushRender
 * içinden artıyor, yani bir yayın gerçekten bir çizim karesi demek.
 *
 * ÖLÇÜLDÜ (SwiftShader, kat = 7, ısınma sayılmıyor; gül başına):
 * {{{
 *   nokta =  250, 10 gül   ->  3 - 3.4 yayın,   3.3 - 3.7 rAF karesi   (üç koşu)
 *   nokta = 1000,  5 gül   ->  9.6 yayın,      14 rAF karesi
 *   nokta =    4, 10 gül   ->  0 yayın,         0 rAF karesi
 * }}}
 *
 * Yani ALETİN KENDİ ÖLÇEĞİNDE (250 ve 1000) endişe ISIRMIYOR: bir gül birkaç
 * kareye yayılıyor, her gülün payına birden çok boyama düşüyor, sayılan gül ile
 * boyanan gül aynı. Ama mekanizma gerçek -- 4 noktalı gülde on gülün onu da
 * ilk rAF hiç ateşlenmeden bitiyor ve SIFIR kez boyanıyor. Bu yüzden sav bir
 * oran savı, sabit değil: aletin ölçeği değişirse (ya da makine çok hızlanırsa)
 * kırılır ve haber verir.
 */
class MeshAletiOlcumTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try dünyaKur()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
    new KojoWorldImpl()
  }

  /** Örnekteki `gülÇiz`in kitaplık dilindeki aynısı. */
  private def gülÇiz(t: Turtle, nokta: Int, kat: Int, yarıçap: Double): Unit = {
    t.setPenThickness(0)
    t.setFillColor(kojo.doodle.Color.blue)
    val kenar = 2 * yarıçap * math.sin(math.toRadians(kat * 180.0 / nokta))
    val dönüş = kat * 360.0 / nokta
    var i = 0
    while (i < nokta) { t.forward(kenar); t.right(dönüş); i += 1 }
  }

  /** (sayılan gül, o sırada geçen yayın, o sırada geçen rAF karesi) */
  private def aletiKoştur(nokta: Int, ısınma: Int, sayılan: Int): Future[(Int, Long, Int)] = {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()

    var kare = 0
    var kareTaban = 0
    var gül = 0
    var yayınTaban = 0L
    val söz = Promise[(Int, Long, Int)]()

    // Aletle YARIŞMAYAN bir kare sayacı: yalnız sayıyor, render istemiyor.
    def kareSay(): Unit = {
      kare += 1
      if (!söz.isCompleted) window.requestAnimationFrame(_ => kareSay())
    }
    window.requestAnimationFrame(_ => kareSay())

    def tur(): Unit = {
      t.clear()
      gülÇiz(t, nokta, 7, 140.0)
      // Örnekteki `konumuOku { _ => ... }` ile aynı dikiş: kuyruk buraya
      // varınca gül gerçekten bitmiştir.
      t.sync { () =>
        gül += 1
        if (gül == ısınma) { yayınTaban = w.yayınSayısı; kareTaban = kare }
        if (gül >= ısınma + sayılan) söz.success((sayılan, w.yayınSayısı - yayınTaban, kare - kareTaban))
        else tur()
      }
    }
    tur()
    söz.future
  }

  test("alet boyamayla eşleşiyor: sayılan her gülün en az bir boyaması var (#130 §2)") {
    aletiKoştur(nokta = 250, ısınma = 3, sayılan = 10).map {
      case (güller, yayınlar, kareler) =>
        val yayınOranı = yayınlar.toDouble / güller
        val kareOranı = kareler.toDouble / güller
        withClue(
          s"$güller gül -> $yayınlar yayın ($yayınOranı/gül), $kareler rAF karesi ($kareOranı/gül) -- "
        ) {
          // ASIL SAV: gül başına en az bir yayın. Altına düşerse alet
          // tamamlanan güllerin bir kısmını hiç boyatmadan sayıyor demektir --
          // o durumda okunan sayı gül/s değil KUYRUK HIZI olur ve #125'in
          // render kazancını göstermez.
          //
          // Kırılabilir, ölçüldü: nokta = 4'te on gülün onu da ilk rAF
          // ateşlenmeden bitiyor -- oran 0, sav da alt sınır da kırmızı.
          yayınOranı should be >= 1.0
          // Alt sınır sıfır yayının savı boşa düşürmesini engelliyor.
          yayınlar should be > 0L
        }
    }
  }
}
