# Öneri: Kendini Kesen Yolların Dolgusu

**Amaç:** `tan-theta.kojo` gibi kendi üstünden geçen bir yol boyandığında
ikojo'nun masaüstü Koco ile aynı şekli çizmesi.

**Durum (2026-09-10):** Sebep bulundu ve ölçüldü, çözüm önerisi. Bu PR yalnız
belge; kod değişikliği yok. İlk taslak incelendi ve ölçümler yeniden
koşuldu — hız savı daraltıldı, yıldızın alan hesabı düzeltildi, 9. bölüm
(doğrulanmayanlar) eklendi.

---

## 1. Belirti

`ornekler/masaustu/.../samples/tr/tan-theta.kojo` canlıda yanlış çiziyor.
Betik tanjant eğrisini çiziyor ve `boyamaRenginiKur` ile altını dolduruyor:

```
eğriÇizgesi(-12, 12, 0.1) { x => tanjant(x) }
yaklaşXY(40, 10, 0, 0)
```

`tanjant`ın -12..12 arasında sekiz düşey asimptotu var. Yol her asimptotta
yukarıdan aşağıya atlıyor, yani **kendi üstünden geçiyor**. Masaüstünde
dönüşümlü kanca deseni çıkıyor; ikojo'da üst yarı baştan sona dolu mavi.

## 2. Sebep: iki tarafın dolgu kuralı farklı

İki uçtaki kodu okuduğumda fark tek bir satırda toplanıyor.

**Masaüstü** — `kgeom/PolyLine.scala`:

```scala
val polyLinePath = new Path2D.Double()      // 35. satır
...
if (fillPaint != null) { g2.setPaint(fillPaint); g2.fill(polyLinePath) }   // 100-102
```

`Path2D.Double()`in varsayılan sarım kuralı `WIND_NON_ZERO`. Java2D dolguyu
bu kurala göre yapıyor: bir noktanın içeride sayılması için etrafındaki
dönme sayısının sıfırdan farklı olması gerekiyor.

**ikojo** — `Turtle.scala:111` ve `:122`:

```scala
boyamaYolu.drawPolygon(scala.scalajs.js.Array(boyamaÇokgeni.düzDizi: _*))
```

PIXI `Graphics`, `drawPolygon`u **earcut** ile üçgenliyor. Bu bir tahmin
değil, earcut'ın kendi belgesinde yazıyor (`earcut@3.2.3`, README, "Robustness"):

> Earcut does **not** guarantee a correct triangulation on arbitrary input...
> The input is assumed to be a valid polygon: rings that don't self-cross or
> overlap... On input that breaks these assumptions, the result can be
> noticeably wrong... If correctness matters... for a guaranteed-correct
> triangulation even on bad data, see **libtess.js** (slower and larger).

Yani earcut'ın girdi varsayımı **basit** (kendini kesmeyen) çokgen; ve kendi
belgesi, doğruluk gerektiğinde yönlendirdiği yeri de söylüyor.

(Alıntı earcut 3.2.3'ün belgesinden; PIXI 4 daha eski bir earcut paketliyor.
Sürüm farkının sonucu değiştirmediğini ölçtüm: aşağıdaki sayısal ölçümler
earcut 3.2.3 ile, görsel ölçümler PIXI'nin kendi paketlediği earcut ile
yapıldı, ikisi de aynı yanlış şekli veriyor.)

Yani ikojo'nun bir dolgu *hatası* yok; **başka bir dolgu kuralı** uyguluyor,
daha doğrusu hiçbir kural uygulamıyor.

## 3. Ölçüm 1: yıldız (alanı elle hesaplanabilen en küçük örnek)

Karmaşık betikle tartışmak yerine mekanizmayı en küçük şekilde sabitledim:
beş köşeli yıldız, tek çevrit, R=100.

Doğru alanı elle hesaplayabiliyoruz. NON_ZERO kuralında yıldızın tamamı
(ortadaki beşgen dahil) doluyor:

R = 100 için, iç yarıçap `r = R·cos72°/cos36°`:

```
iç yarıçap    r = 38.19660
beşgen          = (5/2)·r²·sin72°           =  3468.9319
bir uç üçgeni   = ½·(2r·sin36°)·(R - r·cos36°) = 1551.3535
toplam          = beşgen + 5·uç             = 11225.6994
```

(Ara değerleri yuvarlamadan toplamak gerekiyor: 3469 + 5×1551 = 11224
çıkar, doğru toplam değil.)

Aynı nokta listesini iki üçgenleyiciye verdim (Node, `earcut@3.2.3`,
`libtess@1.2.2`):

| üçgenleyici | üçgen | toplam alan | elle hesaba oran |
|---|---|---|---|
| earcut | 2 | 21266.3 | **1.894x** |
| libtess `GLU_TESS_WINDING_NONZERO` | 8 | **11225.7** | **1.0000** |

libtess'in ölçtüğü alan elle hesapla dört ondalık basamak uyuşuyor
(11225.6994 / 11225.7). earcut şeklin neredeyse iki katını dolduruyor.

Tarayıcıda da (PIXI 4.8.9, WebGL) aynı şey görünüyor: earcut'ta yıldız
tanınmaz bir ok başına dönüşüyor, libtess'te düzgün yıldız çıkıyor.

## 4. Ölçüm 2: tan-theta'yı masaüstüne karşı piksel piksel

Masaüstü tarafın birebir modelini Java2D ile kurdum: `Path2D.Double` +
`g2.fill`, `yaklaşXY(40, 10, 0, 0)` dönüşümü `SpriteCanvas.zoomXY`den
alınarak, 640x300. Aynı yolu PIXI'ye iki yoldan verdim ve üç görüntüyü
piksel piksel karşılaştırdım (kenar yumuşatmayı saymamak için her piksel
"dolgu rengine mi artalana mı yakın" diye sınıflandırıldı):

| PIXI tarafı | masaüstünden farklı piksel | oran |
|---|---|---|
| earcut (bugünkü ikojo) | 96 128 / 192 000 | **%50.07** |
| libtess NONZERO | 406 / 192 000 | **%0.21** |

%0.21 yalnız kenar yumuşatma sınırları. Yani libtess NONZERO ile ikojo
masaüstüyle **aynı** şekli çiziyor.

## 5. PIXI 5'e geçmek bunu çözmüyor

Deponun bir PIXI 5 kolu var, önce onu eledim. PIXI 4.8.9 ve PIXI 5.3.12'nin
ikisi de earcut paketliyor (`lib/pixi.min.js`, `lib/pixi5.min.js`). Aynı
yıldızı PIXI 5.3.12 + WebGL ile çizdiğimde şekil yine yanlış — PIXI 4'ten
farklı biçimde yanlış, ama yıldız değil. **Sürüm yükseltmesi bu sorunu
çözmüyor**, ayrı bir iş.

## 6. Değerlendirilen çözümler

### A. libtess.js ile NONZERO üçgenleme  — **önerilen**

GLU tessellator'ın JS'e taşınmış hali — earcut'ın belgesinin doğruluk
gerektiğinde yönlendirdiği kütüphane. `GLU_TESS_WINDING_NONZERO` kuralı
Java2D'nin `WIND_NON_ZERO`suyla aynı tanım.

Hız (Node, tek atış). **Sayılar tek turluk değil**: aynı girdiyle on ayrı
tur koşturunca daire için oran 0.21x ile 2.58x arasında geziniyor, ortanca
0.43x. Yani buradaki rakamları büyüklük mertebesi olarak okumak gerek,
iki ondalık kesinlik gerçek değil.

| küme | earcut | libtess | oran (ortanca) |
|---|---|---|---|
| daire 240, basit **konveks** | 0.140 ms | 0.151 ms | ~1x |
| daire 1000, basit **konveks** | ~2.0 ms | ~0.85 ms | **0.43x** |
| testere dişi 1000, basit **içbükey** | 0.97 ms | 5.04 ms | **5.2x** |
| spiral şerit 1000, basit kıvrımlı | 1.06 ms | 1.55 ms | 1.5x |
| tan-theta 241, kesişen | 0.20 ms | 0.34 ms | 1.7x |
| gül 1000, çok kesişen | 2.23 ms | 109 ms | 49x |

Dikkat: **libtess basit çokgenlerde bedava değil.** Konveks dairede earcut'tan
hızlı, ama içbükey (ama yine de kendini kesmeyen) bir testere dişinde beş kat
yavaş — üçü de 998 üçgen veriyor, yani girdiler gerçekten basit. Önceki bir
taslakta bu "basit çokgenlerde libtess yavaş değil" diye yazılmıştı; o
genelleme yalnız konveks şekiller için doğruydu.

Yine de **"kesişiyor mu" diye önden sınama yazmayı önermiyorum**, ama gerekçe
hız değil: böyle bir sınamanın kendisi O(n²)'ye yakın ve doğru yazması zor.
Bugünkü şekillerde tek atış maliyeti en kötü ihtimalle birkaç ms; sorun tek
atış değil, tekrar sayısı (bkz. 7. bölüm). Faz 1'den sonra gerçek çizimlerle
yeniden ölçülmeli, ve gerekirse nokta sayısına bir eşik konabilir.

Pahalı olan durum yoğun kesişme (gül: 1000 noktada 109 ms). Orada earcut
2.23 ms'de bitiriyor ama çıktısı yanlış: 992 üçgen üretiyor, libtess 11 998.
Aradaki fark libtess'in bütün kesişme noktalarını hesaplaması. 109 ms tek
seferlik bir çizim için kabul edilebilir — **yeter ki tek seferlik olsun**
(bkz. 7. bölüm).

Paket boyutu (`lib/pixi.min.js` = 438 KB / gzip 104.8 KB yanında):

| dosya | ham | gzip | pencereye sızan küresel |
|---|---|---|---|
| `libtess.min.js` | 16.9 KB | **7.0 KB** | **70** |
| `libtess.cat.js` | 146 KB | 37.8 KB | **1** (`libtess`) |

Burada bir tuzak ölçtüm: yayımlanmış `libtess.min.js` Closure çıktısı ve
`t`, `H`, `W`, `n`, `x`, `z` gibi **tek harfli 70 küresel adı** `window`a
bırakıyor. Ölçüm sayfamda `var W = 250, H = 250` yazdığım için kütüphane
kendi içinden `H is not a function` ile patladı; ikinci denemede `var t`
aynı şekilde çakıştı. Node'da (CommonJS kapsamı) hiç görünmeyen, tarayıcıda
kesin çarpan bir tuzak. `libtess.cat.js` yalnız `libtess`i tanımlıyor.

Öneri: **`libtess.cat.js`i alıp `lib/`e koymak** (depo zaten Maven'de
olmayan jar/js'leri orada tutuyor). gzip'te +37.8 KB, PIXI'nin yanında
%36 — doğru şekil için makul. İsteyen ileride kaynaktan yeniden küçültüp
7 KB'a inebilir; bu ayrı ve acele olmayan bir iş.

Bilinmesi gereken iki şey daha:

- **Bakımsız.** Son sürüm 1.2.2, 19 Aralık 2015 — on bir yıldır güncelleme
  yok. Kütüphane küçük ve işi dar (GLU tessellator'ın birebir taşınması),
  yani bu ölümcül değil; ama bir hata çıkarsa yamayı biz yazacağız.
- **Lisans uyumlu.** SGI Free Software License B 2.0; metni Expat/MIT ile
  aynı ("without restriction... subject to [attribution]"), deponun
  GPLv3'üyle (`LICENCE`) uyumlu. Telif notu `lib/`e olduğu gibi taşınmalı.

### B. Ekran dışı Canvas2D dokusu — **elendi**

Canvas2D'nin `fill()`i varsayılan olarak nonzero, yani Java2D ile aynı.
Yolu ekran dışı bir tuvale çizip PIXI dokusu yapmak da doğru şekli veriyor
(ölçtüm: 1x'te yıldız düzgün çıkıyor).

Elenme sebebi **çözünürlük**: doku dünya ölçeğinde çiziliyor, sonra kamera
onu büyütüyor. Dünyada küçük bir yıldızı 8x yaklaştırdığımda kenarlar
bulanık ve basamaklı çıktı; aynı şekil libtess üçgenleriyle keskin kaldı.
tan-theta'nın son satırı `yaklaşXY(40, 10, 0, 0)` — yani tam bu durum, üstelik
8x değil 40x. Ayrıca `yaklaş`ın her değişiminde dokuyu yeniden çizmek gerekir.

### C. Yolu asimptotlarda bölmek — **elendi**

tan-theta'yı düzeltir, sorunu düzeltmez. Kendini kesen her yol (yıldız, gül,
düğüm, kullanıcının yazdığı herhangi bir eğri) aynı yerde kalır. Üstelik
masaüstü böyle yapmıyor; amacımız iki tarafı aynı kılmak.

## 7. Ön koşul: dolguyu her kenarda yeniden üçgenlemeyi bırakmak

libtess'i bugünkü akışa öylece takarsak yavaşlar, ve sebebi libtess değil.

`Turtle.turtlePathLineTo` **her kenarda** `boyamayıTazele()` çağırıyor
(`Turtle.scala:95`), o da bütün çokgeni baştan `drawPolygon`a veriyor.
Yani n kenarlı bir şekil n kez üçgenleniyor. `BoyamaYolu`nun başındaki
açıklama bunun sebebini anlatıyor: PIXI 5 yarım kalan yolu render sırasında
olduğu yerde kapatıyor, o yüzden şekil her seferinde tamamlanmış olarak
yayınlanıyor.

Toplam maliyet (3 noktadan n noktaya kadar bütün ara adımlar):

| küme | earcut toplam | libtess toplam |
|---|---|---|
| daire 1000, basit | 557 ms | **258 ms** |
| tan-theta 241, kesişen | 34.7 ms | 74.8 ms |
| gül 1000, çok kesişen | 452 ms | **27 302 ms** |

27 saniye kabul edilemez. Ama bu israfın bugün de var olduğuna dikkat:
`hızıKur(çokHızlı)` (gecikme 0) ile bütün kenarlar tek blokta geliyor ve
**tek bir render** oluyor — buna karşılık tan-theta 241 kez üçgenleniyor.
240'ı atılıyor.

Çare üçgenlemeyi tembelleştirmek: kenar eklenince yalnız "kirli" işaretle,
üçgenlemeyi gerçekten render edilmeden hemen önce yap. O zaman maliyet kare
başına bir üçgenlemeye iner; `çokHızlı`da gülün maliyeti 27 302 ms yerine
tek atış 109 ms olur.

Bu değişiklik libtess'ten bağımsız olarak da doğru — bugünkü earcut'la da
kazandırıyor.

## 8. Önerilen sıra

1. **Faz 1 — tembel üçgenleme.** `BoyamaYolu`ya kirli bayrağı, `Turtle`da
   `boyamayıTazele`yi render öncesine taşımak. Üçgenleyici değişmiyor,
   şekiller değişmiyor; yalnız tekrar sayısı düşüyor. Kendi başına
   ölçülebilir ve ayrı incelenebilir.
2. **Faz 2 — libtess NONZERO.** Scala tarafı dar: `düzDizi`nin yanına bir
   `üçgenler` yolu ve `Turtle.scala`da iki `drawPolygon` çağrısı (`:111`,
   `:122`). Asıl iş **kütüphaneyi her yere aynı anda koymak**. PIXI bugün
   iki depoda beş yerde eşgüdümlü duruyor ve libtess de aynısını isteyecek:

   | yer | ne için |
   |---|---|
   | `lib/libtess.cat.js` | kaynak kopya |
   | `run.html`, `run5.html` | geliştirme sayfaları (`<script>`) |
   | `build.sbt` `jsDependencies` | tarayıcı sınamaları |
   | `src/test/resources/` | sınama kopyası |
   | `kojojs-editor/.../resultframe.scala.html` | **canlı site** (ayrı depo) |

   `build.sbt`'nin kendi DİKKAT yorumu bu tuzağı anlatıyor: *"Editör
   yükseltilirse ÜÇÜ birden tazelenmeli — CI depolar arasını göremiyor"*.
   PIXI'de bu bir kez ısırdı: sınama harness'ı uzun süre 4'te kalınca
   `PixiUyum.beşVeÜstü` hep false döndü ve bütün doku dolgusu yolu sessizce
   sınanmadan kaldı. Faz 2 bunu hesaba katmalı; `uretecler.yml`'deki PIXI
   `cmp` denetiminin eşi libtess için de konmalı.
3. **Faz 3 — sınama.** Yıldızın alanı elle hesaplanabildiği için üçgen
   alanları toplamını **11225.6994**'e karşı sınayan bir test yazılabilir.
   Ama bu **tarayıcı sınamasıdır, Node sınaması değil**: Node kaçış yolu
   `set jsDependencies := Seq()` ile bütün sağlanan JS'i düşürüyor, libtess
   dahil. (Üçgenleyici saf Scala olsaydı Node'da koşardı; libtess'le
   koşmuyor.) Ayrıca tan-theta için `araclar/`a masaüstü referansına karşı
   piksel karşılaştırması eklenebilir.

## 9. Doğrulanmayanlar

Belgenin dayandığı ölçümlerin sınırları:

- **Masaüstü referansı gerçek Koco değil, Java2D modeli.** 4. bölümdeki
  %0.21 rakamı PIXI'yi, Kojo'yu açıp ekran görüntüsü alarak değil, masaüstü
  çizim yolunun kaynaktan doğrulanmış bir yeniden kurulumuna karşı ölçüyor
  (aynı `Path2D.Double` + `g2.fill`, `zoomXY`'nin kendi sayıları). Model
  sadık ama yine de model.
- **libtess PIXI içinde denenmedi.** Görsel ölçümlerde üçgenleri PIXI'ye
  üçgen başına `drawPolygon` ile verdim. Gerçek uygulamada bunun mu yoksa
  bir `Mesh`in mi doğru olduğu ölçülmedi — üçgen başına `drawPolygon` her
  üçgen için earcut'ı yeniden çağırıyor (üç köşede ucuz, ama bedava değil).
- **Faz 1'in kazancı hesaplandı, ölçülmedi.** 7. bölümdeki "27 302 ms yerine
  109 ms" tembel üçgenlemenin tek atışa ineceği varsayımından çıkıyor;
  uygulanıp ölçülmedi.
- **Süre ölçümleri Node'da**, tarayıcıda değil. Tarayıcının JIT'i ve GC'si
  farklı davranabilir.
- **Kesişen şekillerde earcut'ın çıktısı "yanlış" diye ölçüldü** (alan ve
  piksel), ama kaç kojo örneğinin bu duruma düştüğü sayılmadı; tan-theta
  bilinen tek somut örnek.

## 10. Ölçümün tekrarı

Ölçümler `earcut@3.2.3` ve `libtess@1.2.2` ile, Node v22.22.2 ve headless
Chromium 141.0.7390.37 (WebGL, ANGLE/SwiftShader) üzerinde yapıldı. Masaüstü referansı
`Path2D.Double` + `Graphics2D.fill` ile üretilen 640x300 PNG.

libtess'i çağırırken dikkat edilecek nokta: `GLU_TESS_BEGIN` geri çağrısı
kayıtlı değilse kütüphane çağrı sırasında patlıyor; `GLU_TESS_COMBINE` de
şart, çünkü kesişme noktalarında yeni köşe üretiliyor — zaten bütün mesele o.

```js
ts.gluTessCallback(libtess.gluEnum.GLU_TESS_VERTEX_DATA, (d, a) => a.push(d[0], d[1]))
ts.gluTessCallback(libtess.gluEnum.GLU_TESS_BEGIN,   () => {})
ts.gluTessCallback(libtess.gluEnum.GLU_TESS_COMBINE, (c) => [c[0], c[1], c[2]])
ts.gluTessProperty(libtess.gluEnum.GLU_TESS_WINDING_RULE,
                   libtess.windingRule.GLU_TESS_WINDING_NONZERO)
ts.gluTessNormal(0, 0, 1)
```
