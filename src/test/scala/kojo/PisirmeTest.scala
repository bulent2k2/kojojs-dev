/*
 * Baskılamanın (bake) SAHNE düzeyi sınaması. Karar mantığının saf çekirdeği
 * BakePolicyTest'te (Node'da, DOM'suz); burada gerçek KojoWorldImpl ile
 * "gerçekten pişiyor mu" ölçülüyor.
 *
 * Sorun #96: pişirme adaylığı ADA bakıyordu (`name != "Turtle Layer"`), ama
 * Turtle.init o adı forPic kaplumbağalara DA veriyor -- yani her Resim{}
 * katmanı da onu taşıyor. Sonuç: Resim{} katmanları muafiyete takılıp HİÇ
 * pişmiyordu, baskılamanın kazancı tam da kalabalık sahnede devre dışıydı.
 * Ölçüldü (500 durağan Resim{}, canlandır döngüsü, 90 kare): düzeltmeden önce
 * sahne çocuğu 501'de sabit, eşzamanlı render ortancası 1.2-1.4 ms; sonra
 * çocuk 17-126'ya iniyor, render 0.1-0.4 ms.
 *
 * NEDEN canlandır ŞART: maybeBake YALNIZ animateHelper'dan çağrılıyor. Resimleri
 * çizip requestAnimationFrame ile kare saymak YETMEZ -- pişirme hiç denenmez
 * (ilk ölçümümde tam bu oldu, sayaçların hepsi sıfır çıktı).
 *
 * SAYILAR NEDEN GENİŞ ARALIKTA: draw() eşzamansız (ready.foreach), yani 300
 * katman birkaç kareye yayılarak sahneye giriyor; pişirme ancak sahne
 * bakeChildThreshold'u geçtikten ve adaylar bakeAfterFrames kare durağan
 * kaldıktan sonra başlıyor. Bu yüzden savlar KESİN SAYI değil TAVAN çiviliyor:
 * çivilenen şey "çoğu katman pişti", "şu kadarı pişti" değil.
 */
package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

class PisirmeTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private val resimSayısı = 300

  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval)
    document.body.appendChild(kap)
    new KojoWorldImpl()
  }

  /** WebGL'siz ortamda (bu sınamalar gerçek çiziciye bağlı) patlamak yerine iptal. */
  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try dünyaKur()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def kareler(n: Int)(adım: Int => Unit): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def döngü(): Unit = { i += 1; adım(i); if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => döngü()) }
    window.requestAnimationFrame(_ => döngü())
    söz.future
  }

  private def küçükResim()(implicit w: KojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setAnimationDelay(0)
    var i = 0
    while (i < 12) { t.forward(6); t.right(30); i += 1 }
  }

  test("canlandır döngüsünde durağan Resim{} katmanları pişiyor (#96)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val çocuklar = scala.collection.mutable.ArrayBuffer.empty[Int]
    var kare = 0
    w.animate {
      kare += 1
      if (kare == 2) { var n = 0; while (n < resimSayısı) { küçükResim().draw(); n += 1 } }
      if (kare > 20) çocuklar += w.stage.children.length
    }
    kareler(120) { _ => () }.map { _ =>
      w.animating = false
      val enAz = çocuklar.min
      withClue(s"sahne çocuğu: ilk=${çocuklar.head} son=${çocuklar.last} enAz=$enAz " +
        s"(çizilen resim: $resimSayısı) -- ") {
        çocuklar should not be empty
        // Düzeltmeden ÖNCE bu sayı resimSayısı+1'de sabit kalıyordu (ölçüldü).
        // Tavan cömert: çivilenen şey "katmanların çoğu pişti", kesin sayı değil.
        enAz should be < (resimSayısı / 2)
      }
    }
  }

  test("her karede bir resim oynarken pişirme çırpınmaya girmiyor (#96)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    // Resim{} katmanları pişmeye başlayınca akla gelen ilk risk bu: pişmiş bir
    // düğüm değişince unbakeAll çalışıyor, kısa pencerede art arda olursa
    // sigorta pişirmeyi KALICI kapatıyor (shouldDisableAfterUnbake). Oynayan
    // düğüm ilk değişiminde __kojoNoBake alıp aday olmaktan çıktığı için
    // gel-git kurulmuyor -- ölçüldü: sahne çocuğu 2'de düz kalıyor.
    val çocuklar = scala.collection.mutable.ArrayBuffer.empty[Int]
    var kare = 0
    var oynayan: TurtlePicture = null
    w.animate {
      kare += 1
      if (kare == 2) {
        var n = 0
        while (n < resimSayısı) { val p = küçükResim(); p.draw(); if (n == 0) oynayan = p; n += 1 }
      }
      if (kare > 15 && oynayan != null) oynayan.translate(1, 0)
      if (kare > 25) çocuklar += w.stage.children.length
    }
    kareler(120) { _ => () }.map { _ =>
      w.animating = false
      val (ilkYarı, sonYarı) = çocuklar.splitAt(çocuklar.size / 2)
      withClue(s"sahne çocuğu: ilk=${çocuklar.head} son=${çocuklar.last} " +
        s"enAz=${çocuklar.min} enÇok=${çocuklar.max} -- ") {
        çocuklar should not be empty
        çocuklar.min should be < (resimSayısı / 2)
        // Sigorta atsaydı pişmiş her şey sahneye geri döner ve bir daha
        // pişmezdi: son yarı ilk yarıdan BÜYÜK olurdu.
        withClue("son yarı ilk yarıdan büyük olmamalı (sigorta atmadı) -- ") {
          sonYarı.max should be <= ilkYarı.max
        }
      }
    }
  }

  test("kalabalık sahnede GERÇEK kaplumbağa pişmiyor (sahnede kalıyor) (#96)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    // Bu savın var olma sebebi: #96 düzeltmesi "Turtle Layer" adlı düğümleri
    // pişirmeye AÇIYOR, ayrımı ada değil simge çocuğuna bırakarak. Ayrım
    // bozulursa gerçek kaplumbağa da pişer -- sahneden çıkıp dokuya gömülür ve
    // BİR DAHA HAREKET EDEMEZ. Kırma sınamasıyla görüldü: muafiyeti
    // kaldırınca yalnız BakePolicyTest'in saf savları kırmızı yanıyordu,
    // sahnede donmuş kaplumbağayı gören hiçbir şey yoktu. Bu sav o boşluk.
    val kaplumbağa = new Turtle(0, 0)
    var kare = 0
    var sahnedeKaldı = true
    var enAzÇocuk = Int.MaxValue
    w.animate {
      kare += 1
      if (kare == 2) { var n = 0; while (n < resimSayısı) { küçükResim().draw(); n += 1 } }
      if (kare > 25) {
        enAzÇocuk = math.min(enAzÇocuk, w.stage.children.length)
        if (kaplumbağa.turtleLayer.parent == null) sahnedeKaldı = false
      }
    }
    kareler(120) { _ => () }.map { _ =>
      w.animating = false
      withClue(s"en az sahne çocuğu=$enAzÇocuk (pişirme gerçekten çalıştı mı) -- ") {
        // Kurulum savı: pişirme çalışmadıysa bu sınama hiçbir şey kanıtlamaz.
        enAzÇocuk should be < (resimSayısı / 2)
      }
      withClue("gerçek kaplumbağanın katmanı sahnede kalmalı (pişerse donar) -- ") {
        sahnedeKaldı shouldBe true
      }
    }
  }
}
