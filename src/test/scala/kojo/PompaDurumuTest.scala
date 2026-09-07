package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// Kaplumbağa komut pompasının saf çekirdeğinin (PompaDurumu) Node testi.
// Turtle'ın geri kalanı PIXI/DOM'a bağlı olduğundan burada yalnız "pompa ne
// zaman yeniden zamanlanmalı" kararı sınanır (BakePolicyTest ile aynı kalıp).
class PompaDurumuTest extends AnyFunSuite with Matchers {

  test("başlamadan önce komut zamanlama istemez (kuyruk dolar, init pompayı çalıştırır)") {
    val p = new PompaDurumu
    p.başladıMı shouldBe false
    p.komutGirdi() shouldBe false
    p.komutGirdi() shouldBe false
  }

  test("başlatma pompayı çalışır sayar; çalışırken gelen komut yeniden zamanlamaz") {
    val p = new PompaDurumu
    p.başlat()
    p.başladıMı shouldBe true
    p.boştaMı shouldBe false
    p.komutGirdi() shouldBe false // zaten koşuyor: ikinci bir zamanlama çift pompa demek
  }

  test("kuyruk boşalınca ilk komut -- ve yalnız ilki -- pompayı yeniden başlatır") {
    val p = new PompaDurumu
    p.başlat()
    p.kuyrukBoşaldı()
    p.boştaMı shouldBe true
    p.komutGirdi() shouldBe true // canlandır/tuşaBasınca içinden gelen komut
    p.boştaMı shouldBe false
    p.komutGirdi() shouldBe false // arkasından gelenler aynı pompaya biner
    p.komutGirdi() shouldBe false
  }

  test("boşalma ve yeniden başlama tekrar tekrar çalışır (her karede bir komut)") {
    val p = new PompaDurumu
    p.başlat()
    for (_ <- 1 to 5) {
      p.kuyrukBoşaldı()
      p.komutGirdi() shouldBe true
    }
  }

  test("başlamamış pompa boşalma bildirseler de zamanlama istemez") {
    val p = new PompaDurumu
    p.kuyrukBoşaldı()
    p.komutGirdi() shouldBe false
  }
}
