package kojo.tr

/** Option'ın Türkçesi. */
trait BelkiYöntemleri extends TemelTürler {
  type Belki[T] = Option[T]
  type Biri[T] = Some[T]
  val Hiçbiri = None

  object Biri {
    def apply[T](elem: T): Belki[T] = Some(elem)
    def unapply[T](b: Belki[T]) = b match {
      case None    => Hiçbiri
      case Some(n) => Some(n)
    }
  }

  def varMı[T](o: Belki[T]): İkil = o.isDefined
  def yokMu[T](o: Belki[T]): İkil = o.isEmpty

  implicit class BelkiMetotları[T](protected val b: Belki[T]) {
    def al = b.get
    def alYoksa[T2 >: T](t: => T2): T2 = b.getOrElse(t)

    def varMı: İkil = b.nonEmpty
    def yokMu: İkil = b.isEmpty
    def boşMu: İkil = b.isEmpty
    def doluMu: İkil = b.nonEmpty

    def işle[A](işlev: T => A): Belki[A] = b.map(işlev)
    def düzİşle[A](işlev: T => Option[A]): Belki[A] = b.flatMap(işlev)
    def ele(deneme: T => İkil): Belki[T] = b.filter(deneme)
    def eleDeğilse(deneme: T => İkil): Belki[T] = b.filterNot(deneme)
    def elekle(deneme: T => İkil) = b.withFilter(deneme)

    def dizine: Dizin[T] = b.toList
  
    // --- ortak çekirdek --------------------------------------------------
    def seçİşle[B](işlev: PartialFunction[T, B]): Belki[B] = b.collect(işlev)
    def içeriyorMu[S >: T](öge: S): İkil = b.contains(öge)
    def varMı(deneme: T => İkil): İkil = b.exists(deneme)
    def hepsiDoğruMu(deneme: T => İkil): İkil = b.forall(deneme)
    def hepsiİçinDoğruMu(deneme: T => İkil): İkil = b.forall(deneme)
    def herbiriİçin[S](işlev: T => S): Birim = b.foreach(işlev)
    // katla: doluysa işlevi uygula, boşsa varsayılanı ver
    def katla[B](boşsa: => B)(işlev: T => B): B = b.fold(boşsa)(işlev)
    def düzleştir[S](implicit delil: T <:< Belki[S]): Belki[S] = b.flatten(delil)
    def ikile[S](öbürü: Belki[S]): Belki[(T, S)] = b.zip(öbürü)
    def ikiliyiAç[A1, A2](implicit delil: T <:< (A1, A2)): (Belki[A1], Belki[A2]) = b.unzip(delil)
    def diziye: Dizi[T] = b.toList
}
}
