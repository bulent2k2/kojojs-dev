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
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js
import scala.scalajs.js.typedarray.{Float32Array, Uint8Array}

import pixiscalajs.PIXI

/**
 * #147'nin 1. ADIMI: NON_ZERO dolguyu HİÇ ÜÇGENLEMEDEN, stencil tamponuyla
 * çizmek bugünkü libtess yoluyla aynı pikselleri veriyor mu -- ve ne kadar
 * sürüyor?
 *
 * YÖNTEM (OpenGL'in klasik "kesişen çokgen dolgusu"):
 *   1. YELPAZE, stencil'e: çokgenin ilk köşesini merkez alıp ardışık her
 *      kenar için bir üçgen (p0, p_i, p_{i+1}); renk yazma kapalı, stencil
 *      önyüzde INCR_WRAP, arkayüzde DECR_WRAP. Sonunda her pikselin stencil
 *      değeri o noktanın SARIM SAYISI (mod 256).
 *   2. KAPLAMA: şeklin sınır kutusu kadar bir dörtgen, stencilFunc NOTEQUAL 0
 *      -- NON_ZERO tam olarak bu. Geçen piksellerde stencilOp ZERO: temizlik
 *      aynı geçişte. Doku dolgusu dörtgenin dört köşesindeki UV'den ara
 *      değerleniyor; eşleme afin olduğu için Graphics'in beginTextureFill'i
 *      ile aynı sonuç (MeshUvSondaTest'in gerekçesi).
 *
 * REFERANS bugünkü yol: Üçgenleyici.nonzero (libtess) + üçgen başına
 * drawPolygon. İki yol `readPixels` ile piksel piksel karşılaştırılıyor.
 *
 * SONUÇ (SwiftShader, PIXI 5.3.12, MSAA'lı tuval; hoşgörü 8/255):
 * {{{
 *   şekil / boya                         boyalı    farklı piksel   en büyük kanal farkı
 *   gül 60x7, düz                        44 624    28  (%0.06)     127 (kenar)
 *   gül 250x7, düz                       61 832    40  (%0.06)      64
 *   L (kesişmeyen içbükey)               25 600     0               0
 *   doğrusal / merkezden / dalgalı grad. 44 624    28-29 (%0.06)   64-127
 *   merkezden, dönüştürülmüş düğüm       44 098    29  (%0.07)      64
 *   yarı saydam (alfa 0.4)               44 624    28  (%0.06)      51
 *   beş köşeli yıldız                    merkez beşgen iki yolda da DOLU (NON_ZERO)
 * }}}
 * Fark yalnız silüet kenarında (kenar yumuşatma örnekleme farkı), boyalı
 * alanın binde altısından az. DENETİM: aynı düzenek çift-tek kuralıyla
 * (INVERT) yıldızın ortasını boş bırakıyor ve gülde %7.3 ayrışıyor -- yani
 * karşılaştırma sarım kuralını gerçekten ölçüyor.
 *
 * SÜRE (şekil başına kurulum + render + gl.finish, ısınmış, 9 ölçümün
 * ortancası, sıra dönüşümlü):
 * {{{
 *   nokta    A libtess + drawPolygon    B stencil        oran
 *    250     19-34 ms                   0.1-0.3 ms       ~100-160x
 *   1000    127-132 ms                  0.3-0.6 ms       ~280-430x
 * }}}
 * Oran bir sayı değil bir MERTEBE: iki ölçümde 75-430 arası okundu (#152
 * incelemesi kendi koşusunda 75 / 233), aralığın kendisi 5 kat oynuyor.
 * Sağlam okunuşu "iki mertebe". B'nin bedeli nokta sayısıyla doğrusal ve
 * GPU'da; A'nın bedeli libtess'in kesişme sayısıyla karesele yakın büyüyen
 * CPU işi. Oran gerçek donanımda küçülür (libtess orada ~5x hızlı, #143'ün
 * canlı sayıları) ama sınıfı değişmez.
 *
 * SARIM ARİTMETİĞİ (#152 incelemesi §2): tablodaki güller, L ve boya
 * varyantları sarım kuralını değil boyaları ve dönüşümleri tutuyor -- arkayüz
 * DECR yerine INCR yapılsa (ters sarım iptal etmese) onların hepsi yine yeşil
 * kalır, çünkü yelpaze birleşimi NON_ZERO bölgesiyle çakışıyor; yalnız yıldız
 * ayrışıyordu, o da geometrisi yüzünden. Kanonik sınama ayrıca var: ters
 * yönlerde iki kez sarılan bölge NON_ZERO'da delik, iptalsiz aritmetikte
 * dolu (`iptalsiz` denetimi).
 *
 * ARA TAMPONLAR. PİŞİRME (#96): RenderTexture'ın varsayılan çerçeve
 * tamponunda stencil YOK, ve sonuç sessiz bir kusur: stencil sınaması hep
 * geçer, kaplama dörtgeni SINIR KUTUSUNUN TAMAMINI boyar (%32 fark). Çare
 * düğümün içinde tek satır: `renderer.framebuffer.forceStencil()` --
 * PIXI'nin maske sistemi de bunu yapıyor; onunla %0.02, pişirme koduna
 * dokunmadan. SÜZGEÇ (Soluk): ara tampon ölçümde stencil'li geldi, orada
 * sorun yok; ama süzgeç ara tamponu düğümün SINIRLARINDAN boyutluyor ve
 * çocuksuz Container'ın sınırı boş -- `_calculateBounds` verilmeden süzgeç
 * altında hiçbir şey çizilmedi. Üretim düğümü sınır vermek zorunda.
 *
 * Bu bir SONDA: üretim kodu (`Turtle`) hâlâ libtess çiziyor; sav "teknik
 * işliyor" diyor. Kapsamadıkları: PIXI'nin kendi maskeleriyle (stencil
 * yığını) bir arada çalışma -- iKojo maske kullanmıyor --, parti (batch)
 * kırılmasının çok şekilli sahnedeki bedeli, gerçek GPU, 8 bitlik sarım
 * taşması (|sarım| = 256).
 */
class StencilDolguSondaTest extends AnyFunSuite with Matchers {

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
      val w = new KojoWorldImpl()
      val gl = dyn(w.renderer).gl
      val öz = gl.getContextAttributes()
      if (!öz.stencil.asInstanceOf[Boolean]) cancel("bağlamda stencil tamponu yok")
      w
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def dyn(o: Any): js.Dynamic = o.asInstanceOf[js.Dynamic]
  private val P = js.Dynamic.global.PIXI

  /** Kendini kesen gül: nokta köşe, kat sarım. */
  private def gülDüz(nokta: Int, kat: Int, yarıçap: Double): Array[Double] = {
    val a = new Array[Double](nokta * 2)
    var i = 0
    while (i < nokta) {
      val açı = i * kat * 2 * math.Pi / nokta
      a(2 * i) = yarıçap * math.cos(açı); a(2 * i + 1) = yarıçap * math.sin(açı)
      i += 1
    }
    a
  }

  // ---- A: bugünkü yol ----
  private def grafikDüğüm(düz: Array[Double], boya: Boya): js.Dynamic = {
    val ü = Üçgenleyici.nonzero(düz)
    val gr = new PIXI.Graphics()
    PixiUyum.boyamayaBaşla(gr, boya)(() => ())
    var i = 0
    while (i + 5 < ü.length) {
      gr.drawPolygon(js.Array(ü(i), ü(i + 1), ü(i + 2), ü(i + 3), ü(i + 4), ü(i + 5)))
      i += 6
    }
    gr.endFill()
    dyn(gr)
  }

  // ---- B: stencil ----
  private val yelpazeVert =
    """attribute vec2 aVertexPosition;
      |uniform mat3 projectionMatrix; uniform mat3 translationMatrix;
      |void main(){ gl_Position = vec4((projectionMatrix * translationMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0); }""".stripMargin
  private val boşFrag = "void main(){ gl_FragColor = vec4(0.0); }"
  private val düzFrag = "uniform vec4 uColor; void main(){ gl_FragColor = uColor; }"
  private val dokuVert =
    """attribute vec2 aVertexPosition; attribute vec2 aUv;
      |uniform mat3 projectionMatrix; uniform mat3 translationMatrix;
      |varying vec2 vUv;
      |void main(){ gl_Position = vec4((projectionMatrix * translationMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0); vUv = aUv; }""".stripMargin
  private val dokuFrag = "precision mediump float; varying vec2 vUv; uniform sampler2D uSampler; void main(){ gl_FragColor = texture2D(uSampler, vUv); }"

  private lazy val yelpazeShader = P.Shader.from(yelpazeVert, boşFrag, js.Dynamic.literal())
  private lazy val düzShader = P.Shader.from(yelpazeVert, düzFrag, js.Dynamic.literal(uColor = js.Array(1.0, 1.0, 1.0, 1.0)))
  private lazy val dokuShader = P.Shader.from(dokuVert, dokuFrag, js.Dynamic.literal())
  private lazy val durum = { val s = js.Dynamic.newInstance(P.State)(); s.blend = true; s.culling = false; s.depthTest = false; s }

  /**
   * Stencil dolgulu düğüm: kurulum O(n), üçgenleme yok.
   *
   * @param evenOdd DENETİM: INVERT ile çift-tek kuralı. Kendini kesen şekilde
   *                NON_ZERO'dan farklı sonuç vermeli; vermiyorsa karşılaştırma
   *                sarım kuralını ölçmüyor demektir.
   * @param iptalsiz DENETİM (#152 incelemesi §2): arkayüz de INCR_WRAP, yani
   *                 ters yönlü sarım İPTAL ETMİYOR -- stencil sarım sayısı
   *                 değil kesişim sayısı olur. Güllerde ve L'de fark
   *                 çıkmıyor (yelpaze birleşimi NON_ZERO bölgesiyle
   *                 çakışıyor); ayıran şekil ters sarımlı bölge.
   */
  private def stencilDüğüm(düz: Array[Double], boya: Boya, evenOdd: Boolean = false, zorla: Boolean = true,
                           iptalsiz: Boolean = false): js.Dynamic = {
    val n = düz.length / 2
    // Yelpaze: (p0, p_i, p_{i+1}), i = 1 .. n-2 -> 3(n-2) köşe.
    val yelpaze = new Float32Array(3 * 2 * (n - 2))
    var i = 1; var k = 0
    while (i < n - 1) {
      yelpaze(k) = düz(0).toFloat; yelpaze(k + 1) = düz(1).toFloat
      yelpaze(k + 2) = düz(2 * i).toFloat; yelpaze(k + 3) = düz(2 * i + 1).toFloat
      yelpaze(k + 4) = düz(2 * i + 2).toFloat; yelpaze(k + 5) = düz(2 * i + 3).toFloat
      i += 1; k += 6
    }
    var x0 = Double.MaxValue; var y0 = Double.MaxValue; var x1 = Double.MinValue; var y1 = Double.MinValue
    i = 0
    while (i < n) {
      val x = düz(2 * i); val y = düz(2 * i + 1)
      if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
      i += 1
    }
    // Kaplama: sınır kutusu, iki üçgen; bir piksel pay (kenar örnekleri).
    x0 -= 1; y0 -= 1; x1 += 1; y1 += 1
    val kutu = new Float32Array(12)
    val kx = js.Array(x0, x1, x1, x0, x1, x0); val ky = js.Array(y0, y0, y1, y0, y1, y1)
    i = 0
    while (i < 6) { kutu(2 * i) = kx(i).toFloat; kutu(2 * i + 1) = ky(i).toFloat; i += 1 }

    val yelpazeGeo = js.Dynamic.newInstance(P.Geometry)()
    yelpazeGeo.addAttribute("aVertexPosition", yelpaze, 2)
    val kutuGeo = js.Dynamic.newInstance(P.Geometry)()
    kutuGeo.addAttribute("aVertexPosition", kutu, 2)

    val (kaplamaShader, kaplamaGeo) = boya match {
      case DüzBoya(renk) =>
        val rgb = renk.toRGBDouble.toInt; val a = renk.alpha.get
        val r = ((rgb >> 16) & 0xff) / 255.0; val g = ((rgb >> 8) & 0xff) / 255.0; val b = (rgb & 0xff) / 255.0
        val sh = P.Shader.from(yelpazeVert, düzFrag, js.Dynamic.literal(uColor = js.Array(r * a, g * a, b * a, a)))
        (sh, kutuGeo)
      case DokuBoya(doku, matris, _) =>
        // UV yalnız dört köşede, ara değerleme afin: yerel -> doku pikseli -> 0..1.
        val m = dyn(matris); val dokuEn = dyn(doku).width.asInstanceOf[Double]; val dokuBoy = dyn(doku).height.asInstanceOf[Double]
        val uv = new Float32Array(12); val nokta = js.Dynamic.newInstance(P.Point)(0, 0)
        i = 0
        while (i < 6) {
          m.applyInverse(js.Dynamic.literal(x = kx(i), y = ky(i)), nokta)
          uv(2 * i) = (nokta.x.asInstanceOf[Double] / dokuEn).toFloat
          uv(2 * i + 1) = (nokta.y.asInstanceOf[Double] / dokuBoy).toFloat
          i += 1
        }
        kutuGeo.addAttribute("aUv", uv, 2)
        val sh = P.Shader.from(dokuVert, dokuFrag, js.Dynamic.literal(uSampler = doku.asInstanceOf[js.Any]))
        (sh, kutuGeo)
    }

    val d = js.Dynamic.newInstance(P.Container)()
    // SINIRLAR: çocuksuz Container'ın sınırı boş, ve süzgeç ara tamponunun
    // boyutu sınırlardan geliyor -- sınır boşsa süzgeç hiçbir şey çizmiyor
    // (ölçüldü: süzgeç altında boyalı 0). Üretimde de gerekli: isabet
    // eleme, pişirme ve süzgeç hepsi getBounds'a bakıyor.
    val bx0 = x0; val by0 = y0; val bx1 = x1; val by1 = y1
    val sınırla: js.Function0[Unit] = () => { d._bounds.addFrame(d.transform, bx0, by0, bx1, by1); () }
    d._calculateBounds = sınırla
    val çiz: js.Function1[js.Dynamic, Unit] = (renderer: js.Dynamic) => {
      renderer.batch.flush()
      val gl = renderer.gl
      val dünya = d.worldTransform.toArray(true)
      renderer.state.set(durum)
      // 1. Yelpaze -> stencil (sarım sayısı). Renk yazılmıyor.
      yelpazeShader.uniforms.translationMatrix = dünya
      renderer.shader.bind(yelpazeShader)
      renderer.geometry.bind(yelpazeGeo, yelpazeShader)
      // O anki çerçeve tamponunda stencil eki yoksa TAK: RenderTexture'lar
      // (pişirme) ve süzgeç ara tamponları stencil'siz doğuyor, ve stencil
      // eki olmayan tamponda stencil sınaması HEP GEÇER -- kaplama sınır
      // kutusunun tamamını boyar. PIXI'nin kendi maske sistemi de aynı
      // çağrıyı yapıyor (StencilSystem.push). Ana tuvalde (current == null)
      // hiçbir şey yapmıyor; orası zaten stencil'li.
      if (zorla) renderer.framebuffer.forceStencil()
      gl.enable(gl.STENCIL_TEST)
      gl.stencilMask(0xff)
      gl.colorMask(false, false, false, false)
      gl.stencilFunc(gl.ALWAYS, 0, 0xff)
      if (evenOdd) gl.stencilOp(gl.KEEP, gl.KEEP, gl.INVERT)
      else if (iptalsiz) gl.stencilOp(gl.KEEP, gl.KEEP, gl.INCR_WRAP)
      else {
        gl.stencilOpSeparate(gl.FRONT, gl.KEEP, gl.KEEP, gl.INCR_WRAP)
        gl.stencilOpSeparate(gl.BACK, gl.KEEP, gl.KEEP, gl.DECR_WRAP)
      }
      renderer.geometry.draw(gl.TRIANGLES)
      // 2. Kaplama: sarım != 0 olan pikseller boyanır, stencil'leri sıfırlanır.
      gl.colorMask(true, true, true, true)
      gl.stencilFunc(gl.NOTEQUAL, 0, 0xff)
      gl.stencilOp(gl.KEEP, gl.KEEP, gl.ZERO)
      kaplamaShader.uniforms.translationMatrix = dünya
      renderer.shader.bind(kaplamaShader)
      renderer.geometry.bind(kaplamaGeo, kaplamaShader)
      renderer.geometry.draw(gl.TRIANGLES)
      gl.disable(gl.STENCIL_TEST)
      ()
    }
    d._render = çiz
    d
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

  /** (farklı piksel, en büyük kanal farkı, boş olmayan piksel) */
  private def karşılaştır(a: Uint8Array, b: Uint8Array, hoşgörü: Int): (Int, Int, Int) = {
    var farklı = 0; var enBüyük = 0; var dolu = 0; var i = 0
    while (i < a.length) {
      if (a(i + 3) != 0 || b(i + 3) != 0) dolu += 1
      var k = 0; var pf = false
      while (k < 4) { val d = math.abs(a(i + k) - b(i + k)); if (d > enBüyük) enBüyük = d; if (d > hoşgörü) pf = true; k += 1 }
      if (pf) farklı += 1
      i += 4
    }
    (farklı, enBüyük, dolu)
  }

  private def dönüştürülmüş(d: js.Dynamic): js.Dynamic = { d.position.set(37, -23); d.scale.set(1.7, 0.6); d.rotation = 0.4; d }

  private def sonda(ad: String, düz: Array[Double], boya: Boya, dönüşümlü: Boolean = false, eşikYüzde: Double = 2.0): (Int, Int, Int) = {
    val w = dünyaKurYaDaİptal()
    def hazırla(d: js.Dynamic) = if (dönüşümlü) dönüştürülmüş(d) else d
    val a = sahneyeKoyVeOku(w, hazırla(grafikDüğüm(düz, boya)))
    val b = sahneyeKoyVeOku(w, hazırla(stencilDüğüm(düz, boya)))
    val (fark, enBüyük, dolu) = karşılaştır(a, b, hoşgörü = 8)
    val oran = 100.0 * fark / math.max(1, dolu)
    info(s"$ad: boyalı $dolu, farklı $fark (yüzde ${(oran * 100).round / 100.0}), en büyük kanal farkı $enBüyük")
    withClue(s"$ad -- boyalı $dolu, farklı $fark (yüzde ${(oran * 100).round / 100.0}), en büyük $enBüyük -- ") {
      dolu should be > 1000
      oran should be < eşikYüzde
    }
    (fark, enBüyük, dolu)
  }

  test("kendini kesen gül (60x7), düz renk: stencil dolgu libtess dolgusuyla aynı pikseller") {
    sonda("gül 60x7 düz", gülDüz(60, 7, 120.0), DüzBoya(kojo.doodle.Color.blue))
  }

  test("beş köşeli yıldız: NON_ZERO -- ortadaki beşgen DOLU (earcut/even-odd boş bırakırdı)") {
    val w = dünyaKurYaDaİptal()
    val düz = gülDüz(5, 2, 100.0)
    val a = sahneyeKoyVeOku(w, grafikDüğüm(düz, DüzBoya(kojo.doodle.Color.red)))
    val b = sahneyeKoyVeOku(w, stencilDüğüm(düz, DüzBoya(kojo.doodle.Color.red)))
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]; val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val merkez = 4 * ((boy / 2) * en + en / 2)
    val (fark, enBüyük, dolu) = karşılaştır(a, b, 8)
    withClue(s"merkez alfa: libtess ${a(merkez + 3)}, stencil ${b(merkez + 3)}; farklı $fark / $dolu -- ") {
      a(merkez + 3).toInt should be > 0 // referansın kendisi NON_ZERO: beşgen dolu
      b(merkez + 3).toInt should be > 0 // stencil de
      (100.0 * fark / dolu) should be < 2.0
    }
  }

  test("250x7 gül, düz renk (bugünkü örneklerin ölçeği)") {
    sonda("gül 250x7 düz", gülDüz(250, 7, 140.0), DüzBoya(kojo.doodle.Color.blue))
  }

  test("kesişmeyen içbükey çokgen (L): yelpaze kesişmeyen şekli de doğru dolduruyor") {
    val düz = Array[Double](-100, -100, 100, -100, 100, -20, -20, -20, -20, 100, -100, 100)
    sonda("L", düz, DüzBoya(kojo.doodle.Color.green))
  }

  test("doğrusal gradyan: kaplama dörtgeninin UV'si beginTextureFill ile aynı") {
    sonda("doğrusal gradyan", gülDüz(60, 7, 120.0),
      Boya.doğrusal(-120, -120, kojo.doodle.Color.red, 120, 120, kojo.doodle.Color.blue, dalgalıDevam = false))
  }

  test("merkezden gradyan, DÖNÜŞTÜRÜLMÜŞ düğümde (taşıma + eşit olmayan ölçek + dönme)") {
    sonda("merkezden gradyan, dönüşümlü", gülDüz(60, 7, 120.0),
      Boya.merkezden(0, 0, kojo.doodle.Color.yellow, 120, kojo.doodle.Color.green, dalgalıDevam = false), dönüşümlü = true)
  }

  test("kısa dalgalı gradyan: uv [0,1] dışına çıkınca sarma kipi aynı") {
    sonda("kısa dalgalı gradyan", gülDüz(60, 7, 120.0),
      Boya.doğrusal(-30, 0, kojo.doodle.Color.red, 30, 0, kojo.doodle.Color.blue, dalgalıDevam = true))
  }

  test("yarı saydam düz renk: ön çarpımlı alfa Graphics ile aynı") {
    sonda("yarı saydam", gülDüz(60, 7, 120.0), DüzBoya(kojo.doodle.Color.blue.alpha(0.4)))
  }

  test("DENETİM: çift-tek (INVERT) varyantı yıldızın ortasını BOŞ bırakıyor ve gülde ayrışıyor") {
    val w = dünyaKurYaDaİptal()
    val yıldız = gülDüz(5, 2, 100.0)
    val a = sahneyeKoyVeOku(w, grafikDüğüm(yıldız, DüzBoya(kojo.doodle.Color.red)))
    val e = sahneyeKoyVeOku(w, stencilDüğüm(yıldız, DüzBoya(kojo.doodle.Color.red), evenOdd = true))
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]; val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val merkez = 4 * ((boy / 2) * en + en / 2)
    val gül = gülDüz(60, 7, 120.0)
    val ga = sahneyeKoyVeOku(w, grafikDüğüm(gül, DüzBoya(kojo.doodle.Color.blue)))
    val ge = sahneyeKoyVeOku(w, stencilDüğüm(gül, DüzBoya(kojo.doodle.Color.blue), evenOdd = true))
    val (fark, _, dolu) = karşılaştır(ga, ge, 8)
    val oran = 100.0 * fark / math.max(1, dolu)
    info(s"çift-tek: yıldız merkezi alfa ${e(merkez + 3)} (libtess ${a(merkez + 3)}); gül 60x7 fark yüzde ${(oran * 100).round / 100.0}")
    withClue(s"yıldız merkezi: libtess ${a(merkez + 3)}, çift-tek ${e(merkez + 3)}; gül farkı yüzde $oran -- ") {
      a(merkez + 3).toInt should be > 0
      e(merkez + 3).toInt shouldBe 0 // çift-tek: merkez iki kez sarılı -> boş
      // Ölçüldü: 7.3 -- 60x7 gülde iki kez sarılı bölgeler boyalı alanın ~%7'si.
      // NON_ZERO'nun 0.06'sının 100 katından fazla: karşılaştırma sarım
      // kuralını gerçekten ayırt ediyor.
      oran should be > 3.0
    }
  }

  /**
   * SARIM ARİTMETİĞİNİN KANONİK SINAMASI (#152 incelemesi §2): aynı bölge
   * ters yönlerde iki kez sarılıyor -- dış kare CCW, içinde köprüyle
   * bağlanmış bir kare CW. Tek kontur; kaplumbağa da böyle bir yol çizebilir
   * (dışa çık, ters yönde ilmek at, geri dön). NON_ZERO'da iç bölgenin
   * sarımı +1 - 1 = 0, yani DELİK; iptal etmeyen aritmetik (iptalsiz: iki
   * yüz de INCR) orayı 2 sayar ve DOLDURUR. Güller ve L bu ayrımı
   * göstermiyordu: onlarda yelpaze üçgenlerinin birleşimi NON_ZERO
   * bölgesiyle çakışıyor. Bu sav sarım kuralını tesadüfen değil tasarımla
   * tutuyor; libtess referansı da delikli olmalı, yoksa sav iki yanlışı
   * karşılaştırır.
   */
  test("TERS SARIM: dış CCW + iç CW ilmek -> NON_ZERO'da delik; iptalsiz aritmetik doldurur") {
    val w = dünyaKurYaDaİptal()
    // dış kare CCW, köprü (-100,-100)->(-50,-50), iç kare CW, köprüden geri.
    val düz = Array[Double](
      -100, -100, 100, -100, 100, 100, -100, 100, -100, -100,
      -50, -50, -50, 50, 50, 50, 50, -50, -50, -50,
      -100, -100)
    val boya = DüzBoya(kojo.doodle.Color.red)
    val a = sahneyeKoyVeOku(w, grafikDüğüm(düz, boya))
    val b = sahneyeKoyVeOku(w, stencilDüğüm(düz, boya))
    val c = sahneyeKoyVeOku(w, stencilDüğüm(düz, boya, iptalsiz = true))
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]; val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val merkez = 4 * ((boy / 2) * en + en / 2)
    val (fB, _, dolu) = karşılaştır(a, b, 8)
    val (fC, _, _) = karşılaştır(a, c, 8)
    val oB = 100.0 * fB / math.max(1, dolu); val oC = 100.0 * fC / math.max(1, dolu)
    info(s"ters sarım: merkez alfa libtess ${a(merkez + 3)}, stencil ${b(merkez + 3)}, iptalsiz ${c(merkez + 3)}; fark yüzde ${(oB * 100).round / 100.0}, iptalsiz yüzde ${(oC * 100).round / 100.0}")
    withClue(s"merkez alfa: libtess ${a(merkez + 3)}, stencil ${b(merkez + 3)}, iptalsiz ${c(merkez + 3)}; fark $fB / iptalsiz $fC, boyalı $dolu -- ") {
      dolu should be > 1000
      a(merkez + 3).toInt shouldBe 0 // referans: NON_ZERO delik bırakıyor
      b(merkez + 3).toInt shouldBe 0 // stencil de
      c(merkez + 3).toInt should be > 0 // iptal etmeyen aritmetik dolduruyor
      oB should be < 2.0
      oC should be > 10.0 // sav bozulmuş sarım aritmetiğini görüyor
    }
  }

  /**
   * PİŞİRME YOLU (#96): durağan resimler bir RenderTexture'a çiziliyor. Onun
   * çerçeve tamponunda stencil var mı? YOK, varsayılanda -- ve sonuç "boş"
   * değil, daha kötüsü: stencil eki olmayan tamponda stencil sınaması hep
   * geçer, kaplama dörtgeni sınır kutusunun TAMAMINI boyar (ölçüldü: 58 564
   * piksel, yüzde 32 fark). Çare düğümün kendi içinde: `forceStencil()` o
   * anki tampona eki takıyor (PIXI'nin maske sistemi de böyle yapıyor); onunla
   * fark yüzde 0.02, pişirme koduna dokunmadan.
   */
  test("RenderTexture: forceStencil ile stencil'siz tamponda da aynı; onsuz KUTU dolar") {
    val w = dünyaKurYaDaİptal()
    val r = dyn(w.renderer)
    val düz = gülDüz(60, 7, 120.0)
    def rtÇizOku(düğüm: js.Dynamic): Uint8Array = {
      val rt = P.RenderTexture.create(js.Dynamic.literal(width = 300, height = 300))
      val kap = js.Dynamic.newInstance(P.Container)()
      düğüm.position.set(150, 150)
      kap.addChild(düğüm)
      r.render(kap, rt, true)
      r.extract.pixels(rt).asInstanceOf[Uint8Array]
    }
    val boya = DüzBoya(kojo.doodle.Color.blue)
    val a = rtÇizOku(grafikDüğüm(düz, boya))
    val b = rtÇizOku(stencilDüğüm(düz, boya))
    val bZorlamasız = rtÇizOku(stencilDüğüm(düz, boya, zorla = false))
    val (fB, _, dolu) = karşılaştır(a, b, 8)
    val (fZ, _, _) = karşılaştır(a, bZorlamasız, 8)
    val oB = 100.0 * fB / math.max(1, dolu); val oZ = 100.0 * fZ / math.max(1, dolu)
    info(s"RenderTexture: forceStencil ile fark yüzde ${(oB * 100).round / 100.0}; onsuz yüzde ${(oZ * 100).round / 100.0}")
    withClue(s"forceStencil'li $fB / onsuz $fZ farklı, boyalı $dolu -- ") {
      dolu should be > 1000
      oB should be < 2.0
      oZ should be > 10.0 // stencil'siz tampon: kutu dolar -- forceStencil şart
    }
  }

  /**
   * SÜZGEÇ YOLU (Soluk, #147'nin "ölçülmemiş riski"): süzgeçli bir kap
   * çocuklarını önce bir ARA tampona çiziyor. Beklenti o tamponun da
   * stencil'siz olmasıydı; ÖLÇÜM ÖYLE DEMEDİ: süzgecin ara dokusu stencil'li
   * geliyor, forceStencil'siz de fark eşik altı. forceStencil orada zararsız
   * (eki varsa hiçbir şey yapmıyor). Asıl bulgu başka çıktı: çocuksuz
   * Container'ın SINIRI BOŞ ve süzgeç ara tamponunu sınırlardan boyutluyor
   * -- `_calculateBounds` olmadan süzgeç altında HİÇBİR ŞEY çizilmedi
   * (boyalı 0). Üretim düğümünün sınır vermesi şart; isabet eleme ve
   * pişirme de aynı sınırlara bakıyor.
   */
  test("SÜZGEÇ altında (ara tampon): sınır verilince aynı; ara tampon zaten stencil'li") {
    val w = dünyaKurYaDaİptal()
    val düz = gülDüz(60, 7, 120.0)
    val boya = DüzBoya(kojo.doodle.Color.blue)
    def süzgeçli(düğüm: js.Dynamic): js.Dynamic = {
      val kap = js.Dynamic.newInstance(P.Container)()
      kap.addChild(düğüm)
      kap.filters = js.Array(js.Dynamic.newInstance(P.filters.AlphaFilter)(1.0))
      kap
    }
    val a = sahneyeKoyVeOku(w, süzgeçli(grafikDüğüm(düz, boya)))
    val b = sahneyeKoyVeOku(w, süzgeçli(stencilDüğüm(düz, boya)))
    val bZ = sahneyeKoyVeOku(w, süzgeçli(stencilDüğüm(düz, boya, zorla = false)))
    val (fB, _, dolu) = karşılaştır(a, b, 8)
    val (fZ, _, _) = karşılaştır(a, bZ, 8)
    val oB = 100.0 * fB / math.max(1, dolu); val oZ = 100.0 * fZ / math.max(1, dolu)
    def boyalı(t: Uint8Array): Int = { var n = 0; var i = 3; while (i < t.length) { if (t(i) != 0) n += 1; i += 4 }; n }
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]; val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val m = 4 * ((boy / 2) * en + en / 2)
    info(s"süzgeç: forceStencil ile fark yüzde ${(oB * 100).round / 100.0}; onsuz yüzde ${(oZ * 100).round / 100.0} | boyalı A ${boyalı(a)} B ${boyalı(b)} Bz ${boyalı(bZ)} | merkez A ${a(m)},${a(m+1)},${a(m+2)},${a(m+3)} B ${b(m)},${b(m+1)},${b(m+2)},${b(m+3)}")
    withClue(s"forceStencil'li $fB / onsuz $fZ farklı, boyalı $dolu -- ") {
      dolu should be > 1000
      oB should be < 2.0
      oZ should be < 2.0 // ölçüldü: süzgecin ara tamponu stencil'li geliyor
    }
  }

  /**
   * SÜRE: şekil başına kurulum + render + gl.finish, ısınmış, ortanca.
   * A: libtess + drawPolygon + Graphics geometri kurulumu + çizim.
   * B: yelpaze dizisi + iki çizim çağrısı.
   */
  test("ZZ ölçüm: 250x7 ve 1000x7'de A (libtess) ve B (stencil) şekil başına süre") {
    val w = dünyaKurYaDaİptal()
    val gl = dyn(w.renderer).gl
    def ölç(düz: Array[Double], yap: () => js.Dynamic, tekrar: Int): Double = {
      val süreler = new Array[Double](tekrar)
      var k = 0
      while (k < tekrar) {
        w.stage.children.toList.foreach(c => w.stage.removeChild(c))
        val t0 = window.performance.now()
        dyn(w.stage).addChild(yap())
        w.renderer.render(w.stage)
        gl.finish()
        süreler(k) = window.performance.now() - t0
        k += 1
      }
      val s = süreler.sorted; s(tekrar / 2)
    }
    val boya = DüzBoya(kojo.doodle.Color.blue)
    val satırlar = for (nokta <- List(250, 1000)) yield {
      val düz = gülDüz(nokta, 7, 140.0)
      // ısınma: shader derlemesi, JIT
      ölç(düz, () => grafikDüğüm(düz, boya), 3); ölç(düz, () => stencilDüğüm(düz, boya), 3)
      val a1 = ölç(düz, () => grafikDüğüm(düz, boya), 9); val b1 = ölç(düz, () => stencilDüğüm(düz, boya), 9)
      val b2 = ölç(düz, () => stencilDüğüm(düz, boya), 9); val a2 = ölç(düz, () => grafikDüğüm(düz, boya), 9)
      f"nokta=$nokta%5d | A libtess ${a1}%.1f / ${a2}%.1f ms | B stencil ${b1}%.2f / ${b2}%.2f ms | oran ${(a1 + a2) / (b1 + b2)}%.1f"
    }
    satırlar.foreach(info(_))
    succeed
  }
}
