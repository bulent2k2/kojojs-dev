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
    type Col = Kuyruk[T]
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

    // --- YERİNDE değiştirenler -------------------------------------------
    // Kuyruğun KENDİSİNİ değiştirirler. Kuyruk mantığı: kuyruğaEkle sona
    // koyar, baştanÇıkar baştan alır (ilk giren ilk çıkar).
    def kuyruğaEkle(öge: T): Col = { d.enqueue(öge); d }
    def kuyruğaEkleHepsini(ögeler: YinelenebilirBirKere[T]): Col = { d.enqueueAll(ögeler); d }
    def baştanÇıkar(): T = d.dequeue()
    def baştanÇıkarBelki: Belki[T] = d.removeHeadOption()
    def baştanÇıkarDoğruKaldıkça(deneme: T => İkil): Diz[T] = d.removeHeadWhile(deneme)
    def baştanÇıkarKoşulla(deneme: T => İkil): Belki[T] = d.dequeueFirst(deneme)
    def baştanÇıkarHepsiniKoşulla(deneme: T => İkil): Diz[T] = d.dequeueWhile(deneme)
    def sondanÇıkar(): T = d.removeLast()
    def sondanÇıkarBelki: Belki[T] = d.removeLastOption()
    def ilki: T = d.front
    def eleYerinde(deneme: T => İkil): Col = { d.filterInPlace(deneme); d }
    def işleYerinde(işlev: T => T): Col = { d.mapInPlace(işlev); d }
    def sıralıYerinde(implicit sıralama: Ordering[T]): Col = { d.sortInPlace()(sıralama); d }
    def sıralaYerinde[B](iş: T => B)(implicit sıralama: Ordering[B]): Col = { d.sortInPlaceBy(iş)(sıralama); d }
    def hepsiniEkle(ögeler: YinelenebilirBirKere[T]): Col = { d.addAll(ögeler); d }
    def araEkle(yeri: Sayı, öge: T): Birim = d.insert(yeri, öge)
    def çıkar(yeri: Sayı): T = d.remove(yeri)
    def çıkarHepsini(ögeler: YinelenebilirBirKere[T]): Col = { d.subtractAll(ögeler); d }
    def boşalt(): Birim = d.clear()
    def sıralı(implicit sıralama: Ordering[T]): Diz[T] = d.sorted(sıralama)
    def sırala[B](iş: T => B)(implicit sıralama: Ordering[B]): Diz[T] = d.sortBy(iş)(sıralama)
    def sırayaSok(önce: (T, T) => İkil): Diz[T] = d.sortWith(önce)
    def içeriyorMu[S >: T](öge: S): İkil = d.contains(öge)
    def sırası[S >: T](öge: S): Sayı = d.indexOf(öge)
    def sırasıSondan[S >: T](öge: S): Sayı = d.lastIndexOf(öge)
    def yinelemesiz: Diz[T] = d.distinct
    def yinelemesizİşlevle[B](işlev: T => B): Diz[T] = d.distinctBy(işlev)
}

  implicit class ÖncelikSırasıMetotları[T](d: ÖncelikSırası[T]) {
    type Col = ÖncelikSırası[T]
    type Belki[B] = Option[B]
    type Eşlek[K, D] = collection.immutable.Map[K, D]
    def ekle(öge: T): ÖncelikSırası[T] = d += öge
    def çıkar(): T = d.dequeue()
    def başı: T = d.head
    def boyu: Sayı = d.size
    def tane: Sayı = d.size // boyu ile aynı (kitapçık adı)
    def boşMu: İkil = d.isEmpty
    def doluMu: İkil = d.nonEmpty
    def sil(): Birim = d.clear()
    def dizine: Dizin[T] = d.toList
  
    // --- ortak çekirdek ---------------------------------------------------
    // NOT: Koleksiyon ÜRETEN yöntemler Dizi veriyor, ÖncelikSırası değil.
    // Sebep: yeni bir öncelik sırası kurmak örtük bir Ordering ister ve
    // imzaları gereksiz karmaşıklaştırırdı. Sıralı bir sonuç gerekiyorsa
    // ÖncelikSırası(...) ile açıkça yeniden kurulur.
    def başıBelki: Belki[T] = d.headOption
    def sonuBelki: Belki[T] = d.lastOption
    def bul(deneme: T => İkil): Belki[T] = d.find(deneme)
    def böl(deneme: T => İkil): (Dizi[T], Dizi[T]) = {
      val (e, h) = d.iterator.toSeq.partition(deneme); (e, h)
    }
    def bölDoğruKaldıkça(deneme: T => İkil): (Dizi[T], Dizi[T]) = d.iterator.toSeq.span(deneme)
    def bölYerinden(yeri: Sayı): (Dizi[T], Dizi[T]) = d.iterator.toSeq.splitAt(yeri)
    def öbekli(boy: Sayı): Yineleyici[Dizi[T]] = d.iterator.toSeq.grouped(boy)
    def kayarÖbekli(boy: Sayı): Yineleyici[Dizi[T]] = d.iterator.toSeq.sliding(boy)
    def öbekleİşle[K, B](anahtar: T => K)(değer: T => B): Eşlek[K, Dizi[B]] =
      d.iterator.toSeq.groupMap(anahtar)(değer)
    def öbekleİşleİndirge[K, B](anahtar: T => K)(değer: T => B)(indirge: (B, B) => B): Eşlek[K, B] =
      d.iterator.toSeq.groupMapReduce(anahtar)(değer)(indirge)
    def kuyruklar: Yineleyici[Dizi[T]] = d.iterator.toSeq.tails
    def önler: Yineleyici[Dizi[T]] = d.iterator.toSeq.inits
    def katla[S >: T](z: S)(işlev: (S, S) => S): S = d.fold(z)(işlev)
    def indirgeSoldan[S >: T](işlem: (S, T) => S): S = d.reduceLeft(işlem)
    def indirgeSağdan[S >: T](işlem: (T, S) => S): S = d.reduceRight(işlem)
    def indirgeBelki[S >: T](işlem: (S, S) => S): Belki[S] = d.reduceOption(işlem)
    def indirgeSoldanBelki[S >: T](işlem: (S, T) => S): Belki[S] = d.reduceLeftOption(işlem)
    def indirgeSağdanBelki[S >: T](işlem: (T, S) => S): Belki[S] = d.reduceRightOption(işlem)
    def tara[S >: T](z: S)(işlev: (S, S) => S): Dizi[S] = d.iterator.toSeq.scan(z)(işlev)
    def taraSoldan[B](z: B)(işlev: (B, T) => B): Dizi[B] = d.iterator.toSeq.scanLeft(z)(işlev)
    def taraSağdan[B](z: B)(işlev: (T, B) => B): Dizi[B] = d.iterator.toSeq.scanRight(z)(işlev)
    def enUfağıBelki[S >: T](implicit sıralama: math.Ordering[S]): Belki[T] = d.minOption(sıralama)
    def enUfağıBelki[B](iş: T => B)(implicit karşılaştırma: math.Ordering[B]): Belki[T] = d.minByOption(iş)(karşılaştırma)
    def enİrisiBelki[S >: T](implicit sıralama: math.Ordering[S]): Belki[T] = d.maxOption(sıralama)
    def enİrisiBelki[B](iş: T => B)(implicit karşılaştırma: math.Ordering[B]): Belki[T] = d.maxByOption(iş)(karşılaştırma)
    def seçİşle[B](işlev: PartialFunction[T, B]): Dizi[B] = d.iterator.toSeq.collect(işlev)
    def seçİşleİlk[B](işlev: PartialFunction[T, B]): Belki[B] = d.collectFirst(işlev)
    def düzleştir[B](implicit delil: T => YinelenebilirBirKere[B]): Dizi[B] = d.iterator.toSeq.flatten(delil)
    def devrik[B](implicit delil: T => Yinelenebilir[B]): Dizi[Dizi[B]] = d.iterator.toSeq.transpose(delil)
    def ikiliyiAç[A1, A2](implicit delil: T => (A1, A2)): (Dizi[A1], Dizi[A2]) = d.iterator.toSeq.unzip(delil)
    def ikileHepsini[B, S >: T](öbürü: Yinelenebilir[B], buDolgu: S, oDolgu: B): Dizi[(S, B)] =
      d.iterator.toSeq.zipAll(öbürü, buDolgu, oDolgu)

    // --- YERİNDE değiştirenler -------------------------------------------
    def işleYerinde(işlev: T => T): Col = { d.mapInPlace(işlev); d }
    def hepsiniEkle(ögeler: YinelenebilirBirKere[T]): Col = { d.addAll(ögeler); d }
    def kuyruğa: Kuyruk[T] = d.toQueue
}
}
