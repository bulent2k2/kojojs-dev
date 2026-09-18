package kojo

import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * Kumanda kolu SÜRÜKLENEBİLİR mi -- çevresi saydam olsa bile.
 *
 * NEDEN VAR (#110): `ornekler/08-kumanda-kolu.kojo` kolu çiziyordu ama top
 * kımıldamıyordu. Sebep çizimde değil, İSABET SINAMASINDA: örnek çevreyi
 * saydam yapıyor (telefon oyunlarında istenen görünüm), PIXI 5'in isabet
 * sınaması ise görünmeyen dolguyu atlıyor (GraphicsGeometry.containsPoint,
 * fillStyle.visible) ve bizim setFillColor'ımız alpha 0 verilince tam onu
 * yapıyor (Utils.boyayıKur). Sonuç: kol hiç pointer olayı almıyor,
 * currentVector sıfır kalıyor, top durmuyor -- hata sessiz.
 *
 * ÖLÇÜM PIXI'NİN KENDİ MANTIĞIYLA: renderer.plugins.interaction.hitTest.
 * Taklit etmiyoruz, çünkü sıra önemli -- PIXI önce hitArea'ya, sonra
 * containsPoint'e bakıyor; elle yazılmış bir taklit düzeltmeyi göremezdi.
 *
 * DİKKAT: hitTest'e KÖK açıkça verilmeli. Varsayılan kök
 * renderer._lastObjectRendered ve o ancak bir render'dan sonra doluyor;
 * vermeden ölçünce SAĞLAM durum da false çıkıyor (ölçüldü) ve sonda
 * sessizce yalan söylüyor.
 */
class KumandaIsabetTest extends AnyFunSuite with Matchers {

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"
      kap.style.width = "800px"
      kap.style.height = "600px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"
      kap.appendChild(tuval)
      document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  /** Kolun YERELDE (yx, yy) noktasında PIXI'nin bulduğu hedef ÇEVRE mi? */
  private def çevreyiBuluyorMu(w: KojoWorldImpl, kol: JoyStick, yx: Double, yy: Double): Boolean = {
    val d = kol.perimeter.tnode.asInstanceOf[js.Dynamic]
    val sahne = d.parent
    w.renderer.asInstanceOf[js.Dynamic].render(sahne)
    val küresel = d.toGlobal(js.Dynamic.newInstance(js.Dynamic.global.PIXI.Point)(yx, yy))
    val bulunan = w.renderer.asInstanceOf[js.Dynamic].plugins.interaction.hitTest(küresel, sahne)
    !js.isUndefined(bulunan) && bulunan != null &&
    (bulunan.asInstanceOf[js.Any] eq kol.perimeter.tnode.asInstanceOf[js.Any])
  }

  private val saydam = kojo.doodle.Color(0, 0, 0, 0)

  test("varsayılan çevreli kol sürüklenebiliyor (ön koşul)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kol = new JoyStick(60)(new kojo.syntax.Builtins()(w))
    kol.draw()
    çevreyiBuluyorMu(w, kol, 0, 0) shouldBe true
  }

  test("SAYDAM çevreli kol da sürüklenebiliyor (#110)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kol = new JoyStick(60)(new kojo.syntax.Builtins()(w))
    kol.setPerimeterColor(saydam)
    kol.draw()
    çevreyiBuluyorMu(w, kol, 0, 0) shouldBe true
  }

  test("kolun İÇ dairesinin üstünden de çevre bulunuyor") {
    // Merkez, üstte duran control dairesinin altında. control etkileşimli
    // olmadığı için olay ondan GEÇİP çevreye ulaşmalı; ulaşmasaydı
    // kullanıcının tuttuğu yer -- kolun ta ortası -- ölü olurdu.
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kol = new JoyStick(60)(new kojo.syntax.Builtins()(w))
    kol.setPerimeterColor(saydam)
    kol.draw()
    withClue("iç daire yarıçapı 30; oranın altında çevre bulunmalı -- ") {
      çevreyiBuluyorMu(w, kol, 10, 10) shouldBe true
    }
  }

  test("kol taşınınca isabet alanı da taşınıyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kol = new JoyStick(60)(new kojo.syntax.Builtins()(w))
    kol.setPerimeterColor(saydam)
    kol.draw()
    kol.setPosition(-250, -180) // örneğin yaptığı gibi
    withClue("hitArea yerel koordinatlarda; taşınınca onunla gitmeli -- ") {
      çevreyiBuluyorMu(w, kol, 0, 0) shouldBe true
    }
  }

  test("çevrenin DIŞINDA isabet yok (alan şişirilmemiş)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kol = new JoyStick(60)(new kojo.syntax.Builtins()(w))
    kol.draw()
    withClue("yarıçap 60; 100 birim uzakta çevre bulunmamalı -- ") {
      çevreyiBuluyorMu(w, kol, 100, 0) shouldBe false
    }
  }
}
