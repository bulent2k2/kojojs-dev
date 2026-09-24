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
 * TAMAMLANMAYAN şekil susuyor, tamamlanan konuşuyor -- GERÇEK çizim yolundan
 * (#133 incelemesi §1).
 *
 * NEDEN AYRI BİR SINAMA. `UcgenlemeUyarisiTest`in "şekil bitmeden de
 * konuşuyor" savı aynı mekanizmayı DOĞRUDAN çağrıyla tutuyor ve yeşildi --
 * ama `14-agir-dolgu.kojo` yine de sessiz kaldı ve bunu ancak örneği gerçek
 * tarayıcıda koşturunca gördük. Aradaki boşluk şu: o sav bir şeklin
 * tamamlanıp tamamlanmadığını PARAMETRE olarak alıyor; burada şekli gerçekten
 * `Turtle` tamamlıyor (ya da tamamlamıyor).
 *
 * SINANAN BANT bilerek 16.7 ile 50.1 arasında: bütçenin üstünde ama erken
 * eşiğin altında. Gerçek gülün ölçülen süresi (35 ms, 251 nokta) tam bu
 * bantta, ve #133'ün gerilemesi tam burada yaşıyordu.
 *
 * SAHTE SAAT çünkü sınanan şey sürenin büyüklüğü değil, tamamlamanın olup
 * olmaması. Saat eskiden KÜRESELDİ ve `afterAll` onu geri veriyordu (#124
 * incelemesi §1: sızan sahte saat sonraki takımlarda uyarıyı tümüyle
 * susturuyordu); artık dünya başına (#149), sızacak yer yok.
 */
class UcgenlemeTamamlamaTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   * Rapor durumu dünya başına (#149): sahte saat her sınamanın KENDİ
   * dünyasında kalıyor; geri verilecek küresel saat yok. Önceki dünya
   * yenisi kurulmadan, sonuncusu takım sonunda kapatılıyor.
   */
  private var sonDünya: Option[KojoWorld] = None

  override def afterAll(): Unit = {
    sonDünya.foreach(_.kapat())
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
  }

  private def panelKur(): HTMLElement = {
    Option(document.getElementById("output")).foreach(e => e.parentNode.removeChild(e))
    val d = document.createElement("div").asInstanceOf[HTMLElement]
    d.id = "output"
    document.body.appendChild(d)
    d
  }

  private def panelMetni: String =
    Option(document.getElementById("output")).map(_.textContent).getOrElse("")

  /**
   * Sahte saat: her çağrıda `artış` ms ilerliyor, yani üçgenleme başına delta
   * da `artış` (`üçgenleriÇiz` saati iki kez okuyor).
   *
   * Varsayılan 30: bütçenin (16.7) üstünde, erken eşiğin (50.1) altında --
   * sınanan bant. Çok turlu sınama daha küçük bir artış veriyor, çünkü orada
   * birikim tur tur büyüyor ve erken eşiği aşarsa `üçgenlemeBitti`nin KENDİSİ
   * konuşur; o zaman ölçülen şey artık boşalma kapısı olmaz.
   */
  private def saatiKur(artış: Double = 30.0)(implicit w: KojoWorld): Unit = {
    var tik = 0.0
    w.üçgenlemeRaporu.saat = () => { tik += artış; tik }
  }

  private def dünyaKurYaDaİptal(): TestKojoWorld =
    try { sonDünya.foreach(_.kapat()); val w = new TestKojoWorld(); sonDünya = Some(w); w }
    catch { case t: Throwable => cancel(s"dünya kurulamadı: $t") }

  /**
   * Dolgulu bir kare çizer, tek bir yayın yaptırır, sonra `tamamla` verilmişse
   * şekli tamamlayacak komutları kuyruğa koyar. Dönen sayı düşen not sayısı.
   */
  private def koştur(tamamla: Boolean): Future[Int] = {
    implicit val w: TestKojoWorld = dünyaKurYaDaİptal()
    panelKur()
    saatiKur()

    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()
    t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 4) { t.forward(60); t.right(90); i += 1 }

    val söz = Promise[Int]()
    t.sync { () =>
      // Şekil çizildi ama HENÜZ tamamlanmadı: yarım yayın.
      w.boyalarıBoşalt()
      if (!tamamla) söz.success(w.üçgenlemeRaporu.düşenNotSayısı)
      else {
        // 14-agir-dolgu.kojo'daki düzeltmenin aynısı: kalem kalkık taşınma.
        t.penUp()
        t.setPosition(0, -220)
        t.sync { () => söz.success(w.üçgenlemeRaporu.düşenNotSayısı) }
      }
    }
    söz.future
  }

  test("şekil BÜYÜYEBİLİRKEN susuyor: bütçe üstü ama erken eşik altı") {
    // Bu sav #133'te "tamamlanmayan şekil hep susuyor" diye yazılmıştı ve o
    // zaman doğruydu. #134 davranışı BİLEREK değiştirdi: kuyruk boşalıp
    // canlandırma da dönmüyorsa şekil artık büyüyemez, ve konuşuyor (aşağıdaki
    // sav). Burada ölçülen an kuyruk HENÜZ boşalmadan, yani şekle daha nokta
    // eklenebilecekken -- orada susması hâlâ doğru.
    koştur(tamamla = false).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 0
      }
    }
  }

  /**
   * #134'ün düzeltmesi: kuyruk boşalıp canlandırma da dönmüyorsa betik
   * bitmiştir, şekle bir daha nokta eklenmez, ve biriken süre bildirilir.
   *
   * ESKİDEN bu şekil sonsuza dek sessizdi: onu "bitmiş" sayan iki yoldan
   * (kalem kalkık taşınma / boya değişimi) hiçbiri gelmiyor, ve toplamı
   * erken eşiğin (50.1 ms) altında. `14-agir-dolgu.kojo`'nun 35 ms'lik gülü
   * tam buydu (#133) ve örnek o yüzden sessiz kalmıştı.
   */
  private def kuyrukBoşalanaKadar(w: TestKojoWorld, t: Turtle): Future[Int] = {
    val söz = Promise[Int]()
    // sync geri çağrımı kuyruk HENÜZ boşalmadan koşuyor (pompa devam ediyor):
    // bu yayın "büyüyebilir" sayılır ve susar. Bir tur sonra kuyruk boşalmış
    // olur; karar KARE SINIRINDA verildiği için (Turtle.şekilDurmuş) bir kare
    // daha veriyoruz -- ikinci boşaltmada yayın yok, durma denetimi konuşur.
    t.sync { () =>
      w.boyalarıBoşalt()
      window.setTimeout(
        () => {
          w.boyalarıBoşalt()
          söz.success(w.üçgenlemeRaporu.düşenNotSayısı)
        },
        50
      )
    }
    söz.future
  }

  /** Dolgulu bir kare çizen, hazır bir dünya + kaplumbağa. */
  private def kareÇizenKur(saatArtışı: Double = 30.0): (TestKojoWorld, Turtle) = {
    implicit val w: TestKojoWorld = dünyaKurYaDaİptal()
    panelKur()
    saatiKur(saatArtışı)
    val t = new Turtle(0, 0)
    t.setAnimationDelay(0)
    t.invisible()
    t.setFillColor(kojo.doodle.Color.blue)
    var i = 0
    while (i < 4) { t.forward(60); t.right(90); i += 1 }
    (w, t)
  }

  test("KUYRUK BOŞALINCA konuşuyor: betiğin son şekli artık sessiz değil (#134)") {
    val (w, t) = kareÇizenKur()
    val öncekiDüğüm = t.dolguParçaları.size
    kuyrukBoşalanaKadar(w, t).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        // Tamamlanmış biçim: şekil artık büyüyemez, "şu ana dek" değil.
        panelMetni should include("sürdü")
        // ÖLÇÜT 2'nin yarısı: konuşurken bile SAHNEYE dokunmuyor. `boyamayıİşle`
        // olsaydı burada kalıcı bir dolgu düğümü doğardı.
        t.dolguParçaları.size shouldBe öncekiDüğüm
      }
    }
  }

  /** Bir tur: birkaç kenar daha çiz, boyaları boşalt, kuyruğun boşalmasını bekle. */
  private def tur(w: TestKojoWorld, t: Turtle): Future[Unit] = {
    val söz = Promise[Unit]()
    var i = 0
    while (i < 4) { t.forward(60); t.right(90); i += 1 }
    t.sync { () =>
      w.boyalarıBoşalt()
      window.setTimeout(() => söz.success(()), 20)
    }
    söz.future
  }

  private def turlar(w: TestKojoWorld, t: Turtle, kalan: Int): Future[Unit] =
    if (kalan == 0) Future.successful(())
    else tur(w, t).flatMap(_ => turlar(w, t, kalan - 1))

  /**
   * #134 ÖLÇÜT 2: `canlandır` döngüsünde ne not düşüyor ne de kalıcı düğüm
   * sayısı artıyor.
   *
   * KAÇINILAN TUZAK. Kuyruk boşalması canlandırma döngüsünde kare başına bir
   * kez değil, ölçülen 1.63 kez oluyor. Oraya `boyamayıİşle` gibi KALICI düğüm
   * yazan bir kanca koymak #91/#109'da kapatılan sızıntıyı geri getirirdi:
   * sahne her karede büyür, ve bu bir başarım kusuru olarak değil önce bir
   * görüntü kusuru olarak ortaya çıkardı. O yüzden `şekilDurdu` sahneye hiç
   * dokunmuyor, ve çağrısı `canlandırmaSürüyor` kapısının arkasında.
   *
   * İKİ SAV BİRDEN, çünkü kusurun iki yüzü var: sahne büyümesi (düğüm sayısı)
   * ve panel büyümesi (her karede bir not). Kapı kaldırılırsa ikincisi,
   * `şekilDurdu` sahneye yazarsa birincisi kırmızıya döner.
   */
  /**
   * #140 incelemesi §1: "kuyruk boşaldı" ile "betik bitti" aynı şey DEĞİL.
   *
   * `canlandır` döngüsü tek uyandırıcı değil. `timer(ms)` bir `setInterval`,
   * `tuşaBasınca` bir `keydown` dinleyicisi, resim fare işleyicileri de PIXI
   * olayları -- hiçbiri `animating`i kurmuyor. Bu betiklerde kuyruk TIKLAR
   * ya da TUŞLAR ARASINDA boşalıyor, ve bir sonraki olay şekle nokta ekliyor.
   *
   * İLK SÜRÜM bunları saymıyordu ve iki ayrı yanlış üretiyordu: (1) 17
   * noktaya büyüyecek şekil için "30 ms sürdü (5 nokta)" -- kesin cümle,
   * kısmi sayı; (2) `bildirildi` imi konduğu için `erkenÇarpan`ın sonradan
   * düşeceği DÜRÜST not ("şimdilik N nokta; şekil büyüdükçe artacak") hiç
   * gelmiyordu. İkinci sav aşağıda ayrıca çivili.
   *
   * SUSMAK SEÇİLDİ, "büyüyebilir" biçimiyle konuşmak değil: betiği gerçekten
   * BİTMİŞ olan sıradan bir çizimde "şekil büyüdükçe artacak" demek de yanlış
   * olurdu, ve o durum çok daha sık. Emin olunamayan yerde susup `erkenÇarpan`a
   * bırakmak, master'ın o betikler için bugünkü davranışını aynen koruyor.
   */
  test("ZAMANLAYICI varken boşalma susuyor: tıklar arasında şekil büyüyebilir (#140)") {
    val (w, t) = kareÇizenKur() // 30 ms: tek yayında bile bütçe üstü, yani kapı olmasa KONUŞURDU
    w.zamanlayıcıVarMı = true
    kuyrukBoşalanaKadar(w, t).map { n =>
      withClue(s"panel: '$panelMetni' -- ") { n shouldBe 0 }
    }
  }

  test("GİRDİ işleyicisi varken boşalma susuyor: tuşlar arasında şekil büyüyebilir (#140)") {
    val (w, t) = kareÇizenKur() // 30 ms: tek yayında bile bütçe üstü, yani kapı olmasa KONUŞURDU
    w.girdiİşleyicisiKaydedildi()
    kuyrukBoşalanaKadar(w, t).map { n =>
      withClue(s"panel: '$panelMetni' -- ") { n shouldBe 0 }
    }
  }

  /**
   * Bulgunun ikinci yarısı: boşalma yolu susarken `erkenÇarpan`ın DÜRÜST notu
   * yerinde duruyor. İlk sürüm bunu önceliyordu -- 30 ms'de kesin konuşup imi
   * koyuyor, 50.1 ms'de gelecek doğru sayılı not hiç düşmüyordu.
   *
   * Saat varsayılan 30: iki yayın 60 eder, yani erken eşiğin (50.1) üstü.
   */
  test("GİRDİ işleyicisi varken erkenÇarpan'ın dürüst notu hâlâ geliyor (#140)") {
    val (w, t) = kareÇizenKur()
    w.girdiİşleyicisiKaydedildi()
    turlar(w, t, 2).map { _ =>
      withClue(s"panel: '$panelMetni' -- ") {
        w.üçgenlemeRaporu.düşenNotSayısı shouldBe 1
        panelMetni should include("şimdilik")
        panelMetni should include("büyüdükçe artacak")
      }
    }
  }

  test("CANLANDIRMADA boşalma sessiz ve sahneyi büyütmüyor (#134 ölçüt 2)") {
    // Tur başına 9 ms: dört turun toplamı 36 -- bütçenin (16.7) üstünde, yani
    // `şekilDurdu` çağrılsaydı konuşurdu; ama erken eşiğin (50.1) altında, yani
    // `üçgenlemeBitti` kendiliğinden konuşmuyor. Ölçülen tam da kapı.
    val (w, t) = kareÇizenKur(saatArtışı = 9.0)
    w.canlandırmaDönüyorMu = true
    val öncekiDüğüm = t.dolguParçaları.size
    turlar(w, t, 4).map { _ =>
      withClue(s"panel: '$panelMetni' -- ") {
        w.üçgenlemeRaporu.düşenNotSayısı shouldBe 0
        t.dolguParçaları.size shouldBe öncekiDüğüm
      }
    }
  }

  /**
   * CANLI ÖLÇÜMDEN GELEN KUSUR (#134, 4. deney, yeni yayın). Örnek 251 noktalı
   * bir gül çiziyor ama not "17 ms sürdü (193 NOKTA)" dedi -- betikte olmayan
   * bir sayı, üstelik KESİN cümleyle. Elle tamamlama satırı duruyorken aynı
   * koşu "25 ms sürdü (251 nokta)" veriyordu.
   *
   * SEBEBİ: boşalma anı SON YAYINDAN sonra. Şekil büyümeye devam etmiş ama
   * o noktalar henüz yayınlanmamış (çizer `bekleyenBoyacılar`da), yani
   * `sonNoktaSayısı` ve `toplamMs` şeklin yalnız bir ÖNEKİNİ anlatıyor.
   *
   * Bu, #125'te bir kez düzeltilmiş kusurun ta kendisi. Sınama onu boşalmadan
   * SONRA nokta ekleyerek değil -- o mümkün değil -- son yayından sonra nokta
   * ekleyip yayını bekletmeden kuyruğu boşaltarak kuruyor. Karar artık
   * boşalmada değil yayında veriliyor (#143'ün ölçümü, `UcgenlemeDilimTest`);
   * bu sav o tasarımda da aynen geçerli: son yayın kare sınırında, kuyruk
   * boş, sayı tam.
   */
  test("BEKLEYEN yayın varken not son yayını BEKLİYOR: sayı şeklin tamamı (#134)") {
    val (w, t) = kareÇizenKur()
    val söz = Promise[Unit]()
    t.sync { () =>
      w.boyalarıBoşalt() // 5 noktada yayın
      var i = 0
      while (i < 4) { t.forward(60); t.right(90); i += 1 } // şekil büyüyor, yayın YOK
      window.setTimeout(
        () => {
          // Kuyruk bu arada boşaldı. Şimdi gerçek render'ın yerine son yayın.
          w.boyalarıBoşalt()
          window.setTimeout(() => söz.success(()), 20)
        },
        200
      )
    }
    söz.future.map { _ =>
      withClue(s"panel: '$panelMetni' -- ") {
        w.üçgenlemeRaporu.düşenNotSayısı shouldBe 1
        panelMetni should include("(9 nokta)") // 5 değil: bekleyen yayın da sayıldı
        // Durmuş şekil KESİN biçimde konuşuyor, ama doğru sayıyla. Bu satır
        // aynı zamanda SIRAYI çiviliyor: son yayında toplam 60 ms, yani erken
        // eşiğin üstünde -- "durdu" önce bakılmazsa erkenÇarpan araya girip
        // "şimdilik" biçimini basardı.
        panelMetni should include("sürdü")
        panelMetni should not include "şimdilik"
      }
    }
  }

  test("TAMAMLANAN şekil konuşuyor: kalem kalkık taşınma yetiyor (#133)") {
    koştur(tamamla = true).map { n =>
      withClue(s"panel: '$panelMetni' -- ") {
        n shouldBe 1
        panelMetni should include("sürdü") // bitti = true biçimi
      }
    }
  }
}
