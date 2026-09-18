package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * `çiz(g); ...; çiz(g)` kalıbı GRUP resimlerinde de çalışmalı (#121).
 *
 * Kusur sessizdi: ikinci çizimde `layout() -> makeDone()` zaten tamamlanmış söze ikinci
 * kez `success` diyor ve IllegalStateException atıyor -- ama hata bir future geri
 * çağrısının içinde olduğu için betiğe hiç ulaşmıyor, yalnız tarayıcı konsoluna düşüyor.
 *
 * Çare `realDraw`ta: yerleşim yalnız bir kez koşuyor (`if (!made) layout()`).
 * `makeDone`'u idempotent yapmak DEĞİL -- ikinci koşunun asıl zararı istisna değil,
 * layoutChildren'ın yeniden koşması (bkz. BatchPics savı).
 */
class GrupYenidenCizimTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  private implicit val w: TestKojoWorld = new TestKojoWorld()
  private val b = new kojo.syntax.Builtins()

  private def kare(n: Double): TurtlePicture = b.PictureT { t =>
    t.invisible(); t.setAnimationDelay(0); t.setFillColor(kojo.doodle.Color.blue)
    var i = 0; while (i < 4) { t.forward(n); t.right(90); i += 1 }
  }
  private def yerler(g: BasePicSequence): Seq[(Double, Double)] =
    g.pics.map(p => (p.tnode.position.x, p.tnode.position.y))

  /** layoutChildren'ın kaç kez koştuğunu sayan sınama grubu -- üretim kodunda sayaç yok. */
  private class SayanGrup(ps: Seq[Picture])(implicit val kojoWorld: KojoWorld) extends BasePicSequence(ps) {
    var yerleşimSayısı = 0
    def layoutChildren(): Unit = yerleşimSayısı += 1
    def copy = new SayanGrup(ps)
  }

  test("ikinci çizimde yerleşim BİR KEZ koşuyor (#121)") {
    // Doğrudan sayıyor. İlk yazdığım sav HPics'in konumlarını karşılaştırıyordu ve
    // KIRILAMIYORDU: HPics idempotent olduğu için korumayı söküp koşturduğumda yeşil
    // kaldı -- istisna sessiz, konumlar aynı, gözlenebilir hiçbir şey yok. Sayaç o
    // boşluğu kapatıyor.
    val g = new SayanGrup(Seq(kare(100), kare(60)))
    g.draw()
    for {
      _ <- g.ready
      birinciden = g.yerleşimSayısı
      _ = g.draw() // ESKİDEN: sessiz IllegalStateException
      _ <- g.ready
    } yield {
      withClue(s"\nilk çizimden sonra=$birinciden ikinci çizimden sonra=${g.yerleşimSayısı}\n") {
        birinciden shouldBe 1
        g.yerleşimSayısı shouldBe 1
        g.made shouldBe true
      }
    }
  }

  test("grup ikinci kez çizilince konumlar oynamıyor (#121)") {
    // Bu sav kusuru YAKALAMAZ (HPics idempotent; korumasız hâlde de yeşil kalıyor,
    // ölçüldü) -- kullanıcının gördüğü sonucun bozulmadığını söylüyor, o kadar.
    // Gerilemeyi tutan savlar: yukarıdaki sayaç ve aşağıdaki BatchPics.
    val g = b.HPics(kare(100), kare(60), kare(80))
    g.draw()
    for {
      _ <- g.ready
      ilk = yerler(g)
      _ = g.draw()
      _ <- g.ready
    } yield {
      withClue(s"\nilk=$ilk sonra=${yerler(g)}\n") {
        g.made shouldBe true
        yerler(g) shouldBe ilk
      }
    }
  }

  test("BatchPics ikinci çizimde görünürlüğünü KAYBETMİYOR (#121'in asıl zararı)") {
    // BatchPics.layoutChildren `pics.tail.invisible()` diyor. showNext ilerledikten sonra
    // yerleşim yeniden koşarsa HİÇBİRİ görünmez kalıyor (ölçüldü: true,false,false ->
    // false,true,false -> false,false,false) ve currPicIndex 1'de kaldığı için sonraki
    // showNext pics(2)'ye atlıyor: pics(1) hiç görünmüyor.
    //
    // Bu sav yerleşimin ikinci çizimde koşMAdığını KULLANICININ GÖRDÜĞÜ bir şeyle
    // çiviliyor; "layout kaç kez çağrıldı" gibi bir sayaç dikişi gerekmiyor.
    val g = kojo.BatchPics(kare(100), kare(60), kare(80))
    g.draw()
    for {
      _ <- g.ready
      _ = g.showNext(-1) // bir ilerlet: pics(1) görünür olsun
      ilerletince = g.pics.map(_.tnode.visible)
      _ = g.draw()
      _ <- g.ready
    } yield {
      withClue(s"\nilerletince=$ilerletince ikinci çizimden sonra=${g.pics.map(_.tnode.visible)} " +
        s"currPicIndex=${g.currPicIndex}\n") {
        ilerletince shouldBe Seq(false, true, false)
        g.pics.map(_.tnode.visible) shouldBe Seq(false, true, false)
      }
    }
  }

  test("makeDone'un 'tam bir kez' sözleşmesi DURUYOR: elle ikinci layout() hâlâ patlıyor") {
    // Seçilen yol makeDone'u gevşetmek değil; orada patlaması, beklenmedik bir yerin onu
    // ikinci kez çağırdığının işareti olarak kalsın. Bu sav o tercihi çiviliyor: kusur
    // kapandı ama sözleşme gevşetilmedi.
    val g = b.GPics(kare(100), kare(60))
    g.draw()
    g.ready.map { _ =>
      an[IllegalStateException] should be thrownBy g.layout()
    }
  }

  test("layoutChildren idempotent (kayan nokta toleransında): konum oynamıyor") {
    // Yerleşimi ikinci çizimde koşturMAmayı seçtik, ama seçimin gerekçesi bu ölçüme
    // dayanıyor: konumlandıran alt sınıflarda formül bounds'u yeniden okuyup DELTA
    // hesaplıyor (offset bağıl), yani bir koşuda yakınsıyor. BatchPics'i ayıran şey
    // konum değil görünürlük olması.
    //
    // TOLERANS gerekli, birebir eşitlik DEĞİL: ölçüldü, üst üste uygulama ~3e-15
    // oynatıyor (19.999999999999993 -> ...96 -> ...93). Delta formülü sıfıra yakınsıyor
    // ama tam sıfır çıkmıyor; toplama sırası yuvarlamayı değiştiriyor. İlk sondamda
    // %.1f ile yazdırdığım için bunu görmemiştim.
    val g = b.VPics2(kare(100), kare(60), kare(80))
    g.draw()
    g.ready.map { _ =>
      val a = yerler(g)
      g.layoutChildren()
      val bb = yerler(g)
      g.layoutChildren()
      val c = yerler(g)
      def yakın(x: Seq[(Double, Double)], y: Seq[(Double, Double)]) =
        x.zip(y).forall { case ((ax, ay), (bx, by)) => (ax - bx).abs < 1e-9 && (ay - by).abs < 1e-9 }
      withClue(s"\n1=$a\n2=$bb\n3=$c\n") {
        yakın(bb, a) shouldBe true
        yakın(c, a) shouldBe true
      }
    }
  }
}
