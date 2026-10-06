/*
 * Copyright (C) 2026 Bülent Başaran <bulent2k2@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * readln / satıroku / sayıOku / kesirOku: tarayıcının `window.prompt` penceresi.
 *
 *  - Öntanımlı metin BOŞ olmalı. Eskiden "Type here" gerçek bir değerdi ve cep
 *    tarayıcısında seçili gelmiyordu: oyuncu önce onu silmek zorundaydı.
 *  - İptal ve sayı olmayan girdide Türkçe katman Türkçe ileti veriyor;
 *    İngilizce katman (readln/readInt) eskisi gibi kalıyor.
 */
class OkumaTest extends AnyFunSuite with Matchers {

  private def yeni() = {
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    new kojo.syntax.Builtins()
  }

  // window.prompt'u geçici olarak değiştirir; görülen (istem, öntanımlı) çiftini döndürür.
  private def pencereyle[T](cevap: Option[String])(f: => T): (T, List[(String, String)]) = {
    val pencere = js.Dynamic.global.window
    val eski    = pencere.prompt
    var gorulen = List.empty[(String, String)]
    pencere.prompt = ((istem: String, onta: String) => {
      gorulen = gorulen :+ ((istem, onta))
      cevap.orNull
    }): js.Function2[String, String, String]
    try (f, gorulen)
    finally pencere.prompt = eski
  }

  private def iletisi(f: => Any): String =
    try { f; "FIRLATILMADI" }
    catch { case e: RuntimeException => e.getMessage }

  test("readln: öntanımlı metin boş, istem olduğu gibi iletiliyor") {
    val b          = yeni()
    val (sonuc, g) = pencereyle(Some("merhaba"))(b.readln("Adın?"))
    sonuc shouldBe "merhaba"
    g shouldBe List(("Adın?", ""))
  }

  test("satıroku / sayıOku / kesirOku: öntanımlı metin boş") {
    val b = yeni()
    pencereyle(Some("x"))(b.trTurtle.satıroku("a"))._2 shouldBe List(("a", ""))
    pencereyle(Some("1"))(b.trTurtle.sayıOku("b"))._2 shouldBe List(("b", ""))
    pencereyle(Some("1"))(b.trTurtle.kesirOku("c"))._2 shouldBe List(("c", ""))
  }

  test("geçerli girdi: sayıOku ve kesirOku değeri veriyor") {
    val b = yeni()
    pencereyle(Some("42"))(b.trTurtle.sayıOku("?"))._1 shouldBe 42
    pencereyle(Some("2.5"))(b.trTurtle.kesirOku("?"))._1 shouldBe 2.5
  }

  test("sayılar kırpılarak okunur (#201): cep klavyesinin eklediği boşluk hata vermez") {
    val b = yeni()
    pencereyle(Some("5 "))(b.trTurtle.sayıOku("?"))._1 shouldBe 5
    pencereyle(Some(" 7"))(b.trTurtle.sayıOku("?"))._1 shouldBe 7
    pencereyle(Some(" 2.5 "))(b.trTurtle.kesirOku("?"))._1 shouldBe 2.5
    pencereyle(Some("5 "))(b.readInt("?"))._1 shouldBe 5
    pencereyle(Some(" 2.5 "))(b.readDouble("?"))._1 shouldBe 2.5
  }

  test("satıroku / readln kırpmaz: yazıda boşluk anlamlı") {
    val b = yeni()
    pencereyle(Some(" a b "))(b.trTurtle.satıroku("?"))._1 shouldBe " a b "
    pencereyle(Some(" a b "))(b.readln("?"))._1 shouldBe " a b "
  }

  test("yalnız boşluk hâlâ hata; ileti girileni olduğu gibi (boşluklarıyla) gösteriyor") {
    val b = yeni()
    pencereyle(Some("   "))(iletisi(b.trTurtle.sayıOku("?")))._1 shouldBe "Sayı bekleniyordu: \"   \""
    pencereyle(Some("5 x"))(iletisi(b.trTurtle.kesirOku("?")))._1 shouldBe "Sayı bekleniyordu: \"5 x\""
    pencereyle(Some("   "))(try { b.readInt("?"); false } catch { case _: NumberFormatException => true })._1 shouldBe true
  }

  test("iptal: Türkçe katman Türkçe, İngilizce katman eskisi gibi") {
    val b = yeni()
    pencereyle(None)(iletisi(b.trTurtle.satıroku("?")))._1 shouldBe "Okuma iptal edildi."
    pencereyle(None)(iletisi(b.trTurtle.sayıOku("?")))._1 shouldBe "Okuma iptal edildi."
    pencereyle(None)(iletisi(b.trTurtle.kesirOku("?")))._1 shouldBe "Okuma iptal edildi."
    pencereyle(None)(iletisi(b.readln("?")))._1 shouldBe "Read failed."
  }

  test("sayı olmayan girdi: Türkçe ileti girileni gösteriyor") {
    val b = yeni()
    pencereyle(Some("abc"))(iletisi(b.trTurtle.sayıOku("?")))._1 shouldBe "Sayı bekleniyordu: \"abc\""
    pencereyle(Some("x1"))(iletisi(b.trTurtle.kesirOku("?")))._1 shouldBe "Sayı bekleniyordu: \"x1\""
    pencereyle(Some(""))(iletisi(b.trTurtle.sayıOku("?")))._1 shouldBe "Sayı bekleniyordu: \"\""
  }
}
