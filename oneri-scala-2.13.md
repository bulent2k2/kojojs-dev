# Öneri: KojoJS'i Kojo ile Aynı Scala Sürümüne (2.13.18) Taşımak

**Amaç:** Masaüstü Koco'nun Türkçe anahtar kelime yaması (`dez`, `tanım`, `eğer`,
`doğru`, `özellik`, …) ikojo-tr'de de çalışsın — çocuklar tarayıcıda da tam
Türkçe Scala yazabilsin.

**Durum:** Öneri — henüz uygulanmadı. Tarih: 2026-09-02.

---

## 1. Anahtar tespit: yama nerede yaşıyor, nereye gitmeli

`kojo/scala-tr/turkish-keywords.patch` 126 satır ve yalnızca iki dosyaya
dokunuyor:

| Dosya | Jar | İşi |
|---|---|---|
| `Scanners.scala` | **scala-compiler.jar** | Türkçe sözcükleri aynı token'lara eşler (`nme.trVALkw -> VAL`, …) |
| `StdNames.scala` | **scala-reflect.jar** | Türkçe sözcük adlarını (`trVALkw`, …) tanımlar |

`scala-library` **yamasız**. Bu şu demek:

> **Türkçe anahtar kelimeler tamamen derleme zamanı işi. Tarayıcıya giden
> çalışma zamanı (page, scalajs-library, link edilmiş fiddle JS'i) hiç
> etkilenmiyor.**

KojoJS mimarisinde fiddle'ları derleyen tek yer **kojojs-core
`compilerServer`**: kendi JVM classpath'indeki `scala-compiler` +
`scala-reflect` ile, sınıf olarak yüklediği Scala.js derleyici eklentisiyle
(`GlobalInitCompat.initGlobal`), bellek içi `nsc.Global` kurup kullanıcı kodunu
derliyor; sonra Scala.js linker JS üretiyor. Dolayısıyla yamalı jar'ların
gitmesi gereken TEK yer compilerServer'ın JVM classpath'i:

```
koco-deploy/stage/compiler/lib/org.scala-lang.scala-compiler-<sürüm>.jar   <- yamalı
koco-deploy/stage/compiler/lib/org.scala-lang.scala-reflect-<sürüm>.jar    <- yamalı
```

(Yamalı compiler, yamalı reflect'teki `trXXXkw` adlarına başvurduğu için ikisi
BİRLİKTE takas edilmeli, yoksa `NoSuchMethodError`. `LibraryManager.baseLibs`
içindeki — fiddle classpath'ine giren — `scala-reflect` kaynağına dokunmak
GEREKMEZ: o yalnızca kullanıcı kodunun tip denetiminde kütüphane olarak
kullanılıyor, anahtar kelimeler sunucu classpath'indeki çiftten geliyor.)

Editör (Play) ve tarayıcıdaki istemci fiddle derlemiyor — onlara hiçbir şey
gerekmiyor.

## 2. Neden sürüm eşitlemesi şart

Yamalı jar'lar `scala/scala v2.13.18` üzerine kurulu. compilerServer bugün
**2.12.10**. Derleyici jar'ı öylece takas edilemez çünkü:

- fiddle classpath'indeki her şey (scala-library, scalajs-library, **page**
  jar'ı) 2.12 binary'si — 2.13 derleyicisi bunları okuyamaz;
- Scala.js derleyici eklentisi tam sürüme çakılı (`scalajs-compiler_2.12.10`);
- ve asıl kilit: **Scala.js 0.6.x hattı 2.13.18 için hiç yayınlanmadı**
  (0.6.33, 2.13.2'de durdu). 2.13.18 = zorunlu **Scala.js 1.x**, o da zorunlu
  **sbt 1.x**.

Yani "aynı sürüme geç" işi aslında üç bacaklı bir modernizasyon:
**Scala 2.12.10 → 2.13.18, Scala.js 0.6.31 → 1.x, sbt 0.13.18 → 1.x.**

### Yan kazançlar (bedavaya gelenler)

- `kojo/tr/` katmanındaki bütün 2.12 şimleri düşer, dosyalar masaüstü
  `trInit.scala`/`tr/` ile hizalanır: `Diz`/`Dizi` yeniden ayrı türler
  (`ambiguous implicit` çözümü kalkar), `Stream` yerine gerçek `LazyList`
  (`MiskinDizin`), elle yazılmış `distinctBy`/`toIntOption` ailesi yerine
  gerçekleri, `X.from(...)` fabrikaları.
- Ölü `http://repo.typesafe.com` çözümleyicisi sorunu biter (sbt 1 + güncel
  eklentiler HTTPS Maven Central kullanır) → Docker içinde ve **CI'da**
  kaynaktan derleme mümkün olur. Bugün `koco-deploy/build.sh`'ın "yerelde
  paketle, imaja kopyala" mecburiyeti bundan.
- EOL bir derleyici hattından (2.12, sjs 0.6 — 2020'den beri bakımsız)
  çıkılmış olur.

## 3. Bugünkü durum ve hedef

| Parça | Bugün | Hedef | Not |
|---|---|---|---|
| kojojs-dev | 2.12.10 / sjs 0.6.31 / sbt 0.13.18 | 2.13.18 / sjs 1.x / sbt 1.x | çalışma zamanının kaynağı (page'e senkron) |
| kojojs-core page, client, shared | 2.12.10 / sjs 0.6.31 | 2.13.18 / sjs 1.x | page = fiddle classpath'inin parçası |
| kojojs-core compilerServer | 2.12.10 + scalajs-tools 0.6 | 2.13.18 + scalajs-linker 1.x | en büyük kod işi (bkz. Faz 3) |
| kojojs-core router | 2.12.10 / akka-http 10.1.1 | 2.13.18 / akka-http ≥10.1.15 | tel protokolü riskine dikkat |
| kojojs-editor (Play 2.6) | 2.12.10 / sbt 0.13 | **DEĞİŞMİYOR** | fiddle derlemiyor; ayrı iş olarak ertelenebilir |
| koco-deploy | stok jar'lar | + yamalı compiler/reflect takası | tek gerçek "tr" farkı burada |
| Yamalı toolchain | kojo/scala-tr (2.13.18) | compilerServer lib/ içine | scala-library stok kalır |

## 4. Aşamalı plan

Sıralama, her fazın kendi başına test edilebilir olmasına göre. Faz 1–2
kojojs-dev'de dalda kalır; `sync-kojojs-core.sh` ile page'e akıtma ancak Faz
3'le birlikte olur (core'un build'i eski kaldıkça senkron edilemez —
kilitli adım).

**Faz 0 — Doğrulama (yarım gün).** Üç şeyi kanıtla, sonra plana güven:
1. Seçilen Scala.js 1.x sürümü `scalajs-compiler_2.13.18` yayınlamış mı?
   (2.13.18'i destekleyen ilk sürümü seç; 1.19+ olması beklenir — Maven
   Central'dan teyit et.)
2. **PoC:** tek dosyalık bir sbt 1 + Scala.js 1.x projesinde
   `scalaHome := Some(file("kojo/scala-tr/build/pack"))` (sbt 1'de de var)
   ile `eğer (doğru) println("çalıştı")` derlenip Node'da koşuyor mu? Bu,
   "yamalı derleyici + scalajs-compiler eklentisi uyumlu" varsayımını beş
   dakikada kanıtlar/çürütür. (Uyumlu olmalı: eklenti derleyiciye plugin API
   üzerinden bağlanıyor ve yama eklentinin dokunduğu hiçbir yeri
   değiştirmiyor.)
3. `bulent2k2/scala-2` fork'unda 2.12 dönemi Türkçe yama dalı var mı? (Bölüm
   6'daki hızlı alternatif için.)

**Faz 1 — kojojs-dev: sbt 1 + Scala.js 1.x, henüz 2.12'de (2–4 gün).**
Scala.js göçünü Scala göçünden ayırmak için iki sıçrama. 2.12.10 → 2.12.20
(sjs 1.x için gerekli güncel 2.12), sbt 1.x, `sbt-scalajs` 1.x,
`sbt-jsdependencies` 1.0.2 (`ProvidedJS` pixi/jsts aynen çalışır),
`scalajs-env-selenium` 1.1.1, `scalajs-dom` 0.9.8 → 1.2.0, scalatest →
3.2.x (sjs 1.x için 3.0.x yok; `FunSuite` → `AnyFunSuite` import değişikliği).
Çıkış ölçütü: `TurkishStdlibTest` Node'da, tam süit Selenium'da yeşil;
`site/kojo.js` yeniden üretilip demo elle doğrulanmış.

**Faz 2 — kojojs-dev: 2.13.18 (2–3 gün).** Koleksiyon göçü (çekirdek KojoJS
kodu küçük ve düz — `JSConverters`/`mutable` kullanımları elden geçer) +
`tr/` katmanının masaüstüyle hizalanması (şimlerin sökümü — bu iş külfet
değil, bu geçişin ödülü). `scalajs-dom` 2.x'e bumplanabilir (isteğe bağlı).
Çıkış ölçütü: aynı test süiti + `tr/` dosyalarının masaüstü karşılıklarıyla
diff'i "yalnızca platform farkları" düzeyine inmiş.

**Faz 3 — kojojs-core (3–6 gün).** En riskli faz; alt kalemler:
- `page`/`client`/`shared`: Faz 1–2 ile aynı mekanik; sonra kojojs-dev'den
  senkron yeniden akar.
- `compilerServer/Compiler.scala` + `GlobalInitCompat`: `org.scalajs.core.tools`
  (0.6) → `org.scalajs.linker` 1.x API yeniden yazımı (`StandardImpl`,
  `IRFileCache`, `MemIRFile`); eklenti sınıfı `org.scalajs.nscplugin.ScalaJSPlugin`;
  **macro-paradise silinir** (2.13'te `-Ymacro-annotations`), kind-projector ya
  güncellenir ya silinir (Kojo fiddle'ları kullanmıyor — silinmesi önerilir).
- `LibraryManager`: coursier 1.0.3 API'si 2.13'te yok → coursier 2.x'e küçük
  yeniden yazım. `baseLibs` adları zaten `version.properties`'ten türüyor;
  sjs 1.x'te scala stdlib IR'inin geldiği artefakt değişti
  (`scalajs-library` + sürüme bağlı scalalib) — adlar buna göre güncellenir.
- `router`: akka 2.5.32 / akka-http 10.1.15'e bump (2.13 destekli en yakın
  akraba sürümler).
- upickle 0.4.4 2.13 için yok → ≥1.x'e bump. **Dikkat:** editör Faz'larda
  2.12/eski upickle'da kalıyor; editör↔router/client arasındaki JSON tel
  biçimi değişmemeli. Paylaşılan mesaj tipleri için tel biçimini sabitleyen
  bir test yaz (örnek JSON'ları fixture olarak).
Çıkış ölçütü: yerelde üç servis ayakta, İngilizce ve Türkçe (stok derleyicili)
fiddle'lar derlenip çalışıyor.

**Faz 4 — koco-deploy: yamalı toolchain takası (1 gün).** `build.sh`'a bir
adım: stage sonrası
`stage/compiler/lib/org.scala-lang.scala-{compiler,reflect}-2.13.18.jar`
dosyalarını `kojo/scala-tr/build/pack/lib/` kopyalarıyla değiştir (dosya adı
birebir korunarak). Masaüstündeki `scala-en`/`scala-tr` felsefesinin deploy
karşılığı: **repolar stok derleyiciyle derlenebilir kalır, "tr" farkı yalnızca
imaja girer.** İsterse `KOCO_TOOLCHAIN=tr|en` gibi bir build.sh anahtarıyla iki
imaj da üretilebilir (ikojo.fly.dev = en, ikojo-tr = tr gibi).

**Faz 5 — Doğrulama + anahtar kelime içerikleri (1–2 gün).**
- `ornekler/ornekleri-dogrula.sh` canlıya karşı 8/8;
- yeni duman örneği: `09-anahtar-kelimeler.kojo` (`tanım`, `eğer`/`yoksa`,
  `dez`/`den`, `doğru`/`yanlış`, `yinele` birlikte);
- `temel.scala`'daki `val doğru/yanlış` KALIR (stok derleyiciyle de derlensin
  diye) — yamalı derleyicide kullanıcı kodundaki `doğru` keyword olarak lex
  edilir, val'ler zararsız gölgede kalır; masaüstüyle aynı kombinasyon.
- Sözdizimi vurgulaması: bkz. Bölüm 5 — yamalı derleyiciyle AYNI deploy'da
  çıkmalı (derleyicinin kabul etmediği sözcüğü anahtar kelime renginde
  göstermek, ya da tersi, çocuğu yanıltır).

## 5. Vurgulama, biçimlendirme, tamamlama

Yamalı derleyici işin yalnızca "kabul etme" yarısı; editör deneyiminin üç
parçası ayrıca ele alınmalı. Envanter (koda bakılarak çıkarıldı):

| Parça | ikojo'da bugün | Türkçe anahtar kelimelerle |
|---|---|---|
| Vurgulama | Ace, STOK `mode-scala.js` — iki ayrı yükleme yeri | Türkçe sözcükler düz tanımlayıcı renginde kalır → yapılacak iş var |
| Biçimlendirme | **YOK** (düğmeler: Run/Reset; scalariform/scalafmt izi yok) | bozulacak bir şey yok; istenirse ayrı iş |
| Tamamlama | compilerServer'daki presentation compiler (`initInteractiveGlobal`) | yamalı derleyiciyle kendiliğinden çalışır |

### 5a. Vurgulama (Faz 4-5 ile birlikte, ~½–1 gün)

Masaüstünde vurgulama yamalı **scalariform** + `ScalariformTokenMaker`
(RSyntaxTextArea) ile olur — o mekanizmanın ikojo'da karşılığı YOK ve
gerekmiyor: ikojo'da vurgulama tamamen tarayıcıda, Ace'in düzenli-ifade
tabanlı `mode-scala.js`'iyle. Stok modda Türkçe anahtar kelimeler düz
tanımlayıcı görünür.

Ace modu iki ayrı yerden yükleniyor; ikisi de değişmeli:

1. **Editör arayüzü** — `kojojs-editor/server/src/main/twirl/views/index.scala.html`
   (ve `listfiddles.scala.html`): Ace 1.2.4'ü **cdnjs CDN'inden** alıyor,
   `mode-scala.js` dahil.
2. **Gömülü/codeframe görünümleri** — `kojojs-core/router/.../Static.scala`
   `extJSFiles`: Ace 1.2.2 **webjar'ından** `mode-scala.js` (+
   `ext-static_highlight.js` statik vurgulama da aynı modu kullanır).

**Plan:** Tek bir `mode-scala-tr.js` dosyası: Ace'in `mode-scala.js`
kopyasında anahtar kelime listesine `turkish-keywords.patch`'teki ~40 sözcük
eklenir; modül adı `ace/mode/scala` olarak KORUNUR ki `setMode` çağrılan iki
istemcide (kojojs-editor `FiddleEditor.scala`, kojojs-core `Editor.scala`)
tek satır bile değişmesin. Servis tarafında: router'da `extJSFiles`'ın webjar
yolu yerine `/web/mode-scala-tr.js`; editörde cdnjs `mode-scala.js` script
etiketi yerine yerel kopya. Sözcük listesi TEK kaynaktan türetilmeli
(`turkish-keywords.patch` — dosya başına çapraz referans yorumu).

İki not: (a) Bu iş mekanik olarak Scala göçünden bağımsız (saf JS) ama yamalı
derleyiciyle aynı deploy'da çıkmalı — önce çıkarsa derleyicinin reddettiği
sözcükler anahtar kelime renginde görünür. (b) Aynı dosyaya Türkçe API
sözcüklerini (`yinele`, `ileri`, `sağ`, …) `support.function` olarak eklemek
bedava bir iyileştirme — isteğe bağlı.

### 5b. Biçimlendirme (kapsam dışı — bilinçli)

ikojo'da bugün kod biçimlendirme özelliği YOK; dolayısıyla geçişin bozacağı
bir şey de yok ve masaüstünün yamalı `scalariform.jar`'ının buraya taşınması
GEREKMİYOR (o, masaüstü editörünün hem biçimlendiricisi hem vurgulayıcısı).

İleride bir "Biçimle" düğmesi istenirse (ayrı iş, ~1–2 gün): doğru yol,
compilerServer'a küçük bir uç ekleyip **kojo'nun yamalı scalariform'unu**
sunucu tarafında çalıştırmak — scalariform kendi lexer'ını taşıdığı için tr
fork'u Türkçe anahtar kelimeleri zaten tanıyor. scalafmt bir seçenek DEĞİL:
scalameta'nın parser'ı Türkçe sözcükleri bilmez, o da ayrıca fork gerektirir.

### 5c. Tamamlama (kendiliğinden)

Tamamlamalar compilerServer'daki presentation compiler'dan geliyor; yamalı
derleyiciyle aynen çalışır, ek iş yok. Masaüstündeki Türkçe anahtar kelime
şablonları (`CodeCompletionUtils`/`CodeTemplates`) gibi tamamlama listesine
keyword eklemek isteğe bağlı bir gelecek parite kalemi.

## 6. Riskler

| Risk | Etki | Önlem |
|---|---|---|
| Linker 1.x API yeniden yazımında bilinmeyenler | Faz 3 uzar | API yüzeyi ~200 satır; scalafiddle'ın sjs-1 dalları / scastie kaynak koduna bakarak yaz; Faz 0 PoC'siyle plugin tarafı önden kanıtlı |
| upickle/autowire sıçramasının editörle tel uyumsuzluğu | sinsi çalışma zamanı hatası | tel biçimi fixture testi (Faz 3); editörü değiştirmeden entegrasyon testi |
| Yamalı derleyici + scalajs-compiler uyumsuzluğu | planın temeli çöker | Faz 0 PoC ilk iş — yarım saatlik test |
| fly.dev küçük instance'ında yeni linker bellek/ısınma profili | derleme yavaşlar | `compilerCacheSize` ayarı; gerekirse Closure kapalı fastLink servis et |
| 2.13'ün Türkçe kimlikli (ı/İ) kaynaklarda farklı uyarıları | gürültü | `-Xlint` ayarını fazlar sırasında gevşet, sonda geri sık |
| kojojs-dev↔core kilitli adımı | yarım kalmış senkron | Faz 1–2 dalda kalır; core hazır olmadan master'a inmez |

## 7. Hızlı alternatif: yamayı 2.12.10'a backport etmek

Yama küçük ve 2.12'nin Scanners/StdNames'i yapısal olarak çok yakın; masaüstü
Koco 2.12 çağında da Türkçe çalışıyordu, dolayısıyla `bulent2k2/scala-2`
fork'unda hazır bir 2.12 yaması da bulunabilir. `scala/scala v2.12.10`'a
uygulanıp jar'lar üretilir, Faz 4'teki takas bugünkü stack'e aynen yapılır —
**başka hiçbir şey değişmeden** ikojo-tr anahtar kelimeleri kazanır (~1–2 gün).
(Bölüm 5a'daki vurgulama işi bu yolda da aynen gerekir ve aynen uygulanabilir.)

Artısı: hız. Eksisi: EOL stack'e yatırım; `tr/` şimleri ve masaüstüyle sürüm
kopukluğu kalıcılaşır; asıl geçiş yine yapılacaksa iş iki kere yapılır (yamalı
2.12.10 derleyicisini üretme + doğrulama emeği çöpe gider).

**Öneri:** Anahtar kelimeler eğitim takvimi yüzünden ACİLSE önce backport
(hemen kazanım), arkasından bu plan. Acele yoksa doğrudan bu plan — toplam iş
daha az, varış noktası aynı: kojo ile aynı derleyici, aynı yamayla, tek
bakım hattı.

## 8. Kaba toplam efor

Faz 0–5 toplamı: **~2–3 odaklanmış hafta** (tek kişi). En büyük iki kalem
compilerServer yeniden yazımı ve 2.13 koleksiyon/`tr/` hizalaması; ikisi de
geri dönüşü olan, dalda ilerleyen işler.

## 9. İlk somut adım

Faz 0 PoC'si: sbt 1 + Scala.js 1.x + `scalaHome → kojo/scala-tr/build/pack`
ile `eğer (doğru) println("çalıştı")` — yarım saatte planın en kritik
varsayımını test eder. Yeşilse gerisi mühendislik.
