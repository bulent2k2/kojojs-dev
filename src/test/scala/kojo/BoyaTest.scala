package kojo

import kojo.doodle.Color
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Boya'nın PIXI'siz koşabilen bölümü: girdi denetimi ve PIXI 4 (ya da PIXI'nin
 * hiç bulunmadığı) durumdaki düşüş davranışı. Gradyanın gerçekten çizilip
 * çizilmediği ancak tarayıcıda görülebilir; o doğrulama ekran görüntüsüyle
 * yapılıyor (bkz. PR açıklaması).
 */
class BoyaTest extends AnyFunSuite with Matchers {

  test("PIXI yokken gradyanlar ilk renge düşüyor (çökmüyor)") {
    // Node altında PIXI yüklü değil -> PixiUyum.beşVeÜstü false
    PixiUyum.beşVeÜstü shouldBe false

    Boya.doğrusal(0, 0, Color.red, 100, 0, Color.blue, false) shouldBe DüzBoya(Color.red)
    Boya.merkezden(0, 0, Color.green, 50, Color.black, false) shouldBe DüzBoya(Color.green)
    Boya.doğrusalÇoklu(0, 0, 10, 10, Vector(0.0, 1.0), Vector(Color.yellow, Color.blue), false) shouldBe
      DüzBoya(Color.yellow)
    Boya.merkezdenÇoklu(0, 0, 20, Vector(0.0, 0.5, 1.0),
      Vector(Color.white, Color.red, Color.black), false) shouldBe DüzBoya(Color.white)
  }

  test("dağılım ile renkler aynı uzunlukta olmalı") {
    val e = intercept[IllegalArgumentException] {
      Boya.doğrusalÇoklu(0, 0, 10, 0, Vector(0.0, 0.5), Vector(Color.red), false)
    }
    e.getMessage should include("aynı uzunlukta")

    intercept[IllegalArgumentException] {
      Boya.merkezdenÇoklu(0, 0, 10, Vector(0.0), Vector(Color.red, Color.blue), false)
    }
  }

  test("boş durak listesi kabul edilmiyor") {
    val e = intercept[IllegalArgumentException] {
      Boya.doğrusalÇoklu(0, 0, 10, 0, Vector.empty, Vector.empty, false)
    }
    e.getMessage should include("en az bir renk durağı")
  }

  test("düz boya olduğu gibi geçiyor") {
    DüzBoya(Color.red).renk shouldBe Color.red
  }
}
