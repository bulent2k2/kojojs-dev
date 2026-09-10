package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tembel üçgenlemenin ASIL kazancını çiviliyor: n kirlenme -> 1 yayın.
 *
 * NEDEN VAR: `Turtle.turtlePathLineTo` her kenarda dolgu çokgeninin tamamını
 * yeniden yayınlıyordu. Render zaten bir requestAnimationFrame'e toplandığı
 * için (`KojoWorld.render`), `hızıKur(çokHızlı)`ta n kenar TEK render'a
 * düşüyor ve n-1 üçgenleme çöpe gidiyordu. Ölçüldü (tan-theta.kojo, 241
 * nokta): 241 yayın, 1 render.
 *
 * BoyamaGerilemeTest dolgunun OLUŞTUĞUNU sınıyor; bu sınama KAÇ KEZ
 * yayınlandığını sınıyor. Biri olmadan öbürü yetmez: yayını tembelleştirip
 * hiç boşaltmamak gerilemeyi kırar, boşaltmayı her kirlenmede yapmak da
 * kazancı sessizce geri alır.
 */
class TembelBoyamaTest extends AnyFunSuite with Matchers {

  /** Yayın sayan sahte çizer -- gerçek Turtle'a ve PIXI'ye gerek yok. */
  private class Sayaç extends Boyacı {
    var yayın = 0
    private[kojo] def boyayıYayınla(): Unit = yayın += 1
  }

  test("n kirlenme, bir boşaltma, tek yayın") {
    val dünya = new TestKojoWorld
    val s = new Sayaç
    // tan-theta'nın kenar sayısı
    (1 to 241).foreach(_ => dünya.boyaKirlendi(s))
    withClue("boşaltmadan önce hiç yayın olmamalı: yayın tembel -- ") {
      s.yayın should be(0)
    }
    dünya.boyalarıBoşalt()
    withClue("241 kirlenme tek yayına inmeliydi -- ") {
      s.yayın should be(1)
    }
  }

  test("boşaltma bekleyenleri tüketiyor: ikinci boşaltma boşa çalışmıyor") {
    val dünya = new TestKojoWorld
    val s = new Sayaç
    dünya.boyaKirlendi(s)
    dünya.boyalarıBoşalt()
    dünya.boyalarıBoşalt()
    s.yayın should be(1)
  }

  test("her kare için ayrı yayın: kirlenme yeniden başlıyor") {
    val dünya = new TestKojoWorld
    val s = new Sayaç
    for (_ <- 1 to 3) {
      (1 to 50).foreach(_ => dünya.boyaKirlendi(s))
      dünya.boyalarıBoşalt()
    }
    withClue("üç kare, kare başına bir yayın -- ") { s.yayın should be(3) }
  }

  test("birden çok çizer: her biri bir kez, kirlenme sırasıyla") {
    val dünya = new TestKojoWorld
    var sıra = List.empty[String]
    def çizer(ad: String) = new Boyacı {
      private[kojo] def boyayıYayınla(): Unit = sıra = ad :: sıra
    }
    val a = çizer("a"); val b = çizer("b")
    dünya.boyaKirlendi(a); dünya.boyaKirlendi(b); dünya.boyaKirlendi(a)
    dünya.boyalarıBoşalt()
    withClue("katman sırası önemli: kirlenme sırası korunmalı -- ") {
      sıra.reverse should be(List("a", "b"))
    }
  }

  test("yayın sırasında yeniden kirlenme SONRAKİ kareye kalıyor (sonsuz döngü yok)") {
    val dünya = new TestKojoWorld
    var yayın = 0
    lazy val kendiniKirleten: Boyacı = new Boyacı {
      private[kojo] def boyayıYayınla(): Unit = {
        yayın += 1
        if (yayın < 5) dünya.boyaKirlendi(kendiniKirleten) // gerçekte: doku yüklenince
      }
    }
    dünya.boyaKirlendi(kendiniKirleten)
    dünya.boyalarıBoşalt()
    withClue("boşaltma kendi içinde dönmemeli -- ") { yayın should be(1) }
    dünya.boyalarıBoşalt()
    yayın should be(2)
  }

  test("bekleyenBoyayıUnut YALNIZ o çizeri düşürüyor, ötekiler ayakta") {
    val dünya = new TestKojoWorld
    val a = new Sayaç
    val b = new Sayaç
    dünya.boyaKirlendi(a)
    dünya.boyaKirlendi(b)
    dünya.bekleyenBoyayıUnut(a) // A kendi yolunu sildi
    dünya.boyalarıBoşalt()
    withClue("A sil() dedi, onun bekleyeni düşmeli -- ") { a.yayın should be(0) }
    withClue("B dokunmadı, onun dolgusu YAYINLANMALI: küresel düşürme A'nın " +
      "sil()'iyle B'nin boyasını yok ediyordu -- ") { b.yayın should be(1) }
  }
}
