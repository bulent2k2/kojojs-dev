package kojo

/**
 * Sitedeki ScalaFiddle sarmalayıcısının aynısı (JSExport olmadan).
 * yardimKomutlar sayfasındaki kısa gösterilerin ikojo API'sine karşı
 * DERLENDİĞİNİ sınar -- yani sayfaya bozuk örnek girmiyor.
 *
 * Üretilmiştir; kaynak: araclar/gosteri-uret.py
 * Anahtar sözcükler burada İngilizce (bkz. o betiğin başlığı).
 */
object OrnekDerlemeDeneme {
  import kojo.KojoWorldImpl
  import kojo.doodle.Color._
  import kojo.Speed._
  import kojo.RepeatCommands._
  import kojo.syntax.Builtins
  implicit val kojoWorld: kojo.KojoWorld = new KojoWorldImpl()
  val builtins = new Builtins()
  import builtins._
  import trTurtle._

  // döndür
  def g_d_nd_r(): Unit = {
    silVeSakla
    çiz(döndür(30) -> Resim.dikdörtgen(120, 30))
  }

  // büyüt
  def g_b_y_t(): Unit = {
    silVeSakla
    çiz(büyüt(2) -> Resim.daire(25))
  }

  // götür
  def g_g_t_r(): Unit = {
    silVeSakla
    çiz(götür(100, 50) -> Resim.daire(25))
  }

  // kalemRengi
  def g_kalemRengi(): Unit = {
    silVeSakla
    çiz(kalemRengi(kırmızı) -> Resim.dikdörtgen(120, 60))
  }

  // kalemBoyu
  def g_kalemBoyu(): Unit = {
    silVeSakla
    çiz(kalemBoyu(6) * kalemRengi(mavi) -> Resim.dikdörtgen(120, 60))
  }

  // saydamlık
  def g_saydaml_k(): Unit = {
    silVeSakla
    çiz(boyaRengi(mavi) -> Resim.daire(50))
    çiz(saydamlık(0.5) * götür(40, 0) * boyaRengi(kırmızı) -> Resim.daire(50))
  }

  // boyaRengi
  def g_boyaRengi(): Unit = {
    silVeSakla
    çiz(boyaRengi(yeşil) -> Resim.daire(40))
  }

  // Resim.dikdörtgen
  def g_Resim_dikd_rtgen(): Unit = {
    silVeSakla
    çiz(Resim.dikdörtgen(120, 60))
  }

  // Resim.daire
  def g_Resim_daire(): Unit = {
    silVeSakla
    çiz(Resim.daire(50))
  }

  // Resim.elips
  def g_Resim_elips(): Unit = {
    silVeSakla
    çiz(Resim.elips(70, 40))
  }

  // Resim.yatay
  def g_Resim_yatay(): Unit = {
    silVeSakla
    çiz(kalemRengi(mavi) -> Resim.yatay(150))
  }

  // Resim.dikey
  def g_Resim_dikey(): Unit = {
    silVeSakla
    çiz(kalemRengi(mavi) -> Resim.dikey(150))
  }

  // Resim.yazı
  def g_Resim_yaz_(): Unit = {
    silVeSakla
    çiz(Resim.yazı("Merhaba"))
  }

  // Resim.diziYatay
  def g_Resim_diziYatay(): Unit = {
    silVeSakla
    çiz(Resim.diziYatay(
      boyaRengi(kırmızı) -> Resim.daire(25),
      boyaRengi(mavi) -> Resim.daire(25)))
  }

  // Resim.diziDikey
  def g_Resim_diziDikey(): Unit = {
    silVeSakla
    çiz(Resim.diziDikey(
      boyaRengi(kırmızı) -> Resim.daire(25),
      boyaRengi(mavi) -> Resim.daire(25)))
  }

  // Resim.dizi
  def g_Resim_dizi(): Unit = {
    silVeSakla
    çiz(Resim.dizi(
      boyaRengi(kırmızı) -> Resim.daire(40),
      boyaRengi(mavi) -> Resim.daire(20)))
  }

  // Resim.yatayBoşluk
  def g_Resim_yatayBo_luk(): Unit = {
    silVeSakla
    çiz(Resim.diziYatay(
      Resim.daire(25), Resim.yatayBoşluk(40), Resim.daire(25)))
  }

  // Resim.dikeyBoşluk
  def g_Resim_dikeyBo_luk(): Unit = {
    silVeSakla
    çiz(Resim.diziDikey(
      Resim.daire(25), Resim.dikeyBoşluk(40), Resim.daire(25)))
  }

  // kaydır
  def g_kayd_r(): Unit = {
    silVeSakla
    val r = boyaRengi(mavi) -> Resim.daire(30)
    r.çiz()
    r.kaydır(60, 40)
  }

  // yansıtX
  def g_yans_tX(): Unit = {
    silVeSakla
    val r = Resim.yazı("Koco")
    r.çiz()
    r.yansıtX()
  }

  // yansıtY
  def g_yans_tY(): Unit = {
    silVeSakla
    val r = Resim.yazı("Koco")
    r.çiz()
    r.yansıtY()
  }

  // döndürMerkezli
  def g_d_nd_rMerkezli(): Unit = {
    silVeSakla
    val r = boyaRengi(mavi) -> Resim.dikdörtgen(100, 20)
    r.çiz()
    r.döndürMerkezli(45, 0, 0)
  }

  // çizMerkezde
  def g__izMerkezde(): Unit = {
    silVeSakla
    çizMerkezde(boyaRengi(mavi) -> Resim.daire(40))
  }

  // çizMerkezdeYazı
  def g__izMerkezdeYaz_(): Unit = {
    silVeSakla
    çizMerkezdeYazı("Merhaba Koco", kırmızı, 30)
  }

  // resimleriSil
  def g_resimleriSil(): Unit = {
    silVeSakla
    çiz(boyaRengi(mavi) -> Resim.daire(40))
    durakla(1)
    resimleriSil()
  }

  // uzaklık
  def g_uzakl_k(): Unit = {
    val k2 = yeniKaplumbağa(100, 50)
    satıryaz(kaplumbağa.uzaklık(k2))
  }
}
