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
 * ÖLÇÜLDÜ (SwiftShader, kat = 7, ısınma sayılmıyor; gül başına YAYIN dağılımı,
 * ortalama değil -- ortalamanın neyi sakladığı için aşağıdaki `aletiKoştur`):
 * {{{
 *   nokta =  250, 10 gül   5,4,3,3,3,3,3,2,2,2    en az 2   (üç koşu, üçünde de
 *                          3,2,3,3,5,3,2,3,2,4    en az 2    en az 2 -- gövdeyi
 *                          3,3,5,4,2,3,4,4,3,4    en az 2    inceleyenin kendi
 *                                                            koşumu da 2)
 *   nokta = 1000,  5 gül   9,10,13,11,10          en az 9
 *   nokta =    4, 10 gül   0,0,0,0,0,0,0,0,0,0    en az 0
 * }}}
 *
 * Yani ALETİN KENDİ ÖLÇEĞİNDE (250 ve 1000) endişe ISIRMIYOR: bir gül birkaç
 * kareye yayılıyor, EN AZ boyanan gül bile iki kez boyanıyor, sayılan gül ile
 * boyanan gül aynı. Ama mekanizma gerçek -- 4 noktalı gülde on gülün onu da
 * ilk rAF hiç ateşlenmeden bitiyor ve SIFIR kez boyanıyor. Bu yüzden sav sabit
 * bir sayı değil, gül başına EN KÜÇÜK delta: aletin ölçeği değişirse (ya da
 * makine çok hızlanırsa) kırılır ve haber verir.
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

  /**
   * Sayılan güllerin her biri için (o gül boyunca geçen yayın, geçen rAF
   * karesi).
   *
   * TOPLAM DEĞİL, GÜL BAŞINA: ortalama, boyanmamış gülü SAKLAR (#130
   * incelemesi, ikinci tur §3). 10 gül / 22 yayın ortalaması 2.2'dir ama
   * "bir gülde 22, dokuz gülde 0" da aynı ortalamayı verir -- ve o
   * dağılımda savın adı ("her gülün en az bir boyaması var") yalan olurdu.
   * 4 noktalı ölçüm sıfır-yayınlı gül rejiminin gerçek olduğunu gösterdiği
   * için bu kuramsal bir kaygı değil.
   */
  private def aletiKoştur(nokta: Int, ısınma: Int, sayılan: Int): Future[Vector[(Long, Int)]] = {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()

    var kare = 0
    var öncekiKare = 0
    var gül = 0
    var öncekiYayın = 0L
    var deltalar = Vector.empty[(Long, Int)]
    val söz = Promise[Vector[(Long, Int)]]()

    // Aletle YARIŞMAYAN bir kare sayacı: yalnız sayıyor, render istemiyor.
    def kareSay(): Unit = {
      kare += 1
      if (!söz.isCompleted) window.requestAnimationFrame(_ => kareSay())
    }
    window.requestAnimationFrame(_ => kareSay())

    // Örnekteki sürücüyle aynı: her karede en fazla bir gül, ve yalnız
    // önceki `konumuOku`ya vardıysa. Eskiden bir sonraki gül sync geri
    // çağrımının İÇİNDEN başlıyordu; #131'in pompasında bu, gülleri
    // boyanmadan siliyordu (ölçüldü: 0,0,0,1,0,0,0,1,0,0) -- yani bu sav
    // kırmızıya döndü ve alet yeniden tasarlandı. Sürücü `animate` ile:
    // örnekteki `canlandır`ın kendisi.
    var gülBitti = true
    var boyandı = true // iki karelik el sıkışma: bitiş karesi boyar, sonraki başlatır
    def tur(): Unit = {
      gülBitti = false
      t.clear()
      gülÇiz(t, nokta, 7, 140.0)
      t.sync { () =>
        gülBitti = true
        gül += 1
        if (gül > ısınma) deltalar :+= (w.yayınSayısı - öncekiYayın, kare - öncekiKare)
        öncekiYayın = w.yayınSayısı
        öncekiKare = kare
        if (gül >= ısınma + sayılan) { w.stopAnimation(); söz.success(deltalar) }
      }
    }
    // Gül bitiş karesinde değil pompanın devamında bittiyse, aynı karenin
    // `clear`ı onu boyanmadan silerdi -- tam takımda yük altında ölçüldü. O
    // yüzden bir kare beklenip sonrakinde başlatılıyor (örnekteki sürücüyle aynı).
    w.animate { if (gülBitti && !söz.isCompleted) { if (boyandı) { boyandı = false; tur() } else boyandı = true } }
    söz.future
  }

  test("alet boyamayla eşleşiyor: sayılan her gülün en az bir boyaması var (#130 §2)") {
    aletiKoştur(nokta = 250, ısınma = 3, sayılan = 10).map { deltalar =>
      val yayınlar = deltalar.map(_._1)
      val kareler = deltalar.map(_._2)
      val ortalama = yayınlar.sum.toDouble / deltalar.size
      withClue(
        s"gül başına yayın: ${yayınlar.mkString(",")} (en az ${yayınlar.min}, ortalama $ortalama); " +
          s"rAF karesi: ${kareler.mkString(",")} -- "
      ) {
        // Sayılan gül sayısı beklendiği gibi; yoksa aşağıdaki min boş
        // koleksiyon üstünde patlar ya da az örnekle karar verir.
        deltalar.size shouldBe 10
        // ASIL SAV: EN AZ boyanan gül bile en az bir kez boyanıyor. Ortalama
        // DEĞİL -- ortalama boyanmamış gülü saklardı (bkz. aletiKoştur'un
        // notu). Altına düşerse alet tamamlanan güllerin bir kısmını hiç
        // boyatmadan sayıyor demektir; o durumda okunan sayı gül/s değil
        // KUYRUK HIZI olur ve #125'in render kazancını göstermez.
        //
        // Kırılabilir, ölçüldü: nokta = 4'te on gülün onu da ilk rAF
        // ateşlenmeden bitiyor -- her delta 0, sav kırmızı.
        yayınlar.min should be >= 1L
      }
    }
  }
}
