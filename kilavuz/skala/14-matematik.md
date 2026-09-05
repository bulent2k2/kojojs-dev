# Matematiksel İşlevler
<!-- hücreler: çalıştır -->

Matematik fonksiyonları bazen işimize çok yararlar. Birkaç tanesini görelim. Tablolardaki komutlara tıklayınca düzenleyicide açılır; sonucu görmek için `satıryaz` içine aldık.

### Değişmez (sabit) Değerler

Matematik sınıfında iki çok meşhur sabit var:

| Komut | Açıklama |
|---|---|
| `satıryaz(eSayısı)` | e, meşhur matematikçi Euler'den gelir adı 2.718282... değerindedir, doğal logaritmanın da tabanıdır. |
| `satıryaz(piSayısı)` | pi sayısı, 3.14159265 .... yani yarıçapı 1 olan dairenin çevre uzunluğunun yarısıdır. |

### Trigonometri fonksiyonları

Trigonometrik işlevler girdi olarak radyan birimi kullanırlar. Radyan kavramını anlatan çok güzel bir örneğimiz var masaüstü Kojo'da: Örnekler menüsünde en altta Matematik Öğrenme Birimleri menüsü var. Onun en altında 'Açı Nedir?' var. Günlük hayatta biz 90 derece, 180 derece gibi bize daha doğal gelen derece birimini kullanırız açıları ifade etmek için. Radyandan dereceye çevirmek için `dereceye` yöntemini, tersini yapmak için de `radyana` yöntemini kullanabiliriz. Bilmemiz gereken tek şey şu: 2*pi radyan 360 dereceye eşittir. Aşağıda sıraladığımız yöntemlerden başka yay yöntemleri de var.

Kısa not: aşağıdaki tanımlarda G1, G2, ... ile fonksiyona girilen değerleri ifade ediyoruz kısaca. Yani işlevAdı(G1, G2, G3, ...).

| Komut | Açıklama |
|---|---|
| `satıryaz(sinüs(piSayısı/6))` | G1 girdisinin yani burada Pi/6 değerinin sinüsü. |
| `satıryaz(kosinüs(piSayısı/6))` | G1 girdisinin kosinüsü. |
| `satıryaz(tanjant(piSayısı/6))` | G1 girdisinin tanjantı. |
| `satıryaz(radyana(45))` | G1 (derece olsun) girdisini radyana çevirir. |
| `satıryaz(45.radyana)` | Kesirlerin 'radyana' yöntemini de kullanabiliriz. |
| `satıryaz(dereceye(piSayısı/2))` | G1 (radyan olsun) girdisini dereceye çevirir. |
| `satıryaz(piSayısı.dereceye)` | Bir değişik yöntem de bu aynı radyana yöntemi gibi. |

### Üst bulma yöntemleri

Logaritma ve üst bulmak için iki temel işlev var. İkisi de e tabanını kullanır (e sayısı).

| Komut | Açıklama |
|---|---|
| `satıryaz(eüssü(piSayısı))` | e (2.71...) sayısının G1 üssünü hesaplar. |
| `satıryaz(gücü(6, 3))` | G1 girdisinin G2 üssünü bulur. |
| `satıryaz(logaritması(10))` | G1'in E tabanına göre logaritmasını verir. |

### Başka bazı fonksiyonlar

| Komut | Açıklama |
|---|---|
| `satıryaz(karekökü(225))` | G1'in karekökü. |
| `satıryaz(mutlakDeğer(-7))` | Mutlak değeri verir. Girdinin türü neyse, çıktı da aynı tür sayı olur, Sayı, Uzun, UfakKesir, Kesir. |
| `satıryaz(enİrisi(8,3))` | G1 ve G2 arasında büyük olanı bulur. |
| `satıryaz(enUfağı(8,3))` | G1 ve G2 arasında küçük olanı bulur. |

### Sayı türüyle ilgili yöntemler

Bu işlevler kesirli sayıları tam yani kesirsiz sayıya çevirir. Ama dikkat, çıktı türü hala Kesir olabilir.

| Komut | Açıklama |
|---|---|
| `satıryaz(taban(3.12))` | G1 girdisinden küçük ya da ona eşit olan en büyük tam sayıyı hesabeder. |
| `satıryaz(tavan(3.12))` | G1 girdisinden büyük ya da ona eşit olan en küçük tam sayıyı verir. |
| `satıryaz(yakını(3.51))` | G1 değerine en yakın tam sayıyı Kesir türünde bir çıktı olarak verir. |
| `satıryaz(yakın(3.48))` | G1 kesirine en yakın sayıyı Uzun türünde çıktı olarak verir. |
| `satıryaz(yakın(2.6F))` | G1 UfakKesir'ine en yakın sayıyı Sayı türünde çıktı olarak verir. |

### Rastgele (Random) sayılar, rastgele başka şeyler...

| Komut | Açıklama |
|---|---|
| `satıryaz(rasgele)` | 0.0 ile 1.0 arasında rastgele bir sayı verir. Çıktı türü Kesir olur. Gerçekten rastgele olmasa da, çok yakındır. Pseudo-random denir tam doğru anlamıyla. Birkaç kere çalıştır bak ne olacak.. İşte rasgele ya da rastgele. |
| `satıryaz(rastgele(2))` | G1 girdisinden küçük rastgele bir tam sayı verir, sıfır da dahil. |
| `satıryaz(rastgele(2, 5))` | G1 girdisinden başlayarak G2 girdisinden küçük rastgele tam sayı verir. |
| `satıryaz(rastgeleSayı)` | Rastgele tam sayı, Sayı.EnUfağı ile Sayı.Enİrisi arasında. |
| `satıryaz(rastgeleUzun)` | Rastgele tam sayı, Uzun.EnUfağı ile Uzun.Enİrisi arasında. |
| `satıryaz(rastgeleKesir(3.0))` | 0'dan G1 girdisine kadar rastgele bir kesir. |
| `satıryaz(rastgeleKesir(2.0, 3.0))` | G1 ve G2 girdileri arasında rastgele bir kesir |
| `satıryaz(rastgeleÇanEğrisinden)` | Normal dağılım da derler. 0'a yakın sayılar daha çok rastlar, büyüdükçe ve küçüldükçe daha az gelirler. Diğer adları: rastgeleNormalKesir, rastgeleDoğalKesir (ikojo'da bu sonuncusu var). |
| `satıryaz(rastgeleİkil)` | Rastgele doğru (true) ya da yanlış (false). Bir diğer adı da: rastgeleSeçim (ikojo'da bu ad var). |
| `satıryaz(rastgeleRenk)` | Matematikle ilgisi yok gibi. Ama adı üstünde. Resim çizerken işe yarayabilir. |
| `satıryaz(rastgeleŞeffafRenk)` | Deneyerek daha iyi anlarsın. |
| `satıryaz(rastgeleDiziden(Dizi(1, 3, 5)))` | G1 girdisindeki diziden rastgele seçer. |
| `satıryaz(rastgeleKarıştır((1 \|-\| 6).dizine))` | Girilen dizini ya da diziyi karıştırır. |

Ağırlıklı seçim: G2 girdisinde verilen ağırlık oranlarını kullanarak G1'den rastgele seçer.

```scala
satıryaz(rastgeleDiziden(
  Dizi(1.0, 10.0, 100.0),
  Dizi(0.9, 0.09, 0.01)))
```

Bir önceki örneği bin kere çalıştıralım ve 10'dan büyük yani 100 gelenleri toplayalım. Birkaç kere çalıştırarak farklı farklı sonuçlar görebilirsin. Bir de sadece 10 gelenleri toplamayı dene istersen. 100 gelme olasılığı %1. Ama deneysel olarak ne çıkıyor nasıl bulabiliriz?

```scala
yaz({
    için (i <- 1 |-| 1000) ver
    rastgeleDiziden(
        Dizi(1.0, 10.0, 100.0),
        Dizi(0.9, 0.09, 0.01))
}.
    ele(_ > 10).
    topla)
```
