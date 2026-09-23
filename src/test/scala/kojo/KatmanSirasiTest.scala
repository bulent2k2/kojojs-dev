package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js
import scala.concurrent.Future
import pixiscalajs.PIXI

/**
 * Sorun #86'nın gerileme savı: ÖNCE çizilen bir şeklin kenarlığı, SONRA
 * çizilen bir şeklin dolgusunun ÜSTÜNDE kalmamalı.
 *
 * KUSUR NEYDİ: tamamlanmış dolguların tamamı tek bir `boyamaBitmiş`te, kalem
 * izinin tamamı tek bir `turtlePath`te birikiyordu ve katman sırası bir kez,
 * init'te kuruluyordu -- dolgular altta, kalem üstte. Yani kaç şekil çizilirse
 * çizilsin BÜTÜN kenarlıklar BÜTÜN dolguların üstünde kalıyordu. On kare çizen
 * betikte ölçüldü: turtleLayer'ın çocukları
 *   ["Turtle Fill (done)"(27 parça), "Turtle Fill (in progress)",
 *    "Turtle Path"(40 parça), "Turtle Icon"]
 *
 * DÜZELTME: her tamamlanan şekil kendi dolgu düğümünü alıyor ve o şeklin kalem
 * izinin hemen ALTINA konuyor; kalem izi orada donup üstüne yeni bir canlı yol
 * açılıyor. Katman sırası çizilme sırasıyla örtüşüyor.
 *
 * NEDEN SIRAYA BAKIYORUZ: sav tam olarak Z-SIRASI hakkında, piksel hakkında
 * değil; WebGL geri okuması bu takımda güvenilir değil. Sıra yanlışsa piksel de
 * yanlış olur -- kusurlu kodda bu sıra hiç oluşmuyordu.
 */
class KatmanSirasiTest extends AsyncFunSuite with Matchers with RepeatCommands {
  import kojo.syntax.Builtins
  implicit val kojoWorld = new TestKojoWorld()
  val builtins = new Builtins()
  import builtins._
  import builtins.Color._
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def adlar(t: Turtle): Seq[String] =
    t.turtleLayer.children.toSeq.map(_.asInstanceOf[js.Dynamic].name.asInstanceOf[String])

  /** İki dolgulu kare. Her `setFillColor` bir öncekini kalıcıya yazıyor, yani
    * üçüncü çağrı ikinci kareyi de kapatıyor. */
  private def ikiKare(): (TurtlePicture, () => Turtle) = {
    var kaplumbağa: Turtle = null
    val p = PictureT { t =>
      import t._
      invisible()
      setAnimationDelay(0)
      kaplumbağa = t
      setFillColor(blue)
      repeat(4) { forward(100); right() }
      setFillColor(red) // 1. kareyi kapatır, 2.yi açar
      repeat(4) { forward(100); right() }
      setFillColor(green) // 2. kareyi kapatır
    }
    (p, () => kaplumbağa)
  }

  test("her tamamlanan şekil kendi dolgu düğümünü alıyor") {
    val (p, t) = ikiKare()
    p.draw()
    for (_ <- p.ready) yield {
      adlar(t()).count(_ == "Turtle Fill") shouldBe 2
    }
  }

  test("sonraki şeklin dolgusu, önceki şeklin kalem izinin ÜSTÜNDE") {
    val (p, t) = ikiKare()
    p.draw()
    for (_ <- p.ready) yield {
      val a = adlar(t())
      val ilkDolgu = a.indexOf("Turtle Fill")
      val ilkKalem = a.indexOf("Turtle Path")
      val ikinciDolgu = a.indexOf("Turtle Fill", ilkDolgu + 1)
      withClue(s"katman sırası: ${a.mkString(" | ")} -- ") {
        ilkDolgu should be >= 0
        ikinciDolgu should be > ilkDolgu
        // 1. kare: kenarlık kendi dolgusunun ÜSTÜNDE
        ilkKalem should be > ilkDolgu
        // ASIL SAV: 2. karenin dolgusu 1. karenin kenarlığını örtebilmeli
        ikinciDolgu should be > ilkKalem
      }
    }
  }

  test("tamamlanan parçalar dolgu/kalem diye SIRAYLA diziliyor") {
    val (p, t) = ikiKare()
    p.draw()
    for (_ <- p.ready) yield {
      // "(in progress)" ve simge kuyrukta; onlardan öncesi tamamlanmış çift(ler)
      val tamamlanan = adlar(t()).takeWhile(ad => ad == "Turtle Fill" || ad == "Turtle Path")
      withClue(s"tamamlanan parçalar: ${tamamlanan.mkString(" | ")} -- ") {
        tamamlanan shouldBe Seq("Turtle Fill", "Turtle Path", "Turtle Fill", "Turtle Path")
      }
    }
  }

  test("sil() donmuş parçaları da katmandan çıkarıyor") {
    val (p, t) = ikiKare()
    p.draw()
    p.ready.flatMap { _ =>
      adlar(t()).count(_ == "Turtle Fill") shouldBe 2
      // clear KOMUT KUYRUĞUNA giriyor; sync ile tam arkasına düşüyoruz, yoksa
      // sav silme işlenmeden koşar ve hiçbir şey sınamaz.
      val söz = scala.concurrent.Promise[Seq[String]]()
      t().clear()
      t().sync(() => söz.success(adlar(t())))
      söz.future.map { sonra =>
        withClue(s"sil() sonrası katman: ${sonra.mkString(" | ")} -- ") {
          sonra.count(_ == "Turtle Fill") shouldBe 0
        }
      }
    }
  }

  /** Bir çizerin parçalarının çizgi biçemi: (genişlik, görünür) */
  private def çizgiBiçemleri(g: PIXI.Graphics): Seq[(Double, Boolean)] = {
    val gd = g.asInstanceOf[js.Dynamic].geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]]
    gd.toSeq.map(d => (d.lineStyle.width.asInstanceOf[Double], d.lineStyle.visible.asInstanceOf[Boolean]))
  }

  /**
   * #89 incelemesinde yakalanan gerileme. Şekil başına düğüme geçerken
   * TurtlePicture'ın biçem dönüştürücüleri parçaların HEPSİNE uygulanmıştı --
   * dolgu düğümleri dahil. Oysa dolgu düğümleri bilerek ÇİZGİSİZ doğuyor
   * (`lineStyle(0,0,0)`; kenarlığı kalem çiziyor) ve `kalemiKur` her parçaya
   * `lineStyle.visible = true` yazıyor. Sonuç: dolgunun ÜÇGENLEME DİKİŞİ
   * görünür bir çizgiye dönüşüyordu. Ölçülmüştü -- dolgu düğümünün üç parçası
   * (w=0, görünür=false) iken (w=8, görünür=true) oluyor ve
   * `kalemKalınlığı(8) * kalemRengi(yeşil) -> Resim{dolgulu kare}` mavi karenin
   * içinden kalın yeşil bir köşegen geçiriyordu.
   *
   * Takımın 153 sınaması bu gerilemeyi yakalamıyordu; sav bu yüzden var.
   */
  test("kalem biçemi DOLGU düğümlerine uygulanmıyor") {
    val (p, t) = ikiKare()
    p.draw()
    p.ready.flatMap { _ =>
      p.setPenThickness(8)
      p.setPenColor(green)
      // DİKKAT: TurtlePicture'ın dönüştürücüleri gövdelerini `ready.foreach`
      // içinde koşturuyor, yani iş SATIR İÇİNDE değil bir MİKROGÖREVDE
      // yapılıyor. Hemen sav yazmak yarışa girer -- ölçüldü: savlar
      // dönüştürücüler hiç çalışmadan koşuyor ve sınama BOŞUNA geçiyordu
      // (kalem biçemleri (2,true) olarak kalıyordu). Kuyruğa bir tur bırakıp
      // öyle bakıyoruz.
      Future(()).map { _ =>
        // Küçük kareler Graphics'te kalıyor (#147: eşik altı); stencil düğümünde
        // çizgi biçemi diye bir şey yok, o yüzden yalnız Graphics parçaları.
        val dolguBiçemleri = t().dolguParçaları.toSeq.collect { case g: PIXI.Graphics => g }.flatMap(çizgiBiçemleri)
        val kalemBiçemleri = t().kalemParçaları.toSeq.flatMap(çizgiBiçemleri)
        withClue(s"dolgu: $dolguBiçemleri / kalem: $kalemBiçemleri -- ") {
          // Sav ancak dönüştürücü GERÇEKTEN çalıştıysa bir şey söyler:
          kalemBiçemleri.filter(_._2).map(_._1) should contain only 8.0
          // ASIL SAV: dolguya sızmamalı
          dolguBiçemleri should not be empty
          dolguBiçemleri.map(_._2) should contain only false
          dolguBiçemleri.map(_._1) should contain only 0.0
        }
      }
    }
  }
}
