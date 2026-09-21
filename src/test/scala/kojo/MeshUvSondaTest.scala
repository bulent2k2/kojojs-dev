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

import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import pixiscalajs.PIXI

/**
 * #125'in EN BÜYÜK İŞLEVSEL RİSKİ: mesh'e geçilirse doku/gradyan dolgusu
 * üçgen sınırlarında SÜREKLİ kalır mı?
 *
 * Bugün dolgu `Graphics.beginTextureFill` ile yapılıyor ve eşleme ŞEKLİN
 * YEREL uzayında, ŞEKİL BAŞINA: dolgu üçgenleri umursamıyor, şeklin altından
 * geçen tek bir gradyan/döşeme var. Sürekliliğin sebebi "dünya" olması değil,
 * ÜÇGEN BAŞINA DEĞİL ŞEKİL BAŞINA olması (#132 incelemesi §2; ölçüldü,
 * "dolgu şeklin yereline çakılı" savı). Mesh'in kendi shader'ı ve UV'leri olur, yani süreklilik
 * BEDAVA GELMEZ -- kaydın gövdesi de, #129'un incelemesi de bunu yazdı, ama
 * ikisi de DENEMEDİ. Bu sonda o boşluğu kapatıyor.
 *
 * YÖNTEM: aynı libtess üçgenleri iki yoldan çiziliyor, sonuç `readPixels` ile
 * piksel piksel karşılaştırılıyor. Referans, bugünkü Graphics yolu.
 *
 *   A (referans)  Graphics + PixiUyum.boyamayaBaşla  -- bugünkü kod
 *   B (aday)      Mesh + MeshMaterial(doku), aTextureCoord köşe başına ŞEKLİN
 *                 YEREL uzayından: uv = matris⁻¹ · köşe / (doku.en, doku.boy).
 *                 Düğümün worldTransform'u bu hesaba GİRMİYOR (#132 §2).
 *   C (denetim)   Mesh, ama UV'ler ÜÇGEN BAŞINA 0..1 -- "naif" mesh, yani
 *                 sürekliliğin gerçekten bedava gelmediğini gösteren hâl
 *
 * `matris` dokunun PİKSEL uzayından şeklin yerel uzayına eşliyor (bkz.
 * DokuBoya'nın belgesi), yani tersi yerel -> doku pikseli. Eşleme AFİN
 * olduğu için köşede hesaplanan UV üçgen içinde doğrusal ara değerleniyor:
 * süreklilik matematiksel olarak tam, yaklaşık değil. Sonda bunun PIXI'de de
 * böyle olduğunu ölçüyor.
 *
 * SONUÇ (SwiftShader, 60 nokta x 7 kat gül, 44 624 boyalı piksel):
 * {{{
 *   boya                        YEREL UV        NAİF UV (denetim)
 *   doğrusal gradyan            0 piksel fark   41 975 (%94)
 *   merkezden gradyan           0 piksel fark   40 337 (%90)
 *   kısa dalgalı gradyan        0 piksel fark   41 543 (%93)
 *   döşeme, REPEAT              0 piksel fark   26 463 (%59)
 * }}}
 * DÖNÜŞTÜRÜLMÜŞ düğümde de (taşıma 37,-23 + eşit olmayan ölçek 1.7,0.6 +
 * dönme 0.4 rad; 44 096 boyalı piksel) -- gerçek resimlerin hâli bu:
 * {{{
 *   doğrusal gradyan            0 piksel fark   41 514 (%94)
 *   merkezden gradyan           0 piksel fark   40 084 (%91)
 * }}}
 * Dört boyada da fark SIFIR PİKSEL, en büyük kanal farkı SIFIR -- yaklaşık
 * değil, bit birebir, ve iki koşuda da aynı. Yani #125'in "süreklilik bedava
 * gelmez" uyarısı doğru (naif UV gerçekten bozuyor) ama bedeli küçük:
 * köşe başına tek bir `matris.applyInverse`.
 *
 * UV HESABININ BEDELİ (düğüm kurulumu, render hariç; 41 ölçümün ortancası):
 * {{{
 *   2 998 üçgen    Graphics 0.4-0.5 ms   Mesh+yerelUV 0.3 ms   Mesh+naifUV 0.2 ms
 *  11 998 üçgen    Graphics 1.5-1.6 ms   Mesh+yerelUV 1.0 ms   Mesh+naifUV 0.7 ms
 * }}}
 * Yani UV ~0.1 ms (2 998) ve ~0.3 ms (11 998) ekliyor, ve mesh UV'lerle
 * BİRLİKTE bile Graphics'ten ucuz kuruluyor. (Üç koşudan biri gürültülüydü --
 * 11 998'de 3.9/3.5 ms okudu; ötekiler yukarıdaki değerlerde anlaştı.)
 *
 * BU SAVIN KAPSAMADIKLARI -- üçü de gerçek:
 *  - ÜRETİM KODUNU korumuyor. Mesh yolu burada, sınamada kuruluyor; `Turtle`
 *    hâlâ Graphics çiziyor. Bu sav "teknik işliyor" diyor, "kod bozulmadı"
 *    demiyor. Geçiş yapılırsa aynı karşılaştırma üretim düğümüne bakmalı.
 *  - `Boya.dokuma`nın YÜKLEME dansı. Doku bir dosyadan eşzamansız geldiğinde
 *    `boyamayaBaşla` yer tutucu bir dokuyla başlayıp yükleme bitince
 *    `fillStyle`ları yamıyor (#40). O yama Graphics'e özgü; mesh'te karşılığı
 *    YOK ve yazılmadı. Buradaki döşeme savı yüklenmiş dokuyu taklit ediyor.
 *  - Gerçek GPU. SwiftShader'da ölçüldü.
 *  - Kenar yumuşatma farkı. Fark sıfır çıktığı için hoşgörü hiç devreye
 *    girmedi; başka bir sürücüde girebilir.
 */
class MeshUvSondaTest extends AnyFunSuite with Matchers {

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try dünyaKur()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
    new KojoWorldImpl()
  }

  private def dyn(o: Any): js.Dynamic = o.asInstanceOf[js.Dynamic]

  /** Kendini kesen gül -- üçgen sınırları şeklin İÇİNDE sık geçsin diye. */
  private def gülDüz(nokta: Int, kat: Int, yarıçap: Double): Array[Double] = {
    val a = new Array[Double](nokta * 2)
    var i = 0
    while (i < nokta) {
      val açı = i * kat * 2 * math.Pi / nokta
      a(2 * i) = yarıçap * math.cos(açı)
      a(2 * i + 1) = yarıçap * math.sin(açı)
      i += 1
    }
    a
  }

  // ---- A: bugünkü yol ----
  private def grafikDüğüm(ü: Array[Double], boya: Boya): PIXI.Graphics = {
    val gr = new PIXI.Graphics()
    PixiUyum.boyamayaBaşla(gr, boya)(() => ())
    var i = 0
    while (i + 5 < ü.length) {
      gr.drawPolygon(js.Array(ü(i), ü(i + 1), ü(i + 2), ü(i + 3), ü(i + 4), ü(i + 5)))
      i += 6
    }
    gr.endFill()
    gr
  }

  // ---- B / C: mesh ----
  /**
   * @param yerelUv true ise UV ŞEKLİN YEREL uzayından (aday yol); false ise
   *                üçgen başına 0..1 (naif denetim).
   *
   * YEREL, DÜNYA DEĞİL. Bu ayrım kritik ve depoda üç yerde yanlış yazılmıştı
   * (#132 incelemesi §2): eşlemeye düğümün `worldTransform`u HİÇ girmiyor.
   * "Dünya uzayında" diye okuyan biri UV hesabına worldTransform'u katar ve
   * resim her taşındığında dolgusu YÜZEN bir mesh elde eder. Doğru tarif
   * koddaki hâli: yerel köşe -> `matris.applyInverse`, başka hiçbir şey.
   * Ölçüldü, aşağıdaki "dolgu şeklin yereline çakılı" savı.
   */
  private def meshDüğüm(ü: Array[Double], boya: DokuBoya, yerelUv: Boolean): js.Dynamic = {
    val P = js.Dynamic.global.PIXI
    val n = ü.length / 2
    val köşeler = new js.typedarray.Float32Array(ü.length)
    val uvler = new js.typedarray.Float32Array(ü.length)
    // matris: doku pikseli -> yerel. Bize tersi gerekiyor, o da applyInverse.
    val m = dyn(boya.matris)
    val dokuEn = dyn(boya.doku).width.asInstanceOf[Double]
    val dokuBoy = dyn(boya.doku).height.asInstanceOf[Double]
    val nokta = js.Dynamic.newInstance(P.Point)(0, 0)
    var i = 0
    while (i < n) {
      val x = ü(2 * i)
      val y = ü(2 * i + 1)
      köşeler(2 * i) = x.toFloat
      köşeler(2 * i + 1) = y.toFloat
      if (yerelUv) {
        m.applyInverse(js.Dynamic.literal(x = x, y = y), nokta)
        uvler(2 * i) = (nokta.x.asInstanceOf[Double] / dokuEn).toFloat
        uvler(2 * i + 1) = (nokta.y.asInstanceOf[Double] / dokuBoy).toFloat
      }
      else {
        // Naif: her üçgenin üç köşesi (0,0) (1,0) (0,1)
        val k = i % 3
        uvler(2 * i) = if (k == 1) 1f else 0f
        uvler(2 * i + 1) = if (k == 2) 1f else 0f
      }
      i += 1
    }
    val geometri = js.Dynamic.newInstance(P.Geometry)()
    geometri.addAttribute("aVertexPosition", köşeler, 2)
    geometri.addAttribute("aTextureCoord", uvler, 2)
    val malzeme = js.Dynamic.newInstance(P.MeshMaterial)(boya.doku.asInstanceOf[js.Any])
    js.Dynamic.newInstance(P.Mesh)(geometri, malzeme)
  }

  private def sahneyeKoyVeOku(w: KojoWorldImpl, düğüm: js.Dynamic): Uint8Array = {
    w.stage.children.toList.foreach(c => w.stage.removeChild(c))
    dyn(w.stage).addChild(düğüm)
    w.renderer.render(w.stage)
    val gl = dyn(w.renderer).gl
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]
    val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val tampon = new Uint8Array(en * boy * 4)
    gl.readPixels(0, 0, en, boy, gl.RGBA, gl.UNSIGNED_BYTE, tampon)
    tampon
  }

  /** (farklı piksel sayısı, en büyük kanal farkı, boş olmayan piksel sayısı) */
  private def karşılaştır(a: Uint8Array, b: Uint8Array, hoşgörü: Int): (Int, Int, Int) = {
    var farklı = 0
    var enBüyük = 0
    var dolu = 0
    var i = 0
    while (i < a.length) {
      if (a(i + 3) != 0 || b(i + 3) != 0) dolu += 1
      var k = 0
      var pikselFarklı = false
      while (k < 4) {
        val d = math.abs(a(i + k) - b(i + k))
        if (d > enBüyük) enBüyük = d
        if (d > hoşgörü) pikselFarklı = true
        k += 1
      }
      if (pikselFarklı) farklı += 1
      i += 4
    }
    (farklı, enBüyük, dolu)
  }

  /**
   * Düğümü gerçek bir `Resim{}` gibi taşı/döndür/ölçekle.
   *
   * Eşit OLMAYAN ölçek bilerek: A'nın dolgu matrisi çizim anında, B'nin
   * UV'leri geometri kurulurken pişiyor, yani eşdeğerliğin dönüşüme duyarlı
   * olması akla yakın bir kırılma yeriydi (#132 incelemesi §1). Dönüştürülmemiş
   * düğüm göçün göndereceği durum değil -- iKojo'da her resim taşınıyor.
   */
  private def dönüştürülmüş(d: js.Dynamic): js.Dynamic = {
    d.position.set(37, -23)
    d.scale.set(1.7, 0.6)
    d.rotation = 0.4
    d
  }

  private def sonda(ad: String, boya: DokuBoya, dönüşümlü: Boolean = false): Unit = {
    val w = dünyaKurYaDaİptal()
    val düz = gülDüz(60, 7, 120.0)
    val ü = Üçgenleyici.nonzero(düz)
    withClue(s"$ad: libtess üçgen vermedi -- ") { ü.length should be > 0 }

    def hazırla(d: js.Dynamic): js.Dynamic = if (dönüşümlü) dönüştürülmüş(d) else d
    val a = sahneyeKoyVeOku(w, hazırla(dyn(grafikDüğüm(ü, boya))))
    val b = sahneyeKoyVeOku(w, hazırla(meshDüğüm(ü, boya, yerelUv = true)))
    val c = sahneyeKoyVeOku(w, hazırla(meshDüğüm(ü, boya, yerelUv = false)))

    val (bFark, bEnBüyük, bDolu) = karşılaştır(a, b, hoşgörü = 8)
    val (cFark, cEnBüyük, _) = karşılaştır(a, c, hoşgörü = 8)
    val bOran = 100.0 * bFark / math.max(1, bDolu)
    val cOran = 100.0 * cFark / math.max(1, bDolu)

    val bYüzde = (bOran * 100).round / 100.0
    val cYüzde = (cOran * 100).round / 100.0
    withClue(
      s"$ad -- boyalı piksel $bDolu; " +
        s"YEREL UV: $bFark farklı (yüzde $bYüzde, en büyük kanal farkı $bEnBüyük); " +
        s"NAİF UV: $cFark farklı (yüzde $cYüzde, en büyük $cEnBüyük) -- "
    ) {
      // Ön koşul: gerçekten bir şey çizilmiş olmalı, yoksa "hepsi aynı" boş
      // iki siyah kareyi karşılaştırmaktan gelir.
      bDolu should be > 1000
      // ASIL SORU: yerel-uzay UV'li mesh bugünkü Graphics yolunu tutuyor mu?
      // Kenar yumuşatma iki yolda birebir aynı olmak zorunda değil, o yüzden
      // eşik boyalı alanın %2'si.
      bOran should be < 2.0
      // DENETİM: naif (üçgen başına) UV bunu YAPAMAMALI. Yapabiliyorsa
      // karşılaştırma bir şey ölçmüyor demektir.
      cOran should be > 10.0
    }
  }

  /**
   * `Boya.dokuma`nın yüklenmiş hâlinin elle kurulmuş dengi: REPEAT sarmalı bir
   * döşeme dokusu, matris yalnız öteleme.
   *
   * `Boya.dokuma`nın KENDİSİ kullanılamıyor: dokuyu bir dosyadan eşzamansız
   * yüklüyor ve bu konteynerde ağ yok. Yani bu sav REPEAT sarmasını ve öteleme
   * matrisini kapsıyor, `dokuma`nın YÜKLEME dansını değil -- o dans
   * (yer tutucu doku + fillStyle yamalama) Graphics'e özgü ve mesh'te
   * karşılığı yok; bkz. aşağıdaki "kapsamadıkları".
   */
  private def döşemeBoyası(x: Double, y: Double): DokuBoya = {
    val P = js.Dynamic.global.PIXI
    val c = document.createElement("canvas").asInstanceOf[org.scalajs.dom.html.Canvas]
    c.width = 32; c.height = 32
    val ctx = c.getContext("2d").asInstanceOf[js.Dynamic]
    ctx.fillStyle = "#3060c0"; ctx.fillRect(0, 0, 32, 32)
    ctx.fillStyle = "#f0c020"; ctx.fillRect(0, 0, 16, 16); ctx.fillRect(16, 16, 16, 16)
    val taban = js.Dynamic.newInstance(P.BaseTexture)(
      c.asInstanceOf[js.Any],
      js.Dynamic.literal(mipmap = P.MIPMAP_MODES.OFF, wrapMode = P.WRAP_MODES.REPEAT))
    val m = new PIXI.Matrix()
    m.translate(x, y)
    DokuBoya(js.Dynamic.newInstance(P.Texture)(taban).asInstanceOf[PIXI.Texture], m,
      kojo.doodle.Color.gray)
  }

  test("DÖŞEME dokusu (REPEAT) da sürekli: üçgen sınırlarında ek yeri yok (#125)") {
    sonda("döşeme (dokuma dengi)", döşemeBoyası(-7, 11))
  }

  test("DÖNÜŞTÜRÜLMÜŞ düğümde de bit birebir: taşıma + eşit olmayan ölçek + dönme (#132)") {
    // Gerçek resimlerin hâli bu. Kırılma yeri akla yakındı: A'nın dolgu
    // matrisi çizim anında uygulanıyor, B'nin UV'leri geometri kurulurken
    // pişiyor -- dönüşüm ikisine farklı girseydi eşdeğerlik burada bozulurdu.
    sonda("doğrusal gradyan, dönüşümlü",
      Boya.doğrusal(-120, -120, kojo.doodle.Color.red, 120, 120, kojo.doodle.Color.blue,
        dalgalıDevam = false).asInstanceOf[DokuBoya], dönüşümlü = true)
  }

  test("merkezden gradyan, DÖNÜŞTÜRÜLMÜŞ düğümde (#132)") {
    sonda("merkezden gradyan, dönüşümlü",
      Boya.merkezden(0, 0, kojo.doodle.Color.yellow, 120, kojo.doodle.Color.green,
        dalgalıDevam = false).asInstanceOf[DokuBoya], dönüşümlü = true)
  }

  /**
   * Dolgu ŞEKLİN YEREL uzayına çakılı mı, dünyaya mı? (#132 incelemesi §2)
   *
   * Depo üç yerde "dünya uzayında" yazıyordu; `DokuBoya`'nın kendi belgesi ise
   * "doku pikselinden ŞEKLİN YEREL koordinatına" diyor. İkisi aynı anda doğru
   * olamaz, ve fark salt terminoloji değil: göçü yapan kişi "dünya" okursa UV
   * hesabına `worldTransform`u katar ve resim taşındıkça dolgusu YÜZER.
   *
   * AYIRT EDİCİ DENEY: döşeme dokusunu (32 px) yalnız x'te 7 px kaydır --
   * döşemenin tam katı DEĞİL, yoksa iki hâl ayırt edilemez. Sonra kaydırılmamış
   * görüntünün (x,y) pikselini kaydırılmışın (x+7,y) pikseliyle karşılaştır.
   *
   *   YEREL ise: desen şekille birlikte taşınır, görüntü SAF ÖTELEME, fark 0.
   *   DÜNYA ise: desen yerinde kalır, şekil üstünden kayar, fark büyük.
   */
  test("dolgu ŞEKLİN YEREL uzayına çakılı, dünyaya DEĞİL (#132)") {
    val w = dünyaKurYaDaİptal()
    val ü = Üçgenleyici.nonzero(gülDüz(60, 7, 120.0))
    val boya = döşemeBoyası(0, 0)
    val kaydırma = 7

    val yerinde = sahneyeKoyVeOku(w, dyn(grafikDüğüm(ü, boya)))
    val kaymış = sahneyeKoyVeOku(w, {
      val d = dyn(grafikDüğüm(ü, boya)); d.position.set(kaydırma, 0); d
    })

    val en = dyn(w.renderer).view.width.asInstanceOf[Int]
    val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    var farklı = 0
    var dolu = 0
    var y = 0
    while (y < boy) {
      var x = 0
      while (x < en - kaydırma) {
        val i = 4 * (y * en + x)
        val j = 4 * (y * en + x + kaydırma)
        if (yerinde(i + 3) != 0 || kaymış(j + 3) != 0) dolu += 1
        var k = 0
        var pf = false
        while (k < 4) { if (math.abs(yerinde(i + k) - kaymış(j + k)) > 8) pf = true; k += 1 }
        if (pf) farklı += 1
        x += 1
      }
      y += 1
    }
    val oran = 100.0 * farklı / math.max(1, dolu)
    withClue(s"boyalı $dolu, farklı $farklı (yüzde ${(oran * 100).round / 100.0}) -- ") {
      dolu should be > 1000
      // Saf öteleme: desen şekille geldi -> YEREL.
      oran should be < 2.0
    }
  }

  test("gradyan dolgusu mesh'te de SÜREKLİ: yerel-uzay UV Graphics yolunu tutuyor (#125)") {
    sonda("doğrusal gradyan",
      Boya.doğrusal(-120, -120, kojo.doodle.Color.red, 120, 120, kojo.doodle.Color.blue,
        dalgalıDevam = false).asInstanceOf[DokuBoya])
  }

  test("merkezden gradyan da sürekli (#125)") {
    sonda("merkezden gradyan",
      Boya.merkezden(0, 0, kojo.doodle.Color.yellow, 120, kojo.doodle.Color.green,
        dalgalıDevam = false).asInstanceOf[DokuBoya])
  }

  test("TEKRARLAYAN gradyan: uv [0,1] dışına çıkınca da sürekli (#125)") {
    // Gradyanı şekilden KISA yapıyoruz: uv 1'i aşıyor, sarma kipi devreye
    // giriyor. Graphics yolunda wrapMode ÇİVİLİ (Boya.dokuYap); mesh yolunda
    // aynı BaseTexture kullanıldığı için aynı kip geçerli olmalı -- bu sav
    // onu da sınıyor.
    sonda("kısa dalgalı gradyan",
      Boya.doğrusal(-30, 0, kojo.doodle.Color.red, 30, 0, kojo.doodle.Color.blue,
        dalgalıDevam = true).asInstanceOf[DokuBoya])
  }
}
