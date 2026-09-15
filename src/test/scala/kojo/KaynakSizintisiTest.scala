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

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * Sorun #91: her karede `resimleriSil()` + yeniden çizim yapan bir `canlandır`
 * döngüsünde PIXI'nin GL sayaçları SINIRSIZ büyüyordu.
 *
 * İki ayrı kusur vardı, ikisi de burada çivileniyor:
 *
 *  1. `erasePictures()` "Turtle Layer" ADLI çocukları atlıyordu -- ama
 *     `Turtle.init` o adı `forPic` kaplumbağalara da veriyor, yani Resim{}
 *     katmanları HİÇ SİLİNMİYORDU. Bu, sızıntıdan ÖNCE bir doğruluk kusuru:
 *     resimler ekranda kalıyordu. Sahnedeki çocuk sayısının kare başına
 *     artması (GL'den ayrı olarak O(N) çizim) onun yan ürünüydü. Ölçüt artık
 *     BakePolicy'de: gerçek kaplumbağanın katmanında "Turtle Icon" çocuğu var.
 *  2. Katmanı sahneden çıkarmak GL kaynağını BIRAKMIYOR: PIXI 5 geometriyi
 *     çizicinin managedGeometries/managedBuffers haritalarında tutuyor ve
 *     oradan yalnız `dispose()` düşürüyor.
 *
 * NEDEN GERÇEK KojoWorldImpl: kusur tam olarak TestKojoWorld'ün saplamadığı
 * yerdeydi (`TestKojoWorld.erasePictures` boş bir saplama) ve sayaçlar çiziciye
 * ait -- çizicisiz ölçülemiyor.
 *
 * NEDEN ÇOK KÖŞELİ ŞEKİL: PIXI 5 küçük geometrileri tek partide topluyor, onlar
 * kendi GL kaynaklarını hiç almıyor. Kusur yalnız parti DIŞI kalan şekillerde
 * (çok köşeli çokgen, çember/yay) görünür oluyor -- 180 kenar bunun için.
 *
 * SINIR: gradyan (`Boya`) dolguların dokusu bu çarenin dışında kalıyor ve
 * doğrusal büyümeye devam ediyor (ölçüldü; master'da da öyle) -- sorun #95.
 * Pişirmede aynı ad karışıklığının ikinci kolu -- sorun #96.
 *
 * Ölçüm (aşağıdaki döngü, 120 kare; geometri/tampon/sahne çocuğu):
 *   düzeltmeden önce : 20. karede 29/58/36 ... 120. karede 229/458/236 (doğrusal)
 *   düzeltmeden sonra: 2/4/2 -- yaşayan iki resim; tavan var, büyüme yok
 *     (ilk karelerde sahne çocuğu 3: varlık yükleme göstergesi de duruyor)
 */
class KaynakSizintisiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   * Her sınama kendi tuvalini kursun: KojoWorldImpl DOM'u id ile arıyor.
   *
   * WebGL'siz bir ortamda (bu üçü de gerçek çiziciye bağlı) sınamalar
   * PATLAMAK yerine İPTAL olsun diye kurulum try içinde: başsız Chrome WebGL'i
   * SwiftShader'la veriyor, ama bunu CI ortamına şart koşmuyoruz.
   */
  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try dünyaKur()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def dünyaKur(): KojoWorldImpl = {
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

  /** (geometri, tampon). WebGL yoksa (tuval çizici) None. */
  private def glSayaçları(w: KojoWorldImpl): Option[(Int, Int)] = {
    val g = w.renderer.asInstanceOf[js.Dynamic].geometry
    if (js.isUndefined(g) || g == null || js.isUndefined(g.managedGeometries)) None
    else
      Some(
        (
          js.Object.keys(g.managedGeometries.asInstanceOf[js.Object]).length,
          js.Object.keys(g.managedBuffers.asInstanceOf[js.Object]).length
        )
      )
  }

  private def çokKöşeliResim()(implicit w: KojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setAnimationDelay(0)
    var i = 0
    while (i < 180) { t.forward(4); t.right(2); i += 1 }
  }

  /** n kare koştur; her karede adım(kareNo). */
  private def kareler(n: Int)(adım: Int => Unit): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def döngü(): Unit = {
      i += 1
      adım(i)
      if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => döngü())
    }
    window.requestAnimationFrame(_ => döngü())
    söz.future
  }

  /** Koşul sağlanana dek kare kare bekle (kaplumbağa init'i varlık yüklemesine bağlı). */
  private def bekle(koşul: => Boolean, enÇokKare: Int = 300): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def bak(): Unit = {
      i += 1
      if (koşul) söz.success(())
      else if (i >= enÇokKare) söz.failure(new RuntimeException(s"koşul $enÇokKare karede sağlanmadı"))
      else window.requestAnimationFrame(_ => bak())
    }
    window.requestAnimationFrame(_ => bak())
    söz.future
  }

  test("canlandır döngüsünde resimleriSil() GL kaynaklarını ve sahneyi sınırlı tutuyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (glSayaçları(w).isEmpty) {
      Future.successful(cancel("WebGL çizici yok; GL sayaçları okunamıyor"))
    }
    else {
      // HER karede ölçüyoruz ve EN BÜYÜKlere bakıyoruz. Tek tek kareye bakmak
      // kırılgan: render rAF'a toplandığı için ara sıra bir ölçüm o karenin
      // render'ından ÖNCE düşüyor ve 0 okunuyor (ölçüldü: 6 ölçümün biri).
      // En büyük, o gürültüden etkilenmiyor ve asıl çivilediğimiz şeyi --
      // "tavan var mı" -- doğrudan söylüyor.
      val ölçümler = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int)]
      val kareSayısı = 121
      kareler(kareSayısı) { i =>
        // Ölçüm kareyi AÇARKEN: bir önceki karenin render'ı bitti, bu karenin
        // silmesi henüz başlamadı -- yani yaşayan içeriğin gerçek maliyeti.
        if (i > 1) {
          val (g, b) = glSayaçları(w).get
          ölçümler += ((g, b, w.stage.children.length))
        }
        w.erasePictures()
        var n = 0
        while (n < 2) { çokKöşeliResim().draw(); n += 1 }
      }.map { _ =>
        val enBüyük = (ölçümler.map(_._1).max, ölçümler.map(_._2).max, ölçümler.map(_._3).max)
        val (ilkYarı, sonYarı) = ölçümler.splitAt(ölçümler.size / 2)
        withClue(s"en büyük (geometri,tampon,sahne çocuğu) = $enBüyük, " +
          s"ilk yarı ${ilkYarı.map(_._1).max} / son yarı ${sonYarı.map(_._1).max} geometri -- ") {
          ölçümler should have size (kareSayısı - 1)
          // Sızıntı varken 120. karede 229/458/236 idi. Tavanlar cömert:
          // çivilediğimiz şey DOĞRUSAL BÜYÜMENİN OLMAMASI, kesin sayı değil.
          enBüyük._1 should be <= 12
          enBüyük._2 should be <= 24
          enBüyük._3 should be <= 6
          // Son yarı ilk yarıdan BÜYÜK olmamalı. Eşitlik istemiyoruz: ilk
          // karelerde varlık yükleme göstergesi (AssetLoader.showLoading) da
          // sahnede duruyor, yani sahne çocuğu 3'ten 2'ye DÜŞÜYOR. Sızıntı
          // olsaydı son yarı ilk yarının iki katı olurdu.
          withClue("son yarı ilk yarıdan büyük olmamalı (büyüme yok) -- ") {
            sonYarı.map(_._1).max should be <= ilkYarı.map(_._1).max
            sonYarı.map(_._2).max should be <= ilkYarı.map(_._2).max
            sonYarı.map(_._3).max should be <= ilkYarı.map(_._3).max
          }
        }
      }
    }
  }

  test("resimleriSil() duran kaplumbağanın katmanına dokunmuyor, resminkini siliyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kaplumbağa = new Turtle(0, 0)
    val resim = çokKöşeliResim()
    resim.draw()
    for {
      _ <- resim.ready
      _ <- bekle(kaplumbağa.turtleLayer.parent != null)
    } yield {
      withClue("kurulum: resim de sahnede olmalı -- ") {
        resim.tnode.parent should not be null
      }
      w.erasePictures()
      withClue("gerçek kaplumbağa sahnede kalmalı -- ") {
        kaplumbağa.turtleLayer.parent should not be null
      }
      withClue("Resim{} katmanı sahneden çıkmalı (#91: ada bakınca çıkmıyordu) -- ") {
        resim.tnode.parent shouldBe null
      }
    }
  }

  test("silinen resim yeniden çizilebiliyor (destroy değil dispose)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val resim = çokKöşeliResim()
    resim.draw()
    for {
      _ <- resim.ready
      _ <- kareler(2)(_ => ())
      öncesi = şekilSayısı(resim)
      _ = w.erasePictures()
      _ = resim.draw()
      _ <- kareler(2)(_ => ())
    } yield {
      withClue("dispose yalnız GL tarafını bırakmalı; şekil verisi durmalı -- ") {
        öncesi should be > 0
        şekilSayısı(resim) shouldBe öncesi
      }
      resim.tnode.parent should not be null
    }
  }

  /** Katmandaki bütün Graphics parçalarının şekil sayısı (CPU tarafı veri). */
  private def şekilSayısı(p: Picture): Int = {
    val kap = p.tnode.asInstanceOf[pixiscalajs.PIXI.Container]
    kap.children.toSeq
      .map(_.asInstanceOf[js.Dynamic])
      .filter(d => js.typeOf(d.finishPoly) == "function")
      .map { d =>
        d.finishPoly()
        d.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].length
      }
      .sum
  }
}
