package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * `readPosition` / `readHeading`: `konumuOku` / `yönüOku`'nun İngilizce adları.
 * Koco -> Kojo çevirmeni `konumuOku`yu bunlara çeviriyor; iki dil aynı
 * kaplumbağa kuyruğundan AYNI değeri okumalı.
 *
 * Neden geri çağırma: iKojo'da kaplumbağa komutları kuyruğa girer. Okuma da
 * kuyruğa giriyor ve kendisinden ÖNCE verilen komutlar bitince, SONRAKİLER
 * başlamadan çalışıyor. Aşağıdaki savlar tam bunu tutuyor: her okuma kendi
 * yerindeki konumu veriyor, betiğin sonundaki konumu değil.
 */
class KonumOkumaTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def söz[T](kaydet: (T => Unit) => Unit): Future[T] = {
    val s = Promise[T]()
    kaydet(v => s.success(v))
    s.future
  }

  test("readPosition kuyruğun kendi yerindeki konumu verir; konumuOku ile aynı") {
    import kojo.syntax.Builtins
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    import b._
    import turtle._
    val tr = b.trTurtle
    setAnimationDelay(0)

    forward(100)
    val ing1 = söz[(Double, Double)](k => readPosition(p => k((p.x, p.y))))
    val tr1 = söz[(Double, Double)](k => tr.konumuOku(n => k((n.x, n.y))))
    right(); forward(50)
    val ing2 = söz[(Double, Double)](k => readPosition(p => k((p.x, p.y))))
    // Kopya mı: saklanan nokta kaplumbağa yürüyünce değişmemeli
    val saklanan = söz[pixiscalajs.PIXI.Point](k => readPosition(k))
    forward(70)
    // Savlar bu okumadan SONRA: kaplumbağa forward(70)'i yürümüş olmalı, yoksa
    // canlı bir nokta verilse bile saklanan henüz değişmemiş olurdu (ölçüldü)
    val son = söz[(Double, Double)](k => readPosition(p => k((p.x, p.y))))

    for { a <- ing1; t <- tr1; c <- ing2; s <- saklanan; z <- son } yield {
      z._1 shouldBe 120.0 +- 1e-6; z._2 shouldBe 100.0 +- 1e-6
      a._1 shouldBe 0.0 +- 1e-6; a._2 shouldBe 100.0 +- 1e-6
      t shouldBe a
      c._1 shouldBe 50.0 +- 1e-6; c._2 shouldBe 100.0 +- 1e-6
      s.x shouldBe 50.0 +- 1e-6; s.y shouldBe 100.0 +- 1e-6
    }
  }

  test("readHeading 0-360 arasında; yönüOku ile aynı") {
    import kojo.syntax.Builtins
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    import b._
    import turtle._
    val tr = b.trTurtle
    setAnimationDelay(0)

    val başta = söz[Double](readHeading)
    right(); right(); right(); right() // ham heading burada -270 (dönüşler birikiyor)
    val dörtSağ = söz[Double](readHeading)
    val dörtSağTr = söz[Double](tr.yönüOku)
    left(45)
    val sol = söz[Double](readHeading)

    for { a <- başta; d <- dörtSağ; t <- dörtSağTr; s <- sol } yield {
      a shouldBe 90.0 +- 1e-6
      d shouldBe 90.0 +- 1e-6
      t shouldBe d
      s shouldBe 135.0 +- 1e-6
    }
  }
}
