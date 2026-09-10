package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * BoyamaYolu'nun saf çekirdeği: hangi köşeler o anki boyama çokgenini kuruyor.
 * PIXI'siz koştuğu için Node'da da sınanıyor (PompaDurumu ile aynı kalıp).
 *
 * Asıl kusur şuydu: dolgu PIXI'nin yarım yoluna emanetti ve her render onu
 * olduğu yerde kesiyordu. Buradaki savlar çokgenin renderdan BAĞIMSIZ
 * tutulduğunu çiviliyor -- kaç kare geçtiğinin hiçbir etkisi yok, çünkü
 * köşeler burada duruyor.
 */
class BoyamaYoluTest extends AnyFunSuite with Matchers {

  test("boya kurulmadan önce alan yok") {
    val y = new BoyamaYolu
    y.alanVarMı shouldBe false
  }

  test("kare: dört kenar sonunda beş köşe ve alan var") {
    val y = new BoyamaYolu
    y.boyaKuruldu(0, 0)
    y.çizildi(100, 0)
    y.alanVarMı shouldBe false // iki nokta bir doğru, alan değil
    y.çizildi(100, -100)
    y.alanVarMı shouldBe true // üç nokta bir üçgen
    y.çizildi(0, -100)
    y.çizildi(0, 0)
    y.köşeler.size shouldBe 5
    y.köşeler.head shouldBe ((0.0, 0.0))
    y.köşeler.last shouldBe ((0.0, 0.0))
  }

  test("boyama yeniden kurulunca çokgen sıfırlanır -- eski kenarlar yeni boyaya ait değil") {
    val y = new BoyamaYolu
    y.boyaKuruldu(0, 0)
    y.çizildi(100, 0)
    y.çizildi(100, -100)
    y.alanVarMı shouldBe true

    y.boyaKuruldu(200, 0) // kullanıcı boyamaRenginiKur'u yeniden çağırdı
    y.alanVarMı shouldBe false
    y.köşeler.size shouldBe 1
    y.köşeler.head shouldBe ((200.0, 0.0))
  }

  test("kalem kalkık taşınma çokgeni kırar") {
    val y = new BoyamaYolu
    y.boyaKuruldu(0, 0)
    y.çizildi(100, 0)
    y.çizildi(100, -100)
    y.taşındı(500, 500) // atla / zıpla
    y.alanVarMı shouldBe false
    y.köşeler shouldBe Seq((500.0, 500.0))
  }

  test("aynı noktaya art arda çizmek köşe eklemez") {
    // Kaplumbağa ileri(0) ya da yerinde dönme yaptığında aynı nokta iki kez
    // gelebiliyor; yinelenen köşe çokgene bir şey katmıyor.
    val y = new BoyamaYolu
    y.boyaKuruldu(0, 0)
    y.çizildi(100, 0)
    y.çizildi(100, 0)
    y.çizildi(100, 0)
    y.köşeler.size shouldBe 2
  }

  test("temizle her şeyi siler") {
    val y = new BoyamaYolu
    y.boyaKuruldu(0, 0)
    y.çizildi(100, 0)
    y.çizildi(100, -100)
    y.temizle()
    y.alanVarMı shouldBe false
    y.köşeler shouldBe empty
  }

  test("düzDizi PIXI'nin drawPolygon'ının istediği biçimde") {
    val y = new BoyamaYolu
    y.boyaKuruldu(1, 2)
    y.çizildi(3, 4)
    y.çizildi(5, 6)
    y.düzDizi.toSeq shouldBe Seq(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
  }
}
