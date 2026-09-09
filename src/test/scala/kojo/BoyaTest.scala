package kojo

import kojo.doodle.Color

import scala.scalajs.js
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Boya'nın girdi denetimi ve sürüme göre davranışı.
 *
 * Takım İKİ ortamda koşuyor ve `PixiUyum.beşVeÜstü` ikisinde ayrı:
 *  - tarayıcı (test-tarayici.sh): jsDependencies PIXI 5 veriyor  -> true
 *  - Node (jsDependencies boş):  PIXI hiç yüklenmiyor            -> false
 * Bu yüzden buradaki savlar sürüme göre dallanıyor. Eskiden takım PIXI 4
 * yüklüyordu, yani doku dolgusu yolu HİÇBİR testte koşmuyordu -- site ise
 * PIXI 5 sunuyor. Gradyanlarda ortaya çıkan hatalar tam o boşluktan geçti.
 */
class BoyaTest extends AnyFunSuite with Matchers {

  test("test harnessi PIXI 4'e geri kaymamış") {
    // Site PIXI 5 sunuyor (kojojs-editor/.../javascript/pixi.min.js). Takım uzun
    // süre PIXI 4 yükledi ve doku dolgusu yolu -- yani BÜTÜN gradyanlar --
    // hiçbir testte koşmadı. Bu sav o boşluğun sessizce geri açılmasını
    // engelliyor: PIXI yüklüyse sürümü 5+ olmalı.
    // (Node koşusunda PIXI hiç yüklenmiyor; orada denetlenecek bir şey yok.)
    if (js.typeOf(js.Dynamic.global.PIXI) != "undefined") {
      // Utils.scala:104'teki kalıp: PIXI var ama VERSION yoksa düz asInstanceOf
      // savdan ÖNCE patlar ve aşağıdaki ileti hiç görünmezdi.
      val sürüm = js.Dynamic.global.PIXI.VERSION
        .asInstanceOf[js.UndefOr[String]].getOrElse("(VERSION yok)")
      withClue(s"test harnessindeki PIXI sürümü: $sürüm -- ") {
        PixiUyum.beşVeÜstü shouldBe true
      }
    }
  }

  test("PIXI 5 yokken gradyanlar ilk renge düşüyor, varken doku boyası kuruluyor") {
    val doğrusal = Boya.doğrusal(0, 0, Color.red, 100, 0, Color.blue, false)
    val merkezden = Boya.merkezden(0, 0, Color.green, 50, Color.black, false)
    val çoklu = Boya.doğrusalÇoklu(0, 0, 10, 10, Vector(0.0, 1.0), Vector(Color.yellow, Color.blue), false)
    val merkezdenÇoklu = Boya.merkezdenÇoklu(0, 0, 20, Vector(0.0, 0.5, 1.0),
      Vector(Color.white, Color.red, Color.black), false)

    if (PixiUyum.beşVeÜstü) {
      // Doku dolgusu yalnız v5'te var. Yedek renk HER ZAMAN ilk durak olmalı:
      // doku yüklenemezse düşülecek renk odur.
      //
      // Desen eşlemesiyle: asInstanceOf Scala.js'te fullOpt altında
      // DENETLENMİYOR, Defaults altında ClassCastException atıyor -- iki
      // durumda da gerileme "DokuBoya bekleniyordu" diye değil, TypeError ya da
      // undefined karşılaştırması olarak görünürdü.
      Seq(
        "doğrusal" -> (doğrusal, Color.red),
        "merkezden" -> (merkezden, Color.green),
        "doğrusalÇoklu" -> (çoklu, Color.yellow),
        "merkezdenÇoklu" -> (merkezdenÇoklu, Color.white)
      ).foreach {
        case (ad, (boya, beklenen)) =>
          boya match {
            case DokuBoya(_, _, yedek) => withClue(s"$ad yedek rengi: ")(yedek shouldBe beklenen)
            case başka                 => fail(s"$ad için DokuBoya bekleniyordu, gelen: $başka")
          }
      }
    }
    else {
      doğrusal shouldBe DüzBoya(Color.red)
      merkezden shouldBe DüzBoya(Color.green)
      çoklu shouldBe DüzBoya(Color.yellow)
      merkezdenÇoklu shouldBe DüzBoya(Color.white)
    }
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
