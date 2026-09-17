package kojo
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement
import scala.concurrent.{Future, Promise}
import scala.scalajs.js

class DolguBozulmadiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
    new KojoWorldImpl()
  }
  private def kareler(n: Int)(adım: Int => Unit): Future[Unit] = {
    val söz = Promise[Unit](); var i = 0
    def d(): Unit = { i += 1; adım(i); if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => d()) }
    window.requestAnimationFrame(_ => d()); söz.future
  }
  private def gül()(implicit w: KojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setAnimationDelay(0); t.setPenThickness(0); t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 120) { val a = i * 7 * 2 * math.Pi / 120; t.moveTo(150 * math.cos(a), 150 * math.sin(a)); i += 1 }
  }
  private def dolguParça(p: TurtlePicture)(implicit w: KojoWorldImpl): Int = {
    w.boyalarıBoşalt()
    p.tnode.asInstanceOf[js.Dynamic].children.asInstanceOf[js.Array[js.Dynamic]].toSeq.map { g =>
      if (js.typeOf(g.finishPoly) != "function") 0
      else { g.finishPoly(); g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].length }
    }.sum
  }
  test("A: çizilip bırakılan resmin dolgusu oluşuyor") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    val p = gül(); p.draw()
    var son = 0
    kareler(60) { _ => son = dolguParça(p) }.map { _ =>
      withClue(s"\n[A] çizilen resmin dolgu parçası = $son\n") { son should be > 100 }
    }
  }
  test("B: silinip YENİDEN çizilen resmin dolgusu geri geliyor") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    val p = gül(); p.draw()
    var ilk = 0; var silmeSonrası = 0; var yeniden = 0
    var kare = 0
    kareler(80) { i =>
      kare = i
      if (i == 30) { ilk = dolguParça(p); p.erase() }
      if (i == 40) silmeSonrası = dolguParça(p)
      if (i == 45) p.draw()
      if (i == 75) yeniden = dolguParça(p)
    }.map { _ =>
      withClue(s"\n[B] ilk=$ilk, silme sonrası=$silmeSonrası, yeniden çizince=$yeniden\n") {
        ilk should be > 100
        yeniden should be > 100
      }
    }
  }

  test("C: KURULUP bekletilen, sonra çizilen resmin dolgusu oluşuyor") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    // TurtlePicture yapıcıda make() çağırıyor, yani resim gövdesi draw()'dan
    // ÖNCE, katman sahnede DEĞİLKEN çalışıyor. #108 kapısı yalnız "sahnede mi"
    // diye sorsaydı bu yayınlar kesilir ve dolgu hiç oluşmazdı. Kapı bu yüzden
    // "sahneye BİR KEZ girdikten SONRA çıktıysa kes" diyor.
    val p = gül()
    var son = 0
    kareler(80) { i =>
      if (i == 40) p.draw()          // kuruluştan çok sonra çiz
      if (i > 45) son = dolguParça(p)
    }.map { _ =>
      withClue(s"\n[C] geç çizilen resmin dolgu parçası = $son\n") { son should be > 100 }
    }
  }

  test("D: silinen resmin çizeri yayın sırasına GERİ GİRMİYOR (#108)") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    // Düzeltmenin sözleşmesi bu. Katmanı silmek tek başına yetmiyordu:
    // kaplumbağanın komut kuyruğu boşalmaya devam ediyor ve her kenar
    // boyaKirlendi'yi yeniden çağırıyordu (ölçüldü: sıradan bir kez düşürmek
    // ölü yayın sayısını HİÇ değiştirmedi). Kapı boyaKirlendi'de.
    val p = gül()
    var kurulurken = false
    var çizilince = false
    var silinince = false
    var yenidenÇizilince = false
    kareler(80) { i =>
      if (i == 2) kurulurken = p.turtle.boyasıSürüyor      // henüz çizilmedi: SÜRMELİ
      if (i == 5) p.draw()
      if (i == 30) çizilince = p.turtle.boyasıSürüyor      // sahnede: SÜRMELİ
      if (i == 35) p.erase()
      if (i == 50) silinince = p.turtle.boyasıSürüyor      // silindi: DURMALI
      if (i == 55) p.draw()
      if (i == 75) yenidenÇizilince = p.turtle.boyasıSürüyor // geri geldi: SÜRMELİ
    }.map { _ =>
      withClue(s"\n[D] kurulurken=$kurulurken çizilince=$çizilince " +
        s"silinince=$silinince yenidenÇizilince=$yenidenÇizilince\n") {
        withClue("henüz çizilmemiş resmin yayını sürmeli (yoksa dolgu hiç oluşmaz) -- ") {
          kurulurken shouldBe true
        }
        çizilince shouldBe true
        withClue("SİLİNEN resmin yayını durmalı -- asıl düzeltme bu -- ") {
          silinince shouldBe false
        }
        withClue("yeniden çizilince geri gelmeli -- ") { yenidenÇizilince shouldBe true }
      }
    }
  }

  test("E: resimleriSil döngüsünde yayın sayısı canlı resimlerle sınırlı (#108)") {
    implicit val w: KojoWorldImpl = try dünyaKur() catch { case t: Throwable => cancel(s"$t") }
    // D savı boyasıSürüyor'un DEĞERİNİ çiviliyor; bu sav boyaKirlendi'nin onu
    // KULLANDIĞINI. İkisi ayrı: kapıyı boyaKirlendi'den söküp attığımda D
    // yeşil kalıyordu. Tek resmi silip beklemek de ayırt etmiyor -- 120
    // komutluk kuyruk birkaç karede bitiyor, silinecek "canlı" iş kalmıyor.
    // Ayırt eden şey #91'in kendi kalıbı: her karede sil + yeniden çiz, yani
    // her an kuyruğu boşalan SİLİNMİŞ resimler var.
    // Ölçüldü: kapısız 263-441 yayın, kapıyla 80 (kare başına tam 2 canlı resim).
    val kareSayısı = 40
    var kare = 0
    var başlangıç = 0L
    w.animate {
      kare += 1
      if (kare == 3) başlangıç = w.yayınSayısı
      w.erasePictures()
      var n = 0
      while (n < 2) { gül().draw(); n += 1 }
    }
    kareler(kareSayısı) { _ => () }.map { _ =>
      w.animating = false
      val yayın = w.yayınSayısı - başlangıç
      withClue(s"\n[E] $kareSayısı karede yayın = $yayın (kare başına 2 canlı resim çiziliyor)\n") {
        // Tavan iki yarıyı da çiviliyor (ölçüldü, 40 karede):
        //   master               : 320-427
        //   yalnız boyaKirlendi kapısı : 152   <- tavanı AŞAR
        //   kapı + katmanınBoyasınıUnut :  76-85
        yayın should be <= (kareSayısı * 3L)
      }
    }
  }
}
