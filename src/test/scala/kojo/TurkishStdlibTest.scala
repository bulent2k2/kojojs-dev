package kojo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Türkçe stdlib sözcük dağarcığının DAVRANIŞ testi.
 *
 * TurkishPreludeTest'ten ayrı, çünkü o TestKojoWorld kuruyor -- yani PIXI ve bir
 * DOM gerekiyor, dolayısıyla tarayıcı (Selenium + chromedriver). Buradaki
 * trait'ler saf Scala olduğu için düz Node.js'te koşabiliyor:
 *
 *   sbt 'set jsEnv in Test := new org.scalajs.jsenv.nodejs.NodeJSEnv()' \
 *       'testOnly kojo.TurkishStdlibTest'
 *
 * Bu önemli: derleme yalnızca TÜRLERİ kanıtlıyor. gcd/lcm/ortalama gibi elle
 * yazılmış işlevlerin DOĞRU olduğunu ancak çalıştırmak gösterir.
 */
object TRDeneme
    extends kojo.tr.SayıYöntemleri
    with kojo.tr.MatematikYöntemleri
    with kojo.tr.BelkiYöntemleri
    with kojo.tr.BölümselİşlevYöntemleri
    with kojo.tr.YazıYöntemleri
    with kojo.tr.HarfYöntemleri
    with kojo.tr.AralıkYöntemleri
    with kojo.tr.KümeYöntemleri
    with kojo.tr.DiziYöntemleri
    with kojo.tr.EşlemYöntemleri
    with kojo.tr.DizinYöntemleri
    with kojo.tr.YöneyYöntemleri
    with kojo.tr.KökTürYöntemleri
    with kojo.tr.DizimYöntemleri
    with kojo.tr.MiskinDizinYöntemleri
    with kojo.tr.KuyrukYöntemleri

class TurkishStdlibTest extends AnyFunSuite with Matchers {
  import TRDeneme._

  test("matematik: elle yazılan işlevler (Commons Math yerine)") {
    enİriOrtakPayda(12, 18) should be(6)
    enİriOrtakPayda(18, 12) should be(6)
    enİriOrtakPayda(-12, 18) should be(6)
    enİriOrtakPayda(7, 13) should be(1)
    enİriOrtakPayda(0, 5) should be(5)

    enUfakOrtakKat(4, 6) should be(12)
    enUfakOrtakKat(21, 6) should be(42)
    enUfakOrtakKat(0, 5) should be(0)

    ortalama(Array(1.0, 2.0, 3.0)) should be(2.0)
    ortalama(Array(5.0)) should be(5.0)
    // örneklem değişimi (n-1 bölen)
    değişim(Array(1.0, 2.0, 3.0, 4.0)) should be(1.6666666666666667 +- 1e-12)
  }

  test("matematik: geometri ve temel işlevler") {
    karekökü(16.0) should be(4.0)
    karesi(3.0) should be(9.0)
    kuvveti(2.0, 10.0) should be(1024.0)
    uzaklık(0.0, 0.0, 3.0, 4.0) should be(5.0)
    açı(0.0, 0.0, 1.0, 1.0) should be(45.0 +- 1e-9)
    açı(0.0, 0.0, 0.0, 1.0) should be(90.0 +- 1e-9)
    yuvarla(3.14159, 2) should be(3.14)
    yuvarla(2.5) should be(3.0)
    işareti(-7.0) should be(-1)
    işareti(0.0) should be(0)
    dereceye(math.Pi) should be(180.0 +- 1e-9)
  }

  test("sayı uzantıları") {
    (5).yazıya should be("5")
    (5).kesire should be(5.0)
    (-3).mutlakDeğer should be(3)
    (2).enİrisi(7) should be(7)
    (2).enUfağı(7) should be(2)
    (65).harfe should be('A')

    (1 |-| 3).toList should be(List(1, 2, 3)) // kapalı aralık
    (1 |- 3).toList should be(List(1, 2)) // yarı açık

    (3.7).taban should be(3.0)
    (3.2).tavan should be(4.0)
    (3.6).yakın should be(4L)

    Sayılar(3, 1, 2).sorted should be(Vector(1, 2, 3))
    Sayılar().length should be(0)
    Sayı.Enİrisi should be(Int.MaxValue)
    Kesir.EnUfağı should be(Double.MinValue)
  }

  test("belki: Option'ın Türkçesi") {
    val b: Belki[Sayı] = Biri(5)
    b.varMı should be(doğru)
    b.yokMu should be(yanlış)
    b.al should be(5)
    b.işle(_ * 2) should be(Some(10))
    b.alYoksa(0) should be(5)
    b.ele(_ > 10) should be(None)
    b.ele(_ > 1) should be(Some(5))
    b.dizine should be(List(5))

    val h: Belki[Sayı] = Hiçbiri
    h.yokMu should be(doğru)
    h.alYoksa(42) should be(42)
    varMı(h) should be(yanlış)
    yokMu(h) should be(doğru)

    // desen eşleme çalışıyor mu
    (Biri(7) match { case Some(n) => n; case None => -1 }) should be(7)
  }

  test("yazı: String'in Türkçesi") {
    "merhaba".boyu should be(7)
    "merhaba".başı should be('m')
    "merhaba".sonu should be('a')
    "merhaba".tersi should be("abahrem")
    "merhaba".büyükHarfe should be("MERHABA")
    "MERHABA".küçükHarfe should be("merhaba")
    "  bosluk  ".kısalt should be("bosluk")
    "a,b,c".böl(',') should be(List("a", "b", "c"))
    "merhaba".al(3) should be("mer")
    "merhaba".düşür(3) should be("haba")
    "merhaba".içeriyorMu("rha") should be(doğru)
    "merhaba".sırası("h") should be(3)
    "aabbcc".yinelemesiz should be("abc")
    "aabbcc".yinelemesizİşlevle(identity) should be("abc")
    "merhaba".ele(_ != 'a') should be("merhb")
    "abc".düzİşle(h => h.toString * 2) should be("aabbcc")
    "abc".say(_ > 'a') should be(2)
    "3.5".kesire should be(3.5)
    "42".sayıya should be(42)
    // 2.13'teki toIntOption yerine elle yazılan karşılık
    "42".sayıyaBelki should be(Some(42))
    "kırk iki".sayıyaBelki should be(None)
    "3.5".kesireBelki should be(Some(3.5))
    Yazı.olarak(doğru) should be("doğru")
    Yazı.olarak(42) should be("42")
  }

  test("Türkçe i/İ kuralı (noktalı-noktasız i)") {
    // Java/JS varsayılanı İNGİLİZCE: 'i'.toUpper = 'I', 'I'.toLower = 'i'.
    // Türkçede i -> İ ve I -> ı olmalı.
    'i'.büyükHarfe should be('İ')
    'ı'.büyükHarfe should be('I')
    'I'.küçükHarfe should be('ı')
    'İ'.küçükHarfe should be('i')
    // diğer harfler değişmemeli
    'a'.büyükHarfe should be('A')
    'Z'.küçükHarfe should be('z')

    "istanbul".büyükHarfe should be("İSTANBUL")
    "ışık".büyükHarfe should be("IŞIK")
    "IRMAK".küçükHarfe should be("ırmak")
    "İZMİR".küçükHarfe should be("izmir")
    "izmir".ilkHarfiBüyült should be("İzmir")
  }

  test("harf: Char'ın Türkçesi") {
    '5'.sayıMı should be(doğru)
    'a'.sayıMı should be(yanlış)
    'a'.harfMi should be(doğru)
    ' '.boşlukMu should be(doğru)
    'a'.küçükHarfMi should be(doğru)
    'A'.büyükHarfMi should be(doğru)
    'A'.sayıya should be(65)
    Harf.sayıMı('7') should be(doğru)
    Harf.enUfağı should be(Char.MinValue)
    Harf.enİrisi should be(Char.MaxValue)
  }

  test("aralık: Range'in Türkçesi") {
    val a = Aralık(1, 5)
    a.boyu should be(4)
    a.başı should be(1)
    a.sonu should be(4)
    a.dizine should be(List(1, 2, 3, 4))
    a.içindeMi(3) should be(doğru)
    a.içindeMi(5) should be(yanlış)
    a.indirge(_ + _) should be(10)
    a.işle(_ * 2).toList should be(List(2, 4, 6, 8))
    a.toString should be("Aralık(1, 2, 3, 4)")

    Aralık.kapalı(1, 5).dizine should be(List(1, 2, 3, 4, 5))
    Aralık(0, 10, 2).dizine should be(List(0, 2, 4, 6, 8))
    // uzun aralık kısaltılarak yazılıyor
    Aralık(1, 101).toString should include("...")

    (1 to 5).boyu should be(5)
    (1 to 10).adım(3).toList should be(List(1, 4, 7, 10))
    (1 to 4).soldanKatla(0)(_ + _) should be(10)
  }

  test("küme: Set'in Türkçesi") {
    val k = Küme(1, 2, 3)
    k.boyu should be(3)
    k.içeriyorMu(2) should be(doğru)
    k.doluMu should be(doğru)
    Küme.boş[Sayı].boşMu should be(doğru)
    k.işle(_ * 2) should be(Set(2, 4, 6))
    k.ele(_ > 1) should be(Set(2, 3))
    k.indirge(_ + _) should be(6)
    k.topla should be(6)
    k.enİrisi should be(3)
    k.enUfağı should be(1)
    k.kesişim(Küme(2, 3, 4)) should be(Set(2, 3))
    k.bileşim(Küme(4)) should be(Set(1, 2, 3, 4))
    k.fark(Küme(1)) should be(Set(2, 3))
    k.böl(_ > 1) should be((Set(2, 3), Set(1)))
    k.dizine.sorted should be(List(1, 2, 3))
    k.altKümeleri(2).size should be(3)
  }

  test("dizi: Seq'in Türkçesi") {
    val d = Dizi(3, 1, 2)
    d.boyu should be(3)
    d.başı should be(3)
    d.sonu should be(2)
    d.kuyruğu should be(Seq(1, 2))
    d.önü should be(Seq(3, 1))
    d.sıralı should be(Seq(1, 2, 3))
    d.tersi should be(Seq(2, 1, 3))
    d.işle(_ * 2) should be(Seq(6, 2, 4))
    d.ele(_ > 1) should be(Seq(3, 2))
    d.indirge(_ + _) should be(6)
    d.topla should be(6)
    d.çarp should be(6)
    d.enİrisi should be(3)
    d.enUfağı should be(1)
    d.enİrisiİşlevle(x => -x) should be(1)
    d.soldanKatla(0)(_ + _) should be(6)
    d.yazıYap("-") should be("3-1-2")
    d.içeriyorMu(2) should be(doğru)
    d.sırası(1) should be(1)
    d.al(2) should be(Seq(3, 1))
    d.düşür(1) should be(Seq(1, 2))
    d.değiştir(0, 9) should be(Seq(9, 1, 2))
    d.say(_ > 1) should be(2)
    d.böl(_ > 1) should be((Seq(3, 2), Seq(1)))
    d.ikileSırayla should be(Seq((3, 0), (1, 1), (2, 2)))
    d.sırala(x => x) should be(Seq(1, 2, 3))
    d.dizine should be(List(3, 1, 2))
    d.kümeye should be(Set(1, 2, 3))

    Dizi(1, 1, 2, 2, 3).yinelemesiz should be(Seq(1, 2, 3))
    Dizi("aa", "ab", "bc").yinelemesizİşlevle(_.head) should be(Seq("aa", "bc"))
    Dizi.doldur(4)(i => i * i) should be(Seq(0, 1, 4, 9))
    Dizi.boş[Sayı].boşMu should be(doğru)

    // Diz de aynı türe açılıyor (2.12'de collection.Seq == Seq)
    Diz(1, 2).boyu should be(2)

    // yineleyici
    Dizi(1, 2, 3).öbekli(2).dizine should be(List(Seq(1, 2), Seq(3)))
  }

  test("eşlek / eşlem: Map'in Türkçesi") {
    val e = Eşlek("a" -> 1, "b" -> 2)
    e.boyu should be(2)
    e.al("a") should be(Some(1))
    e.al("z") should be(None)
    e.alYoksa("z", 0) should be(0)
    e.eşli("b") should be(doğru)
    e.içeriyorMu("z") should be(yanlış)
    e.anahtarKümesi should be(Set("a", "b"))
    e.değerler.toList.sorted should be(List(1, 2))
    e.ele(_._2 > 1) should be(Map("b" -> 2))
    e.değiştirilmiş("a", 9).al("a") should be(Some(9))
    e.say(_._2 > 0) should be(2)
    e.enİrisiİşlevle(_._2) should be(("b", 2))
    Eşlek.boş[Yazı, Sayı].boşMu should be(doğru)

    // değişebilir eşlem
    val m = Eşlem("x" -> 1)
    m.sayı should be(1)
    m += ("y" -> 2)
    m.sayı should be(2)
    m.al("y") should be(Some(2))
    m -= "x"
    m.içeriyorMu("x") should be(yanlış)
    m("y") should be(2)
    Eşlem.boş[Yazı, Sayı].boşMu should be(doğru)
    Eşlem.değişmezden(Map("k" -> 9)).al("k") should be(Some(9))
  }

  test("dizin: List'in Türkçesi") {
    val l = Dizin(3, 1, 2)
    l.boyu should be(3)
    l.başı should be(3)
    l.sıralı should be(List(1, 2, 3))
    l.tersi should be(List(2, 1, 3))
    l.işle(_ * 2) should be(List(6, 2, 4))
    l.düzİşle(x => Dizin(x, x)) should be(List(3, 3, 1, 1, 2, 2))
    l.topla should be(6)
    l.enİrisi should be(3)
    l.ele(_ > 1) should be(List(3, 2))
    l.böl(_ > 1) should be((List(3, 2), List(1)))
    l.ikileSırayla should be(List((3, 0), (1, 1), (2, 2)))
    l.yazıYap("+") should be("3+1+2")
    Dizin(1, 1, 2).yinelemesiz should be(List(1, 2))
    Dizin.doldur(3)(i => i + 1) should be(List(1, 2, 3))
    Dizin.boş[Sayı].boşMu should be(doğru)
    Boş should be(Nil)
  }

  test("yöney: Vector'ün Türkçesi") {
    val v = Yöney(3, 1, 2)
    v.boyu should be(3)
    v.sıralı should be(Vector(1, 2, 3))
    v.işle(_ + 1) should be(Vector(4, 2, 3))
    v.topla should be(6)
    v.çarp should be(6)
    v.tersi should be(Vector(2, 1, 3))
    v.değiştir(0, 9) should be(Vector(9, 1, 2))
    v.dilim(1, 3) should be(Vector(1, 2))
    v.enUfağı should be(1)
    v.dizine should be(List(3, 1, 2))
    Yöney.boş[Sayı].boşMu should be(doğru)
    Yöney.doldur(3)(7) should be(Vector(7, 7, 7))
  }

  test("miskin dizin: tembel dizi (2.12'de Stream)") {
    val m = MiskinDizin.sayalım(1).al(5)
    m.dizine should be(List(1, 2, 3, 4, 5))
    MiskinDizin.sayalım(0, 2).al(4).dizine should be(List(0, 2, 4, 6))
    MiskinDizin.yinele(1)(_ * 2).al(5).dizine should be(List(1, 2, 4, 8, 16))
    MiskinDizin.doldur(3)(7).dizine should be(List(7, 7, 7))
    MiskinDizin.boş[Sayı].boşMu should be(doğru)
    // sonsuz diziden tembel süzme
    MiskinDizin.sayalım(1).ele(_ % 3 == 0).al(3).dizine should be(List(3, 6, 9))
    MiskinDizin.sayalım(1).işle(x => x * x).al(3).topla should be(1 + 4 + 9)
    MiskinDizin.sayalım(1).alDoğruKaldıkça(_ < 4).dizine should be(List(1, 2, 3))
  }

  test("yığın / kuyruk / öncelik sırası") {
    val y = Yığın.boş[Sayı]
    y.it(1); y.it(2)
    y.tepesi should be(2)
    y.çek() should be(2)
    y.boyu should be(1)

    val k = Kuyruk.boş[Sayı]
    k.ekle(1); k.ekle(2)
    k.başı should be(1)
    k.çıkar() should be(1)
    k.boyu should be(1)

    val ö = ÖncelikSırası(3, 1, 2)
    ö.çıkar() should be(3) // en büyük önce
    ö.boyu should be(2)
  }

  test("dizim: Array sarmalayıcıları") {
    val d = Dizim(1, 2, 3)
    d.boyu should be(3)
    d(0) should be(1)
    d.dizine should be(List(1, 2, 3))
    Dizim.doldur(3)(5).dizine should be(List(5, 5, 5))

    val e = EsnekDizim(1, 2, 3)
    e.sayı should be(3)
    e += 4
    e.sayı should be(4)
    e.eleYerinde(_ % 2 == 0)
    e.dizine should be(List(2, 4))
    e.çıkar(0)
    e.dizine should be(List(4))
  }

  test("kök türler: nesne yöntemleri") {
    "abc".yazıya should be("abc")
    val n1: Nesne = "x"
    n1.eşitMi("x") should be(doğru)
    val a: HerGönder = "q"
    a.aynıMı(a) should be(doğru)

    // Türkçe üye adlı özellikler (masaüstünde Türkçe ANAHTAR KELİMELERLE yazılmış)
    class Nokta(val x: Sayı) extends BaskınYazıyaYöntemiyle {
      def yazıya = s"Nokta($x)"
    }
    new Nokta(3).toString should be("Nokta(3)")
  }

  // NOT: Nokta/Dikdörtgen testleri burada DEĞİL, TurkishPreludeTest'te.
  // Nokta = PIXI.Point, yani gerçek PIXI kütüphanesi gerekiyor; bu süit ise
  // PIXI'siz düz Node'da koşuyor (jsDependencies := Seq()), çünkü PIXI DOM
  // olmadan yüklenmiyor. Burada denenince "Cannot read properties of
  // undefined (reading 'Point')" alınıyor.

  test("bölümsel işlev") {
    val bi: Bölümselİşlev[Sayı, Yazı] = { case 1 => "bir"; case 2 => "iki" }
    bi.tanımlıMı(1) should be(doğru)
    bi.tanımlıMı(3) should be(yanlış)
    bi(2) should be("iki")
  }
}
