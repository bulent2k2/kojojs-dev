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
import scala.scalajs.js

/**
 * `kapat()`tan ÖNCE zamanlanmış rAF geri çağrıları kullanıcının fn'ini
 * kapanmış dünyada koşturmuyor mu? (#149, core #44 incelemesi §1.)
 *
 * `setup` ve `animateHelper` kareyi kurup dönüyor; `kapat` o kareyi iptal
 * etmiyor. Koruma geri çağrının SONUNDA olsaydı (animateHelper'da öyleydi) ya
 * da hiç olmasaydı (setup'ta öyleydi), kare gelince fn bir kez daha koşardı.
 * Mutasyon: iki korumadan birini sökünce ilgili sav kırmızı.
 *
 * Aynı soru pompa ve `timer` için: kapanmış dünyada kaplumbağaya verilen
 * komut ya da kuyrukta bekleyen komut kaplumbağayı oynatmıyor, `timer`
 * aralık kurmuyor (#165 incelemesi §3 ve §4). Mutasyon: `scheduleLater`,
 * `pompayıSürdür` ve kare teslimindeki `kapandı` kapıları ile `kapat`'taki
 * `bekleyenİşler.clear()` birlikte sökülünce pompa savları kırmızı;
 * `timer`ın kapısı sökülünce `timer` savı kırmızı.
 *
 * #166: kapanmış dünyanın pencere dinleyicileri (kullanıcının tuş
 * işleyicileri, `pressedKeys`, tekerlek) koşmuyor ve `kocoResetView` onu
 * tutmuyor. Çizici bırakılıyor: tuval sayfadan sökülüyor, WebGL bağlamı
 * düşürülüyor, çiziciye dokunan çağrılar patlamıyor, ikinci `kapat()`
 * zararsız. 40 dünya kurup kapatmak açık kalan bir dünyanın bağlamını
 * düşürmüyor (düzeltmeden önce düşürüyordu: tarayıcı en eskiyi atıyor).
 * Kapanmış dünyada sonradan takılan tuş işleyicisi koşmuyor, fare sorguları
 * patlamıyor; pişmiş doku `kapat`'ta bırakılıyor.
 * Mutasyon: dinleyici sökümü, `kocoResetView` silme, bağlam düşürme,
 * `kapat`'ın `kapandı` kapısı, `size` ve artalan kapıları, `penceredeDinle`
 * kapısı, `mouseXY` ve `isAMouseButtonPressed` kapıları, `kapat`'taki
 * `resetBake` -- her biri bir savı kırıyor.
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

  private def dünyaKurYaDaİptal(): KojoWorldImpl = {
    // Önceki dünyayı kapatmak try'ın DIŞINDA: kapat()'ın hatası "WebGL yok"
    // diye iptale dönüşmesin, kırmızı görünsün (#166, ikinci kapat()).
    sonDünya.foreach(_.kapat())
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    try {
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
  }

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

  test("#149: kapanmış dünyada verilen komut kaplumbağayı oynatmıyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    bekle(200).flatMap { _ =>
      t.forward(50) // düzenek: açık dünyada komut kaplumbağayı oynatıyor
      bekle(200).flatMap { _ =>
        val açıkX = t.position.x
        val açıkY = t.position.y
        w.kapat()
        t.forward(100)
        t.right(90)
        t.forward(100)
        bekle(200).map { _ =>
          withClue("düzenek: açık dünyada ileri(50) konumu değiştirmeli -- ") { açıkY should not be 0.0 }
          (t.position.x, t.position.y) shouldBe ((açıkX, açıkY))
        }
      }
    }
  }

  test("#149: kapat()tan önce kuyrukta bekleyen komutlar koşmuyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    bekle(200).flatMap { _ =>
      // Dilim sıfır: pompa ilk komuttan sonra kareye teslim eder, kalanlar kuyrukta bekler.
      w.DilimMs = 0.0
      var i = 0
      while (i < 50) { t.forward(10); t.right(7); i += 1 }
      w.DilimMs = 8.0
      val kuyrukta = t.commandQs.head.size
      val x0 = t.position.x
      val y0 = t.position.y
      w.kapat()
      bekle(300).map { _ =>
        withClue("düzenek: kapat() anında kuyrukta komut olmalı -- ") { kuyrukta should be > 0 }
        (t.position.x, t.position.y) shouldBe ((x0, y0))
      }
    }
  }

  test("#149: kapanmış dünyada timer aralık kurmuyor") {
    val w = dünyaKurYaDaİptal()
    var açıkta = 0
    w.timer(10) { açıkta += 1 }
    bekle(100).flatMap { _ =>
      w.stopAnimation()
      w.kapat()
      var kapalıda = 0
      w.timer(10) { kapalıda += 1 }
      bekle(150).map { _ =>
        withClue("düzenek: açık dünyada timer koşmalı -- ") { açıkta should be > 0 }
        kapalıda shouldBe 0
      }
    }
  }

  private def tuşOlayı(tür: String): org.scalajs.dom.KeyboardEvent =
    js.Dynamic
      .newInstance(js.Dynamic.global.KeyboardEvent)(tür, js.Dynamic.literal(keyCode = 65, bubbles = true))
      .asInstanceOf[org.scalajs.dom.KeyboardEvent]

  test("#166: kapanmış dünyanın pencere dinleyicileri koşmuyor") {
    val w = dünyaKurYaDaİptal()
    var basılan = 0
    var bırakılan = 0
    w.onKeyPress(_ => basılan += 1)
    w.onKeyRelease(_ => bırakılan += 1)
    val aşağı = tuşOlayı("keydown")
    val k = aşağı.keyCode
    window.dispatchEvent(aşağı)
    val açıktaBasılı = w.pressedKeys.contains(k)
    window.dispatchEvent(tuşOlayı("keyup"))
    val açıktaSayılar = (basılan, bırakılan)
    val açıktaSıfırla = !js.isUndefined(window.asInstanceOf[js.Dynamic].kocoResetView)
    w.kapat()
    window.dispatchEvent(tuşOlayı("keydown"))
    window.dispatchEvent(tuşOlayı("keyup"))
    window.dispatchEvent(tuşOlayı("keydown")) // pressedKeys'e yazılırsa görünsün
    // Kapandıktan sonra takılan işleyici de koşmamalı (sökecek kimse yok).
    var sonradanTakılan = 0
    w.onKeyPress(_ => sonradanTakılan += 1)
    w.onKeyRelease(_ => sonradanTakılan += 1)
    window.dispatchEvent(tuşOlayı("keydown"))
    window.dispatchEvent(tuşOlayı("keyup"))
    val ölçek = w.stage.scale.x
    window.dispatchEvent(
      js.Dynamic.newInstance(js.Dynamic.global.WheelEvent)("wheel", js.Dynamic.literal(deltaY = 100))
        .asInstanceOf[org.scalajs.dom.Event]
    )
    bekle(50).map { _ =>
      withClue("düzenek: açık dünyada tuş işleyicileri koşmalı -- ") { açıktaSayılar shouldBe ((1, 1)) }
      withClue("düzenek: açık dünyada pressedKeys dolmalı -- ") { açıktaBasılı shouldBe true }
      withClue("düzenek: açık dünya kocoResetView'ı kurmalı -- ") { açıktaSıfırla shouldBe true }
      withClue("kullanıcının tuş işleyicileri -- ") { (basılan, bırakılan) shouldBe ((1, 1)) }
      withClue("kapandıktan sonra takılan işleyiciler -- ") { sonradanTakılan shouldBe 0 }
      withClue("iç keydown dinleyicisi (pressedKeys) -- ") { w.pressedKeys.contains(k) shouldBe false }
      withClue("tekerlek dinleyicisi (yakınlaştırma) -- ") { w.stage.scale.x shouldBe ölçek }
      withClue("kocoResetView kapanmış dünyayı tutuyor -- ") {
        js.isUndefined(window.asInstanceOf[js.Dynamic].kocoResetView) shouldBe true
      }
    }
  }

  test("#166: kapat() çiziciyi bırakıyor, tuvali sayfadan söküyor") {
    val w = dünyaKurYaDaİptal()
    val tuval = w.renderer.view
    val gl = w.renderer.asInstanceOf[js.Dynamic].gl
    withClue("düzenek: tuval sayfada olmalı -- ") { document.body.contains(tuval) shouldBe true }
    w.kapat()
    w.kapat() // idempotent: ikinci destroy patlamamalı
    // Kapanmış dünyaya gelen çağrılar çiziciye dokunmamalı (view/plugins artık yok).
    w.size(200, 100)
    w.setBackground(kojo.doodle.Color.red)
    w.erasePictures()
    val fare = w.mouseXY
    val basılı = w.isAMouseButtonPressed
    bekle(50).map { _ =>
      withClue("kapanmış dünyada fare sorguları -- ") { ((fare.x, fare.y), basılı) shouldBe (((0.0, 0.0), false)) }
      document.body.contains(tuval) shouldBe false
      gl.isContextLost().asInstanceOf[Boolean] shouldBe true
    }
  }

  test("#166: 40 dünya kurup kapatmak açık bir dünyanın WebGL bağlamını düşürmüyor") {
    // Tarayıcı etkin bağlam sayısını sınırlıyor (Chromium ~16) ve aşılınca EN
    // ESKİYİ düşürüyor. Açık kalan dünya burada en eski; kapanan dünyalar
    // bağlamlarını bırakmazsa onunki kaybolur.
    val açık = dünyaKurYaDaİptal()
    sonDünya = None // fikstür onu kapatmasın
    val gl = açık.renderer.asInstanceOf[js.Dynamic].gl
    var i = 0
    while (i < 40) { dünyaKurYaDaİptal(); i += 1 }
    sonDünya.foreach(_.kapat())
    sonDünya = None
    bekle(100).map { _ =>
      val kayıp = gl.isContextLost().asInstanceOf[Boolean]
      açık.kapat()
      kayıp shouldBe false
    }
  }

  test("#166: pişmiş dokusu olan dünya kapanınca pişmişler geri konuyor, sonra silmek patlamıyor") {
    // kapat() pişmiş dokunun framebuffer'ını çizici ölmeden bırakıyor
    // (resetBake); gözlenebilir izi pişmiş çocukların sahneye dönmesi.
    // resetBake sökülünce dokuyu çizici öldükten sonra silmek de patlamadı
    // (ölçüldü), yani sıra savunma; sav yalnız çağrıyı çiviliyor. Pişirme
    // yalnız canlandır döngüsünde ve kalabalık sahnede oluyor (PisirmeTest
    // ile aynı düzenek).
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val resimSayısı = BakePolicy.bakeChildThreshold + 50
    def küçükResim(): TurtlePicture = TurtlePicture { t =>
      t.setAnimationDelay(0)
      var i = 0
      while (i < 12) { t.forward(6); t.right(30); i += 1 }
    }
    var kare = 0
    var enAz = Int.MaxValue
    w.animate {
      kare += 1
      if (kare == 2) { var n = 0; while (n < resimSayısı) { küçükResim().draw(); n += 1 } }
      if (kare > 20) enAz = math.min(enAz, w.stage.children.length)
    }
    // Düzenek yalnız "pişirme başladı"yı istiyor. İlk sürüm 2 sn bekleyip
    // çocukların yarıdan aza inmesini bekliyordu; yüklü makinede kırmızı
    // (#178 incelemesi: enAz 115). Resimler birkaç kareye yayılarak geliyor
    // ve pişirme çocuk sayısı bakeChildThreshold'un (150) altına inince
    // duruyor, yani "yarıdan az" düzeneğin garantisi değil (ölçüldü: 148'de
    // durdu). Pişene dek bekle (en çok 10 sn), sonra biraz daha.
    def pişeneDek(kalan: Int): Future[Unit] =
      if (kare > 20 && enAz < resimSayısı || kalan <= 0) bekle(300)
      else bekle(100).flatMap(_ => pişeneDek(kalan - 1))
    pişeneDek(100).flatMap { _ =>
      w.kapat()
      val kapanınca = w.stage.children.length
      w.erasePictures()
      w.size(200, 100)
      bekle(100).map { _ =>
        withClue(s"düzenek: resimler pişmiş olmalı (enAz=$enAz, resim=$resimSayısı) -- ") {
          enAz should be < resimSayısı
        }
        withClue("kapat() pişmişleri sahneye geri koymalı (resetBake) -- ") {
          kapanınca should be >= resimSayısı
        }
      }
    }
  }
}
