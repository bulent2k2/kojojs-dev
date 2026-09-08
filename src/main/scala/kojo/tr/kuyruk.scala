package kojo.tr

import scala.collection.mutable.{Stack, Queue, PriorityQueue}

/**
 * Yığın (Stack), Kuyruk (Queue) ve Öncelik Sırası (PriorityQueue).
 *
 */
trait KuyrukYöntemleri extends TemelTürler {
  type Yığın[T] = Stack[T]
  type Kuyruk[T] = Queue[T]
  type ÖncelikSırası[T] = PriorityQueue[T]

  object Yığın {
    def apply[T](elems: T*): Yığın[T] = Stack(elems: _*)
    def boş[T]: Yığın[T] = Stack.empty[T]
  }

  object Kuyruk {
    def apply[T](elems: T*): Kuyruk[T] = Queue(elems: _*)
    def boş[T]: Kuyruk[T] = Queue.empty[T]
  }

  object ÖncelikSırası {
    def apply[T](elems: T*)(implicit sıralama: Ordering[T]): ÖncelikSırası[T] =
      PriorityQueue(elems: _*)(sıralama)
    def boş[T](implicit sıralama: Ordering[T]): ÖncelikSırası[T] = PriorityQueue.empty[T](sıralama)
  }

  implicit class YığınMetotları[T](d: Yığın[T]) {
    def it(öge: T): Yığın[T] = d.push(öge)
    def çek(): T = d.pop()
    def koy(öge: T): Yığın[T] = d.push(öge) // it ile aynı (kitapçık adı)
    def al(): T = d.pop()          // çek ile aynı (kitapçık adı)
    def tepesi: T = d.top
    def boyu: Sayı = d.size
    def tane: Sayı = d.size // boyu ile aynı (kitapçık adı)
    def boşMu: İkil = d.isEmpty
    def doluMu: İkil = d.nonEmpty
    def sil(): Birim = d.clear()
    def dizine: Dizin[T] = d.toList
    def herbiriİçin[S](işlev: T => S): Birim = d.foreach(işlev)
  }

  implicit class KuyrukMetotları[T](d: Kuyruk[T]) {
    type Eşlek[K, D] = collection.immutable.Map[K, D]
    type Belki[B] = Option[B]
    def ekle(öge: T): Kuyruk[T] = d += öge
    def çıkar(): T = d.dequeue()
    def baştanAl(): T = d.dequeue() // çıkar ile aynı (kitapçık adı)
    def başı: T = d.head
    def boyu: Sayı = d.size
    def tane: Sayı = d.size // boyu ile aynı (kitapçık adı)
    def boşMu: İkil = d.isEmpty
    def doluMu: İkil = d.nonEmpty
    def sil(): Birim = d.clear()
    def dizine: Dizin[T] = d.toList
    def herbiriİçin[S](işlev: T => S): Birim = d.foreach(işlev)
  
    // --- uçlar, arama --------------------------------------------------
    def başıBelki: Belki[T] = d.headOption
    def sonuBelki: Belki[T] = d.lastOption
    def bul(deneme: T => İkil): Belki[T] = d.find(deneme)
    def bulSondan(deneme: T => İkil): Belki[T] = d.findLast(deneme)
    def nerede(deneme: T => İkil): Sayı = d.indexWhere(deneme)
    def nerede(deneme: T => İkil, başlamaNoktası: Sayı): Sayı = d.indexWhere(deneme, başlamaNoktası)
    def neredeSondan(deneme: T => İkil): Sayı = d.lastIndexWhere(deneme)
    def dilimSırası[S >: T](dilim: Diz[S]): Sayı = d.indexOfSlice(dilim)
    def dilimSırasıSondan[S >: T](dilim: Diz[S]): Sayı = d.lastIndexOfSlice(dilim)
    def sıralar: Range = d.indices
    def başındaMı[S >: T](dizi: Yinelenebilir[S]): İkil = d.startsWith(dizi)
    def sonundaMı[S >: T](dizi: Yinelenebilir[S]): İkil = d.endsWith(dizi)
    def karşılıklıMı[S](öbürü: Diz[S])(deneme: (T, S) => İkil): İkil = d.corresponds(öbürü)(deneme)

    // --- bölme, öbekleme -----------------------------------------------
    def böl(deneme: T => İkil): (Kuyruk[T], Kuyruk[T]) = d.partition(deneme)
    def bölİşle[A1, A2](işlev: T => Either[A1, A2]): (Kuyruk[A1], Kuyruk[A2]) = d.partitionMap(işlev)
    def bölDoğruKaldıkça(deneme: T => İkil): (Kuyruk[T], Kuyruk[T]) = d.span(deneme)
    def bölYerinden(yeri: Sayı): (Kuyruk[T], Kuyruk[T]) = d.splitAt(yeri)
    def öbekli(boy: Sayı): Yineleyici[Kuyruk[T]] = d.grouped(boy)
    def kayarÖbekli(boy: Sayı): Yineleyici[Kuyruk[T]] = d.sliding(boy)
    def kayarÖbekli(boy: Sayı, adım: Sayı): Yineleyici[Kuyruk[T]] = d.sliding(boy, adım)
    def öbekleİşle[K, B](anahtar: T => K)(değer: T => B): Eşlek[K, Kuyruk[B]] = d.groupMap(anahtar)(değer)
    def öbekleİşleİndirge[K, B](anahtar: T => K)(değer: T => B)(indirge: (B, B) => B): Eşlek[K, B] =
      d.groupMapReduce(anahtar)(değer)(indirge)
    def kombinasyonlar(ögeSayısı: Sayı): Yineleyici[Kuyruk[T]] = d.combinations(ögeSayısı)
    def permütasyonlar: Yineleyici[Kuyruk[T]] = d.permutations
    def kuyruklar: Yineleyici[Kuyruk[T]] = d.tails
    def önler: Yineleyici[Kuyruk[T]] = d.inits

    // --- katlama, indirgeme, tarama ------------------------------------
    def katla[S >: T](z: S)(işlev: (S, S) => S): S = d.fold(z)(işlev)
    def indirgeSoldan[S >: T](işlem: (S, T) => S): S = d.reduceLeft(işlem)
    def indirgeSağdan[S >: T](işlem: (T, S) => S): S = d.reduceRight(işlem)
    def indirgeBelki[S >: T](işlem: (S, S) => S): Belki[S] = d.reduceOption(işlem)
    def indirgeSoldanBelki[S >: T](işlem: (S, T) => S): Belki[S] = d.reduceLeftOption(işlem)
    def indirgeSağdanBelki[S >: T](işlem: (T, S) => S): Belki[S] = d.reduceRightOption(işlem)
    // tara: katla gibi, ama ara sonuçların HEPSİNİ verir
    def tara[S >: T](z: S)(işlev: (S, S) => S): Kuyruk[S] = d.scan(z)(işlev)
    def taraSoldan[B](z: B)(işlev: (B, T) => B): Kuyruk[B] = d.scanLeft(z)(işlev)
    def taraSağdan[B](z: B)(işlev: (T, B) => B): Kuyruk[B] = d.scanRight(z)(işlev)
    def enUfağıBelki[S >: T](implicit sıralama: math.Ordering[S]): Belki[T] = d.minOption(sıralama)
    def enUfağıBelki[B](iş: T => B)(implicit karşılaştırma: math.Ordering[B]): Belki[T] = d.minByOption(iş)(karşılaştırma)
    def enİrisiBelki[S >: T](implicit sıralama: math.Ordering[S]): Belki[T] = d.maxOption(sıralama)
    def enİrisiBelki[B](iş: T => B)(implicit karşılaştırma: math.Ordering[B]): Belki[T] = d.maxByOption(iş)(karşılaştırma)

    // --- ekleme, çıkarma -----------------------------------------------
    def sonunaEkle[S >: T](öge: S): Kuyruk[S] = d.appended(öge)
    def önüneEkle[S >: T](öge: S): Kuyruk[S] = d.prepended(öge)
    def sonunaEkleHepsini[S >: T](öbürü: YinelenebilirBirKere[S]): Kuyruk[S] = d.appendedAll(öbürü)
    def önüneEkleHepsini[S >: T](öbürü: YinelenebilirBirKere[S]): Kuyruk[S] = d.prependedAll(öbürü)
    def uzat[S >: T](boy: Sayı, öge: S): Kuyruk[S] = d.padTo(boy, öge)
    def yama[S >: T](nereden: Sayı, yenisi: YinelenebilirBirKere[S], kaçTane: Sayı): Kuyruk[S] =
      d.patch(nereden, yenisi, kaçTane)
    def fark[S >: T](öbürü: Diz[S]): Kuyruk[T] = d.diff(öbürü)
    def kesişim[S >: T](öbürü: Diz[S]): Kuyruk[T] = d.intersect(öbürü)
    def bileşim[S >: T](öbürü: Diz[S]): Kuyruk[S] = d.concat(öbürü)

    // --- seçme, düzleştirme, ikili işlemler ----------------------------
    def seçİşle[B](işlev: PartialFunction[T, B]): Kuyruk[B] = d.collect(işlev)
    def seçİşleİlk[B](işlev: PartialFunction[T, B]): Belki[B] = d.collectFirst(işlev)
    def düzleştir[B](implicit delil: T => YinelenebilirBirKere[B]): Kuyruk[B] = d.flatten(delil)
    def devrik[B](implicit delil: T => Yinelenebilir[B]): Kuyruk[Kuyruk[B]] = d.transpose(delil)
    def ikiliyiAç[A1, A2](implicit delil: T => (A1, A2)): (Kuyruk[A1], Kuyruk[A2]) = d.unzip(delil)
    def ikileHepsini[B, S >: T](öbürü: Yinelenebilir[B], buDolgu: S, oDolgu: B): Kuyruk[(S, B)] =
      d.zipAll(öbürü, buDolgu, oDolgu)
    def tersİşle[B](işlev: T => B): Kuyruk[B] = d.reverse.map(işlev)
}

  implicit class ÖncelikSırasıMetotları[T](d: ÖncelikSırası[T]) {
    def ekle(öge: T): ÖncelikSırası[T] = d += öge
    def çıkar(): T = d.dequeue()
    def başı: T = d.head
    def boyu: Sayı = d.size
    def tane: Sayı = d.size // boyu ile aynı (kitapçık adı)
    def boşMu: İkil = d.isEmpty
    def doluMu: İkil = d.nonEmpty
    def sil(): Birim = d.clear()
    def dizine: Dizin[T] = d.toList
  }
}
