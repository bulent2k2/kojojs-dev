# Öneri: Eskiz ve TuvalÇizim'i iKoco'ya getirmek

**Amaç:** masaüstü Koco'nun (bulent2k2/kojo#88) Processing tarzı eskiz katmanının
(`Eskiz`, `Resim.eskizden`, `tuvalEskizi`, `canlandırKurulumluTuvalÇizimle`) ve
tam `TuvalÇizim` yüzeyinin iKoco'da da çalışması; masaüstünde yazılan eskiz
betikleri tarayıcıda derlenip çizebilsin.

**Durum (2026-10-11):** inceleme ve öneri. Bu PR yalnız belge; kod değişikliği
yok. Aşağıdaki "ölçüldü" işaretli her şeyi iki depoda (kojo `565d55f`, kojojs-dev
`75d74f8`) okuyarak ya da çalıştırarak doğruladım; "ölçülmedi" işaretliler
tahmindir ve ilgili dilimde ölçülmesi gerekir.

---

## 1. Bugünkü durum (ölçüldü)

**Masaüstü, #88 ile:** `tr/eskiz.scala` (`Eskiz` arayüzü: `kurulum(tuval)` bir kez,
`çizimDöngüsü(tuval)` her karede), `Resim.eskizden(eskiz, ölçekÇarpanı)`,
`tuvalEskizi(...)`, `canlandırKurulumluTuvalÇizimle(kurulum)(kare)`, ve `TuvalÇizim`e
yirmiden fazla yöntem (`rastgeleTohum`, `döngüyüDurdur`, `götür`, `döndürRadyan`,
`döndürDerece`, `büyüt`, `dönüşümüKaydet`/`GeriYükle`, `fırçaUcu`/`fırçaBirleşimi`,
`karışımKipi`, `şekilBaşla`/`köşe`/`şekilBitir` ve köşe aileleri).

**iKoco'da bunların hiçbiri yok.** `src/main/scala/kojo/` ve `kojo/tr/` içinde
`eskiz`, `sketch`, `CanvasDraw`, `TuvalÇizim`, `canlandırTuvalÇizimle` için sıfır
eşleşme. Bununla ilgili iki göstergeyi de gördüm:

- Masaüstü örneklerinden yalnız **`fireworks-canvas.kojo`** (İngilizce ve Türkçe
  kopya) `TuvalÇizim` kullanıyor. `ornekler/masaustu/derleme.tsv`: `kaldı — not
  found: type TuvalÇizim`; `tarama.tsv`: `platform`, engel `tuvalçizim`
  (`araclar/ucurum.py` bunu "Graphics2D tuval çizimi" diye sınıflıyor).
- `Picture.fromCanvas` / `Resim.tuvalden` de iKoco'da yok; yani sorun yalnız
  eskiz değil, "kendi piksel yüzeyine çiz" ailesinin tamamı.

**Bu boşluk CI'da görünmüyor.** `araclar/adlar.py` masaüstü ile iKojo'yu yalnız
elle yazılmış **iki** kapsayıcı çiftinde karşılaştırıyor (`Renkler`, `tuşlar`);
`TuvalÇizimYöntemleri`, `Resim` nesnesi (`yoldan`, `noktadan`, `tuvalden`,
`eskizden`) ve `trInit` düzeyindeki işlevler (`canlandırTuvalÇizimle`,
`tuvalEskizi`) çiftli değil. `adlar.py --tsv --kojo ../kojo`'yu güncel kojo ile
koştum: `masaustu-adlar.tsv` değişmedi ve `toplam eksik: 0` dedi — araç
bakmadığı yerde "temiz" diyor. Boşluğu yalnız örnek taraması (`ucurum.py`) görmüş.

## 2. #88'in asıl nedeni iKoco'da aynı biçimde yok (ölçüldü)

#88, #87'yi çözmek için yazıldı: masaüstü `draw(pic)`, `PicCache.size > 60000`
olunca `Too many pics to draw` ile **sert duruyor**. iKoco'da:

- `PicCache.scala` yalnız bir `seen` kaydı ve `hits`/`misses` sayaçları tutuyor;
  boyut sınırı da `checkForLargeDrawing` de yok. Aynı biçimde sert bir durma olmaz.
- iKoco'nun kendi önlemi var: `KojoWorld.maybeBake` kalabalık sahnede durağan
  çocukları bir `RenderTexture`'a **pişiriyor** (`BakePolicy`); kodundaki
  yorumlar tam olarak "iz noktaları" iş yükünü anlatıyor.

Dolayısıyla eskiz iKoco için **bir hata düzeltmesi değil**: API eşitliği ve yeni
bir yetenek (piksel üstü çizim: parçacıklar, üretken sanat, iz bırakan
benzetimler). Bu önemli, çünkü aciliyet derecesini belirliyor; #88'in "Too many
pics" gerekçesini iKoco'ya taşımak yanlış olur. (İleti çevirisi
`translate.scala`'da yalnız masaüstü; iKoco'da yapılacak bir şey yok.)

*Ölçülmedi:* #88'in değiştirdiği nbody örneklerinin (kaplumbağa izi) iKoco'da
hızlı çalışıp çalışmadığı. Kullandıkları adların hepsi iKoco kaynağında var
(`yeniKaplumbağa`, `gizle`, `canlandırmaHızınıKur`, `kalemRenginiKur`,
`kalemKalınlığınıKur`, `atla`, `ilerle(x, y)`, `ikile`), ama kopyamız eski sürüm
(`derleme.tsv`: hepsi `geçti`, yani **eski** nbody gövdeleri). Bkz. Dilim 0.

## 3. Masaüstünün eskiz anlamları, iKoco'nun karşılaması gerekenler

Kaynak: `lite/Builtins.scala` (`SketchRunner`), `lite/CanvasDraw.scala`,
`picture/package.scala` (`fromJava2dDynamic`).

1. **Yüzey kalıcı.** Eskiz tek bir resim: `kurulum` bir kez, sonra her karede
   `çizimDöngüsü`. Yüzey kareler arasında **silinmiyor**; `artalan` çağrılmazsa
   çizimler birikiyor. İz bırakmanın nedeni bu.
2. **Durdurma.** `döngüyüDurdur()` (`noLoop`) → `shouldStop` → animasyon durur;
   `döngüdeMi` bunu okur.
3. **`ölçekÇarpanı`.** Yüzey `genişlik × f` ve `yükseklik × f` piksel, resim
   `1/f` ölçeklenir (daha ince çizgi için).
4. **Koordinatlar.** `artalan`, dönüşümün tersiyle yüzeyin tamamını boyuyor
   (`bgRectCoords`); `yazı` ve `imge` y'si ters çevrilerek çiziliyor
   (`drawText`: `scale(1, -1)`). #88 de **bu eşlemeyi doğrulamadığını** yazıyor.
   iKoco'da kendimiz ölçmeliyiz (Dilim 1'in ilk işi).
5. **Dönüşüm yığını.** `dönüşümüKaydet`/`GeriYükle` yalnız dönüşüm matrisini
   saklıyor (`g2d.getTransform`), fırça/dolgu stilini değil.

## 4. iKoco'da nasıl yapılır

### Seçenek A (önerilen): HTML `canvas` + tek PIXI `Sprite`

Eskiz yüzeyi bir `dom.html.Canvas`; her karede 2B bağlama çizilir, sonra doku
güncellenip tek `Sprite` olarak sahneye konur (`ImagePic`'in `Container` +
`kojoWorld.addLayer` kalıbı). Bu iKoco'da zaten iki yerde var:

- `Boya.scala`, `dom.document.createElement("canvas")` ile tuvalden doku üretiyor
  ve **`Texture.from`u bilerek kullanmıyor**: önbelleğe (`TextureCache`) hiç
  çıkarmadan girdiği için her çağrıda kalıcı sızıntı bıraktığını ölçmüş
  (`new Texture(new BaseTexture(...))` kullanıyor). Eskiz de aynısını yapmalı;
  yoksa `canlandır` içinde sızdırırız.
- `pixi.scala` `Texture.fromCanvas`/`BaseTexture.fromCanvas` cephelerini zaten
  taşıyor.

Neden A: eskizin tanımı "çizilenler piksel, tek resim"; bu, #88'in verdiği
garantinin (nesne büyümez) iKoco karşılığı. Vektör olarak biriktirmek (Seçenek
B: kalıcı `PIXI.Graphics`'e eklemek) bellek ve çizim maliyetini sınırsız
büyütür, eskizin amacına ters; elendi.

### Java2D → Canvas 2B eşlemesi

| masaüstü `CanvasDraw` | iKoco (`CanvasRenderingContext2D`) |
|---|---|
| `artalan(renk)` | dönüşümün tersiyle yüzeyin tamamına `fillRect` |
| `boya` / `fırça` / `fırçasız` / `boyasız` | `fillStyle` / `strokeStyle` / kapat |
| `fırçaAğırlığı` | `lineWidth` |
| `fırçaUcu`: `yuvarlakUç` / `projeUç` / `kareUç` | `lineCap`: `round` / `square` / `butt` (masaüstü adları ters görünür: `PROJECT = CAP_SQUARE`, `SQUARE = CAP_BUTT`) |
| `fırçaBirleşimi`: `sivriBirleşim` / `eğikBirleşim` | `lineJoin`: `miter` / `bevel` |
| `elips(cx,cy,w,h)`, `çizgi`, `nokta` | `ellipse` (**w/h tam boy**, Canvas yarıçap ister: `w/2`, `h/2`), `moveTo/lineTo`, çok kısa çizgi |
| `dikdörtgen(x1,y1,x2,y2)` | `rect(x, y, w, h)`. **Parametre adları yanıltıcı:** masaüstü `Rectangle2D.setRect(x1, y1, x2, y2)` çağırıyor, yani 3. ve 4. değer **köşe değil genişlik ve yükseklik** (okudum, çalıştırmadım) |
| `yay(cx,cy,w,h,başla,uzunluk)` | `ellipse(..., başla, başla+uzunluk)`. Girdi **radyan** (`CanvasDraw.arc` `toDegrees` ile Java'ya çeviriyor), Canvas'la aynı birim; yönü ve kapanış biçimi (`Arc2D.OPEN`) ölçülmedi |
| `götür`, `döndürRadyan`, `döndürDerece`, `büyüt` | `translate`, `rotate`, `scale` |
| `dönüşümüKaydet`/`GeriYükle` | `getTransform`/`setTransform` (`save`/`restore` **değil**: o stili de saklar, masaüstü saklamıyor) |
| `şekilBaşla`/`köşe`/`bezierKöşesi`/... | `Path2D`; iKoco'nun `VertexShapeSupport`'u `PIXI.Graphics` üstüne kurulu, soyut `shapeDone` ve `shapePath` var — yol soyutlaması gerekiyor (inceleme maddesi: aynı trait yeniden kullanılabilir mi?) |
| `yazı`, `yazıYüzü` | `fillText`/`strokeText`; iKoco `Font`u var (`kalın`/`eğik` bugün çizimde yok, bilinen sınır) |

**Karşılığı olmayan ya da değişen (açık karar, §7):** `karışımKipi(java.awt.Composite)`
ve `drawShape(java.awt.Shape)` Java2D türleri; iKoco'da AWT yok. `karışımKipi` için
`globalCompositeOperation` adlı bir yazı değeri önerilir, ama imza masaüstünden
ayrılır.

### Türkçe ve İngilizce yüzey

Masaüstüyle **aynı adlar** (betik taşınabilirliği için): `Eskiz`, `Resim.eskizden`,
`tuvalEskizi`, `canlandırTuvalÇizimle`, `canlandırKurulumluTuvalÇizimle`,
`TuvalÇizim` yöntemleri → `kojo/tr/` altında `eskiz.scala` ve `tuvalcizim.scala`;
İngilizce `Picture.fromSketch`, `canvasSketch`, `animateWithCanvasDraw`,
`animateWithSetupCanvasDraw`, `CanvasDraw` → `kojo/` altında.

**Yapısal tür sorunu.** Masaüstü `fromSketch` parametresi yapısal tür
(`{ def setup(cd); def drawLoop(cd) }`) ve yansımayla çağrılıyor; #88 de
"`Eskiz` alt sınıfı İngilizceye çevrilemez" diyor. iKoco Scala.js 1.20.2 / Scala
2.13.18; kaynakta hiç yapısal tür/yansıma kullanımı yok ve **yapısal tür
çağrısının Scala.js'de çalışıp çalışmadığını ölçmedim**. Güvenli varsayılan:
`Sketch`/`Eskiz` düz `trait` olsun (alt sınıf `extends Sketch`). Bedeli:
masaüstünde yapısal türle yazılmış İngilizce betikler (`new { def setup... }`)
iKoco'da derlenmez. Bu bir tasarım kararı (§7).

## 5. Dilimler

Her dilim tek başına birleşebilir ve ölçülebilir.

**Dilim 0 — masaüstü anlık görüntülerini yenile (kod yok).**
`ornekler/masaustu/guncelle.sh` ile kopyayı #88'e getir; `ornekleri-dogrula.sh -g`
ile `derleme.tsv`'yi yeniden üret. Ölçülecek: #88'in değiştirdiği on örnek
(nbody ×5, TR ve EN) iKoco'da derleniyor mu, çalışırken akıyor mu (Playwright ile
kare süresi). Bu dilim eskizden bağımsız ve iKoco'nun bugün #88'den ne aldığını
söyler.

**Dilim 1 — `CanvasDraw` çekirdeği (İngilizce).** Canvas + Sprite yüzeyi,
`Picture.fromSketch`/`canvasSketch`/`animateWithCanvasDraw`. İlk iş koordinat
eşlemesini (§3.4) `getImageData` ile ölçmek: sabit noktalar (artalan, eksen
çizgileri, dönüşümlü bir elips, yazı) beklenen piksellerde mi. Ayrıca ölçülecek:
yüzey boyutu × `devicePixelRatio` telefonda bellek ve kare maliyeti; pişirmeyle
etkileşim (eskiz Sprite'ı her karede değişiyor: `__kojoNoBake` ile muaf tutmak
gerekir, yoksa her karede çözülüp yeniden pişer — `noteMutation` yorumlarındaki
tuzağın aynısı).

**Dilim 2 — Türkçe katman.** `kojo/tr/eskiz.scala`, `tuvalcizim.scala`,
`trInit` düzeyindeki işlevler, sözlük (`sozluk/`), `kilavuz/masaustu-adlar.txt`
tazele. Beklenen ilerleme: `fireworks-canvas.kojo` `kaldı` → `geçti`
(`ornekleri-dogrula.sh -b` bunu ilerleme diye yazar). Bu dilimde `adlar.py`'ye
`TuvalÇizimYöntemleri` ve `Resim` çiftlerini ekleyerek §1'deki körlüğü kapat.

**Dilim 3 — "Çevir" ve yardım.** `araclar/kojo-cevirmen-surumu.txt` sabit SHA'sı
`3056e4d`, yani #88'in **öncesi**; #88 `ceviri-sozlugu.tsv`'ye 29 satır ekledi
(`eskizden ↔ Picture.fromSketch`, `tuvalEskizi`, köşe adları...). Yükseltme
tarifi dosyanın başında yazılı (SHA değiştir → `cevir-derle.py --guncelle` → ikisi
aynı commit'te). **Sıra önemli:** önce Dilim 1-2, sonra bu; yoksa "Çevir"
iKoco'da hiç bulunmayan bir adı üretir. Ayrıca kojojs-editor'daki `Komutlar`
kılavuzuna kısa bir eskiz bölümü ve iKoco'ya özgü bir örnek (`19-...kojo`);
üretilmiş sayfalar için sıra yine **editör önce, kojojs-dev sonra** (#84 / #212
dersi: kojojs-dev'in master push denetimi editördeki şablonla karşılaştırıyor).

**Kapsam dışı:** `PictureDraw`/`pictureSketch` (#88'de de yok).

## 6. Neyi doğrulamadım

- Masaüstü ve iKoco'da **koordinat eşlemesi** (§3.4): #88'in kendi itirafı
  geçerli; iKoco için de ölçülmedi.
- Yapısal tür çağrısının Scala.js'de davranışı.
- Yayın yönü (Kojo'nun y-ters dönüşümüyle Java `Arc2D` saat yönüne ters; Canvas'ın
  `anticlockwise` bayrağı varsayılan saat yönü) ve `nokta`nın `lineCap`e bağlı
  görünümü. `dikdörtgen`/`elips`in y-ters dönüşümde hangi yöne uzandığı da.
- Her karede tam yüzeyli doku yüklemesinin (`texImage2D`) telefondaki maliyeti.
- Pişirme (`maybeBake`) ile etkileşim; yukarıdaki `__kojoNoBake` önerisi okumaya
  dayanıyor, çalıştırmadım.
- nbody örneklerinin iKoco'da çalışması (Dilim 0).
- `VertexShapeSupport`ın Canvas yoluyla paylaşılabilirliği: yalnız ilk 40 satırını
  okudum.

## 7. Karar bekleyenler

1. **Yapısal tür mü, düz trait mi?** Masaüstü uyumu (yapısal) ile Scala.js'de
   güvenli yol (trait) arasında. Önerim: önce Dilim 1'de yapısal türün çalışıp
   çalışmadığını ölç; çalışıyorsa masaüstüyle aynı kalsın, çalışmıyorsa trait.
2. **`karışımKipi`, `drawShape`.** Java2D türleri: dışarıda mı kalsın, yoksa
   Canvas karşılığıyla (yazı değeri) ayrı imza mı?
3. **Sıra:** Dilim 0'ı eskizden önce, tek başına mı birleştirelim? Önerim evet:
   küçük, kod değiştirmiyor ve #88'in iKoco'ya bugün ne yaptığını gösteriyor.
