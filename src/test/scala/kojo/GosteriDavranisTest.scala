package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Gösterilerin NE GÖSTERDİĞİNİ sınayan ilk denetim.
 *
 * Var olan iki denetim bu hatayı göremiyordu: üreteçteki gidiş-dönüş sınaması
 * KODLAMAYI doğruluyor, OrnekDerlemeDeneme ise API ADLARINI. İkisi de yeşilken
 * `resimleriSil` gösterisi bomboş tuval gösteriyordu.
 *
 * Sebep: betiğin gövdesi TEK bir eşzamanlı blok. `çiz` (Picture.draw) ve
 * `resimleriSil` (KojoWorld.erasePictures) doğrudan çalışıyor, `durakla` ise
 * kuyruğa girip hemen dönüyor (Turtle.sıraya -> commandQ). JavaScript tek iş
 * parçacıklı olduğu için tarayıcı arada hiç boyama yapamıyor: daire çizilip
 * AYNI karede siliniyor. `durakla(1)` koymak bunu değiştirmiyor.
 *
 * Buradaki sav pikselleri değil MEKANİZMAYI çiviliyor: silme, betiğin gövdesi
 * dönerken DEĞİL, sonraki karelerde olmalı.
 */
class GosteriDavranisTest extends AnyFunSuite with Matchers {

  /** erasePictures çağrılarını sayan ve canlandırma karelerini elle süren dünya. */
  private class SayanDünya extends TestKojoWorld {
    var silmeSayısı = 0
    private var kareİşlevi: Option[() => Unit] = None
    private var duruldu = false

    override def erasePictures(): Unit = silmeSayısı += 1
    override def animate(fn: => Unit): Unit = kareİşlevi = Some(() => fn)
    override def stopAnimation(): Unit = duruldu = true

    /** Tarayıcının yapacağı şeyi elle yapar: n kare çevirir. */
    def kareSür(n: Int): Unit =
      (1 to n).foreach(_ => if (!duruldu) kareİşlevi.foreach(_()))
  }

  test("resimleriSil gösterisi: silme gövde dönerken değil, sonraki karelerde oluyor") {
    implicit val dünya = new SayanDünya
    val builtins = new kojo.syntax.Builtins()
    import builtins._
    import builtins.trTurtle._

    // araclar/gosteri-uret.py'deki gösterinin ta kendisi (anahtar sözcükler
    // İngilizce -- kojojs-dev stok Scala.js ile derleniyor)
    silVeSakla
    çiz(boyaRengi(mavi) -> Resim.daire(40))
    var kare = 0
    canlandır {
      kare += 1
      if (kare == 60) { resimleriSil(); canlandırmayıDurdur() }
    }

    // ASIL SAV: gövde döndüğünde daire HÂLÂ duruyor olmalı. Eski gösteri
    // (çiz; durakla(1); resimleriSil()) burada 1 verirdi -- çocuğun boş tuval
    // görmesinin sebebi tam olarak buydu.
    dünya.silmeSayısı should be(0)

    dünya.kareSür(59)
    dünya.silmeSayısı should be(0) // 60. kareden önce hâlâ görünür

    dünya.kareSür(1)
    dünya.silmeSayısı should be(1) // ve sonra siliniyor

    dünya.kareSür(120)
    dünya.silmeSayısı should be(1) // canlandırmayıDurdur işini görüyor
  }
}
