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

import kojo.doodle.Color
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
 * Sorun #91: her karede `resimleriSil()` + yeniden çizim yapan bir `canlandır`
 * döngüsünde PIXI'nin GL sayaçları SINIRSIZ büyüyordu.
 *
 * İki ayrı kusur vardı, ikisi de burada çivileniyor:
 *
 *  1. `erasePictures()` "Turtle Layer" ADLI çocukları atlıyordu -- ama
 *     `Turtle.init` o adı `forPic` kaplumbağalara da veriyor, yani Resim{}
 *     katmanları HİÇ SİLİNMİYORDU. Bu, sızıntıdan ÖNCE bir doğruluk kusuru:
 *     resimler ekranda kalıyordu. Sahnedeki çocuk sayısının kare başına
 *     artması (GL'den ayrı olarak O(N) çizim) onun yan ürünüydü. Ölçüt artık
 *     BakePolicy'de: gerçek kaplumbağanın katmanında "Turtle Icon" çocuğu var.
 *  2. Katmanı sahneden çıkarmak GL kaynağını BIRAKMIYOR: PIXI 5 geometriyi
 *     çizicinin managedGeometries/managedBuffers haritalarında tutuyor ve
 *     oradan yalnız `dispose()` düşürüyor.
 *
 * NEDEN GERÇEK KojoWorldImpl: kusur tam olarak TestKojoWorld'ün saplamadığı
 * yerdeydi (`TestKojoWorld.erasePictures` boş bir saplama) ve sayaçlar çiziciye
 * ait -- çizicisiz ölçülemiyor.
 *
 * NEDEN ÇOK KÖŞELİ ŞEKİL: PIXI 5 küçük geometrileri tek partide topluyor, onlar
 * kendi GL kaynaklarını hiç almıyor. Kusur yalnız parti DIŞI kalan şekillerde
 * (çok köşeli çokgen, çember/yay) görünür oluyor -- 180 kenar bunun için.
 *
 * ÜÇÜNCÜ KOL, gradyan dokusu (sorun #95): gradyan (`Boya`) dolgunun
 * BaseTexture'ı geometriden ayrı bir kaynak ve bir süre bu çarenin dışında
 * kaldı -- geometri/tampon/sahne tavanlanırken doku sayacı doğrusal büyümeyi
 * sürdürüyordu. Artık `gradyanDokularınıBırak` onu da bırakıyor; aşağıda iki
 * sınamayla çivili.
 * Pişirmede aynı ad karışıklığının ikinci kolu -- sorun #96.
 *
 * Ölçüm (aşağıdaki döngü, 120 kare; geometri/tampon/sahne çocuğu):
 *   düzeltmeden önce : 20. karede 29/58/36 ... 120. karede 229/458/236 (doğrusal)
 *   düzeltmeden sonra: 2/4/2 -- yaşayan iki resim; tavan var, büyüme yok
 *     (ilk karelerde sahne çocuğu 3: varlık yükleme göstergesi de duruyor)
 */
class KaynakSizintisiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   * Her sınama kendi tuvalini kursun: KojoWorldImpl DOM'u id ile arıyor.
   *
   * WebGL'siz bir ortamda (bu üçü de gerçek çiziciye bağlı) sınamalar
   * PATLAMAK yerine İPTAL olsun diye kurulum try içinde: başsız Chrome WebGL'i
   * SwiftShader'la veriyor, ama bunu CI ortamına şart koşmuyoruz.
   */
  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try dünyaKur()
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def dünyaKur(): KojoWorldImpl = {
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
    val kap = document.createElement("div").asInstanceOf[HTMLElement]
    kap.id = "fiddle-container"
    kap.style.width = "400px"
    kap.style.height = "300px"
    val tuval = document.createElement("div").asInstanceOf[HTMLElement]
    tuval.id = "canvas-holder"
    kap.appendChild(tuval)
    document.body.appendChild(kap)
    new KojoWorldImpl()
  }

  /** (geometri, tampon). WebGL yoksa (tuval çizici) None. */
  private def glSayaçları(w: KojoWorldImpl): Option[(Int, Int)] = {
    val g = w.renderer.asInstanceOf[js.Dynamic].geometry
    if (js.isUndefined(g) || g == null || js.isUndefined(g.managedGeometries)) None
    else
      Some(
        (
          js.Object.keys(g.managedGeometries.asInstanceOf[js.Object]).length,
          js.Object.keys(g.managedBuffers.asInstanceOf[js.Object]).length
        )
      )
  }

  /** Çizicinin yüklü doku sayısı. PIXI 5'te managedTextures bir DİZİ (harita değil). */
  private def dokuSayısı(w: KojoWorldImpl): Option[Int] = {
    val t = w.renderer.asInstanceOf[js.Dynamic].texture
    if (js.isUndefined(t) || t == null || js.isUndefined(t.managedTextures)) None
    else Some(t.managedTextures.asInstanceOf[js.Array[js.Any]].length)
  }

  private def gradyan(): Boya = Boya.doğrusal(-100, -100, Color.red, 100, 100, Color.blue, false)

  /** Bir resimdeki dolgu dokularının imlerini döndürür: GRADYAN / başka / dokusuz. */
  private def dolguDokuları(p: TurtlePicture)(implicit w: KojoWorldImpl): Seq[String] = {
    w.boyalarıBoşalt()
    p.tnode.asInstanceOf[js.Dynamic].children.asInstanceOf[js.Array[js.Dynamic]].toSeq.flatMap { g =>
      if (js.typeOf(g.finishPoly) != "function") Nil
      else
        g.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].toSeq.map { gd =>
          val t = gd.fillStyle.texture
          if (js.isUndefined(t) || t == null) "dokusuz"
          else if (t.baseTexture.selectDynamic(Boya.GradyanDokusuİmi)
                     .asInstanceOf[js.UndefOr[Boolean]].contains(true)) "GRADYAN"
          else "başka"
        }
    }
  }

  private def gradyanlıResim(b: Boya)(implicit w: KojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setAnimationDelay(0)
    t.setFillPaint(b)
    var i = 0
    while (i < 180) { t.forward(4); t.right(2); i += 1 }
  }

  private def çokKöşeliResim()(implicit w: KojoWorld): TurtlePicture = TurtlePicture { t =>
    t.setAnimationDelay(0)
    var i = 0
    while (i < 180) { t.forward(4); t.right(2); i += 1 }
  }

  /** n kare koştur; her karede adım(kareNo). */
  private def kareler(n: Int)(adım: Int => Unit): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def döngü(): Unit = {
      i += 1
      adım(i)
      if (i >= n) söz.success(()) else window.requestAnimationFrame(_ => döngü())
    }
    window.requestAnimationFrame(_ => döngü())
    söz.future
  }

  /** Koşul sağlanana dek kare kare bekle (kaplumbağa init'i varlık yüklemesine bağlı). */
  private def bekle(koşul: => Boolean, enÇokKare: Int = 300): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    def bak(): Unit = {
      i += 1
      if (koşul) söz.success(())
      else if (i >= enÇokKare) söz.failure(new RuntimeException(s"koşul $enÇokKare karede sağlanmadı"))
      else window.requestAnimationFrame(_ => bak())
    }
    window.requestAnimationFrame(_ => bak())
    söz.future
  }

  test("canlandır döngüsünde resimleriSil() GL kaynaklarını ve sahneyi sınırlı tutuyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (glSayaçları(w).isEmpty) {
      Future.successful(cancel("WebGL çizici yok; GL sayaçları okunamıyor"))
    }
    else {
      // HER karede ölçüyoruz ve EN BÜYÜKlere bakıyoruz. Tek tek kareye bakmak
      // kırılgan: render rAF'a toplandığı için ara sıra bir ölçüm o karenin
      // render'ından ÖNCE düşüyor ve 0 okunuyor (ölçüldü: 6 ölçümün biri).
      // En büyük, o gürültüden etkilenmiyor ve asıl çivilediğimiz şeyi --
      // "tavan var mı" -- doğrudan söylüyor.
      val ölçümler = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int)]
      val kareSayısı = 121
      kareler(kareSayısı) { i =>
        // Ölçüm kareyi AÇARKEN: bir önceki karenin render'ı bitti, bu karenin
        // silmesi henüz başlamadı -- yani yaşayan içeriğin gerçek maliyeti.
        if (i > 1) {
          val (g, b) = glSayaçları(w).get
          ölçümler += ((g, b, w.stage.children.length))
        }
        w.erasePictures()
        var n = 0
        while (n < 2) { çokKöşeliResim().draw(); n += 1 }
      }.map { _ =>
        val enBüyük = (ölçümler.map(_._1).max, ölçümler.map(_._2).max, ölçümler.map(_._3).max)
        val (ilkYarı, sonYarı) = ölçümler.splitAt(ölçümler.size / 2)
        withClue(s"en büyük (geometri,tampon,sahne çocuğu) = $enBüyük, " +
          s"ilk yarı ${ilkYarı.map(_._1).max} / son yarı ${sonYarı.map(_._1).max} geometri -- ") {
          ölçümler should have size (kareSayısı - 1)
          // Sızıntı varken 120. karede 229/458/236 idi. Tavanlar cömert:
          // çivilediğimiz şey DOĞRUSAL BÜYÜMENİN OLMAMASI, kesin sayı değil.
          enBüyük._1 should be <= 12
          enBüyük._2 should be <= 24
          enBüyük._3 should be <= 6
          // Son yarı ilk yarıdan BÜYÜK olmamalı. Eşitlik istemiyoruz: ilk
          // karelerde varlık yükleme göstergesi (AssetLoader.showLoading) da
          // sahnede duruyor, yani sahne çocuğu 3'ten 2'ye DÜŞÜYOR. Sızıntı
          // olsaydı son yarı ilk yarının iki katı olurdu.
          withClue("son yarı ilk yarıdan büyük olmamalı (büyüme yok) -- ") {
            sonYarı.map(_._1).max should be <= ilkYarı.map(_._1).max
            sonYarı.map(_._2).max should be <= ilkYarı.map(_._2).max
            sonYarı.map(_._3).max should be <= ilkYarı.map(_._3).max
          }
        }
      }
    }
  }

  test("resimleriSil() duran kaplumbağanın katmanına dokunmuyor, resminkini siliyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val kaplumbağa = new Turtle(0, 0)
    val resim = çokKöşeliResim()
    resim.draw()
    for {
      _ <- resim.ready
      _ <- bekle(kaplumbağa.turtleLayer.parent != null)
    } yield {
      withClue("kurulum: resim de sahnede olmalı -- ") {
        resim.tnode.parent should not be null
      }
      w.erasePictures()
      withClue("gerçek kaplumbağa sahnede kalmalı -- ") {
        kaplumbağa.turtleLayer.parent should not be null
      }
      withClue("Resim{} katmanı sahneden çıkmalı (#91: ada bakınca çıkmıyordu) -- ") {
        resim.tnode.parent shouldBe null
      }
    }
  }

  test("silinen resim yeniden çizilebiliyor (destroy değil dispose)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val resim = çokKöşeliResim()
    resim.draw()
    for {
      _ <- resim.ready
      _ <- kareler(2)(_ => ())
      öncesi = şekilSayısı(resim)
      _ = w.erasePictures()
      _ = resim.draw()
      _ <- kareler(2)(_ => ())
    } yield {
      withClue("dispose yalnız GL tarafını bırakmalı; şekil verisi durmalı -- ") {
        öncesi should be > 0
        şekilSayısı(resim) shouldBe öncesi
      }
      resim.tnode.parent should not be null
    }
  }

  /** Katmandaki bütün Graphics parçalarının şekil sayısı (CPU tarafı veri). */
  test("gradyan dolgulu canlandır döngüsünde doku sayacının tavanı var (#95)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (dokuSayısı(w).isEmpty) Future.successful(cancel("WebGL çizici yok; doku sayacı okunamıyor"))
    else {
      // Her karede YENİ bir Boya: #91'in betiğinin gradyan hâli. Her Boya kendi
      // BaseTexture'ını üretiyor, resim silinince onu bırakan tek şey dispose().
      // Ölçüldü (121 kare): bırakmadan 0 -> 132 doğrusal; bırakmayla en çok 3.
      val ölçümler = scala.collection.mutable.ArrayBuffer.empty[Int]
      val kareSayısı = 121
      kareler(kareSayısı) { i =>
        if (i > 1) ölçümler += dokuSayısı(w).get
        w.erasePictures()
        var n = 0
        while (n < 2) { gradyanlıResim(gradyan()).draw(); n += 1 }
      }.map { _ =>
        val (ilkYarı, sonYarı) = ölçümler.splitAt(ölçümler.size / 2)
        withClue(s"doku sayacı: en büyük ${ölçümler.max}, ilk yarı ${ilkYarı.max} / son yarı ${sonYarı.max} -- ") {
          ölçümler should have size (kareSayısı - 1)
          // Tavan cömert: çivilediğimiz şey DOĞRUSAL BÜYÜMENİN OLMAMASI.
          ölçümler.max should be <= 12
          withClue("son yarı ilk yarıdan büyük olmamalı (büyüme yok) -- ") {
            sonYarı.max should be <= ilkYarı.max
          }
        }
      }
    }
  }

  test("bırakılan gradyan dokusu yeniden çizilince gradyan olarak geri geliyor (#95)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (dokuSayısı(w).isEmpty) Future.successful(cancel("WebGL çizici yok; doku sayacı okunamıyor"))
    else {
      // Asıl risk sızıntı değil, GERİLEME: `Boya` kullanıcının elinde yaşıyor
      // olabilir (aynı b birden çok resimde), yani bıraktığımız dokuyu bir
      // başkası hâlâ kullanıyor olabilir. dispose() yalnız GL yüklemesini
      // bırakıyor, tuval KAYNAĞI duruyor ve doku yeniden çizilince geri
      // yükleniyor.
      //
      // AYIRT EDİCİ ALAN `resource`, `valid` DEĞİL -- ölçüldü: destroy() ile de
      // valid TRUE kalıyor ve doku sayacı yine artıyor, ama resource kopuyor,
      // yani doku bir daha asla üretilemiyor. Yalnız valid'e bakan bir sav
      // dispose ile destroy'u AYIRT EDEMİYORDU (kırma sınamasıyla görüldü:
      // destroy'a çevirince sav yeşil kalıyordu). Asıl çivi resource.
      val b = gradyan()
      val taban = b.asInstanceOf[DokuBoya].doku.asInstanceOf[js.Dynamic].baseTexture
      var ilkÇizim = Seq.empty[String]
      var bırakmaSonrası = (true, -1)
      var yenidenÇizim = Seq.empty[String]
      var yenidenSayaç = -1
      var yenidenValid = false
      var yenidenKaynak = false
      var p1: TurtlePicture = null
      var p2: TurtlePicture = null
      kareler(40) { i =>
        if (i == 3) { p1 = gradyanlıResim(b); p1.draw() }
        if (i == 12) { ilkÇizim = dolguDokuları(p1); w.erasePictures() }
        if (i == 18) { bırakmaSonrası = (taban.valid.asInstanceOf[Boolean], dokuSayısı(w).get) }
        if (i == 22) { p2 = gradyanlıResim(b); p2.draw() }
        if (i == 34) { yenidenÇizim = dolguDokuları(p2); yenidenSayaç = dokuSayısı(w).get
                       yenidenValid = taban.valid.asInstanceOf[Boolean]
                       yenidenKaynak = !js.isUndefined(taban.resource) && taban.resource != null }
      }.map { _ =>
        withClue(s"ilk çizim ${ilkÇizim.count(_ == "GRADYAN")} gradyan parça, " +
          s"bırakma sonrası valid=${bırakmaSonrası._1} sayaç=${bırakmaSonrası._2}, " +
          s"yeniden çizim ${yenidenÇizim.count(_ == "GRADYAN")} gradyan parça, " +
          s"sayaç=$yenidenSayaç valid=$yenidenValid kaynak=$yenidenKaynak -- ") {
          ilkÇizim.count(_ == "GRADYAN") should be > 0
          // valid true kalmalı: yoksa yeniden çizim yedek renge düşer.
          bırakmaSonrası._1 shouldBe true
          yenidenÇizim.count(_ == "GRADYAN") should be > 0
          yenidenÇizim.count(_ == "GRADYAN") shouldBe ilkÇizim.count(_ == "GRADYAN")
          // Gerçekten GERİ YÜKLENDİ mi: doku çizicinin kaydına dönmeli, ve
          // tuval kaynağı ayakta kalmalı (destroy onu koparırdı).
          withClue("yeniden çizimden sonra doku çizicinin kaydına dönmeli -- ") {
            yenidenSayaç should be > bırakmaSonrası._2
          }
          yenidenValid shouldBe true
          yenidenKaynak shouldBe true
        }
      }
    }
  }

  test("PAYLAŞILAN Boya: bir resim silinince ayakta kalan öteki bozulmuyor (#95)") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    if (dokuSayısı(w).isEmpty) Future.successful(cancel("WebGL çizici yok; doku sayacı okunamıyor"))
    else {
      // #95'in ASIL kaygısı buydu: "aynı b'yi birden çok resme vermek geçerli;
      // bir resim silindi diye dokusunu körlemesine bırakmak, b'yi hâlâ tutan
      // başka bir resmi bozardı." Yukarıdaki sav sil-sonra-YENİDEN-ÇİZ kurar;
      // burada resim silinmiyor, SAHNEDE KALIYOR -- yani bırakılan dokuyu hâlâ
      // kullanan canlı bir resim var. dispose() tembel yeniden yüklediği için
      // bu da bozulmuyor; destroy() olsaydı bozulurdu (inceleme #97).
      //
      // YÜKÜ `kaynak` TAŞIYOR: kırma sınamasında (dispose -> destroy) parça
      // sayısı 179'da KALIYOR, yani yapısal sayım bozulmayı görmüyor -- sav
      // yalnız ona dayansa yeşil kalırdı. Kıran sav `resource`. İnceleme bunu
      // bir kademe aşağıdan da doğruladı: destroy ile opak piksel 41975'ten
      // 1441'e, ayrı renk 39'dan 1'e düşüyor (yani gradyan gerçekten çöküyor),
      // ama parça sayısı aynı kalıyor.
      val b = gradyan()
      val taban = b.asInstanceOf[DokuBoya].doku.asInstanceOf[js.Dynamic].baseTexture
      var önce = (0, 0)
      var sonra = (0, -1)
      var sağlam = (false, false, false)
      var a: TurtlePicture = null
      var kalan: TurtlePicture = null
      kareler(40) { i =>
        if (i == 3) { a = gradyanlıResim(b); a.draw(); kalan = gradyanlıResim(b); kalan.draw() }
        if (i == 14) {
          önce = (dolguDokuları(a).count(_ == "GRADYAN"), dolguDokuları(kalan).count(_ == "GRADYAN"))
          a.erase()
        }
        if (i == 30) {
          sonra = (dolguDokuları(kalan).count(_ == "GRADYAN"), dokuSayısı(w).get)
          val düğüm = kalan.tnode.asInstanceOf[js.Dynamic]
          sağlam = (
            taban.valid.asInstanceOf[Boolean],
            !js.isUndefined(taban.resource) && taban.resource != null,
            !js.isUndefined(düğüm.parent) && düğüm.parent != null
          )
        }
      }.map { _ =>
        withClue(s"önce(A=${önce._1}, kalan=${önce._2}) parça, sonra kalan=${sonra._1} parça, " +
          s"doku sayacı=${sonra._2}, valid=${sağlam._1} kaynak=${sağlam._2} sahnede=${sağlam._3} -- ") {
          önce._1 should be > 0
          önce._2 should be > 0
          sağlam._3 shouldBe true // silinmeyen resim sahnede kalmalı
          // Ayakta kalan resmin gradyan dolgusu eksiksiz duruyor.
          sonra._1 shouldBe önce._2
          // Kaynak ayakta: doku yeniden yüklenebilir. destroy() burayı koparır.
          sağlam._2 shouldBe true
          sağlam._1 shouldBe true
          // Paylaşılan tek doku: sayaç şişmiyor.
          sonra._2 should be <= 12
        }
      }
    }
  }

  private def şekilSayısı(p: Picture): Int = {
    val kap = p.tnode.asInstanceOf[pixiscalajs.PIXI.Container]
    kap.children.toSeq
      .map(_.asInstanceOf[js.Dynamic])
      .filter(d => js.typeOf(d.finishPoly) == "function")
      .map { d =>
        d.finishPoly()
        d.geometry.graphicsData.asInstanceOf[js.Array[js.Dynamic]].length
      }
      .sum
  }
}
