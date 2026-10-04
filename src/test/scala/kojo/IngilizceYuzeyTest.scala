package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * Masaüstü Kojo'nun İngilizce adları iKojo'da da olmalı: Koco -> Kojo çevirmeni
 * (bulent2k2/kojo, lite/i18n/tr/cevirmen.scala) Türkçe bir betiği bu adlara
 * çeviriyor ve çıkan betik derlenmeli (kojojs-dev#183 Aşama 3). Bu adlar
 * `araclar/cevir-derle.py` kapısında "çeviri açığı" olarak görünüyordu.
 *
 * Her ad Türkçe karşılığıyla AYNI işi yapmalı; çevirmenin seçtiği çift buraya
 * tek tek yazıldı, biri kayarsa iki dil ayrışır.
 */
class IngilizceYuzeyTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  import kojo.syntax.Builtins

  private def yeni() = {
    implicit val kojoWorld: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    b.turtle0.setAnimationDelay(0)
    b
  }

  private def oku(t: TurtleAPI): Future[(Double, Double)] = {
    val s = Promise[(Double, Double)]()
    t.readPosition(p => s.success((p.x, p.y)))
    s.future
  }

  test("jumpTo = setPosition = atla: çizmeden konuma gider") {
    val b = yeni()
    b.turtle0.jumpTo(30, 40)
    oku(b.turtle0).map { p => p shouldBe ((30.0, 40.0)) }
  }

  test("newTurtle: konum ve giysi, yeniKaplumbağa ile aynı; jumpTo bu kaplumbağada da çalışıyor") {
    val b = yeni()
    val t0 = b.newTurtle()
    val t1 = b.newTurtle(10, 20)
    val t2 = b.newTurtle(-5, 7, b.Costume.car)
    t2.jumpTo(1, 2)
    for { p0 <- oku(t0); p1 <- oku(t1); p2 <- oku(t2) } yield {
      p0 shouldBe ((0.0, 0.0)); p1 shouldBe ((10.0, 20.0)); p2 shouldBe ((1.0, 2.0))
    }
  }

  test("mousePosition = fareKonumu: aynı nokta, Point türünde") {
    val b = yeni()
    val p: b.Point = b.mousePosition
    val q = b.trTurtle.fareKonumu
    (p.x, p.y) shouldBe ((q.x, q.y))
  }

  test("gridOn/gridOff/axesOn/axesOff ve Türkçe karşılıkları çağrılabiliyor (etkisi KojoWorld sınamalarında)") {
    val b = yeni()
    // TestKojoWorld'de bu dört sabit işlev; burada yalnız patlamadıkları ve Türkçe karşılıklarının
    // aynı çağrıya gittiği sınanabiliyor (görüntü etkisi KojoWorld sınamalarında).
    b.gridOn(); b.gridOff(); b.axesOn(); b.axesOff()
    b.trTurtle.gridiGöster(); b.trTurtle.gridiGizle(); b.trTurtle.eksenleriGöster(); b.trTurtle.eksenleriGizle()
    succeed
  }

  test("Costume / Background: masaüstündeki değerler; Türkçe Görünüş / Artalan'la aynı yollar") {
    val b = yeni()
    val g = b.trTurtle.Görünüş
    b.Costume.car shouldBe g.araba
    b.Costume.pencil shouldBe g.kalem
    b.Costume.bat1 shouldBe g.yarasa1a
    b.Costume.bat2 shouldBe g.yarasa1b
    b.Costume.womanWaving shouldBe g.kadınElSallarken
    b.Background.trainTrack shouldBe b.trTurtle.Artalan.demiryolu
    // masaüstü Tw.Costume'daki yollar (TurtleWorldAPI.scala:139-143)
    b.Costume.car shouldBe "/media/costumes/car.png"
    b.Costume.womanWaving shouldBe "/media/costumes/womanwaving.png"
  }

  test("scale(fx, fy) = büyüt(x, y): iki argümanlı, aynı dönüştürücü") {
    val b = yeni()
    import b._
    val tr = b.trTurtle
    val ing = Picture.rectangle(10, 20)
    val türkçe = tr.Resim.dikdörtgen(10, 20)
    draw(scale(2, 3) -> ing)
    tr.çiz(tr.büyüt(2, 3) -> türkçe)
    val (a, t) = (ing.tnode.scale, türkçe.tnode.scale)
    (a.x, a.y) shouldBe ((2.0, 3.0))
    (t.x, t.y) shouldBe ((a.x, a.y))
    // tek argümanlı hâl bozulmadı
    val tek = Picture.rectangle(5, 5)
    draw(scale(4) -> tek)
    (tek.tnode.scale.x, tek.tnode.scale.y) shouldBe ((4.0, 4.0))
  }

  test("Rectangle'ın getMinX/getMaxY/...: Türkçe tuvalAlanı x, y, X, Y ile aynı") {
    val b = yeni()
    import b._
    val tr = b.trTurtle
    val cb = canvasBounds
    cb.getMinX shouldBe cb.x
    cb.getMinY shouldBe cb.y
    cb.getMaxX shouldBe tr.tuvalAlanı.X
    cb.getMaxY shouldBe tr.tuvalAlanı.Y
    cb.getWidth shouldBe tr.tuvalAlanı.en
    cb.getHeight shouldBe tr.tuvalAlanı.boy
    cb.getCenterX shouldBe (cb.x + cb.width / 2)
    cb.getCenterY shouldBe (cb.y + cb.height / 2)
    cb.getMaxY should be > cb.getMinY
    // Test dünyasının tuvali kare: en/boy karışsa fark edilmezdi. Kare olmayanla da sına.
    val r = new pixiscalajs.PIXI.Rectangle(10, 20, 30, 40)
    (r.getMinX, r.getMinY, r.getMaxX, r.getMaxY) shouldBe ((10.0, 20.0, 40.0, 60.0))
    (r.getCenterX, r.getCenterY, r.getWidth, r.getHeight) shouldBe ((25.0, 40.0, 30.0, 40.0))
  }

  test("Picture.update: yazı resminde çalışır (dönüştürücü içinden de), ötekilerde masaüstü gibi desteklenmiyor") {
    val b = yeni()
    import b._
    val yazı = Picture.text("ilk")
    val sarılı: Picture = trans(1, 1) * penColor(kojo.doodle.Color.red) -> yazı // duvar-tenisi.kojo'daki biçim
    sarılı.update("ikinci")
    yazı.textNode.text shouldBe "ikinci"
    // Türkçe güncelle aynı yere yazıyor
    val tr = b.trTurtle
    val trYazı = Picture.text("a")
    tr.ResimMetotları(trYazı).güncelle("tr")
    trYazı.textNode.text shouldBe "tr"
    // yazı olmayan: masaüstü notSupported = UnsupportedOperationException
    an[UnsupportedOperationException] should be thrownBy Picture.rectangle(10, 10).update("x")
    an[UnsupportedOperationException] should be thrownBy (trans(1, 1) -> Picture.rectangle(10, 10)).update("x")
  }

  test("Turtle.act / react = Türkçe davran / tepkiVer") {
    class KareDünya extends TestKojoWorld {
      var kare: Option[() => Unit] = None
      override def animate(fn: => Unit): Unit = kare = Some(() => fn)
    }
    implicit val dünya: KareDünya = new KareDünya
    val b = new Builtins()
    b.turtle0.setAnimationDelay(0)
    val t = b.newTurtle(0, 0)
    var çalışma = 0
    t.act { k => çalışma += 1; k.forward(10) } // bir kez, hemen
    çalışma shouldBe 1
    t.react { k => k.forward(1) }
    dünya.kare should not be empty
    dünya.kare.foreach { f => f(); f(); f() } // 3 kare
    oku(t).map { p => p._1 shouldBe 0.0 +- 1e-9; p._2 shouldBe 13.0 +- 1e-9 }
  }

  test("Color.*Gradient: Türkçe Renk.* değişimlerle aynı boya; fillColor/setFillColor Boya alır") {
    import kojo.doodle.Color
    implicit val dünya: KojoWorld = new TestKojoWorld()
    val b = new Builtins()
    val tr = b.trTurtle
    def özet(boya: Boya): Any = boya match {
      case DüzBoya(r)         => ("düz", r)
      case DokuBoya(_, m, y)  => ("doku", y, m.a, m.b, m.c, m.d, m.tx, m.ty)
    }
    val d = Seq(0.0, 0.7, 1.0)
    val r = Seq(Color.red, Color.green, Color.blue)
    özet(Color.linearGradient(0, 0, Color.red, 10, 20, Color.blue, true)) shouldBe
      özet(tr.Renk.doğrusalDeğişim(0, 0, Color.red, 10, 20, Color.blue, true))
    özet(Color.radialGradient(5, 6, Color.red, 40, Color.blue)) shouldBe
      özet(tr.Renk.merkezdenDışarıDoğruDeğişim(5, 6, Color.red, 40, Color.blue))
    özet(Color.linearMultipleGradient(0, 0, 30, 40, d, r)) shouldBe
      özet(tr.Renk.doğrusalÇokluDeğişim(0, 0, 30, 40, d, r))
    özet(Color.radialMultipleGradient(0, 0, 25, d, r, true)) shouldBe
      özet(tr.Renk.merkezdenDışarıDoğruÇokluDeğişim(0, 0, 25, d, r, true))
    // farklı girdi farklı boya (karşılaştırma boş değil): yarıçap matrisi değiştiriyor
    if (PixiUyum.beşVeÜstü)
      özet(Color.radialGradient(0, 0, Color.red, 40, Color.blue)) should not be
        özet(Color.radialGradient(0, 0, Color.red, 80, Color.blue))

    // Boya setFillColor / fillColor üzerinden setFillPaint'e gidiyor
    class Casus extends TextPic("a", 15, Color.red) {
      var gelen: Option[Boya] = None
      override def setFillPaint(boya: Boya): Unit = gelen = Some(boya)
    }
    val g = Color.linearGradient(0, 0, Color.red, 1, 1, Color.blue)
    val c1 = new Casus; c1.setFillColor(g); c1.gelen shouldBe Some(g)
    // dönüştürücü işlevi çizim anında koşuyor
    val c2 = new Casus; (b.fillColor(g) -> c2).draw(); c2.gelen shouldBe Some(g)
  }
}
