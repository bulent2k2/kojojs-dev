# Öneri: Aralık ve EsnekYazı sarmalayıcıları

> **DURUM: KARARA BAĞLANDI VE UYGULANDI (Eylül 2026).**
> Aralık için **Seçenek A** seçildi (`type Aralık = Range`), öğrenci dostu
> gösterim `yazı()`/`yazıya` yöntemi olarak korundu ve `translate.scala`'ya
> `Range ` → `Aralık ` çevirisi eklendi. EsnekYazı'da `delete` için
> **`aralığıSil`** adı seçildi. Belge, kararın gerekçesiyle birlikte kayıt
> olarak duruyor.
>
> **Uygulama sırasında çıkan düzeltme:** Belgenin ilk halindeki "%6" ve "%1"
> rakamları YANILTICIYDI. `araclar/kapsam.py` sınıf başına ölçüyor; oysa
> `Range` bir `IndexedSeq`, `EsnekYazı` (StringBuilder) bir `collection.Seq`
> olduğu için **SıralıDizi/Diz sarmalayıcılarını zaten alıyorlar** (ölçülerek
> doğrulandı: `(1 |-| 5).bul(_ > 3)` → `Some(4)`, `EsnekYazı("merhaba").böl(_ == 'a')`
> → `(aa, merhb)`). Yani eksik olan, sanılandan çok daha azdı:
> - **Aralık'ta** asıl sorun kapsam değil, **iki ayrı yüz**tü: `Aralık(1, 10)`
>   case class'ı ~20 yöntem veriyordu, `1 |-| 10` (Range) ~110. A seçeneği
>   bunu birleştirdi.
> - **EsnekYazı'da** dizi tarafı zaten vardı; yalnız **tampon tarafı**
>   (araEkle, aralığıSil, harfiSil, değiştirAralığını, harfiKur, boyuKur,
>   tersiYerinde, yerAyır, kapasitesi, harf, parçası…) eksikti, o eklendi.
>
> **Sonradan:** `kapsam.py` bu sınırı artık ölçüyor. Hangi sarmalayıcının hangi
> türe uyduğunu `isAssignableFrom` ile JVM'e soruyor ve iki sütun veriyor:
> `kendi` (o sınıfta yazılanlar) ve `+miras` (kullanıcının gerçekten
> çağırabildikleri). Düzeltilmiş rakamlar: masaüstü %92, ikojo %86;
> Aralık %94/%93, EsnekYazı %90/%89, Yığın %71/%70.

## 1. Aralık: iki ayrı yüz

Bugün **iki farklı şey** aynı adı taşıyor:

```scala
// (a) case class -- lite/i18n/tr/aralik.scala
case class Aralık(ilki: Sayı, sonuncu: Sayı, adım: Sayı = 1) {
  val r = Range(ilki, sonuncu, adım)
  def boyu = r.size
  def işle[B](f: Sayı => B) = r.map(f)
  override def toString() = ... // "Aralık(1, 2, 3 ...)" -- öğrenci dostu
  // ~20 yöntem
}

// (b) Range üstünde örtük sınıf -- aynı dosya
implicit class RangeYöntemleri(r: Range) {
  def adım(c: Sayı): Range = r by c
  def boyu = r.length
  // ~13 yöntem
}
```

Kullanıcı hangisini görüyor?

- `Aralık(1, 10)` yazarsa **(a)**'yı alır: kendi `toString`'i olan bir sarmalayıcı.
- `1 |-| 10` ya da `1 |- 10` yazarsa (`sayi.scala`) **düz bir `Range`** alır,
  yani **(b)**'yi. Örneklerde ve testlerde ezici çoğunluk bu yol.

Sonuç: `Aralık` yazan öğrenci ile `1 |-| 10` yazan öğrenci **farklı yöntem
kümeleri** görüyor. `sıralı`, `bul`, `böl`, `tara`, `enİrisi`, `içeriyorMu`…
hiçbiri (b)'de yok; (a)'da da yok. Kapsamın %6'da kalmasının sebebi bu.

### Seçenek A — `Aralık`ı tür takma adı yap (önerilen)

```scala
type Aralık = Range
object Aralık {
  def apply(ilki: Sayı, sonuncu: Sayı, adım: Sayı = 1): Aralık = Range(ilki, sonuncu, adım)
  def kapalı(ilki: Sayı, sonuncu: Sayı, adım: Sayı = 1): Aralık = Range.inclusive(ilki, sonuncu, adım)
  // kesirden / kesirdenKapalı olduğu gibi kalır
}
```

`Küme = Set`, `Dizin = List`, `Yöney = Vector` ile **aynı kalıp**. Tek bir yüz
kalır; `RangeYöntemleri`'ne ortak çekirdek uygulanır ve kapsam bir hamlede
%6'dan ~%90'a çıkar. `1 |-| 10` ile `Aralık(1, 11)` aynı şeyi verir.

- **Kazanç:** tutarlılık, tek yüz, ~100 yöntem bedavaya gelir.
- **Bedel (düzeltme):** İlk taslakta "`translate.scala`'da `Range` → `Aralık`
  çevirisi zaten var" demiştim; **yanlış**. `dict.scala`'daki `"Range" -> "Aralık"`
  girdisi yardım/tamamlama sözlüğü için; `translate.scala` REPL ÇIKTISINDA böyle
  bir değişim yapmıyor (`LazyList` → `MiskinDizin` var, `Range` yok). Yani A
  seçilirse çıktı `Range(1, 4, 7)` görünür.
  İki hafifletici var:
  1. Öğrenci dostu gösterim bir **yöntem** olarak korunabilir: `yazı()` /
     `yazıya()` bugünkü biçimlendirmeyle `RangeYöntemleri`'ne taşınır
     (`toString` ezilemez ama bu yöntem ezilmez de). Mevcut testteki
     `a.yazı() shouldBe "Aralık(1, 4, 7)"` böylece **geçmeye devam eder**.
  2. Örtük gösterim de istenirse `translate.scala`'ya bir satır
     (`.replace("Range(", "Aralık(")`) ile çevrilebilir.
- **Taşınması gereken üyeler:** `Aralık` case class'ının Range'de karşılığı
  olmayan üyeleri `RangeYöntemleri`'ne eklenmeli — `ilki` (`r.start`),
  `sonuncu` (`r.end`), `adım` (var ama arg alıyor; `adımı` olarak `r.step`),
  `uzunluğu` (`r.size`), `herÖgeİçin` (foreach), `yazı()`/`yazıya()`.
  Bunlar olmadan `TurkishAPITest`'teki mevcut Aralık testi kırılır.
- **Kırılma riski (tarandı):** `Aralık(...)` çağıran betikler çalışmaya devam
  eder (`apply` aynı imzada, `Range` de `map`/`flatMap`/`withFilter` taşıyor, yani
  `kojo-documentation.kojo`'daki `için (g <- Aralık(1, 10))` sürer). `.r` alanına
  doğrudan erişen betik **yok**. Tek somut etki: `scala-tutorial.kojo:130`
  `yazı(Aralık(10, 0, -1))` ile aralığı EKRANA YAZIYOR; çıktı
  `Aralık(10, 9, 8 ...)` yerine `Range(10, 9, 8, ...)` olur. Bu satır ya
  güncellenir ya da `Aralık` nesnesine `yazıya(a: Aralık)` biçiminde bir
  gösterim yardımcısı konur.

### Seçenek B — iki yüzü koru, ikisini de doldur

Hem case class'a hem örtük sınıfa ortak çekirdeği ayrı ayrı ekle.

- **Kazanç:** hiçbir davranış değişmez, özel `toString` durur.
- **Bedel:** ~100 yöntem **iki kez** yazılır; iki yüz kalıcı olarak ayrışmaya
  devam eder ve "hangi Aralık?" sorusu her yeni yöntemde tekrar sorulur.

### Seçenek C — case class'ı kaldır, yalnız örtük sınıfı doldur

`Aralık` case class'ı silinir, `Aralık` bir fabrika nesnesine indirgenir.
A ile hemen hemen aynı sonucu verir ama `type Aralık` olmadığından
`dez a: Aralık = ...` yazan betikler kırılır. A daha güvenli.

**Öneri: A.** `translate.scala` zaten `Range` → `Aralık` çeviriyor; kayıp
yalnız özel `toString`, kazanç ~100 yöntem ve tek bir kavram.

---

## 2. EsnekYazı: StringBuilder'a bakan yüz çok dar

```scala
type EsnekYazı = collection.mutable.StringBuilder
implicit class EsnekYazıYöntemleri(ey: EsnekYazı) {
  def boşMu = ey.size == 0
  def doluMu = ey.size != 0
  def boyu = ey.size
  def sil() = ey.clear()
  def ekle[T](x: T) = ey.append(x)
  def yazıya = ey.toString
  def sayıya = ey.toString.toInt
}
```

Yedi yöntem; `StringBuilder`'ın 131 yöntemlik genel arayüzünün %1'i.
`EsnekYazı` **hem bir Dizi (Seq[Harf]) hem de bir yazı tamponu**; iki ayrı
yöntem ailesi gerekiyor:

1. **Dizi tarafı** — `bul`, `böl`, `öbekli`, `tara`, `seçİşle`, `enİrisi`…
   Ortak çekirdek olduğu gibi uygulanabilir (Harf ögeli). Yeni ad gerekmez.
2. **Tampon tarafı** — yerinde değiştirenler; adlar `…Yerinde` ailesiyle
   uyumlu ama birkaç yeni ad gerekiyor:

| İngilizce | öneri | not |
|---|---|---|
| `insert` / `insertAll` | `araEkle` / `araEkleHepsini` | EsnekDizik'te aynı ad |
| `delete` | `sil(nereden, nereye)` | mevcut `sil()` = clear ile aynı ad, farklı arite |
| `deleteCharAt` | `harfiSil(yeri)` | |
| `replace` | `değiştirAralığı(nereden, nereye, yenisi)` | `değiştir` Yazı'da replace demek |
| `setCharAt` | `harfiKur(yeri, harf)` | |
| `setLength` | `boyuKur(boy)` | |
| `reverseInPlace` | `tersiYerinde` | |
| `ensureCapacity` / `capacity` | `yerAyır` / `kapasitesi` | ileri düzey; atlanabilir |
| `subSequence` / `substring` | `parçası` | Yazı'da aynı ad var |
| `clear` | `sil()` | **zaten var**; `delete` ile çakışma sorusu aşağıda |
| `toCharArray` | `harfDiziğine` | |

**Öneri:** EsnekYazı'yı iki adımda doldur — önce dizi tarafı (ad kararı
gerektirmiyor), sonra tampon tarafı yukarıdaki adlarla. Kapsam %1'den ~%85'e
çıkar.

---

## 3. Sıra ve tahmini iş

| adım | iş | kapsam etkisi |
|---|---|---|
| 1 | Aralık: Seçenek A + ortak çekirdek | Aralık %6 → ~%90 |
| 2 | EsnekYazı: dizi tarafı | %1 → ~%60 |
| 3 | EsnekYazı: tampon tarafı (yeni adlar) | ~%60 → ~%85 |
| 4 | ikojo'ya taşı (ikojo'da Range sarmalayıcısı hiç yok, o da eklenir) | — |

Her adım kendi testiyle gelir; ölçüm `araclar/kapsam.py` ile doğrulanır.

---

## 4. Karar gereken noktalar

1. **Aralık için A, B yoksa C?** (öneri: A)
2. **A seçilirse özel `toString` kaybı kabul mü?** (REPL'de tür adı yine
   `Aralık` görünür, yalnız değer gösterimi `Range(1, 2, 3)` olur)
3. **EsnekYazı tampon adları** yukarıdaki tabloya uygun mu? Özellikle
   `sil()` (clear) ile `sil(nereden, nereye)` (delete) aynı adı paylaşsın mı,
   yoksa ikincisi `aralığıSil` mi olsun?
