package kojo

import org.scalajs.dom
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

/**
 * CANLI ÇALIŞMA: Türkçe Eskiz örneğinin masaüstü çevirmeniyle üretilmiş İNGİLİZCE çevirisi
 * derlemekle kalmıyor, tuvale gerçekten çiziyor mu.
 *
 * NEDEN: `araclar/cevir-derle.py` kapısı yalnız tür denetimi yapıyor ("sitede çalışır" kanıtı
 * değil). `fireworks-canvas.kojo`nun çevirisi o kapıdan GEÇİYORDU, ama eski çevirmen
 * `tuval.nokta(x, y)`yi `tuval.vertex(x, y)`e çeviriyordu: `vertex` bir şekle köşe ekler, nokta
 * ÇİZMEZ, şekil açılmadan çağrılınca da istisna atar. Betik derleniyor, ilk parçacıkta düşüyordu
 * (düzeltme: kojo#94). Bu sınama o sınıf hatayı (derlenir ama çizmez) yakalamak için duruyor.
 *
 * KOPYA: `FireworksCanvasEn.gövde` bu depodaki `ornekler/masaustu/.../tr/fireworks-canvas.kojo`nun
 * `TranslatorMain --tr2en` çıktısıdır (çevirmen: araclar/kojo-cevirmen-surumu.txt), YORUMLARI
 * ve çevirmenin yapıştırdığı ÇIKTIYA DOKUNULMADAN. Çevirmen sürümü yükseltilince bu kopya
 * yeniden üretilmeli; elle düzeltmek sınamanın amacını (çevirmenin ürettiği) boşa çıkarır.
 */
object FireworksCanvasEn {
  // araclar/cevir-derle.py ONSOZ'uyla aynı içe aktarmalar (sitedeki sarmalayıcı).
  import kojo.doodle.Color._
  import kojo.Speed._
  import kojo.RepeatCommands._
  import kojo.syntax.Builtins

  def gövde(builtins: Builtins)(implicit kojoWorld: KojoWorld): Unit = {
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
    cleari()
    originBottomLeft()
    // artalanıKur(siyah)

    setNoteInstrument(Instrument.ACOUSTIC_BASS)
    playNote(50, 150) // nota, süre
    playNote(45, 200)

    val yerçekimi = Vector2D(0, -0.1)
    val tuvalEni = canvasBounds.width.toInt
    val tuvalBoyu = canvasBounds.height.toInt

    class Parçacık(x0: Double, y0: Double, arıRenk: Double, tohum: Boolean) {
        var yer = Vector2D(x0, y0)
        private var hız = {
            if (tohum) Vector2D(0, random(5, 12))
            else rastgelePatlamaYöneyi
        }
        private var ivme = Vector2D(0, 0)
        private var yaşamSüresi = 255.0
        private var patladıMı = false

        def rastgelePatlamaYöneyi: Vector2D =
            Vector2D(randomNormalDouble, randomNormalDouble) * randomDouble(2, 4)

        def kuvvetiUygula(kuvvet: Vector2D) = { ivme += kuvvet }

        def patlasınMı: Boolean = !patladıMı && yaşamSüresi <= 0

        def ölüMü: Boolean = if (tohum) patladıMı else yaşamSüresi <= 0

        def patla() {
            playNote(15, 30, 127) // nota, süre, ses
            patladıMı = true
        }

        def by() {
            hız += ivme
            yer += hız
            if (!tohum) {
                yaşamSüresi -= 5
                hız *= 0.95
            }
            ivme *= 0
            if (tohum && hız.y < 0) {
                yaşamSüresi = 0
            }
        }

        // gizli dez nokta = Resim.daire(1)

        def visible(tuval: CanvasDraw) {
            tuval.stroke(ColorMaker.hsla(arıRenk, 1, 0.5, yaşamSüresi / 255))
            tuval.strokeWeight(if(tohum) 4 else 2)
            tuval.point(yer.x, yer.y)
        }
    }

    class HavaiFişek() {
        private val arıRenk = random(360)
        private val havaiFişek = new Parçacık (random(tuvalEni), 0, arıRenk, true)
        private val parçacıklar = ArrayBuffer.empty[Parçacık]

        def bittiMi: Boolean = {
            if (havaiFişek.ölüMü & parçacıklar.isEmpty) true else false
        }

        def by() {
            parçacıklar.filterInPlace(p => !p.ölüMü)

            if (!havaiFişek.ölüMü) {
                havaiFişek.kuvvetiUygula(yerçekimi)
                havaiFişek.by()

                if (havaiFişek.patlasınMı) {
                    repeat(100) {
                        parçacıklar.append(
                            new Parçacık (havaiFişek.yer.x, havaiFişek.yer.y, arıRenk, false)
                        )
                    }
                    havaiFişek.patla()
                }
            }
            parçacıklar.foreach { p =>
                p.kuvvetiUygula(yerçekimi)
                p.by()
            }
        }

        def visible(tuval: CanvasDraw) {
            havaiFişek.visible(tuval)
            parçacıklar.foreach { p =>
                p.visible(tuval)
            }
        }
    }

    val havaifişek = ArrayBuffer.empty[HavaiFişek]

    def evreyiGüncelle() {
        havaifişek.filterInPlace(f => !f.bittiMi)
        if (randomDouble(1) < 0.08) {
            havaifişek.append(new HavaiFişek ())
        }
        havaifişek.foreach { _.by() }
    }

    def evreyiGöster(tuval: CanvasDraw) {
        tuval.fill(0, 0, 0, 30)
        tuval.noStroke()
        tuval.rect(0, 0, tuvalEni, tuvalBoyu)
        havaifişek.foreach { _.visible(tuval) }
    }

    animateWithCanvasDraw { tuval =>
        evreyiGüncelle()
        evreyiGöster(tuval)
    }

    // Esin kaynağı:
    //   https://github.com/CodingTrain/Coding-Challenges/tree/main/027_FireWorks/Processing/CC_027_FireWorks_2D

    // Ayrıntılı bilgiler:
    //   https://github.com/litan/kojo-examples/tree/main/fireworks

  }
}

/**
 * `eskiz-gezginler.kojo`nun (masaüstü, Türkçe) `TranslatorMain --tr2en` çıktısı, kojo 7b69c98 ile. Fireworks
 * gibi YORUMLARA ve çıktıya dokunulmadan; çevirmen sürümü yükseltilince yeniden üretilmeli.
 * Bu kopya "sketch sınıfı" dönüşümünü sınar: `yayar Eskiz` atılmış, `kurulum`/`çizimDöngüsü` adları
 * `setup`/`drawLoop` olmuş; `Picture.fromSketch` onları AD ile buluyor.
 */
object GezginlerEn {
  import kojo.doodle.Color._
  import kojo.Speed._
  import kojo.RepeatCommands._
  import kojo.syntax.Builtins

  def gövde(builtins: Builtins)(implicit kojoWorld: KojoWorld): Unit = {
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
    // Eskiz örneği: iz bırakan gezginler.
    // Her gezgin her karede rastgele küçük bir adım atar ve arkasında renkli bir iz bırakır.
    // İzler zamanla sönüyor: her karede tuvalin üstüne çok saydam siyah bir örtü çekiliyor.
    //
    // Neden Eskiz? Her adım için yeni bir Resim (örneğin Resim.daire) çizseydik, Kojo birkaç
    // dakika sonra "Too many pics" sınırına (60000) takılıp çizmeyi bırakırdı (#87).
    // Eskiz tek bir Resim: çizdikleri piksel olduğundan sınır yok, istediğiniz kadar çalışır.
    cleari()
    originBottomLeft() // eskiz resmi tuvalin sol alt köşesinden başlar

    val en = canvasBounds.width
    val boy = canvasBounds.height
    val gezginSayısı = 12
    val adımBoyu = 7.0

    class Gezgin(arıRenk: Double) {
        var x = en / 2
        var y = boy / 2

        def by(tuval: CanvasDraw) {
            val eskiX = x
            val eskiY = y
            x = math.min(en, math.max(0.0, x + randomDouble(-adımBoyu, adımBoyu))) // kenarda durur
            y = math.min(boy, math.max(0.0, y + randomDouble(-adımBoyu, adımBoyu)))
            tuval.stroke(ColorMaker.hsla(arıRenk, 1, 0.5, 0.9))
            tuval.strokeWeight(2)
            tuval.line(eskiX, eskiY, x, y)
        }
    }

    class Gezginler {
        val gezginler = (1 to gezginSayısı).map(i => new Gezgin(i * 360.0 / gezginSayısı))

        def setup(tuval: CanvasDraw) {
            tuval.background(black)
        }

        def drawLoop(tuval: CanvasDraw) {
            tuval.fill(0, 0, 0, 20) // soldurucu örtü
            tuval.noStroke()
            tuval.rect(0, 0, en, boy)
            gezginler.foreach(_.by(tuval))
        }
    }

    draw(Picture.fromSketch(new Gezginler))

  }
}

/**
 * `eskiz-gul.kojo`nun (masaüstü, Türkçe) `TranslatorMain --tr2en` çıktısı, kojo 99d0811 ile; yorumlara ve çıktıya dokunulmadan. iKoco'nun `19-eskiz-gul.kojo` örneğinin GÖVDESİ bununla aynıdır (yalnız yorumlar farklı), o yüzden ayrıca gömülmedi.
 */
object GulEn {
  import kojo.doodle.Color._
  import kojo.Speed._
  import kojo.RepeatCommands._
  import kojo.syntax.Builtins

  def gövde(builtins: Builtins)(implicit kojoWorld: KojoWorld): Unit = {
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
    // Eskiz örneği: gül eğrisi (rhodonea). r = kosinüs(k * açı), k = n / d.
    // Eğri kare kare, her karede biraz daha uzayarak çiziliyor; renk açıyla değişiyor.
    // Eğri kapanınca birkaç saniye bekleyip yeni bir (n, d) seçilir.
    //
    // Dönüşüm kaydet/geri yükle örneği de var: merkeze götür ile çizip dışarıyı bozmuyoruz.
    // Eskiz tek bir Resim olduğundan, binlerce küçük çizgi "Too many pics" sınırına takılmaz (#87).
    cleari()
    originBottomLeft() // eskiz resmi tuvalin sol alt köşesinden başlar

    val en = canvasBounds.width
    val boy = canvasBounds.height
    val yarıçap = 0.45 * math.min(en, boy)
    val seçenekler = Seq((7, 4), (5, 3), (9, 4), (5, 2), (8, 5), (3, 1))
    val karedeAdım = 40       // her karede kaç küçük çizgi
    val açıAdımı = 0.01

    class Gül {
        var (n, d) = seçenekler(0)
        var angle = 0.0
        var bekleme = 0

        def yarıçapı(a: Double) = yarıçap * math.cos(n.toDouble / d * a)

        def yeniSeç(tuval: CanvasDraw) {
            val (n2, d2) = seçenekler(random(seçenekler.length))
            n = n2; d = d2
            angle = 0.0
            tuval.background(black)
        }

        def setup(tuval: CanvasDraw) {
            tuval.background(black)
        }

        def drawLoop(tuval: CanvasDraw) {
            if (angle > 2 * math.Pi * d) {   // eğri kapandı
                bekleme += 1
                if (bekleme > 180) { bekleme = 0; yeniSeç(tuval) }
            }
            else {
                tuval.pushMatrix()
                tuval.translate(en / 2, boy / 2)
                tuval.strokeWeight(2)
                repeat(karedeAdım) {
                    val a2 = angle + açıAdımı
                    val (x1, y1) = (yarıçapı(angle) * math.cos(angle), yarıçapı(angle) * math.sin(angle))
                    val (x2, y2) = (yarıçapı(a2) * math.cos(a2), yarıçapı(a2) * math.sin(a2))
                    tuval.stroke(ColorMaker.hsla((angle * 180 / math.Pi) % 360, 1, 0.55, 1))
                    tuval.line(x1, y1, x2, y2)
                    angle = a2
                }
                tuval.popMatrix()
            }
        }
    }

    draw(Picture.fromSketch(new Gül))
  }
}

/**
 * `eskiz-halkalar.kojo`nun (masaüstü, Türkçe) `TranslatorMain --tr2en` çıktısı, kojo 99d0811 ile; yorumlara ve çıktıya dokunulmadan.
 */
object HalkalarEn {
  import kojo.doodle.Color._
  import kojo.Speed._
  import kojo.RepeatCommands._
  import kojo.syntax.Builtins

  def gövde(builtins: Builtins)(implicit kojoWorld: KojoWorld): Unit = {
    import builtins._
    import turtle._
    import svTurtle._
    import trTurtle._
    // Eskiz örneği: su yüzeyinde halkalar.
    // Rastgele yerlere damlalar düşer; her damla genişleyen ve sönen halkalar bırakır.
    // Her karede tuval baştan boyanıyor (artalan) ve canlı halkaların hepsi yeniden çiziliyor:
    // iz kalmıyor, ama yüzlerce elips bile tek bir Resim, yani tek bir eskiz.
    //
    // Burada neden Resim.daire değil de eskiz? Her halka her karede yeni bir Resim olsaydı
    // Kojo kısa sürede "Too many pics" sınırına (60000) takılırdı (#87).
    cleari()
    originBottomLeft() // eskiz resmi tuvalin sol alt köşesinden başlar

    val en = canvasBounds.width
    val boy = canvasBounds.height
    val enBüyükYarıçap = 0.18 * math.min(en, boy)

    class Halka(x: Double, y: Double, arıRenk: Double) {
        var yaş = 0.0
        def ölüMü = yaş >= 1.0
        def by() { yaş += 0.012 }
        def visible(tuval: CanvasDraw) {
            val opac = 1.0 - yaş            // yaşlandıkça sönüyor
            val r = enBüyükYarıçap * yaş
            tuval.noFill()
            tuval.strokeWeight(3 * opac + 0.5)
            tuval.stroke(ColorMaker.hsla(arıRenk, 0.8, 0.6, opac))
            tuval.ellipse(x, y, 2 * r, 2 * r)
            tuval.ellipse(x, y, 1.3 * r, 1.3 * r)  // iç halka
        }
    }

    class Su {
        val halkalar = ArrayBuffer.empty[Halka]

        def setup(tuval: CanvasDraw) {
            tuval.background(0, 20, 50)
        }

        def drawLoop(tuval: CanvasDraw) {
            if (randomDouble(1) < 0.06) {
                halkalar.append(new Halka(randomDouble(en), randomDouble(boy), randomDouble(160, 220)))
            }
            halkalar.filterInPlace(h => !h.ölüMü)
            tuval.background(0, 20, 50)
            halkalar.foreach { h =>
                h.by()
                h.visible(tuval)
            }
        }
    }

    draw(Picture.fromSketch(new Su))
  }
}

class CeviriCalismaTest extends AnyFunSuite with Matchers {
  private def dünya(): TestKojoWorld = {
    val w = new TestKojoWorld()
    w.sınırlar = new pixiscalajs.PIXI.Rectangle(0, 0, 800, 600)
    w
  }

  /** Sahnedeki son eskiz katmanının ham tuvali. */
  private def tuval(w: TestKojoWorld): dom.html.Canvas = {
    val katman = w.katmanlar.last.asInstanceOf[js.Dynamic]
    katman.children.asInstanceOf[js.Array[js.Dynamic]](0).texture.baseTexture.resource.source
      .asInstanceOf[dom.html.Canvas]
  }

  /** Saydam olmayan ve gri olmayan (kanallar arası fark > 60) piksel sayısı. */
  private def renkliPiksel(c: dom.html.Canvas): Int = {
    val d = c.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D].getImageData(0, 0, c.width, c.height).data
    var n = 0
    var i = 0
    while (i < d.length) {
      val r = d(i).toInt; val g = d(i + 1).toInt; val b = d(i + 2).toInt
      if (d(i + 3).toInt > 0 && (math.max(r, math.max(g, b)) - math.min(r, math.min(g, b))) > 60) n += 1
      i += 4
    }
    n
  }

  test("sınama hattı: eskiz kareleri sürülünce `point` ile çizilen renkli noktalar tuvalde bulunuyor") {
    implicit val w: TestKojoWorld = dünya()
    val b = new kojo.syntax.Builtins()
    b.animateWithCanvasDraw { tuval =>
      tuval.stroke(255, 0, 0)
      tuval.strokeWeight(4)
      tuval.point(100, 100)
    }
    w.kareleriSür(3)
    renkliPiksel(tuval(w)) should be > 10
  }

  // Çevirmen kojo 4af4e3a'dan beri `tuval.nokta(x, y)`yi `tuval.point(x, y)`e çeviriyor (alıcı
  // `tuval: TuvalÇizim` bildirimli ya da canlandırTuvalÇizimle lambda parametresi; bkz. kojo#94).
  // Önceki sürümde `vertex`e çevriliyor, betik derlenip çalışınca beginShape olmadan düşüyordu.
  test("fireworks-canvas İngilizce çevirisi çalışıyor: 400 kare sonra tuvalde RENKLİ piksel var (parçacıklar çiziliyor)") {
    implicit val w: TestKojoWorld = dünya()
    val b = new kojo.syntax.Builtins()
    b.setRandomSeed(7)
    FireworksCanvasEn.gövde(b)
    w.kareleriSür(400)
    val n = renkliPiksel(tuval(w))
    info(s"renkli piksel: $n")
    n should be > 200
  }

  test("eskiz-gezginler İngilizce çevirisi çalışıyor: sketch sınıfı (setup/drawLoop) bulunuyor ve tuvale renkli iz çiziliyor") {
    implicit val w: TestKojoWorld = dünya()
    val b = new kojo.syntax.Builtins()
    b.setRandomSeed(7)
    GezginlerEn.gövde(b)
    w.kareleriSür(200)
    val n = renkliPiksel(tuval(w))
    info(s"renkli piksel: $n")
    n should be > 200
  }

  test("eskiz-gul İngilizce çevirisi çalışıyor: 200 kare sonra tuvalde renkli gül eğrisi var") {
    implicit val w: TestKojoWorld = dünya()
    val b = new kojo.syntax.Builtins()
    b.setRandomSeed(7)
    GulEn.gövde(b)
    w.kareleriSür(200)
    val n = renkliPiksel(tuval(w))
    info(s"renkli piksel: $n")
    n should be > 200
  }

  test("eskiz-halkalar İngilizce çevirisi çalışıyor: 300 kare sonra tuvalde renkli halkalar var") {
    implicit val w: TestKojoWorld = dünya()
    val b = new kojo.syntax.Builtins()
    b.setRandomSeed(7)
    HalkalarEn.gövde(b)
    w.kareleriSür(300)
    val n = renkliPiksel(tuval(w))
    info(s"renkli piksel: $n")
    n should be > 200
  }
}
