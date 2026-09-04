package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// Pişirme kararının saf çekirdeğinin (BakePolicy) Node testi. KojoWorldImpl'in
// geri kalanı DOM/PIXI'ye bağlı olduğundan burada yalnız karar mantığı test
// edilir (inceleme önerisi: kırılganlığa karşı sigorta).
class BakePolicyTest extends AnyFunSuite with Matchers {
  import BakePolicy._

  test("az çocukluyken pişirme düşünülmez") {
    shouldConsider(childCount = 10, unzoomed = true) shouldBe false
  }

  test("kalabalık + yakınlaştırılmamışken pişirme düşünülür") {
    shouldConsider(childCount = bakeChildThreshold, unzoomed = true) shouldBe true
    shouldConsider(childCount = 400, unzoomed = true) shouldBe true
  }

  test("yakınlaştırmadayken pişirme düşünülmez") {
    shouldConsider(childCount = 400, unzoomed = false) shouldBe false
  }

  test("bakeAfterFrames karedir damgalanmayan sıradan çocuk adaydır") {
    // frame 10, en son 0'da değişmiş -> 10 > 3, aday
    isStaleCandidate(name = null, interactive = false, lastMut = 0, frame = 10) shouldBe true
  }

  test("yeni/az önce değişen çocuk aday değildir") {
    isStaleCandidate(name = null, interactive = false, lastMut = 9, frame = 10) shouldBe false
    isStaleCandidate(name = null, interactive = false, lastMut = 10, frame = 10) shouldBe false
  }

  test("kaplumbağa katmanı hiç pişmez") {
    isStaleCandidate(name = "Turtle Layer", interactive = false, lastMut = 0, frame = 100) shouldBe false
  }

  test("etkileşimli düğüm hiç pişmez") {
    isStaleCandidate(name = null, interactive = true, lastMut = 0, frame = 100) shouldBe false
  }

  test("hiç damgalanmamış (lastMut çok eski) çocuk adaydır") {
    isStaleCandidate(name = null, interactive = false, lastMut = -1, frame = 0) shouldBe false // 0 - (-1) = 1, not > 3
    isStaleCandidate(name = null, interactive = false, lastMut = -1, frame = 5) shouldBe true  // 5 - (-1) = 6 > 3
  }

  test("isStaleByName ucuz ön kontrol: ad + durağanlık (etkileşimden bağımsız)") {
    isStaleByName(name = null, lastMut = 0, frame = 10) shouldBe true
    isStaleByName(name = "Turtle Layer", lastMut = 0, frame = 10) shouldBe false
    isStaleByName(name = null, lastMut = 9, frame = 10) shouldBe false
  }

  test("çırpınma sigortası: art arda geri alma eşiği aşınca pişirme kapanır") {
    shouldDisableAfterUnbake(0) shouldBe false
    shouldDisableAfterUnbake(maxUnbakeStreak - 1) shouldBe false
    shouldDisableAfterUnbake(maxUnbakeStreak) shouldBe true
    shouldDisableAfterUnbake(maxUnbakeStreak + 3) shouldBe true
  }
}
