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

import org.scalajs.dom
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.timers.setTimeout

/**
 * #213: `canlandır` içinde `ilerle(x, y)` (= Turtle.moveTo) çizmiyordu.
 *
 * NEDEN: `realMoveTo` iç içe bir kuyruk açıp (pushQ) içine yön, ileri ve PopQ
 * koyuyordu. Varsayılan hızda ileri adım rAF ile yüzlerce ms sürüyor; o sırada
 * `canlandır` her karede yeni bir moveTo ekliyor ve komut EN İÇTEKİ kuyruğa,
 * PopQ'nun ARKASINA düşüyordu. PopQ çalışınca `realPopQ`daki
 * `assert(commandQ.size == 0)` patlıyor, istisna queueHandler'ın içinde olduğu için
 * komut pompası KALICI ölüyordu. Canlıda ölçüldü (2026-10-16): konsolda
 * `AssertionError: assertion failed at realPopQ`, ve `konumuOku` geri çağrıları
 * (komut pompası yaşıyorsa gelir) ilk andan beri hiç gelmedi.
 *
 * DÜZELTME: pushQ..popQ penceresinin DIŞINDA gelen komutlar taban kuyruğa yazılıyor.
 *
 * MUTASYON: `Turtle.sıraya`da `commandQs.last`ı `commandQs.head` yap (eski davranış)
 * -> bu iki sav "zaman aşımı"yla düşer.
 *
 * Gerçek `KojoWorldImpl` ile (rAF ve pompa gerçek olmalı). WebGL yoksa İPTAL.
 */
class MoveToYarisiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def zamanAşımı(ms: Double): Future[String] = {
    val söz = Promise[String]()
    setTimeout(ms)(söz.success("zaman aşımı"))
    söz.future
  }

  /**
   * Bir moveTo SÜRERKEN (ileri adım rAF'ta) her karede yeni bir moveTo ekler,
   * sonra bir sync koyar. Sync'e ulaşılırsa "bitti", pompa ölürse "zaman aşımı".
   */
  private def karedeMoveToEkle(gecikme: Long): Future[(String, Double, Seq[String])] = {
    val w = dünyaKurYaDaİptal()
    implicit val iw: KojoWorldImpl = w
    val hatalar = mutable.Buffer.empty[String]
    val dinleyici: scala.scalajs.js.Function1[dom.ErrorEvent, Unit] =
      (e: dom.ErrorEvent) => hatalar += e.message
    window.addEventListener("error", dinleyici)

    val t = new Turtle(0, 0)
    t.invisible()
    t.setAnimationDelay(gecikme) // 100 adım = gecikme ms; kareler ileri adımın içine düşsün
    val bitti = Promise[String]()
    t.moveTo(60, 0) // uzun bir hareket; sürerken aşağıdaki kareler komut ekliyor

    var kare = 0
    def adım(): Unit = {
      kare += 1
      t.moveTo(60 + kare, 0) // son hedef: 60 + 30 = 90
      if (kare < 30) window.requestAnimationFrame((_: Double) => adım())
      else t.sync { () => bitti.success("bitti") }
    }
    window.requestAnimationFrame((_: Double) => adım())

    Future.firstCompletedOf(Seq(bitti.future, zamanAşımı(8000))).map { sonuç =>
      window.removeEventListener("error", dinleyici)
      (sonuç, t.position.x, hatalar.toList)
    }
  }

  test("#213: sürerken eklenen moveTo'lar kuyruğu öldürmüyor (varsayılan hıza yakın gecikme)") {
    karedeMoveToEkle(400).map { case (sonuç, x, hatalar) =>
      withClue(s"hatalar=$hatalar -- ") {
        sonuç shouldBe "bitti"
        x shouldBe 90.0 +- 0.5 // sıra korunuyor: sonuncu hedefe varıldı
        hatalar.filter(_.contains("assertion failed")) shouldBe empty
      }
    }
  }

  test("#213: gecikme 0 (anında) ile de aynı sonuç") {
    karedeMoveToEkle(0).map { case (sonuç, x, hatalar) =>
      withClue(s"hatalar=$hatalar -- ") {
        sonuç shouldBe "bitti"
        x shouldBe 90.0 +- 0.5
      }
    }
  }

  test("#213: sıra korunuyor -- iç kuyruk bitince dışarıdan gelen komut sırasını alıyor") {
    val w = dünyaKurYaDaİptal()
    implicit val iw: KojoWorldImpl = w
    val t = new Turtle(0, 0)
    t.invisible()
    t.setAnimationDelay(200)
    val sıra = mutable.Buffer.empty[String]
    val bitti = Promise[String]()
    t.moveTo(40, 0)
    t.sync { () => sıra += "a" }
    // moveTo sürerken (bir sonraki karede) iki komut daha
    window.requestAnimationFrame { (_: Double) =>
      t.sync { () => sıra += "b" }
      t.moveTo(40, 40)
      t.sync { () => sıra += "c"; bitti.success("bitti") }
    }
    Future.firstCompletedOf(Seq(bitti.future, zamanAşımı(8000))).map { sonuç =>
      withClue(s"sıra=$sıra -- ") {
        sonuç shouldBe "bitti"
        sıra.toList shouldBe List("a", "b", "c")
        t.position.y shouldBe 40.0 +- 0.5
      }
    }
  }
}
