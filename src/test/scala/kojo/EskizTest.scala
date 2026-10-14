package kojo

import org.scalajs.dom
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * Eskiz (Processing tarzı tuval çizimi): `Picture.fromSketch`, `Resim.eskizden`, `CanvasDraw`.
 *
 * BEKLENEN DEĞERLER GERÇEK JAVA2D'DEN: masaüstü `CanvasDraw`ın çizdiğini bire bir üreten
 * `Arc2D` / `Rectangle2D` / `Line2D` + `BasicStroke` ile başsız (java.awt.headless) bir
 * `BufferedImage`a çizip pikselleri okudum; aşağıdaki "Java2D:" notları o ölçümler
 * (yay yönü, rect'in 3.-4. değerinin genişlik/yükseklik olması, uç biçimleri). Eskiz
 * koordinatı = ham görüntü satırı; bkz. CanvasDraw.
 */
class EskizTest extends AnyFunSuite with Matchers {
  private implicit val w: TestKojoWorld = new TestKojoWorld()
  private val b = new kojo.syntax.Builtins()

  private def bağlam(p: SketchPic) = p.tuvalElemanı.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  /** (r, g, b, alfa) */
  private def piksel(p: SketchPic, x: Int, y: Int): (Int, Int, Int, Int) = {
    val d = bağlam(p).getImageData(x, y, 1, 1).data
    (d(0).toInt, d(1).toInt, d(2).toInt, d(3).toInt)
  }
  private def alfa(p: SketchPic, x: Int, y: Int): Int = piksel(p, x, y)._4

  /** Eskiz çizip kurulum + `kare` kare döngüyü koşturur. */
  private def eskiz(kurulum: CanvasDraw => Unit, döngü: CanvasDraw => Unit = _ => (), kare: Int = 1, f: Double = 1): SketchPic = {
    val p = new SketchPic(kurulum, döngü, f, b.setRandomSeed)
    p.draw()
    (1 to kare).foreach(_ => p.adım())
    p
  }

  test("kurulum resim ilk çizilince bir kez, çizimDöngüsü her karede; döngüyüDurdur yalnız eskizi durdurur") {
    var kurulumSayısı, döngüSayısı = 0
    val p = new SketchPic(_ => kurulumSayısı += 1, cd => { döngüSayısı += 1; if (döngüSayısı == 3) cd.noLoop() }, 1, b.setRandomSeed)
    kurulumSayısı shouldBe 0 // çizilmeden kurulmaz
    p.draw()
    kurulumSayısı shouldBe 1
    döngüSayısı shouldBe 0 // ilk döngü çağrısı animate'ten gelir
    (1 to 6).foreach(_ => p.adım())
    kurulumSayısı shouldBe 1
    döngüSayısı shouldBe 3 // 3. karede noLoop: sonraki kareler atlanır
    p.cd.loop shouldBe false
  }

  test("yüzey kareler arasında silinmez: iz kalır (artalan çağrılmazsa çizimler birikir)") {
    var kare = 0
    val p = eskiz(_ => (), cd => {
      cd.fill(255, 0, 0); cd.noStroke()
      cd.rect(10 + 30 * kare, 10, 10, 10)
      kare += 1
    }, kare = 3)
    alfa(p, 15, 15) shouldBe 255
    alfa(p, 45, 15) shouldBe 255
    alfa(p, 75, 15) shouldBe 255
    alfa(p, 25, 15) shouldBe 0
  }

  test("rect(x1, y1, x2, y2): 3. ve 4. değer genişlik ve yükseklik. Java2D: (35,55)=dolu (45,25) (15,65) (5,25)=boş") {
    val p = eskiz(cd => { cd.fill(255, 0, 0); cd.noStroke(); cd.rect(10, 20, 30, 40) })
    alfa(p, 35, 55) shouldBe 255
    alfa(p, 45, 25) shouldBe 0
    alfa(p, 15, 65) shouldBe 0
    alfa(p, 5, 25) shouldBe 0
  }

  test("ellipse(cx, cy, w, h): w ve h TAM boy") {
    val p = eskiz(cd => { cd.fill(0, 0, 255); cd.noStroke(); cd.ellipse(100, 100, 40, 20) })
    alfa(p, 118, 100) shouldBe 255 // yarıçap 20
    alfa(p, 122, 100) shouldBe 0
    alfa(p, 100, 108) shouldBe 255 // yarıçap 10
    alfa(p, 100, 112) shouldBe 0
  }

  test("arc: açılar radyan, artan açı ham koordinatta saat yönünün TERSİ. Java2D: sağ-üst (134,66) dolu, sağ-alt (134,134) boş") {
    val p = eskiz(cd => { cd.fill(255, 0, 0); cd.noStroke(); cd.arc(100, 100, 100, 100, 0, math.Pi / 2) })
    alfa(p, 134, 66) shouldBe 255
    alfa(p, 134, 134) shouldBe 0
    alfa(p, 66, 66) shouldBe 0
  }

  test("arc fırçası AÇIK: yay çizilir, kiriş (iki uç arası düz çizgi) çizilmez. Java2D: yay (135,65) dolu, kiriş ortası (117,83) boş") {
    val p = eskiz(cd => { cd.noFill(); cd.stroke(255, 0, 0); cd.strokeWeight(2); cd.arc(100, 100, 100, 100, 0, math.Pi / 2) })
    alfa(p, 135, 65) should be > 200 // kenar pikseli: kenar yumuşatma Java2D'den azıcık ayrılır (243 / 255)
    alfa(p, 117, 83) shouldBe 0
  }

  test("point: yuvarlak uçla dolu nokta; fırça ucu sabitleri masaüstüyle aynı: ROUND, PROJECT (kare uç), SQUARE (düz uç!)") {
    val p = eskiz(cd => { cd.stroke(255, 0, 0); cd.strokeWeight(20); cd.point(100, 100) })
    alfa(p, 100, 100) shouldBe 255
    alfa(p, 108, 100) shouldBe 255 // Java2D: round dot yarıçap 10
    alfa(p, 115, 100) shouldBe 0

    // Java2D: CAP_SQUARE çizgiyi kalınlığın yarısı kadar uzatır (105,100 dolu); CAP_BUTT uzatmaz
    val kare = eskiz(cd => { cd.stroke(255, 0, 0); cd.strokeWeight(20); cd.strokeCap(cd.PROJECT); cd.line(50, 100, 100, 100) })
    alfa(kare, 105, 100) shouldBe 255
    alfa(kare, 45, 100) shouldBe 255
    alfa(kare, 115, 100) shouldBe 0
    val düz = eskiz(cd => { cd.stroke(255, 0, 0); cd.strokeWeight(20); cd.strokeCap(cd.SQUARE); cd.line(50, 100, 100, 100) })
    alfa(düz, 105, 100) shouldBe 0
    alfa(düz, 45, 100) shouldBe 0
    alfa(düz, 75, 100) shouldBe 255
  }

  test("dönüşümler: götür çizimi kaydırır; dönüşümüKaydet/GeriYükle yalnız dönüşümü saklar, boyayı değil") {
    val p = eskiz(cd => {
      cd.noStroke()
      cd.pushMatrix()
      cd.translate(100, 50)
      cd.fill(255, 0, 0)
      cd.rect(0, 0, 10, 10)
      cd.popMatrix()
      cd.rect(0, 0, 10, 10) // dönüşüm geri geldi ama boya kırmızı KALDI
    })
    alfa(p, 105, 55) shouldBe 255
    piksel(p, 5, 5) shouldBe ((255, 0, 0, 255))
    alfa(p, 105, 5) shouldBe 0
  }

  test("artalan dönüşümden bağımsız tüm yüzeyi boyar; saydam artalan öncekini soldurur") {
    val p = eskiz(cd => {
      cd.translate(300, 300); cd.rotate(0) // tamamen çevrilmiş
      cd.background(0, 255, 0)
    })
    piksel(p, 0, 0) shouldBe ((0, 255, 0, 255))
    piksel(p, p.tuvalElemanı.width - 1, p.tuvalElemanı.height - 1) shouldBe ((0, 255, 0, 255))

    var kare = 0
    val s = eskiz(cd => cd.background(255, 255, 255), cd => { cd.background(0, 0, 0, 51); kare += 1 }, kare = 2)
    val (r, _, _, a) = piksel(s, 5, 5)
    a shouldBe 255
    r should be < 255 // iki kare soldu
    r should be > 100 // ama tümüyle kararmadı: 255 -> ~204 -> ~163
  }

  test("yazı fırça renginde ve DİK görünür (masaüstü gibi y çevrilir): T'nin çubuğu taban çizgisinin ÜSTÜNDE (büyük y)") {
    val p = eskiz(cd => { cd.stroke(255, 0, 0); cd.textFont(Font("Sans Serif", 40)); cd.text("T", 50, 100) })
    def toplam(y0: Int, y1: Int): Int = (for (y <- y0 until y1; x <- 40 until 90) yield alfa(p, x, y)).sum
    toplam(100, 145) should be > 0 // glif taban çizgisinin üstüne (artan y) çıkar
    toplam(55, 98) shouldBe 0
    // çubuk üstte: glifin üst üçte biri (büyük y), alt üçte birinden (yalnız ince gövde) daha dolu
    toplam(120, 132) should be > toplam(100, 112)
  }

  test("köşe noktalarıyla şekil: üçgen dolar; yayKöşesi iki nokta arasına yarım daire çizer") {
    val üçgen = eskiz(cd => {
      cd.fill(255, 0, 0); cd.noStroke()
      cd.beginShape(); cd.vertex(10, 10); cd.vertex(110, 10); cd.vertex(10, 110); cd.endShape()
    })
    alfa(üçgen, 30, 30) shouldBe 255
    alfa(üçgen, 100, 100) shouldBe 0

    // (100,100) -> (200,100) yarım daire: merkez (150,100), yarıçap 50
    val yay = eskiz(cd => {
      cd.fill(0, 0, 255); cd.noStroke()
      cd.beginShape(); cd.vertex(100, 100); cd.arcVertex(200, 100, 180); cd.endShape()
    })
    // Java2D (Rich2DPath formülü, Arc2D): 180 derecede yarım disk ÜSTTE (küçük y): (150,70) dolu, (150,130) boş
    alfa(yay, 150, 70) shouldBe 255
    alfa(yay, 150, 130) shouldBe 0
    alfa(yay, 150, 40) shouldBe 0 // yarıçap 50'nin dışı

    // 90 derece (100,100) -> (150,50): merkez (150,100), yarıçap 50. Java2D: (110,70) ve (130,60) dolu; (140,80), (115,115) boş
    val çeyrek = eskiz(cd => {
      cd.fill(0, 0, 255); cd.noStroke()
      cd.beginShape(); cd.vertex(100, 100); cd.arcVertex(150, 50, 90); cd.endShape()
    })
    alfa(çeyrek, 110, 70) should be >= 245 // kenar pikseli (Java2D 254)
    alfa(çeyrek, 130, 60) shouldBe 255
    alfa(çeyrek, 140, 80) shouldBe 0
    alfa(çeyrek, 115, 115) shouldBe 0
  }

  test("ölçekÇarpanı 2: yüzey iki kat piksel, sprite yarı ölçek; kullanıcı koordinatı yüzey pikseli") {
    val p = eskiz(cd => { cd.fill(255, 0, 0); cd.noStroke(); cd.rect(1500, 1100, 10, 10) }, f = 2)
    p.tuvalElemanı.width shouldBe 1600 // TestKojoWorld 800 x 600
    p.tuvalElemanı.height shouldBe 1200
    alfa(p, 1505, 1105) shouldBe 255
    val sprite = p.tnode.asInstanceOf[pixiscalajs.PIXI.Container].children(0)
    sprite.scale.x shouldBe 0.5 +- 1e-12
  }

  test("blendMode(\"lighter\") renkleri toplar") {
    val p = eskiz(cd => {
      cd.noStroke(); cd.blendMode("lighter")
      cd.fill(100, 0, 0); cd.rect(0, 0, 10, 10)
      cd.fill(100, 0, 0); cd.rect(0, 0, 10, 10)
    })
    piksel(p, 5, 5)._1 shouldBe 200 +- 2
  }

  test("randomSeed Builtins'in rastgele üretecini tohumlar: aynı tohum aynı dizi") {
    val p = eskiz(cd => cd.randomSeed(7L))
    val a = b.random(1000)
    p.cd.randomSeed(7L)
    a should not be (-1)
    b.random(1000) shouldBe a
  }

  test("sil sonra yeniden çiz: çalışır; çizili; GL kaynağı bırakılır ama yüzey durur") {
    val p = eskiz(cd => { cd.fill(255, 0, 0); cd.rect(0, 0, 20, 20) })
    p.isDrawn shouldBe true
    p.erase()
    p.draw()
    alfa(p, 10, 10) shouldBe 255
  }

  test("sızıntı yok: 200 eskiz kur-çiz-sil, PIXI doku önbellekleri büyümez (Texture.from kullanılmıyor)") {
    val P = js.Dynamic.global.PIXI.utils
    def say(o: js.Dynamic) = js.Object.keys(o.asInstanceOf[js.Object]).length
    val öncekiDoku = say(P.TextureCache)
    val öncekiTaban = say(P.BaseTextureCache)
    (1 to 200).foreach { _ =>
      val p = new SketchPic(_ => (), cd => cd.rect(0, 0, 5, 5), 1, b.setRandomSeed)
      p.draw(); p.adım(); p.erase()
    }
    say(P.TextureCache) shouldBe öncekiDoku
    say(P.BaseTextureCache) shouldBe öncekiTaban
  }

  test("sınırlar: tuvalin sol alt köşesinden (0,0)-(eni, boyu)") {
    val p = new SketchPic(_ => (), _ => (), 1, b.setRandomSeed)
    val cs = p.picGeom.getCoordinates
    cs.map(_.x).min shouldBe 0.0
    cs.map(_.y).min shouldBe 0.0
    cs.map(_.x).max shouldBe 800.0
    cs.map(_.y).max shouldBe 600.0
  }

  // --- İngilizce yüzey ---------------------------------------------------------

  test("Picture.fromSketch yapısal türle: masaüstündeki setup/drawLoop betikleri aynen derlenir ve çalışır") {
    import scala.language.reflectiveCalls
    var kurulum, döngü = 0
    val sketch = new {
      def setup(cd: b.CanvasDraw): Unit = { kurulum += 1; cd.fill(255, 0, 0); cd.noStroke() }
      def drawLoop(cd: b.CanvasDraw): Unit = { döngü += 1; cd.rect(0, 0, 10, 10) }
    }
    val pic = b.Picture.fromSketch(sketch).asInstanceOf[SketchPic]
    pic.draw()
    pic.adım(); pic.adım()
    kurulum shouldBe 1
    döngü shouldBe 2
    alfa(pic, 5, 5) shouldBe 255
  }

  test("canvasSketch / animateWithCanvasDraw / animateWithSetupCanvasDraw çizer (animate sahte dünyada dönmez, patlamamalı)") {
    import scala.language.reflectiveCalls
    var kurulum = 0
    val sketch = new {
      def setup(cd: b.CanvasDraw): Unit = kurulum += 1
      def drawLoop(cd: b.CanvasDraw): Unit = ()
    }
    b.canvasSketch(sketch, 1)
    b.animateWithCanvasDraw(cd => cd.noLoop())
    b.animateWithSetupCanvasDraw(cd => kurulum += 1)(cd => ())
    kurulum shouldBe 2
  }

  // --- Türkçe yüzey ------------------------------------------------------------

  test("Türkçe: Eskiz alt sınıfı + Resim.eskizden + TuvalÇizim yöntemleri (masaüstü eskiz-gul örneğindeki kalıp)") {
    import b.trTurtle._
    var döngü = 0
    class Deneme extends Eskiz {
      def kurulum(tuval: TuvalÇizim): Birim = {
        kurulumSay += 1
        tuval.artalan(0, 0, 0)
      }
      var kurulumSay = 0
      def çizimDöngüsü(tuval: TuvalÇizim): Birim = {
        döngü += 1
        tuval.dönüşümüKaydet()
        tuval.götür(100, 100)
        tuval.fırçasız()
        tuval.boya(255, 0, 0)
        tuval.dikdörtgen(0, 0, 10, 10)
        tuval.dönüşümüGeriYükle()
        tuval.fırça(Renkler.mavi)
        tuval.fırçaAğırlığı(3)
        tuval.fırçaUcu(tuval.projeUç)
        tuval.fırçaBirleşimi(tuval.eğikBirleşim)
        tuval.çizgi(0, 0, 20, 0)
        tuval.elips(200, 200, 10, 10)
        tuval.yay(300, 300, 20, 20, 0, 1)
        tuval.nokta(1, 1)
        tuval.yazıYüzü(yazıyüzü("serif", 12))
        tuval.yazı("a", 5, 400)
        tuval.döndürDerece(10); tuval.döndürRadyan(0.1); tuval.büyüt(2); tuval.büyüt(1, 1)
        tuval.karışımKipi("source-over")
        tuval.şekilBaşla(); tuval.köşe(0, 0); tuval.köşe(5, 0); tuval.köşe(5, 5)
        tuval.şekilBitir()
        tuval.şekilBaşla(); tuval.köşe(0, 0); tuval.bezierKöşesi(1, 1, 2, 2, 3, 3); tuval.şekilBitir()
        tuval.şekilBaşla(); tuval.köşe(0, 0); tuval.ikinciDereceKöşe(1, 1, 2, 0); tuval.şekilBitir()
        tuval.şekilBaşla(); (1 to 4).foreach(i => tuval.eğriKöşesi(i * 3, i * i)); tuval.şekilBitir()
        tuval.şekilBaşla(); tuval.açısalKöşe(10, 0); tuval.açısalKöşe(10, 90); tuval.şekilBitir()
        tuval.rastgeleTohum(1L)
        tuval.döngüdeMi shouldBe true
      }
    }
    val e = new Deneme
    val r = Resim.eskizden(e)
    r.draw()
    r.asInstanceOf[SketchPic].adım()
    e.kurulumSay shouldBe 1
    döngü shouldBe 1
    alfa(r.asInstanceOf[SketchPic], 105, 105) shouldBe 255
  }

  test("Türkçe: tuvalEskizi, canlandırTuvalÇizimle, canlandırKurulumluTuvalÇizimle") {
    import b.trTurtle._
    var kurulum = 0
    class E extends Eskiz {
      def kurulum(t: TuvalÇizim): Birim = ()
      def çizimDöngüsü(t: TuvalÇizim): Birim = ()
    }
    tuvalEskizi(new E, 1)
    canlandırTuvalÇizimle(t => t.döngüyüDurdur())
    canlandırKurulumluTuvalÇizimle(t => kurulum += 1)(t => ())
    kurulum shouldBe 1
  }

  test("durdur() sonrası sil + çiz: yeni döngü kuruluyor, eski zincir etkisiz (tek döngü adım atar)") {
    val kayıtlı = new TestKojoWorld {
      var döngüler = Vector.empty[() => Unit]
      override def animate(fn: => Unit): Unit = { döngüler :+= (() => fn) }
    }
    var döngü = 0
    val p = new SketchPic(_ => (), _ => döngü += 1, 1, b.setRandomSeed)(kayıtlı)
    p.draw()
    kayıtlı.döngüler.size shouldBe 1
    kayıtlı.döngüler(0)()
    döngü shouldBe 1
    p.erase()
    p.draw() // canlandırma arada durdurulmuş olabilir: yeni zincir gerek
    kayıtlı.döngüler.size shouldBe 2
    kayıtlı.döngüler(0)() // eski zincir artık etkisiz
    döngü shouldBe 1
    kayıtlı.döngüler(1)()
    döngü shouldBe 2
  }

  test("Türkçe Eskiz alt sınıfı İngilizce çevirisiyle: Picture.fromSketch(new Gül) ve onAnimationStart") {
    // Çevirmen `çiz(Resim.eskizden(yeni Gül))` -> `draw(Picture.fromSketch(new Gül))` yapıyor; sınıf `extends Eskiz` kalıyor.
    var kurulum, döngü = 0
    class Gül extends kojo.tr.Eskiz {
      def kurulum(tuval: CanvasDraw): Unit = { Gül.this.k += 1; tuval.fill(255, 0, 0); tuval.noStroke() }
      var k = 0
      def çizimDöngüsü(tuval: CanvasDraw): Unit = { döngü += 1; tuval.rect(0, 0, 10, 10) }
    }
    val g = new Gül
    val p = b.Picture.fromSketch(g).asInstanceOf[SketchPic]
    val q = b.Picture.fromSketch(g, 2.0).asInstanceOf[SketchPic]
    p.draw(); p.adım()
    g.k shouldBe 1
    döngü shouldBe 1
    alfa(p, 5, 5) shouldBe 255
    q.tuvalElemanı.width shouldBe 1600
    var basladi = false
    b.onAnimationStart { basladi = true }
    basladi shouldBe true
  }
}
