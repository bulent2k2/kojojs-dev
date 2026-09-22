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

/**
 * Komut pompası: trampolin + kareye teslim (#131).
 *
 * Gerçek `KojoWorldImpl` ile, çünkü `TestKojoWorld.scheduleLater` düz bir
 * `setTimeout(0)` -- pompanın kendisini hiç sınamıyor. WebGL yoksa İPTAL
 * (SolukTest gerekçesi).
 *
 * ÖLÇÜLDÜ (SwiftShader, 300k komutluk kalemli iş, iki geçiş):
 *
 *   Dilim  toplam ms      komut/ms     kare/s   en uzun kare arası
 *     4    409            737          64       18
 *     8    187 / 187      1609 / 1611  64 / 59  17 / 19
 *    12    111 / 210      2714 / 1432  54 / 43  18 / 35
 *    16    139 / 247      2161 / 1220  43 / 36  25 / 31
 *
 * 8 ms: iki geçişte aynı sayı, 60 kare/s korunuyor, en uzun ara tek kare.
 * 12 ve üstü kare bütçesini aşıyor: bir vsync bütünüyle kaçıyor, pompa 33
 * ms'de bir uyanıyor ve toplam süre ARTIYOR (12'de ikinci geçiş 8'den
 * yavaş). 4 kadansı koruyor ama işi yarıya indiriyor. Eski pompa
 * (setTimeout, 100 komutta bir): aynı iş için bkz. sınıf sonundaki not.
 */
class KuyrukPompasiTest extends AsyncFunSuite with Matchers {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"; kap.style.width = "400px"; kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"; kap.appendChild(tuval); document.body.appendChild(kap)
      new KojoWorldImpl()
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def kaplumbağa(w: KojoWorldImpl): Turtle = {
    implicit val iw: KojoWorldImpl = w
    val t = new Turtle(0, 0); t.setAnimationDelay(0); t.invisible(); t.setPenThickness(1); t
  }

  /** n kenarlı kalemli gül; kuyruk buraya varınca sözü tamamlar. */
  private def gülVeBekle(t: Turtle, n: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    val kenar = 2 * 140.0 * math.sin(math.toRadians(7 * 180.0 / n)); val dönüş = 7 * 360.0 / n
    var i = 0; while (i < n) { t.forward(kenar); t.right(dönüş); i += 1 }
    t.sync { () => söz.success(()) }
    söz.future
  }

  /**
   * ÖLÇÜT 1: yığın derinliği seri boyundan bağımsız. Dilim sonsuz, yani hiç
   * teslim yok: 20 000 komut tek eşzamanlı koşuda. Eski yapı (iş doğrudan
   * çağrılıyor, queueHandler yine scheduleLater diyor) bunu karşılıklı
   * özyinelemeyle 20 000 derinliğe iner ve kırılırdı -- 1000'de kırıldığı
   * ölçülmüştü (#131). Mutasyon: `pompayıSürdür`ün döngüsü yerine işi
   * doğrudan çağır -> bu sav JS hatasıyla düşüyor.
   */
  test("YIĞIN: dilimsiz 20 000 komut tek koşuda bitiyor, özyineleme yok (#131 ölçüt 1)") {
    val w = dünyaKurYaDaİptal()
    w.DilimMs = Double.PositiveInfinity
    val t = kaplumbağa(w)
    val t0 = window.performance.now()
    gülVeBekle(t, 10000).map { _ =>
      w.DilimMs = 8.0
      // Kuyruk buraya vardıysa özyineleme yok demektir; süre yalnız bilgi.
      info(s"20 000 komut ${math.round(window.performance.now() - t0)} ms")
      succeed
    }
  }

  /**
   * İç içe zamanlama pompayı DONDURMUYOR ve sırayı koruyor. `a`nın sync geri
   * çağrımı içinden boştaki `b`ye komut: `b`nin pompası kuyruğa girer,
   * ardından `a`nınki. Tek yuvalı bir kutu ikincisini ezer, `b` `boşta =
   * false`ta takılır ve bir daha hiç kıpırdamazdı.
   */
  test("İÇ İÇE: geri çağrımdan başka kaplumbağaya komut -- ikisi de bitiyor, b önce (#131)") {
    val w = dünyaKurYaDaİptal()
    val a = kaplumbağa(w); val b = kaplumbağa(w)
    // Pompalar kaynak (kaplumbağa simgesi) yüklenince başlıyor; ikisinin de
    // başladığından emin olmadan sıra savı zamanlamaya bağlı kalırdı.
    val hazır = for { _ <- gülVeBekle(a, 4); _ <- gülVeBekle(b, 4) } yield ()
    hazır.flatMap { _ =>
      a.clear(); b.clear()
      val söz = Promise[(Double, Double)]()
      var bBitti = false; var aDevam = false; var sıra = ""
      a.sync { () =>
        // Kuyruk buraya varınca a'nın kuyruğu boş, b boşta.
        b.forward(50) // b boşta: pompası bu geri çağrımın içinden zamanlanır
        b.sync { () => bBitti = true; sıra += "b" }
        a.forward(20)
        a.sync { () => aDevam = true; sıra += "a"; söz.success((a.position.y, b.position.y)) }
      }
      söz.future.map { case (ay, by) =>
        withClue(s"sıra=$sıra a.y=$ay b.y=$by -- ") {
          bBitti shouldBe true
          aDevam shouldBe true
          // b'nin pompası kuyruğa ÖNCE girdi (geri çağrımın içinden), a'nınki
          // realSync'ten sonra: b'nin 50'si a'nın 20'sinden önce biter.
          sıra shouldBe "ba"
          by shouldBe 50.0 +- 0.01 // yön kuzey: ileri y'yi büyütür
          ay shouldBe 20.0 +- 0.01
        }
      }
    }
  }

  /**
   * BİR İŞ PATLARSA ötekiler mahsur kalmıyor (#145 incelemesi §1, sondanın
   * şekli incelemecinin). En dıştaki işin içinden üç iş zamanlanıyor -- o
   * sırada pompa dönüyor, üçü de yalnız kuyruğa giriyor -- ortadaki
   * fırlatıyor. Üçüncüsü AYNI koşuda koşmalı, ve hata çağırana ulaşmalı.
   * Düzeltmeden önce iz [A] ve C bir sonraki scheduleLater'a kadar mahsurdu.
   */
  test("PATLAK İŞ: ortadaki fırlatınca üçüncüsü aynı koşuda koşuyor, hata sızıyor (#145 §1)") {
    val w = dünyaKurYaDaİptal()
    var iz = ""
    var sızan: Throwable = null
    try w.scheduleLater {
      w.scheduleLater { iz += "A" }
      w.scheduleLater { iz += "!"; throw new RuntimeException("sonda-patlak") }
      w.scheduleLater { iz += "C" }
    }
    catch { case t: Throwable => sızan = t }
    withClue(s"iz=$iz sızan=$sızan -- ") {
      iz shouldBe "A!C" // C aynı eşzamanlı koşuda, bir sonraki zamanlamayı beklemeden
      sızan should not be null
      sızan.getMessage shouldBe "sonda-patlak"
    }
    Future.successful(succeed)
  }

  /**
   * ÖLÇÜT 4 (tepkisellik): uzun bir çokHızlı iş boyunca kareler gelmeye
   * devam ediyor. 800 kalemli gül (~400k komut) çizilirken en uzun kare
   * arası bir-iki kare; MessageChannel'lı uyandırıcıda bu 190 ms'ye
   * çıkıyordu (ölçüldü, o yüzden elendi). Sınır SwiftShader payıyla bol:
   * ölçülen 17-19 ms.
   *
   * Güller burada FUTURE ZİNCİRİYLE sürülüyor, bilerek: her gül ayrı bir
   * koşu, devamı mikro görev. Koşu başına dilim saati bunu görmüyordu --
   * 394 ms, sıfır kare (ölçüldü) -- bütçe artık kareler arası birikiyor.
   * Mutasyon: `harcananMs`i koşu sonunda biriktirme -> bu sav kare 0 ile
   * düşer.
   */
  test("KADANS: mikro görev zinciriyle sürülen uzun iş boyunca kareler aç kalmıyor (#131 ölçüt 4)") {
    val w = dünyaKurYaDaİptal()
    val t = kaplumbağa(w)
    var son = 0.0; var enUzun = 0.0; var kare = 0; var bitti = false; var ölçüyor = false
    def kareSay(): Unit = {
      val ş = window.performance.now()
      if (ölçüyor) { enUzun = math.max(enUzun, ş - son); kare += 1 }
      son = ş
      if (!bitti) window.requestAnimationFrame(_ => kareSay())
    }
    window.requestAnimationFrame(_ => kareSay())
    def tur(k: Int): Future[Unit] = if (k == 0) Future.successful(()) else gülVeBekle(t, 250).flatMap(_ => { t.clear(); tur(k - 1) })
    def kareBekle(n: Int): Future[Unit] = { val s = Promise[Unit](); var k = 0; def f(): Unit = { k += 1; if (k >= n) s.success(()) else window.requestAnimationFrame(_ => f()) }; window.requestAnimationFrame(_ => f()); s.future }
    var t0 = 0.0
    // ISINMA: soğuk başlangıçta ilk kare (shader derlemesi, JIT) 130-140 ms
    // ölçüldü; pompanın değil çizicinin bedeli. Bir gül + üç kare bekleniyor.
    val ısınmış = for { _ <- tur(3); _ <- kareBekle(3) } yield { ölçüyor = true; w.enUzunKoşuMs = 0; w.koşuSayısı = 0; w.aşanKoşuSayısı = 0; t0 = window.performance.now() }
    // 800 gül ~= 400k komut: ısınmış pompada ~250 ms, yani on beş kadar kare.
    // 120 gül denendi ve 42 ms'de tek kareye sığdı -- kadans ölçülemedi.
    ısınmış.flatMap(_ => tur(800)).map { _ =>
      bitti = true
      val toplam = window.performance.now() - t0
      val özet = s"toplam ${math.round(toplam)} ms, kare $kare, en uzun kare arası ${math.round(enUzun)} ms, " +
        s"pompa koşusu ${w.koşuSayısı} (aşan ${w.aşanKoşuSayısı}, en uzun ${math.round(w.enUzunKoşuMs * 10) / 10.0} ms)"
      info(özet)
      withClue(özet + " -- ") {
        // İKİ SÖZ, iki sav. (1) Pompanın sözü: koşular dilimi aşmaz. Sav EN
        // UZUN koşuya değil AŞAN KOŞU SAYISINA bakıyor, ölçümle: tam takımda
        // yük altında tek bir `Forward` 14 ms sürüp bir koşuyu 8'den 14'e
        // (bir kez de 27'ye) taşıdı -- koşunun ortasına düşen GC durması,
        // pompanın kararı değil. Tek durma geçer; dilimi yok sayan pompa her
        // koşuda aşar ve ~160 koşuda 2'yi çok aşar. Eşik dilim + 8 = 16 ms,
        // kare bütçesinin altında (#145 incelemesi §4).
        // (2) Kadans: kareler gelmeye devam ediyor. Kare ARALIĞINA mutlak
        // sınır konmuyor: aynı koşularda 102-166 ms'lik boşluklar ölçüldü ve
        // o sırada pompa koşuları 8-14 ms'ydi -- boşluk tarayıcının (on sağ
        // kalan çizici, SwiftShader). Kare/s MessageChannel'lı uyandırıcıyı
        // (237 ms / 3 kare = 12.7/s) yine de yakalıyor.
        // Koşu sayısı komut sayısına yakın çıkar (ölçüldü: 391k / 400k):
        // kuyruk boşken verilen komut hemen bittiği için düz döngüde her
        // komut kendi koşusu. Kusur değil, eşzamanlılığın sonucu; bütçe
        // koşular arası biriktiği için dilim yine tutuyor (en uzun 2.8 ms).
        w.koşuSayısı should be > 20
        w.aşanKoşuSayısı should be <= 2
        (kare * 1000.0 / toplam) should be > 20.0
        // Ve iş gerçekten hızlı: eski pompa 100 komut / 4.2 ms = 24 komut/ms
        // verirdi; 400k komut 17 s sürerdi. Ölçülen ~1600 komut/ms; sınır 8x.
        (800 * 502 / toplam) should be > 200.0
      }
    }
  }

  /**
   * İlk iş EŞZAMANLI: kuyruk boşken verilen komut, çağrı dönmeden biter.
   * Bu, `sıraya`nın notundaki davranış ve bir canlandır karesinin komutlarının
   * o karede bitmesinin dayanağı. Dilime sığdığı sürece geçerli.
   */
  test("EŞZAMANLI: boş kuyruğa verilen komut çağrı dönmeden bitiyor (#131)") {
    val w = dünyaKurYaDaİptal()
    val t = kaplumbağa(w)
    // Pompa kaynak yüklenince başlıyor; ilk komut o yüzden beklenir.
    gülVeBekle(t, 4).map { _ =>
      t.clear()
      val söz = Promise[Unit]()
      t.sync { () => söz.success(()) }
      söz.future
    }.flatMap(identity).map { _ =>
      val y0 = t.position.y
      t.forward(30)
      // Eşzamanlı bittiyse konum çağrı döndüğünde güncel (yön kuzey: y).
      t.position.y shouldBe (y0 + 30.0) +- 0.01
    }
  }
}
