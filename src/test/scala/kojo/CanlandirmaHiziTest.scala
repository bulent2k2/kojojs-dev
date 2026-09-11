package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * `canlandırmaHızınıKur` / `setAnimationDelay` girdisinin savı.
 *
 * KUSUR NEYDİ: eksi bir gecikme (örneğin `canlandırmaHızınıKur(-1)`) kaplumbağayı
 * KALICI olarak donduruyordu. Zincir şöyle:
 *
 *   1. `Turtle.realForward` yalnız `animationDelay == 0` için canlandırmayı
 *      atlıyor; -1 bu kestirmeye takılmıyor ve canlandırma yolu seçiliyor.
 *   2. `TurtleHelper.delayFor(n, -1)` `< 1` dalına girip -1'i OLDUĞU GİBİ
 *      döndürüyor (aşağıda çivileniyor).
 *   3. `frac = geçenSüre / -1` her karede eksi çıkıyor, yani `frac > 1` bitiş
 *      koşulu hiç sağlanmıyor: `queueHandler` bir daha zamanlanmıyor.
 *
 * Ölçüldü (2026-09, PIXI 5 + Chromium): aDelay=-1 için ilk dört karede
 * frac = -7.5, -24.1, -74.1, -90.9 ve hepsinde bitiş koşulu false; ekran
 * 1. saniyeden 6. saniyeye kadar bayt bayt aynı kaldı. Tuvalde yalnız
 * dışarı taşan turuncu bir geçici çizgi kalıyordu.
 *
 * DÜZELTME: masaüstü Kojo ile aynı davranış -- eksi gecikme reddediliyor
 * (net.kogics.kojo.turtle.Turtle.setAnimationDelay). Hata komutun çağrıldığı
 * yerde, kuyruğa girmeden atılıyor; böylece düzenleyicideki sonuç çerçevesi
 * onu yakalayıp öğrenciye yazdırabiliyor.
 */
class CanlandirmaHiziTest extends AnyFunSuite with Matchers {

  // -- 1. Kusurun saf çekirdeği: delayFor eksi değeri süzmüyor ---------------

  test("delayFor 1'den küçük gecikmeyi olduğu gibi döndürüyor -- eksi de dahil") {
    TurtleHelper.delayFor(100, 0) shouldBe 0
    // Donmanın kaynağı tam burası: -1 girdi, -1 çıktı.
    TurtleHelper.delayFor(100, -1) shouldBe -1
    TurtleHelper.delayFor(100, -1000) shouldBe -1000
  }

  test("delayFor pozitif gecikmeyi uzaklığa göre ölçekliyor") {
    TurtleHelper.delayFor(100, 10) shouldBe 10 // 100 adım = tam gecikme
    TurtleHelper.delayFor(50, 10) shouldBe 5
    TurtleHelper.delayFor(200, 10) shouldBe 20
  }

  // -- 2. Koruma: eksi gecikme kuyruğa hiç girmiyor --------------------------

  test("eksi canlandırma hızı hemen hata veriyor, kaplumbağa donmuyor") {
    implicit val kojoWorld = new TestKojoWorld()
    val t = new Turtle(0, 0)
    val hata = intercept[IllegalArgumentException] {
      t.setAnimationDelay(-1)
    }
    hata.getMessage should include("eksi olamaz")
    // Öğrenciye ne yapacağı da söyleniyor.
    hata.getMessage should include("0")
    // Gecikme değişmedi: geçersiz girdi durumu bozmuyor.
    t.animationDelayMs shouldBe 1000L
  }

  test("0 ve pozitif değerler kabul ediliyor") {
    implicit val kojoWorld = new TestKojoWorld()
    val t = new Turtle(0, 0)
    noException should be thrownBy t.setAnimationDelay(0)
    noException should be thrownBy t.setAnimationDelay(1)
    noException should be thrownBy t.setAnimationDelay(1000)
  }

  // -- 3. Kılavuzdaki hız çizelgesi ----------------------------------------

  /** setSpeed'in gecikmeye çevirdiği değeri KAYDEDEN saplama. Gerçek Turtle
    * komutu kuyruğa atıyor; buradaki soru "hangi sayıya çevriliyor", o da
    * RichTurtleCommands'ın kendi eşlemesi. */
  private class HızKaydedici extends RichTurtleCommands {
    var son: Long = -99
    def turn(angle: Double): Unit = ()
    def forward(n: Double): Unit = ()
    def hop(n: Double): Unit = ()
    def setAnimationDelay(i: Long) = son = i
    def arc2(r: Double, a: Double): Unit = ()
    def clear(): Unit = ()
    def invisible(): Unit = ()
  }

  test("hız adlarının gecikme karşılıkları -- çokHızlı 0, 1 DEĞİL") {
    val k = new HızKaydedici
    // Kılavuzdaki (kilavuz/komutlar/02-kaplumbaga-komutlari.md) çizelge
    // birebir bu dört satırdan geliyor.
    k.setSpeed(Speed.superFast); k.son shouldBe 0
    k.setSpeed(Speed.fast); k.son shouldBe 10
    k.setSpeed(Speed.medium); k.son shouldBe 100
    k.setSpeed(Speed.slow); k.son shouldBe 1000
  }
}
