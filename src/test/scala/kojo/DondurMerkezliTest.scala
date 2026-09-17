package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

/**
 * `döndürMerkezli` DÖNÜŞTÜRÜCÜ olarak da çalışmalı.
 *
 * KUSUR NEYDİ: ikojo'da `döndürMerkezli` yalnız Resim YÖNTEMİ olarak vardı
 * (`r.döndürMerkezli(45, 0, 0)`). Masaüstündeki yazılımcıklar ise onu
 * dönüştürücü olarak kullanıyor -- `*` ile zincirlenip `->` ile uygulanan
 * biçimde:
 *
 * {{{
 * çiz(götür(-30, -200) * döndürMerkezli(-90, 0, 0) -> Resim.yazı(...))
 * }}}
 *
 * (ornekler/masaustu/.../samples/tr/unit-circle.kojo:105; aynı biçim
 * hunted.kojo ve tangram-skier.kojo'da da geçiyor.) Bu biçim olmadan her
 * satır üç satıra bölünmek zorundaydı: dez r = ...; r.çiz(); r.döndürMerkezli(...).
 *
 * Sınamanın derdi yalnız "ad var mı" değil: üçüncü sav MERKEZİN GERÇEKTEN
 * KULLANILDIĞINI çiviliyor. O olmasa `kb.rotp` yerine yanlışlıkla `kb.rot`
 * yazılsa bile ilk iki sav geçerdi.
 */
class DondurMerkezliTest extends AsyncFunSuite with Matchers {
  implicit val kojoWorld: KojoWorld = new TestKojoWorld()
  val builtins = new kojo.syntax.Builtins()
  import builtins._
  import trTurtle._
  // Tür ExecutionContext DEĞİL, ExecutionContextExecutor: Türkçe önsöz de bir
  // örtük ExecutionContext taşıyor (GelecekYöntemleri.küreselİşletimBağlamı),
  // ikisi aynı türde olunca "ambiguous implicit values" ile derlenmiyor.
  // Daha özel tür bu çakışmayı çözüyor.
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  val pay = 0.5

  def sınırlar(r: Resim): Future[(Double, Double, Double, Double)] = {
    r.çiz()
    r.ready.map { _ =>
      val b = r.bounds
      (b.x, b.y, b.width, b.height)
    }(executionContext)
  }

  def eşit(ad: String, a: (Double, Double, Double, Double), b: (Double, Double, Double, Double)) = {
    withClue(s"$ad sol: ") { a._1 should be(b._1 +- pay) }
    withClue(s"$ad alt: ") { a._2 should be(b._2 +- pay) }
    withClue(s"$ad en: ") { a._3 should be(b._3 +- pay) }
    withClue(s"$ad boy: ") { a._4 should be(b._4 +- pay) }
  }

  test("dönüştürücü biçimi, yöntem biçimiyle aynı yere koyuyor") {
    val dönüştürücüyle = döndürMerkezli(90, 100, 0) -> Resim.dikdörtgen(50, 30)
    val yöntemle = Resim.dikdörtgen(50, 30)
    for {
      a <- sınırlar(dönüştürücüyle)
      b <- { yöntemle.döndürMerkezli(90, 100, 0); sınırlar(yöntemle) }
    } yield eşit("dönüştürücü/yöntem", a, b)
  }

  test("`*` ile zincirleniyor -- unit-circle.kojo:105'teki biçim") {
    val zincir = götür(-30, -200) * döndürMerkezli(-90, 0, 0) -> Resim.dikdörtgen(50, 30)
    val elle = götür(-30, -200) -> (döndürMerkezli(-90, 0, 0) -> Resim.dikdörtgen(50, 30))
    for {
      a <- sınırlar(zincir)
      b <- sınırlar(elle)
    } yield eşit("zincir", a, b)
  }

  test("merkez gerçekten kullanılıyor -- döndür ile aynı DEĞİL") {
    // (0,0) çevresinde dönmek `döndür` ile aynı; (100,0) çevresinde dönmek
    // resmi başka yere taşımalı. Bu sav olmasaydı kb.rot'a delege etmek de
    // sınamayı geçerdi.
    for {
      merkezli <- sınırlar(döndürMerkezli(90, 100, 0) -> Resim.dikdörtgen(50, 30))
      düz <- sınırlar(döndür(90) -> Resim.dikdörtgen(50, 30))
    } yield withClue(s"merkezli=$merkezli düz=$düz: ") {
      (math.abs(merkezli._1 - düz._1) + math.abs(merkezli._2 - düz._2)) should be > 1.0
    }
  }
}
