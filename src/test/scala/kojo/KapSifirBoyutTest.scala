package kojo

import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * Kap sıfır boyuta inince çizici eksi boyuta sokulmamalı (#173).
 *
 * Editörde GitHub oturumu açıkken "Betiklerim"e tıklayınca sonuç çerçevesi 0
 * genişliğe iniyor ve `resize` tetikleniyor. Eskiden
 * `size(clientWidth - margin, ...)` = size(-4, ...) çağrılıyor, `renderer.resize`
 * eksi boyutla çalışıyor ve konsola şu düşüyordu (canlıda görüldü):
 *
 *   GL_INVALID_VALUE : glViewport: negative width/height
 *
 * Çizim çalışmaya devam ediyor, yani kusur gürültü; ama temiz bir konsol gerçek
 * hataları görünür tutuyor. Sınama çiziciye giden BOYUTLARI kaydediyor: eksi bir
 * boyut hiç gitmemeli. (Bu, GL hata ileti metnini yakalamaktan sağlam: ileti
 * tarayıcıya ve sürücüye göre değişiyor, çağrının kendisi değişmiyor.)
 */
class KapSifirBoyutTest extends AnyFunSuite with Matchers {

  private def kapKur(genişlik: String, yükseklik: String): HTMLElement = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = genişlik; kap.style.height = yükseklik
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval)
    document.body.appendChild(kap)
    kap
  }

  /** WebGL'siz ortamda (bu sınamalar gerçek çiziciye bağlı) patlamak yerine iptal. */
  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try new KojoWorldImpl()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  /** `renderer.resize`'a giden (genişlik, yükseklik) çiftlerini kaydeder. */
  private def casusKur(w: KojoWorldImpl): scala.collection.mutable.ArrayBuffer[(Double, Double)] = {
    val çağrılar = scala.collection.mutable.ArrayBuffer.empty[(Double, Double)]
    val r = w.renderer.asInstanceOf[js.Dynamic]
    val özgün = r.resize
    r.resize = ((gen: Double, yük: Double) => {
      çağrılar += ((gen, yük))
      özgün.call(r, gen, yük)
    }): js.Function2[Double, Double, js.Any]
    çağrılar
  }

  test("kap 0 boyuta inince resize çiziciye eksi (ya da sıfır) boyut vermiyor; boyut yerinde kalıyor") {
    val kap = kapKur("400px", "300px")
    val w = dünyaKurYaDaİptal()
    w.canvasWidth shouldBe 396.0 +- 1e-9
    w.canvasHeight shouldBe 296.0 +- 1e-9
    val çağrılar = casusKur(w)

    kap.style.width = "0px" // "Betiklerim" paneli açıldı
    w.resize(null)
    withClue("çiziciye giden boyutlar: ") { çağrılar.filter { case (g, y) => g <= 0 || y <= 0 } shouldBe empty }
    withClue("tuval boyutu yerinde kalmalı: ") {
      (w.canvasWidth, w.canvasHeight) shouldBe ((396.0, 296.0))
    }

    kap.style.height = "0px" // ikisi de sıfır
    w.resize(null)
    çağrılar.filter { case (g, y) => g <= 0 || y <= 0 } shouldBe empty
    (w.canvasWidth, w.canvasHeight) shouldBe ((396.0, 296.0))
    w.kapat()
  }

  test("kap yeniden görününce gelen resize doğru boyutu kuruyor") {
    val kap = kapKur("400px", "300px")
    val w = dünyaKurYaDaİptal()
    val çağrılar = casusKur(w)
    kap.style.width = "0px"; w.resize(null)
    çağrılar shouldBe empty

    kap.style.width = "500px"; kap.style.height = "350px"
    w.resize(null)
    çağrılar.toList shouldBe List((496.0, 346.0))
    (w.canvasWidth, w.canvasHeight) shouldBe ((496.0, 346.0))
    w.kapat()
  }

  test("geçerli boyutta resize eskisi gibi çalışıyor (koruma gereksiz yere engellemiyor)") {
    val kap = kapKur("400px", "300px")
    val w = dünyaKurYaDaİptal()
    val çağrılar = casusKur(w)
    kap.style.width = "250px"; kap.style.height = "180px"
    w.resize(null)
    çağrılar.toList shouldBe List((246.0, 176.0))
    // sınır: tam margin kadar kap (4px) boyut 0 demek, atlanmalı; 5px -> 1
    kap.style.width = "4px"; w.resize(null)
    çağrılar.size shouldBe 1
    kap.style.width = "5px"; w.resize(null)
    çağrılar.toList shouldBe List((246.0, 176.0), (1.0, 176.0))
    w.kapat()
  }

  test("kurulurken kap gizliyse (0 boyut) ilk çizici eksi boyutla kurulmuyor") {
    kapKur("0px", "0px")
    val w = dünyaKurYaDaİptal()
    withClue("tuval boyutu: ") { (w.canvasWidth, w.canvasHeight) shouldBe ((1.0, 1.0)) }
    val r = w.renderer.asInstanceOf[js.Dynamic]
    withClue("çizici boyutu: ") {
      (r.width.asInstanceOf[Double] > 0, r.height.asInstanceOf[Double] > 0) shouldBe ((true, true))
    }
    w.kapat()
  }
}
