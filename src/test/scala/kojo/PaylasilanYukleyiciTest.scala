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

import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * ÜRÜN YOLU PIXI'nin deprecation uyarısını doğurmuyor mu (#158).
 *
 * NEDEN AYRI BİR SINAMA. #158, `AssetLoader`'ın paylaşılan Loader'ı
 * `PIXI.loader` yerine `PixiUyum.paylaşılanYükleyici` üzerinden almasını
 * sağlıyor. Takımın geri kalanı bunu KORUMUYOR: uyarı yalnız konsola
 * düşüyor, hiçbir sav ona bakmıyor, yani biri yarın `Pixi.loader`'a geri
 * dönse her şey yeşil kalırdı.
 *
 * TUZAK -- BU SAV BOŞ YERE YEŞİL YANABİLİR. PIXI aynı iletiyi SAYFA BAŞINA
 * BİR KEZ basıyor (`deprecation()` içindeki `warnings[message]` kapısı,
 * ölçüldü: aynı adı 1 kez okuyunca 1 kayıt, 3 kez okuyunca yine 1). Kusurlu
 * kodda uyarıyı ilk `AssetLoader` dokunuşu çoktan harcamış olurdu ve buradaki
 * "kayıt yok" savı hiçbir şey kanıtlamadan geçerdi.
 *
 * O yüzden ikinci sav bir DENETİM: eski adı bilerek okuyup kaydın GELDİĞİNİ
 * savlıyor. Böylece
 *   - düzeltilmiş kodda: ürün yolu sessiz (1. sav), alet canlı (2. sav),
 *   - kusurlu kodda: ya 1. sav uyarıyı yakalar, ya da uyarı çoktan
 *     harcandığı için 2. sav "kayıt gelmedi" diye düşer.
 * İki yönde de kırmızı yanıyor; sessizce geçemiyor.
 *
 * SIRA ÖNEMLİ: denetim savı uyarı bütçesini HARCIYOR, o yüzden ürün savından
 * SONRA geliyor (ScalaTest bildirim sırasını koruyor).
 *
 * KANCA `console.warn`A DEĞİL `console.groupCollapsed`A: PIXI iletiyi
 * groupCollapsed ile basıyor, yığın izini ayrıca warn'a yolluyor. Yalnız
 * warn'a bakan bir kanca iletiyi KAÇIRIR -- #158 turunda iki kişi arka arkaya
 * bu tuzağa düştü. İkisi de dinleniyor.
 */
class PaylasilanYukleyiciTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  // Kurduğumuz dünyayı ve çiziciyi geride bırakmayalım (komşu sınamaların
  // kuralı; #91'in sızıntı kültürü).
  override def afterAll(): Unit =
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))

  /**
   * Kancayı kurar, gövdeyi koşturur, kancayı SÖKER; "PixiJS Deprecation"
   * geçen kayıtları döndürür.
   *
   * KAPSAM SINIRI: kanca gövde biter bitmez sökülüyor, yani yalnız EŞZAMANLI
   * okumaları görüyor. Bugün doğru: uyarı, `AssetLoader` nesnesinin ilk
   * dokunuşunda (`private val loader = ...`) eşzamanlı olarak doğuyor.
   * Yükleyiciyi SONRADAN (geri çağrıda, zamanlayıcıda) okuyan bir gerileme
   * bu kancanın dışında kalır ve sav onu göremez.
   */
  private def kayıtlarıTopla[T](gövde: () => T): (T, List[String]) = {
    val konsol = js.Dynamic.global.console
    val kayıt = scala.collection.mutable.ListBuffer.empty[String]
    val eskiGroup = konsol.groupCollapsed
    val eskiWarn = konsol.warn
    def kanca(eski: js.Dynamic): js.Function = { (args: js.Array[js.Any]) =>
      val m = args.map(a => String.valueOf(a)).mkString(" ")
      if (m.contains("PixiJS Deprecation")) kayıt += m.take(120)
      eski.asInstanceOf[js.Dynamic].applyDynamic("apply")(konsol, args)
    }
    // js.Function1[js.Array, _] doğrudan varargs'a oturmuyor; ...rest ile sarıyoruz.
    val sar = js.eval("(function (f) { return function () { return f(Array.prototype.slice.call(arguments)); }; })")
      .asInstanceOf[js.Function1[js.Function, js.Dynamic]]
    konsol.groupCollapsed = sar(kanca(eskiGroup))
    konsol.warn = sar(kanca(eskiWarn))
    try {
      val t = gövde()
      (t, kayıt.toList)
    }
    finally {
      konsol.groupCollapsed = eskiGroup
      konsol.warn = eskiWarn
    }
  }

  private def bekle(ms: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    window.setTimeout(() => söz.success(()), ms.toDouble)
    söz.future
  }

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"
      kap.style.width = "400px"
      kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"
      kap.appendChild(tuval)
      document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  test("ürün yolu sessiz: dünya + kaplumbağa -> AssetLoader, deprecation kaydı yok (#158)") {
    if (!PixiUyum.beşVeÜstü) cancel("PIXI 4: deprecation kapısı yok, sav anlamsız")
    val (_, kayıt) = kayıtlarıTopla { () =>
      implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
      // Kaplumbağa simgesi AssetLoader.addAndLoad'dan geçiyor (Turtle.scala:465),
      // yani paylaşılan Loader burada okunuyor.
      new Turtle(0, 0)
    }
    withClue(s"kayıtlar: ${kayıt.mkString(" | ")} -- ") {
      kayıt shouldBe empty
    }
    bekle(300).map(_ => succeed)
  }

  test("DENETİM: eski adı okumak kayıt doğuruyor -- yukarıdaki sıfır boş değil (#158)") {
    if (!PixiUyum.beşVeÜstü) cancel("PIXI 4: deprecation kapısı yok, sav anlamsız")
    val (_, kayıt) = kayıtlarıTopla { () =>
      // BİLEREK eski ad: bu savın tek işi aletin çalıştığını ve uyarı
      // bütçesinin harcanmamış olduğunu göstermek.
      js.Dynamic.global.PIXI.loader
    }
    withClue("eski adı okuduk ama kayıt gelmedi: ya kanca çalışmıyor, ya uyarıyı ÜRÜN KODU çoktan harcadı -- ") {
      kayıt should not be empty
    }
    succeed
  }
}
