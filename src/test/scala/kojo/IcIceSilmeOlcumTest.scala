package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.{Future, Promise}

/**
 * Sorun #115'in ÖLÇÜMÜ: iç içe resimlerde (GPics) silinen dolgu yayınlanmaya
 * devam ediyordu. Doğruluk savları IcIceSilmeTest'te; burası sayıyı çiviliyor.
 *
 * AYRI SUITE: gerçek `KojoWorldImpl` ve gerçek `canlandır` döngüsü kuruyor.
 * IcIceSilmeTest'in TestKojoWorld'üyle aynı dosyada durunca ikisi rAF üstünden
 * birbirine karışıyordu (ölçüldü: çıplak ölçüm 74 yerine 0 okudu).
 *
 * Ölçüldü (40 kare, her karede resimleriSil + yeniden çizim; üçer koşu):
 *   çıplak iki gül     önce  74 / 74 / 74      sonra  74 / 76 / 74
 *   GPics(gül, gül)    önce 512 / 533 / 512    sonra 114 / 114 / 114
 *
 * Kalan 114-74 = 40 ÖLÜ İŞ DEĞİL: kare sayısıyla doğrusal ve belirlenimci
 * (40 kare 114, 80 kare 234; çıplak 74 ve 156). Resim sayısından da BAĞIMSIZ,
 * yani grup BAŞINA sabit bir fazlalık (40 karede ölçüldü):
 *   1 resim  çıplak  36  grup  76      3 resim  çıplak 114  grup 152
 *   2 resim  çıplak  74  grup 113
 *
 * AMA BU SAYI MAKİNEYE BAĞLI: #117'nin incelemesinde aynı senaryo için grup ile
 * çıplak BİREBİR aynı ölçüldü (76 / 156), yani orada fazlalık sıfır. İki ölçüm
 * de kendi makinesinde belirlenimci. Kirlenmeler LinkedHashSet'te çizer başına
 * teklendiği için fazladan kirlenmenin aynı karenin boşaltmasına düşüp
 * düşmemesi zamanlamaya bakıyor -- yani buradaki 40'ı grup sarmalının GENEL
 * bir özelliği saymayın, bu düzeneğin okuması sayın. Niteliksel sonuç iki
 * ölçümde de aynı: sınırlı ve kare sayısıyla doğrusal. Bozuk hâlde kare başına
 * ~13 idi ve zamanlamaya göre oynuyordu -- sınırsız ölü iş.
 */
class IcIceSilmeOlcumTest extends AsyncFunSuite with Matchers {
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

  private def kareler(n: Int): Future[Unit] = {
    val söz = Promise[Unit](); var i = 0
    def d(): Unit = { i += 1; if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => d()) }
    window.requestAnimationFrame(_ => d()); söz.future
  }

  /** Kesişen, dolgulu, pahalı bir şekil -- üçgenleme maliyeti görünür olsun. */
  private def gül()(implicit w: KojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setAnimationDelay(0); t.setPenThickness(0); t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 120) { val a = i * 7 * 2 * math.Pi / 120; t.moveTo(150 * math.cos(a), 150 * math.sin(a)); i += 1 }
  }

  private def döngüdeYayın(kareSayısı: Int)(çiz: KojoWorldImpl => Unit): Future[Long] = {
    val w: KojoWorldImpl = dünyaKurYaDaİptal()
    var kare = 0
    var başlangıç = 0L
    // 3. kareden sayıyoruz: ilk kareler varlık yüklemesi ve kurulum taşıyor.
    w.animate { kare += 1; if (kare == 3) başlangıç = w.yayınSayısı; w.erasePictures(); çiz(w) }
    kareler(kareSayısı).map { _ => w.animating = false; w.yayınSayısı - başlangıç }
  }

  test("GPics döngüsünde yayın sayısı çıplak hâlle aynı mertebede kalıyor (#115)") {
    for {
      çıplak <- döngüdeYayın(40) { w => gül()(w).draw(); gül()(w).draw() }
      grup <- döngüdeYayın(40) { w =>
        val b = new kojo.syntax.Builtins()(w)
        b.GPics(gül()(w), gül()(w))(w).draw()
      }
    } yield withClue(s"çıplak=$çıplak grup=$grup: ") {
      // Alt sınır: sıfır yayın "hiç çizmiyor" demek olurdu ve savı boşa
      // düşürürdü. Tavan cömert: ölçülen 114'e karşı 200; bozuk hâl 512-533
      // idi, yani gerileme tavanın çok üstüne çıkar.
      çıplak should be > 20L
      grup should be > 20L
      grup should be < 200L
    }
  }
}
