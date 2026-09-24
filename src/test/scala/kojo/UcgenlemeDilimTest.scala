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
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}

/**
 * Pompa şeklin ORTASINDA kareye teslim edince not yine şeklin tamamını mı
 * söylüyor? (#143'ün ölçümünden çıkan kusur; çare `Turtle.şekilDurmuş`.)
 *
 * Gerçek pompa (#131) düz bir `yinele { ileri; sağ }` döngüsünde her komutu
 * kuyruğa girer girmez bitiriyor, yani kuyruk HER KOMUTTAN SONRA boşalıyor
 * ve "kuyruk boşaldı" dikişi her komutta çalışıyor. Bütçe dolunca pompa
 * kareye teslim ediyor ve kare, YARIM şekli yayınlıyor. İlk sürüm boşalmada
 * bir im kuruyordu; o yayın imi görüp şekli DURMUŞ sanıyor ve kısmi sayıyla
 * kesin konuşuyordu. ÖLÇÜLDÜ (bu sınama, düzeltmeden önce): 251 noktalı gül
 * için "30 ms sürdü (21 nokta)" -- "17 ms sürdü (193 nokta)" kusurunun
 * (#134) yeni pompadaki yolu. Çare kararı kare sınırına taşımak: yayın
 * anında pompa boşta değilse şekil büyüyebilir (Turtle.şekilDurmuş).
 *
 * Ulaşılabilir: gülün komutları bir dilime (8 ms) sığdığı sürece görünmez;
 * 100 000 komutluk betik ya da yavaş bir makine dilimi doldurur. Burada
 * teslim, dilim sıfırlanarak zorlanıyor -- zamanlamaya değil sabite bağlı
 * (dilimi küçültmek yetmedi: soğuk ilk komut 1 ms'lik dilimi tek başına
 * aşıyor, teslim daha ilk kenarda geliyor ve yarım şeklin alanı olmuyordu).
 *
 * WebGL yoksa İPTAL (SolukTest ile aynı gerekçe).
 */
class UcgenlemeDilimTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   * Rapor durumu dünya başına (#149): sahte saat ve kapalı erken yol bu
   * takımın KENDİ dünyasında kalıyor; geri verilecek küresel bir şey yok.
   * Dünya takım sonunda kapatılıyor ki geç kalan işi sonraki takımın
   * sırasında koşmasın.
   */
  private var sonDünya: Option[KojoWorld] = None

  override def afterAll(): Unit = {
    sonDünya.foreach(_.kapat())
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
  }

  private def dünyaKurYaDaİptal(): KojoWorldImpl =
    try {
      sonDünya.foreach(_.kapat())
      Option(document.getElementById("fiddle-container")).foreach(e => e.parentNode.removeChild(e))
      val kap = document.createElement("div").asInstanceOf[HTMLElement]
      kap.id = "fiddle-container"
      kap.style.width = "400px"
      kap.style.height = "300px"
      val tuval = document.createElement("div").asInstanceOf[HTMLElement]
      tuval.id = "canvas-holder"
      kap.appendChild(tuval)
      document.body.appendChild(kap)
      val w = new KojoWorldImpl()
      // Bu dosya ÜÇGENLEME notunun makinesini sınıyor; o makine libtess
      // yolunda yaşıyor (#147: stencil yolu süre yazmıyor, not düşürmüyor).
      // Anahtar açıkça kapalı ki 251 noktalı gül stencil'e gitmesin.
      w.stencilDolgu = false
      sonDünya = Some(w)
      w
    }
    catch { case t: Throwable => cancel(s"çizici kurulamadı (WebGL yok?): $t") }

  private def panelKur(): Unit = {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  /**
   * Her üçgenleme 30 ms: bütçe (16.7) üstü, yani yarım yayın bile "konuşabilir".
   *
   * ERKEN YOL KAPALI, çünkü savın ölçtüğü şey o değil (#148 incelemesi §2):
   * pompanın devamı işi iki kareye yayarsa şekil iki kez üçgenlenir, toplam
   * 60 ms erken eşiği (50.1) aşar ve erkenÇarpan DÜRÜST notunu ("şimdilik
   * 169 nokta; şekil büyüdükçe artacak") basar -- o not doğru, ama
   * `bildirildi`yi koyup kesin notu susturur ve sav "sürdü" bulamaz. Kaç
   * kareye yayıldığı yüke bağlı: incelemede iki koşuda bir kırmızı. Eşiği
   * sonsuza çekmek savı yayın sayısından bağımsız kılıyor; mutasyon
   * (`şekilDurmuş`ten `boştaMı`yı sök) yine "(21 nokta)" ile kırmızı --
   * ayırt edicilik erken yola değil `durdu` yoluna dayanıyor.
   */
  private def saatiKur()(implicit w: KojoWorld): Unit = {
    var tik = 0.0
    w.üçgenlemeRaporu.saat = () => { tik += 30.0; tik }
    w.üçgenlemeRaporu.erkenÇarpan = Double.PositiveInfinity
  }

  /** Kaplumbağanın kuruluş komutları bitsin (giysi, ilk koşu): koşula bağlı, en çok `sınırMs`. */
  private def kuyrukBoşalsın(t: Turtle, sınırMs: Int = 2000): Future[Unit] =
    if (t.commandQs.head.size == 0 || sınırMs <= 0) bekle(0) else bekle(20).flatMap(_ => kuyrukBoşalsın(t, sınırMs - 20))

  private def bekle(ms: Int): Future[Unit] = {
    val söz = Promise[Unit]()
    window.setTimeout(() => söz.success(()), ms)
    söz.future
  }

  test("DİLİM DOLUNCA: kare yarım şekli yayınlasa da not şeklin TAMAMINI söylüyor") {
    implicit val w: KojoWorldImpl = dünyaKurYaDaİptal()
    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()
    t.setPenThickness(0)
    t.setFillColor(kojo.doodle.Color.blue)
    val nokta = 250
    val kenar = 2 * 140.0 * math.sin(math.toRadians(7 * 180.0 / nokta))
    val dönüş = 7 * 360.0 / nokta
    // Kurulumun kareleri geçsin: pompa boşta, bütçe sıfır. Sonra dilim
    // neredeyse sıfır -- ilk birkaç komut eşzamanlı biter (her birinden sonra
    // kuyruk BOŞALIR), bütçe dolunca pompa kareye teslim eder ve kalan
    // komutlar kuyrukta bekler. Kare gelince önce çizim (yarım şeklin
    // yayını), sonra pompanın devamı. Döngü bitince dilim geri: kalan şekil
    // bir-iki karede biter.
    // Kurulumun kareleri geçsin (giysi yüklemesi, ilk koşu): KENDİ
    // kaplumbağamızın kuyruğu boşalana dek, koşula bağlı. Eskiden burada kör
    // bir 300 ms vardı ve asıl sebebi önceki takımların artıklarıydı:
    // `ÜçgenlemeUyarısı` küreseldi (saat, not sayacı, zaman kapısı), geç
    // yüklenen bir giysiyle sonradan koşan başka bir dünyanın kaplumbağası
    // sahte saatle not düşürüp bu savı kirletiyordu (#148). Rapor dünya
    // başına olunca (#149) o sebep yok; başka dünyanın işi bu dünyanın
    // sayacına ve saatine ulaşamıyor. Sahte saat yine bekleyişten SONRA.
    // Sonra iki evre: ilk 20 kenar bugünkü dilimle -- her komut eşzamanlı biter ve
    // kuyruk her birinden sonra BOŞALIR; ardından dilim sıfır -- pompa ilk
    // komutta kareye teslim eder, kalan kenarlar kuyrukta bekler. Zamanlamaya
    // bağlı değil: teslim dilimle değil sabitle zorlanıyor. Kare gelince önce
    // çizim (21 noktalı yarım şeklin yayını), sonra pompanın devamı; döngü
    // bitince dilim geri, kalan şekil bir-iki karede biter.
    kuyrukBoşalsın(t).flatMap { _ =>
      panelKur()
      saatiKur()
      w.yayınSayısı = 0
      val koşuÖnce = w.koşuSayısı
      var i = 0
      while (i < 20) { t.forward(kenar); t.right(dönüş); i += 1 }
      val ilkEvredeKalan = t.commandQs.head.size
      w.DilimMs = 0.0
      while (i < nokta) { t.forward(kenar); t.right(dönüş); i += 1 }
      w.DilimMs = 8.0
      val kuyruktaKalan = t.commandQs.head.size
      val koşuSonra = w.koşuSayısı
      val kuyruklar = t.commandQs.map(_.size).mkString("/")
      bekle(1500).map { _ =>
        withClue(s"kuyrukta kalan: $kuyruktaKalan, yayın: ${w.yayınSayısı}, en uzun koşu: ${w.enUzunKoşuMs}, koşu $koşuÖnce -> $koşuSonra, ilk evrede kalan $ilkEvredeKalan, kuyruklar $kuyruklar, panel: '$panelMetni' -- ") {
          // Düzenek çalıştı mı: döngü bitince komutların bir kısmı (hepsi
          // DEĞİL: ilk birkaçı eşzamanlı bitti) hâlâ kuyruktaydı, ve şekil
          // birden çok yayınla çizildi.
          ilkEvredeKalan shouldBe 0 // ilk evre eşzamanlı bitti
          kuyruktaKalan should be > 0 // ikinci evre kuyrukta
          t.commandQs.head.size shouldBe 0
          w.yayınSayısı should be > 1L
          w.üçgenlemeRaporu.düşenNotSayısı shouldBe 1
          panelMetni should include("sürdü")
          panelMetni should include(s"(${nokta + 1} nokta)")
        }
      }
    }
  }
}
