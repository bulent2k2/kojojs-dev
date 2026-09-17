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
    isStaleCandidate(gerçekKaplumbağa = false, name = null, interactive = false, lastMut = 0, frame = 10) shouldBe true
  }

  test("yeni/az önce değişen çocuk aday değildir") {
    isStaleCandidate(gerçekKaplumbağa = false, name = null, interactive = false, lastMut = 9, frame = 10) shouldBe false
    isStaleCandidate(gerçekKaplumbağa = false, name = null, interactive = false, lastMut = 10, frame = 10) shouldBe false
  }

  test("GERÇEK kaplumbağa hiç pişmez, ama aynı adlı Resim{} katmanı pişer (#96)") {
    // Ayrım artık ADDA değil bayrakta. Turtle.init "Turtle Layer" adını forPic
    // kaplumbağalara da verdiği için ada bakmak Resim{} katmanlarını da muaf
    // tutuyordu: hiç pişmiyorlardı (ölçüldü: 500 durağan resimde sahne çocuğu
    // 501'de sabit). Bu sav o iki durumu birbirinden ayırıyor -- ikisinin de ADI
    // "Turtle Layer".
    isStaleCandidate(gerçekKaplumbağa = true, name = "Turtle Layer",
      interactive = false, lastMut = 0, frame = 100) shouldBe false
    isStaleCandidate(gerçekKaplumbağa = false, name = "Turtle Layer",
      interactive = false, lastMut = 0, frame = 100) shouldBe true
  }

  test("süs katmanı hiç pişmez") {
    // Pişerse sahneden çıkıp dokuya gömülür; sonraki eksenleriGizle görünür bir
    // etki yapamaz, ekranda hayalet eksen kalır.
    isStaleCandidate(gerçekKaplumbağa = false, name = "Decor Layer",
      interactive = false, lastMut = 0, frame = 100) shouldBe false
  }

  test("etkileşimli düğüm hiç pişmez") {
    isStaleCandidate(gerçekKaplumbağa = false, name = null, interactive = true, lastMut = 0, frame = 100) shouldBe false
  }

  test("hiç damgalanmamış (lastMut çok eski) çocuk adaydır") {
    isStaleCandidate(gerçekKaplumbağa = false, name = null, interactive = false, lastMut = -1, frame = 0) shouldBe false // 0 - (-1) = 1, not > 3
    isStaleCandidate(gerçekKaplumbağa = false, name = null, interactive = false, lastMut = -1, frame = 5) shouldBe true  // 5 - (-1) = 6 > 3
  }

  test("isStaleCheap ucuz ön kontrol: kimlik + durağanlık (etkileşimden bağımsız)") {
    isStaleCheap(gerçekKaplumbağa = false, name = null, lastMut = 0, frame = 10) shouldBe true
    isStaleCheap(gerçekKaplumbağa = true, name = "Turtle Layer", lastMut = 0, frame = 10) shouldBe false
    // ADIN kendisi artık muafiyet vermiyor: bayrak false ise aynı ad pişebilir
    isStaleCheap(gerçekKaplumbağa = false, name = "Turtle Layer", lastMut = 0, frame = 10) shouldBe true
    // süs katmanı (eksen/ızgara) da pişirme dışı: pişerse gizle/göster ölür
    isStaleCheap(gerçekKaplumbağa = false, name = "Decor Layer", lastMut = 0, frame = 10) shouldBe false
    isStaleCheap(gerçekKaplumbağa = false, name = null, lastMut = 9, frame = 10) shouldBe false
  }

  test("gerçek kaplumbağa ölçütü: ad YETMEZ, simge çocuğu şart") {
    // Turtle.init "Turtle Layer" adını forPic kaplumbağalara da veriyor; simgeyi
    // yalnız gerçek kaplumbağaya ekliyor. Ada bakmak iki yeri birden bozuyordu
    // (bkz. gerçekKaplumbağaMı'nın belgesi; sorun #91 erasePictures kolu).
    gerçekKaplumbağaMı("Turtle Layer", Seq("Turtle Fill (in progress)", "Turtle Path", "Turtle Icon")) shouldBe true
    // Resim{} katmanı: aynı ad, simge YOK -> silinebilir
    gerçekKaplumbağaMı("Turtle Layer", Seq("Turtle Fill (in progress)", "Turtle Path")) shouldBe false
    gerçekKaplumbağaMı("Turtle Layer", Seq.empty) shouldBe false
    // başka adlar: simge olsa bile kaplumbağa değil
    gerçekKaplumbağaMı("Resim", Seq("Turtle Icon")) shouldBe false
    gerçekKaplumbağaMı("Decor Layer", Seq("Turtle Icon")) shouldBe false
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
