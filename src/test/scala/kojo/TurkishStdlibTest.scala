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
    with kojo.tr.DizikYöntemleri
    with kojo.tr.YazıyüzüYöntemleri
    with kojo.tr.SesYöntemleri
    with kojo.tr.GörünüşYöntemleri
    with kojo.tr.RenkYöntemleri

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

  test("ortak çekirdek: Dizi, Dizin, Yöney, Dizik, Küme, Eşlek, Kuyruk, Yazı, Belki") {
    val d = Seq(3, 1, 2)
    d.başıBelki should be(Some(3)); d.bul(_ > 1) should be(Some(3)); d.bulSondan(_ > 1) should be(Some(2))
    d.nerede(_ == 1) should be(1); d.neredeSondan(_ > 1) should be(2)
    d.dilimSırası(Seq(1, 2)) should be(1); d.sıralar.toList should be(List(0, 1, 2))
    d.başındaMı(Seq(3, 1)) should be(doğru); d.sonundaMı(Seq(2)) should be(doğru)
    d.karşılıklıMı(Seq(6, 2, 4))(_ * 2 == _) should be(doğru)
    d.böl(_ > 1) should be((Seq(3, 2), Seq(1)))
    d.bölDoğruKaldıkça(_ > 2) should be((Seq(3), Seq(1, 2)))
    d.bölYerinden(1) should be((Seq(3), Seq(1, 2)))
    d.öbekli(2).toList should be(List(Seq(3, 1), Seq(2)))
    d.kayarÖbekli(2).toList should be(List(Seq(3, 1), Seq(1, 2)))
    d.öbekleİşleİndirge(_ % 2)(x => x)(_ + _) should be(Map(1 -> 4, 0 -> 2))
    d.kombinasyonlar(2).toList.boyu should be(3); d.permütasyonlar.size should be(6)
    d.kuyruklar.toList.boyu should be(4); d.önler.toList.boyu should be(4)
    d.katla(0)(_ + _) should be(6); d.indirgeBelki(_ + _) should be(Some(6))
    d.tara(0)(_ + _) should be(Seq(0, 3, 4, 6)); d.taraSağdan(0)(_ + _) should be(Seq(6, 3, 2, 0))
    d.enUfağıBelki should be(Some(1)); d.enİrisiBelki should be(Some(3))
    d.sonunaEkle(9) should be(Seq(3, 1, 2, 9)); d.önüneEkle(9) should be(Seq(9, 3, 1, 2))
    d.uzat(5, 0) should be(Seq(3, 1, 2, 0, 0)); d.yama(1, Seq(8), 1) should be(Seq(3, 8, 2))
    d.fark(Seq(1)) should be(Seq(3, 2)); d.kesişim(Seq(2, 3)) should be(Seq(3, 2))
    d.bileşim(Seq(4)) should be(Seq(3, 1, 2, 4))
    d.seçİşle { case x if x > 1 => x * 10 } should be(Seq(30, 20))
    d.seçİşleİlk { case x if x < 3 => x } should be(Some(1))
    Seq(Seq(1, 2), Seq(3)).düzleştir should be(Seq(1, 2, 3))
    Seq((1, "a"), (2, "b")).ikiliyiAç should be((Seq(1, 2), Seq("a", "b")))
    d.ikileHepsini(Seq("x"), -1, "-") should be(Seq((3, "x"), (1, "-"), (2, "-")))
    d.tersİşle(_ * 2) should be(Seq(4, 2, 6))

    List(3, 1, 2).böl(_ > 1) should be((List(3, 2), List(1)))
    Vector(3, 1, 2).tara(0)(_ + _) should be(Vector(0, 3, 4, 6))
    Vector(3, 1, 2).enİrisiBelki should be(Some(3))
    val dk = Dizik(3, 1, 2)
    dk.bul(_ > 1) should be(Some(3)); dk.böl(_ > 1)._1 should be(Dizik(3, 2))
    dk.taraSoldan(0)(_ + _) should be(Dizik(0, 3, 4, 6)); dk.sonunaEkle(9) should be(Dizik(3, 1, 2, 9))

    val k = Küme(3, 1, 2)
    k.bul(_ > 2) should be(Some(3)); k.katla(0)(_ + _) should be(6)
    k.enUfağıBelki should be(Some(1)); k.fark(Küme(1)) should be(Küme(3, 2))
    k.seçİşle { case x if x > 1 => x * 10 } should be(Küme(30, 20))

    val e = Eşlek("a" -> 1, "b" -> 2)
    e.başıBelki should be(Some(("a", 1))); e.bul(_._2 > 1) should be(Some(("b", 2)))
    e.böl(_._2 > 1)._1 should be(Eşlek("b" -> 2))
    e.öbekleİşleİndirge(_._2 % 2)(_._2)(_ + _) should be(Map(1 -> 1, 0 -> 2))
    e.seçİşle { case (a, v) if v > 1 => a }.toSet should be(Set("b"))

    val ku = Kuyruk(3, 1, 2)
    ku.bul(_ > 1) should be(Some(3)); ku.böl(_ > 1)._1 should be(Kuyruk(3, 2))
    ku.tara(0)(_ + _) should be(Kuyruk(0, 3, 4, 6)); ku.boyu should be(3)

    val y = "merhaba"
    y.başıBelki should be(Some('m')); y.nerede(_ == 'h') should be(3)
    y.bölYerinden(3) should be(("mer", "haba"))
    y.öbekli(3).toList should be(List("mer", "hab", "a"))
    "abc".kombinasyonlar(2).toList should be(List("ab", "ac", "bc"))
    "ab".uzat(4, '-') should be("ab--"); "mer".sonunaEkleHepsini("haba") should be("merhaba")

    val b: Belki[Sayı] = Some(5)
    b.seçİşle { case x if x > 1 => x * 10 } should be(Some(50))
    b.içeriyorMu(5) should be(doğru); b.varMı(_ > 1) should be(doğru)
    b.katla(0)(_ * 2) should be(10); (None: Belki[Sayı]).katla(-1)(_ * 2) should be(-1)
    Some(Some(7)).düzleştir should be(Some(7)); b.ikile(Some("a")) should be(Some((5, "a")))
    Some((1, "a")).ikiliyiAç should be((Some(1), Some("a")))
  }

  test("miskin dizin: masaüstüyle eşitlenen yöntemler") {
    val m = MiskinDizin(3, 1, 2)
    m.önü.dizine should be(List(3, 1)); m.sonu should be(2)
    m.başıBelki should be(Some(3)); MiskinDizin.boş[Sayı].sonuBelki should be(None)
    m.bul(_ > 1) should be(Some(3)); m.bulSondan(_ > 1) should be(Some(2))
    m.nerede(_ == 1) should be(1); m.neredeSondan(_ > 1) should be(2)
    m.başındaMı(Seq(3, 1)) should be(doğru); m.sonundaMı(Seq(2)) should be(doğru)
    m.karşılıklıMı(Seq(6, 2, 4))(_ * 2 == _) should be(doğru)
    m.sıralar.toList should be(List(0, 1, 2))
    m.sıralı.dizine should be(List(1, 2, 3)); m.tersi.dizine should be(List(2, 1, 3))
    m.böl(_ > 1)._1.dizine should be(List(3, 2))
    m.bölDoğruKaldıkça(_ > 2)._1.dizine should be(List(3)); m.bölYerinden(1)._2.dizine should be(List(1, 2))
    m.öbekli(2).toList.map(_.dizine) should be(List(List(3, 1), List(2)))
    m.kayarÖbekli(2).toList.map(_.dizine) should be(List(List(3, 1), List(1, 2)))
    m.öbekle(_ % 2) should be(Map(1 -> LazyList(3, 1), 0 -> LazyList(2)))
    m.öbekleİşleİndirge(_ % 2)(x => x)(_ + _) should be(Map(1 -> 4, 0 -> 2))
    m.katla(0)(_ + _) should be(6); m.soldanKatla("")(_ + _) should be("312")
    m.indirgeSoldan(_ - _) should be(0); m.indirgeBelki(_ + _) should be(Some(6))
    m.tara(0)(_ + _).dizine should be(List(0, 3, 4, 6)); m.taraSağdan(0)(_ + _).dizine should be(List(6, 3, 2, 0))
    m.enUfağı should be(1); m.enİrisiBelki should be(Some(3)); m.enUfağıBelki(x => -x) should be(Some(3))
    m.çarp should be(6); m.yinelemesiz.dizine should be(List(3, 1, 2)); m.değiştir(0, 9).dizine should be(List(9, 1, 2))
    m.içeriyorMu(2) should be(doğru); m.sırası(2) should be(2); m.dilim(1, 3).dizine should be(List(1, 2))
    m.ikileSırayla.dizine should be(List((3, 0), (1, 1), (2, 2))); m.kümeye should be(Set(1, 2, 3))
    // tembellik: hesaplanmayan parça patlamaz
    var sayaç = 0
    val e = m.önüneEkle { sayaç += 1; 0 }
    sayaç should be(0) // öge ancak ilk erişimde hesaplanır
    e.başı should be(0); sayaç should be(1)
    MiskinDizin.sayalım(1).sonunaEkleHepsini(throw new Exception("hesaplanmamalı")).al(3).dizine should be(List(1, 2, 3))
    m.sonunaEkle(9).dizine should be(List(3, 1, 2, 9)); m.önüneEkleHepsini(Seq(7)).dizine should be(List(7, 3, 1, 2))
    m.hepsiniHesapla.dizine should be(List(3, 1, 2))
    m.uzat(5, 0).dizine should be(List(3, 1, 2, 0, 0)); m.yama(1, Seq(8, 9), 1).dizine should be(List(3, 8, 9, 2))
    m.fark(Seq(1)).dizine should be(List(3, 2)); m.kesişim(Seq(2, 3)).dizine should be(List(3, 2)); m.bileşim(Seq(4)).boyu should be(4)
    m.seçİşle { case x if x > 1 => x * 10 }.dizine should be(List(30, 20)); m.seçİşleİlk { case x if x < 3 => x } should be(Some(1))
    MiskinDizin(Seq(1, 2), Seq(3)).düzleştir.dizine should be(List(1, 2, 3))
    MiskinDizin(Seq(1, 2), Seq(3, 4)).devrik.işle(_.dizine).dizine should be(List(List(1, 3), List(2, 4)))
    val (sayılar, harfler) = MiskinDizin((1, "a"), (2, "b")).ikiliyiAç
    sayılar.dizine should be(List(1, 2)); harfler.dizine should be(List("a", "b"))
    m.ikileHepsini(Seq("x"), -1, "-").dizine should be(List((3, "x"), (1, "-"), (2, "-")))
    m.tersİşle(_ * 2).dizine should be(List(4, 2, 6))
    m.kombinasyonlar(2).size should be(3); m.permütasyonlar.size should be(6); m.kuyruklar.size should be(4)
    MiskinDizin.sıraylaDoldur(3)(_ * 2).dizine should be(List(0, 2, 4))
    MiskinDizin.aralık(1, 4).dizine should be(List(1, 2, 3)); MiskinDizin.aralık(1, 10, 4).dizine should be(List(1, 5, 9))
    MiskinDizin.türet(1)(n => if (n > 8) None else Some((n, n * 2))).dizine should be(List(1, 2, 4, 8))
    MiskinDizin.diziden(List(1, 2)).dizine should be(List(1, 2)); MiskinDizin.ekle(Seq(1), List(2)).dizine should be(List(1, 2))
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

  test("dizik: Dizik/EsnekDizik masaüstü kurucuları (Devre 1)") {
    val d = Dizik(3, 1, 2)
    d.length shouldBe 3
    Dizik.boş[Sayı](4).length shouldBe 4
    Dizik.boş[Sayı]().length shouldBe 0
    Dizim.boş[Sayı]().boyu shouldBe 0
    Dizim.boş[Sayı](3).boyu shouldBe 3
    Dizim.boş[Sayı](2, 3).boyu shouldBe 2
    Dizik.boş[Sayı](2, 3).map(_.length).sum shouldBe 6 // aşırı yükleme: (a, b) örtük liste sanılmasın
    Dizik.doldur(2, 3)(7).map(_.sum).sum shouldBe 42
    val e = EsnekDizik(1, 2)
    e += 3
    e.toList shouldBe List(1, 2, 3)
    EsnekDizik.diziden(List("a", "b")).length shouldBe 2
    EsnekDizik.doldur(2)("x").toList shouldBe List("x", "x")
  }

  test("yazıyüzü: Yazıyüzü ailesi (Devre 2)") {
    val yy = yazıyüzü("serif", 24, Yazıyüzü.KALIN | Yazıyüzü.EĞİK)
    yy.ad shouldBe "serif"
    yy.boy shouldBe 24
    yy.kalınMı shouldBe true
    yy.eğikMi shouldBe true
    yazıyüzü("monospace", 12).kalınMı shouldBe false
    yazıyüzleri should contain("sans-serif")
    val f: İşlev1[Sayı, Sayı] = _ + 1
    f(1) shouldBe 2
  }

  test("ses: nota frekansı, çalgı dalgası, Ses/Görünüş sabitleri (Devre 4)") {
    notaFrekansı(69) shouldBe 440.0 +- 1e-9
    notaFrekansı(81) shouldBe 880.0 +- 1e-9
    çalgıDalgası(Çalgı.Piyano) shouldBe "triangle"
    çalgıDalgası(Çalgı.AkustikBas) shouldBe "sine"
    Ses.vuruş shouldBe "/media/collidium/hit.mp3"
    Görünüş.araba shouldBe "/media/costumes/car.png"
    Çizim.top1 shouldBe Görünüş.top1
    // Web Audio yok (Node): sessizce geçmeli, patlamamalı
    notaÇalgısınıKur(Çalgı.AkustikBas)
    notaÇal(50, 150)
    an [IllegalArgumentException] should be thrownBy notaÇal(200, 10)
  }

  test("renk: Türkçe yapıcılar ve Font takma adı (Devre 6)") {
    Renk.kym(255, 0, 0) shouldBe Renkler.kırmızı
    Renk.rgb(255, 0, 0) shouldBe Renk.kym(255, 0, 0)
    Renk.kyms(0, 0, 255, 128).alpha.get shouldBe (128 / 255.0) +- 1e-6
    val a = Renk.ada(120, 1, 0.5)
    a shouldBe Renk.hsl(120, 1, 0.5)
    Renk.adas(120, 1, 0.5, 0.4).alpha.get shouldBe 0.4 +- 1e-6
    // masaüstündeki doğrudan yapıcılar (eski `val Renk = DRenk` uyumu)
    Renk(255, 0, 0) shouldBe Renkler.kırmızı
    Renk(0, 0, 255, 128).alpha.get shouldBe (128 / 255.0) +- 1e-6
    Renk(0x365348) shouldBe Renk.kym(0x36, 0x53, 0x48)
    // renk çemberinde döndürme ve açıklık (masaüstü ColorYöntemleri)
    Renkler.kırmızı.çevir(120) shouldBe Renkler.kırmızı.spin(120)
    Renkler.kırmızı.çevirOranla(1.0 / 3) shouldBe Renkler.kırmızı.spin(120)
    Renkler.gri.dahaAçıkYap(0.2) shouldBe Renkler.gri.lighten(0.2)
    Renkler.gri.dahaKoyuYap(0.2) shouldBe Renkler.gri.darken(0.2)
    // Font(ad, boy) masaüstündeki java.awt.Font yapıcısının iki değişkenli hâli
    Font("JetBrains Mono", 40) shouldBe yazıyüzü("JetBrains Mono", 40)
    Font("serif", 12).boy shouldBe 12
  }

  test("Dizim: çok boyutlu boş/doldur, iç katman Array (tic-tac-toe, genart-tri-mesh)") {
    // 2 boyutlu doldur: tic-tac-toe'nun `Dizim.doldur[Hane](3, 3)(Boş)` kullanımı
    val tahta = Dizim.doldur[Yazı](3, 3)("boş")
    tahta.boyu shouldBe 3
    tahta(0).length shouldBe 3
    tahta(1)(2) shouldBe "boş"
    // asıl mesele: `tahta(x)(y) = değer` derlenebilmeli (Array.update)
    tahta(1)(2) = "insan"
    tahta(1)(2) shouldBe "insan"
    tahta(0)(2) shouldBe "boş" // satırlar paylaşılmıyor

    // 2 boyutlu boş: genart-tri-mesh'in `Dizim.boş[Nokta](n + 2, n + 2)` kullanımı
    val n = Dizim.boş[Sayı](2, 3)
    n.boyu shouldBe 2
    n(0).length shouldBe 3
    n(1)(0) = 7
    n(1)(0) shouldBe 7

    // 3 boyutlu, masaüstündeki üçüncü aşırı yükleme
    val ü = Dizim.doldur[Sayı](2, 2, 2)(0)
    ü(1)(1)(1) = 5
    ü(1)(1)(1) shouldBe 5
    // ara değişken şart: `Dizim.boş[Sayı](1, 1, 1)(0)` yazılırsa Scala 2 `(0)`ı
    // örtük ClassTag listesi sanıyor (dosyanın başındaki `boş()` notuyla aynı tuzak)
    val ü3 = Dizim.boş[Sayı](1, 1, 1)
    ü3(0)(0).length shouldBe 1

    // tek boyutlu imzalar bozulmadı
    Dizim.doldur[Sayı](3)(9).diziye shouldBe Seq(9, 9, 9)
    Dizim.boş[Sayı](2).boyu shouldBe 2
    Dizim.boş[Sayı]().boyu shouldBe 0
  }

  test("Dizik: Array'in Türkçe yöntemleri (masaüstü ArrayMethods)") {
    // tic-tac-toe iç satırlara böyle erişiyor: `tahta(x).diziye`
    val ızgara = Dizim.doldur[Yazı](2, 3)("boş")
    val satır = ızgara(1)
    satır.diziye shouldBe Seq("boş", "boş", "boş")
    satır.boyu shouldBe 3

    val d: Dizik[Sayı] = Dizik(3, 1, 2)
    d.başı shouldBe 3
    d.sonu shouldBe 2
    d.sıralı.diziye shouldBe Seq(1, 2, 3)
    d.ele(_ > 1).diziye shouldBe Seq(3, 2)
    d.işle(_ * 2).diziye shouldBe Seq(6, 2, 4)
    d.topla shouldBe 6
    d.enİrisi shouldBe 3
    d.enUfağı shouldBe 1
    d.say(_ > 1) shouldBe 2
    d.içeriyorMu(2) shouldBe true
    d.sırası(1) shouldBe 1
    d.tersi.diziye shouldBe Seq(2, 1, 3)
    d.yazıYap("-") shouldBe "3-1-2"
    d.dizine shouldBe List(3, 1, 2)
    d.yöneye shouldBe Vector(3, 1, 2)
    d.kümeye shouldBe Set(1, 2, 3)
    d.dizime.diziye shouldBe Seq(3, 1, 2) // Dizik -> Dizim köprüsü
    d.değiştirYerinde(0, 9)
    d(0) shouldBe 9
  }

  test("yazı: küçük/büyük harf ayrımı yapmadan kıyaslama (Devre 1)") {
    "Kojo".eşitMiKüçükHarfBüyükHarfAyrımıYapmadan("kOJO") shouldBe true
    "a".kıyaslaKüçükHarfBüyükHarfAyrımıYapmadan("B") should be < 0
  }
}
