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
    // süs katmanı (eksen/ızgara) da pişirme dışı: pişerse gizle/göster ölür
    isStaleByName(name = "Decor Layer", lastMut = 0, frame = 10) shouldBe false
    isStaleByName(name = null, lastMut = 9, frame = 10) shouldBe false
  }

  test("arkayaAt'ın dip sırası: süs yoksa 0") {
    dipSırası(Seq("Bake Layer", "Resim", "Turtle Layer")) shouldBe 0
    dipSırası(Seq.empty) shouldBe 0
  }

  test("arkayaAt'ın dip sırası: baştaki süs katmanları atlanır") {
    dipSırası(Seq("Decor Layer", "Bake Layer", "Resim")) shouldBe 1
    dipSırası(Seq("Decor Layer", "Decor Layer", "Resim")) shouldBe 2 // eksen + ızgara
    dipSırası(Seq("Decor Layer", "Decor Layer")) shouldBe 2
  }

  test("arkayaAt'ın dip sırası: pişirme katmanı sırayı ATLATMAZ") {
    // bakeSprite'ın dokusu pişmiş resimlerin dışında saydam; onun altına inmek
    // "arkaya at"ın ta kendisi. Bir ara burada 1 dönüyordu ve arkayaAt eksenler
    // kapalıyken düğümü pişmiş resimlerin önüne taşıyordu.
    dipSırası(Seq("Bake Layer", "Resim")) shouldBe 0
  }

  test("arkayaAt'ın dip sırası: aradaki süs katmanı sayılmaz") {
    // yalnız BAŞTAKİ süs atlanır; arada kalan biri sırayı kaydırmaz
    dipSırası(Seq("Bake Layer", "Decor Layer", "Resim")) shouldBe 0
  }

  test("öneAl'ın tepe sırası: kaplumbağa yoksa sona eklenir") {
    // hiç kaplumbağa katmanı yok -> uzunluk, yani addChildAt sona ekler
    tepeSırası(Seq(false, false, false)) shouldBe 3
    tepeSırası(Seq.empty) shouldBe 0
  }

  test("öneAl'ın tepe sırası: sondaki kaplumbağa katmanlarının ALTI") {
    tepeSırası(Seq(false, false, true)) shouldBe 2
    tepeSırası(Seq(false, true, true)) shouldBe 1 // iki kaplumbağa
    tepeSırası(Seq(true, true)) shouldBe 0        // hepsi kaplumbağa
  }

  test("öneAl'ın tepe sırası: aradaki kaplumbağa sırayı kaydırmaz") {
    // yalnız SONDAKİ öbek sayılır; ortada kalan biri düğümü aşağı itmez --
    // dipSırası'nın "yalnız baştaki süs atlanır" kuralının simetriği
    tepeSırası(Seq(true, false, false)) shouldBe 3
    tepeSırası(Seq(false, true, false)) shouldBe 3
  }

  test("çırpınma sigortası: art arda geri alma eşiği aşınca pişirme kapanır") {
    shouldDisableAfterUnbake(0) shouldBe false
    shouldDisableAfterUnbake(maxUnbakeStreak - 1) shouldBe false
    shouldDisableAfterUnbake(maxUnbakeStreak) shouldBe true
    shouldDisableAfterUnbake(maxUnbakeStreak + 3) shouldBe true
  }
}
