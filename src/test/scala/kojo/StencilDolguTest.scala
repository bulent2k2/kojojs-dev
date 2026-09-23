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
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import pixiscalajs.PIXI

/**
 * #147'nin ÜRETİM SAVLARI (ölçüt 3): `Turtle` Eşik'i aşan dolguları artık
 * `StencilDolgu` düğümüne yazıyor; libtess + Graphics yolu yedek olarak
 * duruyor ve `KojoWorldImpl.stencilDolgu` anahtarıyla seçiliyor. Sonda
 * (`StencilDolguSondaTest`) tekniğin çıplak düğümde çalıştığını gösterdi; bu
 * dosya Turtle'ın içinden geçen yolu, yani bağları çiviliyor:
 *
 *  - PİKSEL DENKLİĞİ: aynı `Resim{}` betiği iki yolda çizilip readPixels ile
 *    karşılaştırılıyor -- düz renk, gradyan, ters sarımlı delik.
 *  - İSABET: `içindeMi` sarım sayısı -- gülün ortası tıklanır, sınır
 *    kutusunun köşesi tıklanmaz, ters sarımlı delik tıklanmaz.
 *  - PİŞİRME TAMPONU: stencil'siz doğan RenderTexture'da düğüm kendi
 *    `forceStencil`iyle doğru çiziyor (kutu dolmuyor).
 *  - `boyalı`: dolgu yeniden renkleniyor / gradyana geçiyor, geometri kalıyor.
 *  - `Boya.dokuma`: doku dosyadan sonradan gelince düğüm dokuya geçiyor (#40).
 *  - `sil()` sonrası artık yok: aynı yere başka şekil, eski dolgu pikselleri boş.
 *  - EŞİK: 64 ve altı libtess'te kalıyor (parti kırılması ölçümü, StencilDolgu belgesi).
 *  - BİLİNEN SINIR, 8 bitlik sarım (#153 incelemesi): 255 ve 257 kat temiz,
 *    256 kat koca bölge boş -- ayırıcı imza; sınır bilerek çivili, "düzeltildi"
 *    diye değil "böyle" diye. Düğüm doğrudan (pompasız), libtess referansına karşı.
 *
 * GL BIRAKMA burada değil: `KaynakSizintisiTest`in 180 köşeli şekilleri artık
 * kendiliğinden stencil yolundan geçiyor, tavan savları (managedGeometries /
 * managedBuffers / gradyan dokusu) orada aynen çalışıyor.
 *
 * MUTASYON NOTU (denendi): arkayüz DECR yerine INCR yapılırsa (iptal etmeyen
 * sarım) "TERS SARIM Resim{}" piksel savı kırmızı yanıyor, güller yanmıyor
 * (sondanın bulgusu). İsabet savları o mutasyonu GÖRMEZ: `içindeMi` GL'den
 * bağımsız, kendi sarım sayısını hesaplıyor -- onları kıran şey `içindeMi`nin
 * aritmetiği. Eşik kaldırılırsa "EŞİK" savı yanar.
 *
 * ZAMANLAMA: kaplumbağa çizimi eşzamansız (pompa); savlar kare bekliyor ve
 * KURULUM SAVI olarak düğümün nokta sayısını okuyor -- çizim bitmeden ölçmek
 * "boş" sonucu dolgudan değil yokluğundan verir.
 */
class StencilDolguTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dyn(o: Any): js.Dynamic = o.asInstanceOf[js.Dynamic]
  private val P = js.Dynamic.global.PIXI

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
      val w = new KojoWorldImpl()
      if (!w.stencilDolgu) cancel("bağlamda stencil tamponu yok (ya da PIXI 4)")
      w
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  /** Anahtara bakmadan kurar (anahtarın kendisini sınamak için). */
  private def dünyaKurYaDaİptalHam(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def kareler(n: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def adım(): Unit = { i += 1; if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => adım()) }
    window.requestAnimationFrame(_ => adım())
    söz.future
  }

  // ---- şekiller (kaplumbağa moveTo ile; nokta sayısı bilinçli) ----

  /** Kendini kesen gül: nokta köşe, kat sarım, ilk köşeye dönüşle kapanıyor. */
  private def gül(t: Turtle, nokta: Int, kat: Int, r: Double): Unit = {
    // Kalem kalkık taşınma çokgeni sıfırlıyor: ilk köşe (0,0) değil p0 olsun.
    t.penUp(); t.setPosition(r, 0); t.penDown()
    var i = 1
    while (i <= nokta) {
      val a = (i % nokta) * kat * 2 * math.Pi / nokta
      t.moveTo(r * math.cos(a), r * math.sin(a))
      i += 1
    }
  }

  /**
   * Ters sarım: dış kare CCW, köprü, iç kare CW, köprüden geri -- sondanın
   * kanonik şekli; kenarlar `adım` parçaya bölünmüş ki nokta sayısı Eşik'i
   * aşsın (başlangıç + 8 kenar x adım + 2 köprü = 83 nokta, adım 10'da).
   */
  private def tersSarım(t: Turtle, adım: Int): Unit = {
    def kenar(x0: Double, y0: Double, x1: Double, y1: Double): Unit = {
      var i = 1
      while (i <= adım) { t.moveTo(x0 + (x1 - x0) * i / adım, y0 + (y1 - y0) * i / adım); i += 1 }
    }
    t.penUp(); t.setPosition(-100, -100); t.penDown()
    kenar(-100, -100, 100, -100); kenar(100, -100, 100, 100); kenar(100, 100, -100, 100); kenar(-100, 100, -100, -100)
    t.moveTo(-50, -50)
    kenar(-50, -50, -50, 50); kenar(-50, 50, 50, 50); kenar(50, 50, 50, -50); kenar(50, -50, -50, -50)
    t.moveTo(-100, -100)
  }

  private def resim(boya: Boya, kalem: Boolean = false)(şekil: Turtle => Unit)(implicit w: KojoWorld): TurtlePicture =
    TurtlePicture { t =>
      t.setAnimationDelay(0)
      if (!kalem) t.setPenThickness(0)
      t.setFillPaint(boya)
      şekil(t)
    }

  // ---- okuma ----

  private def stencilDüğümler(p: TurtlePicture): Seq[StencilDolgu] =
    dyn(p.tnode).children.asInstanceOf[js.Array[js.Dynamic]].toSeq
      .filter(c => c.kojoStencilDolgu.asInstanceOf[js.UndefOr[Boolean]].contains(true))
      .map(_.asInstanceOf[StencilDolgu]).filter(_.noktaSayısı > 0)

  /** Dolgu taşıyan Graphics parçaları: biten şekil ("Turtle Fill") ya da büyümekte olan (boyamaYolu). */
  private def grafikDolgular(p: TurtlePicture): Seq[js.Dynamic] =
    dyn(p.tnode).children.asInstanceOf[js.Array[js.Dynamic]].toSeq
      .filter(c => js.typeOf(c.finishPoly) == "function" && c.name.asInstanceOf[String].startsWith("Turtle Fill"))
      .filter { c => c.finishPoly(); c.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].length > 0 }

  private def sahneyiOku(w: KojoWorldImpl): Uint8Array = {
    w.renderer.render(w.stage)
    val gl = dyn(w.renderer).gl
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]
    val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val tampon = new Uint8Array(en * boy * 4)
    gl.readPixels(0, 0, en, boy, gl.RGBA, gl.UNSIGNED_BYTE, tampon)
    tampon
  }

  /** Resmin YEREL (x, y) noktasının tampondaki (r, g, b, a) değeri; readPixels alttan sayıyor. */
  private def piksel(w: KojoWorldImpl, p: TurtlePicture, tampon: Uint8Array, x: Double, y: Double): (Int, Int, Int, Int) = {
    val k = dyn(p.tnode).toGlobal(js.Dynamic.newInstance(P.Point)(x, y))
    val en = dyn(w.renderer).view.width.asInstanceOf[Int]
    val boy = dyn(w.renderer).view.height.asInstanceOf[Int]
    val px = k.x.asInstanceOf[Double].round.toInt; val py = boy - 1 - k.y.asInstanceOf[Double].round.toInt
    val i = 4 * (py * en + px)
    (tampon(i).toInt, tampon(i + 1).toInt, tampon(i + 2).toInt, tampon(i + 3).toInt)
  }

  /** (farklı piksel, boyalı piksel) */
  private def karşılaştır(a: Uint8Array, b: Uint8Array, hoşgörü: Int = 8): (Int, Int) = {
    var farklı = 0; var dolu = 0; var i = 0
    while (i < a.length) {
      if (a(i + 3) != 0 || b(i + 3) != 0) dolu += 1
      var k = 0; var pf = false
      while (k < 4) { if (math.abs(a(i) - b(i)) > hoşgörü) pf = true; k += 1; i += 1 }
      if (pf) farklı += 1
    }
    (farklı, dolu)
  }

  private def isabet(w: KojoWorldImpl, p: TurtlePicture, x: Double, y: Double): Boolean = {
    val d = dyn(p.tnode)
    w.renderer.asInstanceOf[js.Dynamic].render(d.parent)
    val küresel = d.toGlobal(js.Dynamic.newInstance(P.Point)(x, y))
    val bulunan = w.renderer.asInstanceOf[js.Dynamic].plugins.interaction.hitTest(küresel, d.parent)
    !js.isUndefined(bulunan) && bulunan != null && (bulunan.asInstanceOf[js.Any] eq p.tnode.asInstanceOf[js.Any])
  }

  /**
   * Aynı betik iki yolda: önce stencil, sonra libtess; ikisi de kendi
   * resmini çizip siliyor. Dönen: (stencil tamponu, libtess tamponu, stencil resmi, libtess resmi).
   */
  private def ikiYol(w: KojoWorldImpl, boya: Boya, beklenenNokta: Int)(şekil: Turtle => Unit)
                    : Future[(Uint8Array, Uint8Array, Int, Int)] = {
    implicit val iw: KojoWorld = w
    w.stencilDolgu = true
    val ps = resim(boya)(şekil); ps.draw()
    kareler(30).flatMap { _ =>
      val sd = stencilDüğümler(ps)
      val sNokta = sd.map(_.noktaSayısı).sum
      val sGrafik = grafikDolgular(ps).size
      val a = sahneyiOku(w)
      ps.erase()
      w.stencilDolgu = false
      val pl = resim(boya)(şekil); pl.draw()
      kareler(30).map { _ =>
        val lGrafik = grafikDolgular(pl).size
        val lStencil = stencilDüğümler(pl).size
        val b = sahneyiOku(w)
        pl.erase(); w.stencilDolgu = true
        withClue(s"kurulum: stencil yolunda düğüm ${sd.size} ($sNokta nokta, beklenen $beklenenNokta), Graphics dolgu $sGrafik; " +
          s"libtess yolunda Graphics dolgu $lGrafik, stencil $lStencil -- ") {
          sd.size shouldBe 1; sNokta shouldBe beklenenNokta; sGrafik shouldBe 0
          lGrafik shouldBe 1; lStencil shouldBe 0
        }
        (a, b, sNokta, lGrafik)
      }
    }
  }

  private def denk(ad: String, w: KojoWorldImpl, boya: Boya, beklenenNokta: Int)(şekil: Turtle => Unit): Future[org.scalatest.Assertion] =
    ikiYol(w, boya, beklenenNokta)(şekil).map { case (a, b, _, _) =>
      val (fark, dolu) = karşılaştır(a, b)
      val oran = 100.0 * fark / math.max(1, dolu)
      info(s"$ad: boyalı $dolu, farklı $fark (yüzde ${(oran * 100).round / 100.0})")
      withClue(s"$ad -- boyalı $dolu, farklı $fark -- ") {
        dolu should be > 1000
        oran should be < 2.0
      }
    }

  // ---- savlar ----

  test("Resim{} gülü (250x7), düz renk: stencil yolu libtess yoluyla aynı pikseller") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    denk("gül 250x7", w, DüzBoya(kojo.doodle.Color.blue), 251)(gül(_, 250, 7, 130))
  }

  test("Resim{} gülü, merkezden gradyan: doku UV'si iki yolda aynı") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    denk("gül gradyan", w, Boya.merkezden(0, 0, kojo.doodle.Color.yellow, 130, kojo.doodle.Color.green, dalgalıDevam = false), 251)(
      gül(_, 250, 7, 130))
  }

  test("TERS SARIM Resim{}: iç kare iki yolda da DELİK (sarım aritmetiği Turtle üzerinden)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val boya = DüzBoya(kojo.doodle.Color.red)
    ikiYol(w, boya, 83)(tersSarım(_, 10)).flatMap { case (a, b, _, _) =>
      // Pikselleri OKURKEN resim silinmiş; yerel = dünya (resim dönüştürülmedi),
      // sahneye göre okumak için geçici bir resimle aynı dönüşümü kullanıyoruz.
      implicit val iw: KojoWorld = w
      val ölçek = resim(boya)(_ => ()); ölçek.draw()
      kareler(3).map { _ =>
        val merkezA = piksel(w, ölçek, a, 0, 0); val merkezB = piksel(w, ölçek, b, 0, 0)
        val halkaA = piksel(w, ölçek, a, 75, 0); val halkaB = piksel(w, ölçek, b, 75, 0)
        val (fark, dolu) = karşılaştır(a, b)
        ölçek.erase()
        info(s"ters sarım: merkez alfa stencil ${merkezA._4} / libtess ${merkezB._4}; halka ${halkaA._4} / ${halkaB._4}; farklı $fark / $dolu")
        withClue(s"merkez stencil $merkezA libtess $merkezB, halka $halkaA / $halkaB, farklı $fark / $dolu -- ") {
          merkezB._4 shouldBe 0 // referans: NON_ZERO delik
          merkezA._4 shouldBe 0 // stencil de
          halkaA._4 should be > 0; halkaB._4 should be > 0
          (100.0 * fark / math.max(1, dolu)) should be < 2.0
        }
      }
    }
  }

  test("EŞİK: 64 ve altı libtess + Graphics'te kalıyor, 65 stencil'e gidiyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val boya = DüzBoya(kojo.doodle.Color.blue)
    val küçük = resim(boya)(gül(_, 63, 7, 100)) // 63 + kapanış = 64 nokta
    val büyük = resim(boya)(gül(_, 64, 7, 100)) // 65 nokta
    küçük.draw(); büyük.draw()
    kareler(30).map { _ =>
      withClue(s"küçük: stencil ${stencilDüğümler(küçük).size} Graphics ${grafikDolgular(küçük).size}; " +
        s"büyük: stencil ${stencilDüğümler(büyük).map(_.noktaSayısı)} Graphics ${grafikDolgular(büyük).size} -- ") {
        StencilDolgu.Eşik shouldBe 64
        stencilDüğümler(küçük) shouldBe empty; grafikDolgular(küçük).size shouldBe 1
        stencilDüğümler(büyük).map(_.noktaSayısı) shouldBe Seq(65); grafikDolgular(büyük) shouldBe empty
      }
    }
  }

  test("İSABET: gülün ortası tıklanır, sınır kutusunun köşesi tıklanmaz (kutu değil sarım)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = resim(DüzBoya(kojo.doodle.Color.blue))(gül(_, 100, 7, 100))
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(30).map { _ =>
      withClue(s"kurulum: stencil düğüm ${stencilDüğümler(p).map(_.noktaSayısı)} -- ") {
        stencilDüğümler(p).map(_.noktaSayısı) shouldBe Seq(101)
      }
      isabet(w, p, 0, 0) shouldBe true // sarım 7
      isabet(w, p, 95, 95) shouldBe false // sınır kutusunun içi, gülün dışı (uzaklık 134 > 100)
      isabet(w, p, 0, 60) shouldBe true // yaprak içi
    }
  }

  test("İSABET, ters sarım: halka tıklanır, iç kare (delik) tıklanmaz") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = resim(DüzBoya(kojo.doodle.Color.red))(tersSarım(_, 10))
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(30).map { _ =>
      withClue(s"kurulum: stencil düğüm ${stencilDüğümler(p).map(_.noktaSayısı)} -- ") {
        stencilDüğümler(p).map(_.noktaSayısı) shouldBe Seq(83)
      }
      isabet(w, p, 75, 0) shouldBe true // halka
      isabet(w, p, 0, 0) shouldBe false // delik: +1 - 1 = 0
      isabet(w, p, 130, 0) shouldBe false // dış
    }
  }

  test("KALEM ŞERİDİ (#118) stencil dolgulu resimde de tıklanıyor: dolgunun dışında, çizginin üstünde") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    // Dolgu delikli ters sarım; delikte dolgu yok ama iç karenin KALEM izi
    // var (kalınlık 6) -- kalem parçası Graphics yolundan isabet vermeli.
    val p = TurtlePicture { t =>
      t.setAnimationDelay(0); t.setPenThickness(6); t.setPenColor(kojo.doodle.Color.black)
      t.setFillColor(kojo.doodle.Color.red)
      tersSarım(t, 10)
    }
    p.draw(); p.onMouseClick((_, _) => ())
    kareler(30).map { _ =>
      stencilDüğümler(p).map(_.noktaSayısı) shouldBe Seq(83)
      isabet(w, p, 0, 0) shouldBe false // deliğin ortası: ne dolgu ne çizgi
      isabet(w, p, -50, 0) shouldBe true // iç karenin sol kenarı: çizgi
    }
  }

  test("PİŞİRME TAMPONU: RenderTexture'a çizilen sahnede dolgu doğru (kutu dolmuyor)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val boya = DüzBoya(kojo.doodle.Color.blue)
    def rtOku(): Uint8Array = {
      val r = dyn(w.renderer)
      val en = r.view.width.asInstanceOf[Int]; val boy = r.view.height.asInstanceOf[Int]
      val rt = P.RenderTexture.create(js.Dynamic.literal(width = en, height = boy))
      r.render(w.stage, rt, true)
      val t = r.extract.pixels(rt).asInstanceOf[Uint8Array]
      rt.destroy(true); t
    }
    w.stencilDolgu = true
    val ps = resim(boya)(gül(_, 250, 7, 100)); ps.draw()
    kareler(30).flatMap { _ =>
      val a = rtOku()
      // Kutu dolsaydı köşede piksel olurdu: (-98, -98) sınır kutusunun içi, gülün dışı.
      val köşeS = piksel(w, ps, a, -95, -95)
      ps.erase(); w.stencilDolgu = false
      val pl = resim(boya)(gül(_, 250, 7, 100)); pl.draw()
      kareler(30).map { _ =>
        val b = rtOku()
        val köşeL = piksel(w, pl, b, -95, -95)
        pl.erase(); w.stencilDolgu = true
        val (fark, dolu) = karşılaştır(a, b)
        val oran = 100.0 * fark / math.max(1, dolu)
        info(s"RenderTexture: boyalı $dolu, farklı $fark (yüzde ${(oran * 100).round / 100.0}); köşe stencil $köşeS libtess $köşeL")
        withClue(s"boyalı $dolu, farklı $fark, köşe stencil $köşeS libtess $köşeL -- ") {
          dolu should be > 1000
          köşeL._4 shouldBe 0; köşeS._4 shouldBe 0
          oran should be < 2.0
        }
      }
    }
  }

  /**
   * Bitmemiş şeklin (Resim{} çoğu zaman öyle: kalem hiç kalkmıyor) dolgusu
   * eskiden de yeniden boyanmıyordu; görünen renk değişimini kalem
   * parçasının dolgusu veriyor (Turtle.dolgularıBoya, "açık boyama"). Sav
   * bu yüzden PİKSELE bakıyor, düğümün boyasına değil; ve düğümün yerinde
   * durduğunu (geometri) nokta sayısından okuyor.
   */
  test("boyalı: dolgu yeniden renkleniyor, sonra gradyana geçiyor; geometri kalıyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val p = resim(DüzBoya(kojo.doodle.Color.blue))(gül(_, 250, 7, 100)); p.draw()
    kareler(30).flatMap { _ =>
      val önce = piksel(w, p, sahneyiOku(w), 0, 0)
      p.setFillColor(kojo.doodle.Color(0, 255, 0))
      kareler(3).flatMap { _ =>
        val yeşil = piksel(w, p, sahneyiOku(w), 0, 0)
        p.setFillPaint(Boya.merkezden(0, 0, kojo.doodle.Color.red, 100, kojo.doodle.Color.red, dalgalıDevam = false))
        kareler(3).map { _ =>
          val kırmızı = piksel(w, p, sahneyiOku(w), 0, 0)
          val düğüm = stencilDüğümler(p)
          withClue(s"önce $önce, yeşil $yeşil, kırmızı $kırmızı, düğüm ${düğüm.map(_.noktaSayısı)} -- ") {
            düğüm.map(_.noktaSayısı) shouldBe Seq(251)
            önce._3 should be > 200; önce._2 should be < 50
            yeşil._2 should be > 200; yeşil._3 should be < 50
            kırmızı._1 should be > 200; kırmızı._2 should be < 50
          }
        }
      }
    }
  }

  test("Boya.dokuma: doku sonradan yüklenince stencil dolgu dokuya geçiyor (#40)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val tuval = document.createElement("canvas").asInstanceOf[org.scalajs.dom.html.Canvas]
    tuval.width = 8; tuval.height = 8
    val ctx = tuval.getContext("2d").asInstanceOf[org.scalajs.dom.CanvasRenderingContext2D]
    ctx.fillStyle = "#ff0000"; ctx.fillRect(0, 0, 8, 8)
    // Önbelleği atlatmak için her koşuda farklı bir URL (Texture.from adrese göre önbellekliyor).
    val url = tuval.toDataURL("image/png") + "#" + window.performance.now()
    val boya = Boya.dokuma(url, 0, 0)
    val validBaşta = dyn(boya.asInstanceOf[DokuBoya].doku).baseTexture.valid.asInstanceOf[Boolean]
    val p = resim(boya)(gül(_, 250, 7, 100)); p.draw()
    kareler(40).map { _ =>
      val merkez = piksel(w, p, sahneyiOku(w), 0, 0)
      val validSonra = dyn(boya.asInstanceOf[DokuBoya].doku).baseTexture.valid.asInstanceOf[Boolean]
      info(s"dokuma: başta valid=$validBaşta, sonra valid=$validSonra, merkez $merkez")
      withClue(s"başta valid=$validBaşta, sonra valid=$validSonra, merkez $merkez, düğüm ${stencilDüğümler(p).map(_.noktaSayısı)} -- ") {
        stencilDüğümler(p).map(_.noktaSayısı) shouldBe Seq(251)
        validSonra shouldBe true
        merkez._1 should be > 200; merkez._2 should be < 50; merkez._3 should be < 50; merkez._4 should be > 200
      }
    }
  }

  test("sil() sonrası artık yok: aynı yere küçük kare, gülün yerinde piksel kalmıyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val gülR = resim(DüzBoya(kojo.doodle.Color.blue))(gül(_, 250, 7, 100)); gülR.draw()
    kareler(30).flatMap { _ =>
      val dolgulu = piksel(w, gülR, sahneyiOku(w), 0, 60)
      gülR.erase()
      val kare = resim(DüzBoya(kojo.doodle.Color(0, 255, 0))) { t =>
        t.penUp(); t.setPosition(-20, -20); t.penDown(); t.moveTo(20, -20); t.moveTo(20, 20); t.moveTo(-20, 20); t.moveTo(-20, -20)
      }
      kare.draw()
      kareler(10).map { _ =>
        val t = sahneyiOku(w)
        val yaprak = piksel(w, kare, t, 0, 60) // gülün yaprağıydı, artık boş
        val merkez = piksel(w, kare, t, 0, 0) // yeşil kare
        withClue(s"gül varken $dolgulu, silindikten sonra yaprak $yaprak, kare merkezi $merkez -- ") {
          dolgulu._4 should be > 200
          yaprak._4 shouldBe 0
          merkez._2 should be > 200
        }
      }
    }
  }

  /**
   * 8 BİTLİK SARIM SINIRI (#153 incelemesi §2): stencil tamponu 8 bit,
   * INCR_WRAP 256'da sıfıra döner -- sarımı tam 256 olan bölge NON_ZERO'da
   * dolu olması gerekirken BOŞ kalır. Ayırıcı imza: komşuları (255, 257)
   * temiz, yalnız 256 ayrışıyor; yani fark kenar yumuşatma ya da libtess
   * değil, taşmanın kendisi. Bu sav sınırı "böyle" diye çiviliyor: biri
   * sınırı kaldırırsa (ör. 16 bitlik yol) 256 savı kırmızı yanar ve belge
   * güncellenir. Düğüm doğrudan kuruluyor (pompa yok, hız için); libtess
   * referansı sondadaki gibi üçgen başına drawPolygon.
   */
  test("BİLİNEN SINIR: sarım 256'da taşıyor (255 ve 257 temiz, 256 boş bölge)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val boya = DüzBoya(kojo.doodle.Color.blue)
    def gülDüz(nokta: Int, kat: Int, r: Double): Array[Double] = {
      val a = new Array[Double](nokta * 2); var i = 0
      while (i < nokta) { val açı = i * kat * 2 * math.Pi / nokta; a(2 * i) = r * math.cos(açı); a(2 * i + 1) = r * math.sin(açı); i += 1 }
      a
    }
    def libtess(düz: Array[Double]): js.Dynamic = {
      val ü = Üçgenleyici.nonzero(düz); val gr = new PIXI.Graphics()
      PixiUyum.boyamayaBaşla(gr, boya)(() => ())
      var i = 0
      while (i + 5 < ü.length) { gr.drawPolygon(js.Array(ü(i), ü(i + 1), ü(i + 2), ü(i + 3), ü(i + 4), ü(i + 5))); i += 6 }
      gr.endFill(); dyn(gr)
    }
    def stencil(düz: Array[Double]): js.Dynamic = { val s = new StencilDolgu(); s.kur(düz, boya)(() => ()); dyn(s) }
    def oku(düğüm: js.Dynamic): Uint8Array = {
      w.stage.children.toList.foreach(c => w.stage.removeChild(c))
      dyn(w.stage).addChild(düğüm)
      val t = sahneyiOku(w)
      w.stage.removeChild(düğüm.asInstanceOf[pixiscalajs.PIXI.DisplayObject]); t
    }
    val sonuç = for (kat <- List(255, 256, 257)) yield {
      val düz = gülDüz(1000, kat, 130)
      val (fark, dolu) = karşılaştır(oku(libtess(düz)), oku(stencil(düz)))
      (kat, 100.0 * fark / math.max(1, dolu), dolu)
    }
    sonuç.foreach { case (kat, oran, dolu) => info(f"gül 1000 x $kat: boyalı $dolu, farklı yüzde $oran%.2f") }
    val oranlar = sonuç.map { case (kat, oran, _) => kat -> oran }.toMap
    withClue(s"$sonuç -- ") {
      sonuç.foreach { case (_, _, dolu) => dolu should be > 1000 }
      oranlar(255) should be < 2.0
      oranlar(257) should be < 2.0
      oranlar(256) should be > 10.0 // taşma: sınır BÖYLE; kalkarsa bu satır ve belge değişir
    }
    Future.successful(succeed)
  }

  /**
   * ELLE GERİ DÖNÜŞ KANALI: `localStorage.kojoDolgu = "libtess"` dünyayı
   * libtess yoluna kurar; silinince stencil'e döner. Adres sorgusu editörde
   * güvenilmez (yönlendirici düşürüyor, canlıda görüldü), bu kanal onun
   * yerine; sav dünyanın kuruluş anındaki okumayı çiviliyor.
   */
  test("localStorage.kojoDolgu = libtess: dünya libtess yoluyla kuruluyor ve panele yazıyor; silinince stencil, sessiz") {
    val depo = window.localStorage
    def panelKur(): HTMLElement = {
      Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
      val d = document.createElement("div").asInstanceOf[HTMLElement]
      d.id = "output"; document.body.appendChild(d); d
    }
    // Panel ve depo KÜRESEL: ikisi de finally'de temizleniyor ki sonraki
    // savlar (#154 incelemesi §5) ne anahtarı ne satırı miras alsın.
    try {
      depo.setItem("kojoDolgu", "libtess")
      val kapalıPanel = panelKur()
      val kapalı = dünyaKurYaDaİptalHam()
      val kapalıMetin = kapalıPanel.textContent
      depo.removeItem("kojoDolgu")
      val açıkPanel = panelKur()
      val açık = dünyaKurYaDaİptalHam()
      val açıkMetin = açıkPanel.textContent
      withClue(s"kojoDolgu=libtess ile ${kapalı.stencilDolgu}, panel '$kapalıMetin'; silinince ${açık.stencilDolgu}, panel '$açıkMetin' -- ") {
        kapalı.stencilDolgu shouldBe false
        kapalıMetin should include("elle açık")
        kapalıMetin should include("delete localStorage.kojoDolgu")
        açık.stencilDolgu shouldBe true
        açıkMetin shouldBe ""
      }
      Future.successful(succeed)
    }
    finally {
      depo.removeItem("kojoDolgu")
      Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    }
  }
}
