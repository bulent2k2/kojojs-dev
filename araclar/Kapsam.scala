/*
 * araclar/Kapsam.scala -- kapsam.py'nin derleyip koştuğu yansıma yardımcısı.
 *
 * Scala standart kütüphanesindeki koleksiyon türlerinin GENEL arayüzünü
 * yansımayla (reflection) çıkarır ve "tür<TAB>yöntem yöntem ..." satırları
 * olarak yazar. Türkçe sarmalayıcıların ne kadarını kapsadığını kapsam.py
 * bu çıktıyla karşılaştırarak hesaplar.
 *
 * Doğrudan çalıştırmaya gerek yok: araclar/kapsam.py çağırıyor.
 */
object Kapsam {
  private val nesneYöntemleri = classOf[Object].getMethods.map(_.getName).toSet

  private def adlar(c: Class[_]): Seq[String] =
    c.getMethods
      .filter(y => java.lang.reflect.Modifier.isPublic(y.getModifiers))
      .map(_.getName)
      .filterNot(a => a.contains("$") || nesneYöntemleri(a) || !a.head.isLetter)
      .distinct
      .sorted
      .toSeq

  // Türkçe ad -> Scala sınıfı. Yeni bir tür sarmalanınca buraya da eklenmeli.
  private val türler: List[(String, Class[_])] = List(
    "Diz" -> classOf[collection.Seq[_]],
    "Dizi" -> classOf[collection.immutable.Seq[_]],
    "SıralıDizi" -> classOf[collection.immutable.IndexedSeq[_]],
    "Dizin" -> classOf[List[_]],
    "Yöney" -> classOf[Vector[_]],
    "Dizik" -> classOf[collection.ArrayOps[_]],
    "EsnekDizik" -> classOf[collection.mutable.ArrayBuffer[_]],
    "Eşlek" -> classOf[collection.immutable.Map[_, _]],
    "Eşlem" -> classOf[collection.mutable.Map[_, _]],
    "Küme" -> classOf[collection.immutable.Set[_]],
    "Kuyruk" -> classOf[collection.mutable.Queue[_]],
    "ÖncelikSırası" -> classOf[collection.mutable.PriorityQueue[_]],
    "Yığın" -> classOf[collection.mutable.Stack[_]],
    "Aralık" -> classOf[Range],
    "Yazı" -> classOf[collection.StringOps],
    "EsnekYazı" -> classOf[StringBuilder],
    "MiskinDizin" -> classOf[LazyList[_]],
    "Belki" -> classOf[Option[_]],
    "Yineleyici" -> classOf[Iterator[_]]
  )

  /**
   * Sarmalayıcının UYGULANDIĞI tür (örtük sınıfın aldığı alıcı). Yukarıdaki
   * `türler` API'yi çıkarmak için; bu ise "hangi sarmalayıcı hangi değere
   * uyar" sorusu için. İkisi her zaman aynı değil: Yazı'nın API'si StringOps,
   * ama sarmalayıcı String alıyor.
   */
  private val alıcılar: List[(String, Class[_])] = List(
    "Diz" -> classOf[collection.Seq[_]],
    "Dizi" -> classOf[collection.immutable.Seq[_]],
    "SıralıDizi" -> classOf[collection.immutable.IndexedSeq[_]],
    "Dizin" -> classOf[List[_]],
    "Yöney" -> classOf[Vector[_]],
    "Dizik" -> classOf[Array[AnyRef]],
    "EsnekDizik" -> classOf[collection.mutable.ArrayBuffer[_]],
    "Eşlek" -> classOf[collection.immutable.Map[_, _]],
    "Küme" -> classOf[collection.immutable.Set[_]],
    "Kuyruk" -> classOf[collection.mutable.Queue[_]],
    "ÖncelikSırası" -> classOf[collection.mutable.PriorityQueue[_]],
    "Yığın" -> classOf[collection.mutable.Stack[_]],
    "Aralık" -> classOf[Range],
    "Yazı" -> classOf[String],
    "EsnekYazı" -> classOf[StringBuilder],
    "MiskinDizin" -> classOf[LazyList[_]],
    "Belki" -> classOf[Option[_]]
  )

  def main(args: Array[String]): Unit = {
    türler.foreach { case (ad, c) => println(ad + "\t" + adlar(c).mkString(" ")) }
    // Kalıtım: X'in değerine, X'in alıcısının ALT TÜRÜ olduğu her sarmalayıcı da
    // uygulanır (Scala en özel örtük sınıfı seçer; ötekinde olup bunda olmayan
    // yöntem yine bulunur). Bunu JVM'e soruyoruz, elle tahmin etmiyoruz.
    alıcılar.foreach { case (ad, c) =>
      val üstler = alıcılar.collect {
        case (ad2, c2) if ad2 != ad && c2.isAssignableFrom(c) => ad2
      }
      println("ÜSTLER\t" + ad + "\t" + üstler.mkString(" "))
    }
  }
}
