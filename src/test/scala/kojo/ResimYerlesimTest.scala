package kojo

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

/**
 * `Resim.*` üreticileri masaüstündekiyle AYNI YERE çizmeli (#75).
 *
 * NEDEN VAR: `Resim.yay` iki tarafta `r` kadar kaymış çiziyordu ve bunu hiçbir
 * şey yakalamadı -- derleyici sustu, CI yeşildi, `adlar.py` de yerindeydi
 * (o adların VARLIĞINI sayıyor, ANLAMINA bakmıyor). Kusuru ancak betiği
 * çizdirip bakınca gördük. Bu sınama o boşluğu kapatıyor: adı değil, YERİ
 * çiviliyor.
 *
 * BEKLENEN SAYILAR NEREDEN GELİYOR: masaüstü Kojo'da (bulent2k2/kojo) ölçüldü,
 * tahmin edilmedi. Yeniden üretmek için oraya şöyle geçici bir sınama koyun:
 *
 * {{{
 * // src/test/scala/net/kogics/kojo/picture/OlcumTest.scala
 * val kojoCtx = new NoOpKojoCtx
 * implicit val spriteCanvas: SpriteCanvas = new SpriteCanvas(kojoCtx)
 * val p = rect2(170, 46); p.draw(); val b = p.bounds   // getX/getY/getWidth/getHeight
 * }}}
 *
 * ve `xvfb-run ... 'testOnly ...OlcumTest'` ile koşturun (CLAUDE.md'deki
 * modern-JDK reçetesi). Türkçe adların İngilizce karşılıkları masaüstünde
 * `lite/i18n/tr/resim.scala`'da; dikkat: masaüstü `dikdörtgen(en, boy)`
 * argümanları takas edip `Picture.rect(boy, en)` çağırıyor.
 *
 * AYAR SATIRI: `yay(100, 90)`. Masaüstünde `ArcPic.initGeom` ve
 * `KPath.createArc` kaynaktan okununca da x=[0,100] y=[0,100] çıkıyor, ölçüm de
 * bunu verdi -- yani iki taraftaki ölçüm düzeneği aynı eksen düzenini
 * kullanıyor. O satır düşerse önce düzeneğe şüpheyle bakın.
 *
 * Sınırlar kalem kalınlığını içerir: 1 piksellik kalem her kenardan 1 taşırır,
 * bu yüzden 100 boyunda bir çizgi [-1, 101] okur. Boşluklarda (hgap/vgap)
 * kalem 0.001 olduğu için taşma yok.
 *
 * `Resim.yay`ın başlangıç noktası ve yönü ayrıca ResimYayTest'te.
 */
class ResimYerlesimTest extends AsyncFunSuite with Matchers {
  // Kurulum kojojs-editor'ün prelude'ünün aynısı (bkz. TurkishPreludeTest).
  implicit val kojoWorld: KojoWorld = new TestKojoWorld()
  val builtins = new kojo.syntax.Builtins()
  import builtins._
  import trTurtle._
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  val pay = 0.5

  // Kaplumbağa kuyruğundan çizilen resimlerde (yay gibi) sınırları kuyruk
  // boşalmadan okumak x=[0,0] döndürüyor -- `ready` beklemek şart.
  def yerinde(ad: String, r: Resim, x1: Double, x2: Double, y1: Double, y2: Double): Future[org.scalatest.Assertion] = {
    r.çiz()
    r.ready.map { _ =>
      val b = r.bounds
      withClue(s"$ad sol kenarı: ") { b.x should be(x1 +- pay) }
      withClue(s"$ad sağ kenarı: ") { (b.x + b.width) should be(x2 +- pay) }
      withClue(s"$ad alt kenarı: ") { b.y should be(y1 +- pay) }
      withClue(s"$ad üst kenarı: ") { (b.y + b.height) should be(y2 +- pay) }
    }(executionContext)
  }

  def kutu1 = Resim.dikdörtgen(50, 30)
  def kutu2 = Resim.dikdörtgen(70, 20)

  // ad -> (resim, masaüstünde ÖLÇÜLEN sınırlar)
  val çizelge: List[(String, () => Resim, (Double, Double, Double, Double))] = List(
    ("dikdörtgen(170,46)",         () => Resim.dikdörtgen(170, 46),         (  -1.0, 171.0,   -1.0,  47.0)),
    ("daire(100)",                 () => Resim.daire(100),                  (-101.0, 101.0, -101.0, 101.0)),
    ("elips(100,50)",              () => Resim.elips(100, 50),              (-101.0, 101.0,  -51.0,  51.0)),
    ("dikdörtgenİçiElips(170,46)", () => Resim.dikdörtgenİçiElips(170, 46), (  -1.0, 171.0,   -1.0,  47.0)),
    ("düz(100,50)",                () => Resim.düz(100, 50),                (  -1.0, 101.0,   -1.0,  51.0)),
    ("yatay(100)",                 () => Resim.yatay(100),                  (  -1.0, 101.0,   -1.0,   1.0)),
    ("dikey(100)",                 () => Resim.dikey(100),                  (  -1.0,   1.0,   -1.0, 101.0)),
    ("yay(100,90) [ayar]",         () => Resim.yay(100, 90),                (  -1.0, 101.0,   -1.0, 101.0)),
    ("yatayBoşluk(100)",           () => Resim.yatayBoşluk(100),            (   0.0, 100.0,    0.0,   0.0)),
    ("dikeyBoşluk(100)",           () => Resim.dikeyBoşluk(100),            (   0.0,   0.0,    0.0, 100.0)),
    ("dizi(50x30, 70x20)",         () => Resim.dizi(kutu1, kutu2),          (  -1.0,  71.0,   -1.0,  31.0)),
    ("diziYatay(50x30, 70x20)",    () => Resim.diziYatay(kutu1, kutu2),     (  -1.0, 123.0,   -1.0,  31.0)),
    ("diziDikey(50x30, 70x20)",    () => Resim.diziDikey(kutu1, kutu2),     (  -1.0,  71.0,   -1.0,  53.0))
  )

  çizelge.foreach { case (ad, yap, (x1, x2, y1, y2)) =>
    test(s"$ad masaüstündeki yerinde") { yerinde(ad, yap(), x1, x2, y1, y2) }
  }
}
