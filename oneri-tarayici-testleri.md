# Öneri: Tarayıcı Testlerini Çalışır Duruma Getirmek

**Amaç:** Çizim davranışını test edebilmek. Bugün testlerin yarısı hiç
koşmuyor ve `davran`ın her karede çalışması gibi hatalar ancak insan gözüyle
yakalanıyor.

**Durum (2026-09-07): Faz 1 uygulandı** (bu PR). Faz 2-4 öneri.

---

## 1. Anahtar tespit: altyapı zaten var, eksik olan tek şey sürücüydü

`build.sbt` **zaten** Selenium'u varsayılan test ortamı yapıyor ve
`jsDependencies` testlere PIXI ile JSTS'i veriyor:

```scala
Test / jsEnv := new SeleniumJSEnv(capabilities, ...)
jsDependencies += ProvidedJS / "pixi.min.js" % "test"
```

Yani tasarım baştan tarayıcı testi için kurulmuş. Koşmamasının sebebi tek bir
şeydi: **Chrome ile chromedriver'ın ana sürümleri tutmuyordu.**

```
session not created: This version of ChromeDriver only supports Chrome version 147
Current browser version is 141.0.7390.37
```

Bu yüzden herkes (ben dahil) `sbt test`i şu kaçış yoluyla koşuyordu:

```
sbt 'set Test/jsEnv := new NodeJSEnv()' 'set jsDependencies := Seq()' test
```

...ve bu, PIXI isteyen her şeyi sessizce devre dışı bırakıyordu.

## 2. Ölçüm: neyi kazanıyoruz

Eşleşen bir chromedriver indirildikten sonra, aynı depoda:

| | Node kaçış yolu | Tarayıcı (Selenium) |
|---|---|---|
| paket | 5 | **8** |
| geçen test | 39 | **65** |
| düşen test | 3 | **0** |
| süre | 9 sn | **12 sn** |

Düşen 3 test `ReferenceError: PIXI is not defined` veriyordu; bunlar hatalı
değil, ortamları yanlıştı.

**Şu an hiç koşmayan 26 test**, tam da elle doğrulamak zorunda kaldığımız
sınıf: `PictureCollisionTest` (kutu-kutu, kutu-üçgen, resim dizisi
çarpışmaları), `PictureBounceTest` (sekme ve köşe durumları), `PictureTest`,
`SampleTest` (bir Türkçe betiğin editör prelude'uyla derlenip koşması) ve
`TurkishPreludeTest`.

Süre farkı 3 saniye. Yani Node'u varsayılan tutmanın bir gerekçesi yok.

## 3. Faz 1 — koşulabilir hale getirmek (bu PR)

**`test-tarayici.sh`**: Chrome'u bulur (Playwright'ınki, macOS'taki
`/Applications/...`, `KOJO_CHROME` ya da PATH), sürümünü okur, **eşleşen**
chromedriver'ı indirip `.chromedriver/` altında önbelleğe alır ve sbt'yi doğru
`-Dwebdriver.chrome.driver` ile koşar. İndirme yalnız ilk seferde olur.

Linux ve macOS (arm64/x64) destekleniyor: indirme adresi ve zip içindeki dizin
adı `uname` ile seçiliyor. Bash 3.2 uyumlu (macOS'un /bin/bash'i), kardeş
`ornekleri-dogrula.sh` gibi. Tam sürüm bir "Chrome for Testing" yayını değilse
aynı ana sürümün bilinen son yayını deneniyor; o da yoksa betik ne yapılacağını
söyleyip duruyor. Kardeş `kojo` klonu yoksa PATH'teki `sbt` kullanılıyor.

```
./test-tarayici.sh                      # hepsi
./test-tarayici.sh 'testOnly *Boya*'
```

**`build.sbt`**: `capabilities` artık `KOJO_CHROME` ile ikiliyi alıyor ve
varsayılan olarak başsız koşuyor (`--headless=new --no-sandbox
--disable-dev-shm-usage`). Hata ayıklarken pencereli koşmak için
`KOJO_CHROME_PENCERELI=1`. `KOJO_CHROME` verilmezse ikilinin bulunuşu eskisi
gibi (Selenium PATH'e bakar); başsızlık ise artık varsayılan -- ekransız
ortamlarda çalışması için.

Node kaçış yolu **kaldırılmadı**; hızlı olduğu için saf mantık testlerinde
işe yarıyor ve `build.sbt` yorumunda belgeli duruyor.

## 4. Faz 2 — anlam testleri (kojojs-dev #34'ün açık kalan notu)

`davran`ın her karede koşması derleme ölçümünde görünmüyordu (betikler zaten
"geçti" idi) ve hiçbir test yakalamadı. Tarayıcı ortamı bu sınıfı test
edilebilir yapıyor, çünkü artık gerçek bir `Builtins`/`Turtle` kurulabiliyor:

```scala
test("davran gövdesi bir kez, tepkiVer gövdesi her karede koşar") {
  var a = 0; var b = 0
  k1.davran { _ => a += 1 }
  k2.tepkiVer { _ => b += 1 }
  // N kare sürüldükten sonra
  a shouldBe 1
  b should be > 10
}
```

Aynı yolla test edilebilecek öteki davranışlar: kalem izinin gerçekten
çizilmesi (PIXI 5 göçünde elle yakalanmıştı), dönüştürücü zincirinin boyayı
iletmesi (#33 incelemesinde elle yakalanmıştı), süs katmanının
`resimleriSil()` sonrası geri gelmesi (#28 incelemesinde elle yakalanmıştı).
Üçü de insan gözüyle bulundu; üçü de bu ortamda test edilebilirdi.

## 5. Faz 3 — testler üretimdeki PIXI ile koşsun

> **Güncelleme (#52):** bu faz yapıldı. Harness artık PIXI 5.3.12 yüklüyor --
> sitenin sunduğu dosyanın ta kendisi -- ve `BoyaTest` "PIXI 4'e geri kaymamış"
> savıyla bunu çiviliyor; #54 ise CI'da `cmp` ile dosya eşitliğini denetliyor.
> Aşağıdaki gerekçe olduğu gibi bırakıldı, çünkü **ikisini de koşma** fikri
> (v4 ve v5) hâlâ yapılmadı ve hâlâ değerli.

`jsDependencies` testlere uzun süre `lib/pixi.min.js`, yani **PIXI 4** verdi.
Üretim ise PIXI 5'e geçmişti (kojojs-editor #22). Yani tarayıcı testleri
üretimde olmayan bir sürümü sınıyordu.

`PixiUyum` bilerek iki sürümü birden desteklediği için doğrusu **ikisini de**
koşmak: aynı takımı bir kez v4, bir kez v5 dokularıyla. Bu, ikili sürüm
desteğinin gerçekten çalıştığının kalıcı kanıtı olur.

Maliyeti ölçüldü (#37 incelemesi): `lib/pixi5.min.js`'i test kaynaklarına koyup
`jsDependencies`'teki adı değiştirmek yetiyor -- **64/65**, düşen tek test
`BoyaTest`'in tasarımı gereği `beşVeÜstü shouldBe false` diyen satırı.
Çarpışma, sekme, resim ve prelude testlerinin hepsi PIXI 5'te de geçiyor. Yani
bir test ayrımı (v4'te düşme / v5'te doku) uzaklıkta -- ve `Boya`/`PixiUyum`'un
v5 dallarını (`dokuYap`, sarma kipi çivisi, 404 yedeği) test altına alacak olan
da bu: bugün o satırların hiçbiri test altında koşmuyor.

Küçük bir yan etki: `BoyaTest`'teki "PIXI yokken" adı yanıltıcı hale geldi --
tarayıcı koşusunda PIXI 4 yükleniyor ve `beşVeÜstü` yine `false` oluyor, ama
başka sebeple. Bu PR'da adı düzeltildi.

## 6. Faz 4 — CI (karar gerektiriyor)

> **Güncelleme (#51):** kojojs-dev'de CI'ın ilk hâli `uretecler.yml` oldu, ama
> yalnız SAF PYTHON: üreteçlerin çıktısı ağaçtakiyle aynı mı, gömülü sözlük
> bloğu kaynağıyla aynı mı, harnessteki PIXI kütüphanedekiyle aynı mı.
> Saniyeler sürüyor.
>
> **Güncelleme (#55): tarayıcı takımı artık CI'da** -- `sinamalar.yml`, ayrı
> iş akışı olarak. Ölçüldü: soğuk koşu 87 sn, 101/101. Aşağıdaki "açık karar"
> bu yüzden kapandı; paragraf niçin öyle karar verildiğinin kaydı olarak
> duruyor.

Tarayıcı takımını GitHub Actions'ta koşmak mümkün: koşucularda Chrome kurulu
geliyor ve `test-tarayici.sh` eşleşen sürücüyü kendisi indiriyor. Takım
dakikalar sürüyor ve kırılgan -- bu oturumda bir kez `JS error: Script error.`
ile düştü, yeniden koşumda geçti; kaynağı hâlâ anlaşılmadı.

Kararı değiştiren şey şu oldu: takımı koşan bir CI olmadığı için
`boyamaRenginiKur`un varsayılan hızda hiçbir şeyi doldurmadığı kusur master'da
fark edilmeden durdu (#55). Kırılganlık riski gerçek, ama HİÇ koşmamanın
bedeli daha büyük çıktı. `sinamalar.yml` bu yüzden otomatik yeniden deneme
KOYMUYOR: yeniden deneme gerçek kusuru da yutar. Kırılganlık orada bir yorumla
kayda geçti; kaynağı bulununca hem oraya hem buraya yazılmalı.

## 7. Riskler ve sınırlar

- **Sürücü indirmesi ağa bağlı.** `storage.googleapis.com` gerekiyor.
  Chrome'un sürümü bir "Chrome for Testing" yayını değilse indirme başarısız
  olur; betik bu durumda açık bir hata verip elle koyma yolunu söylüyor.
  (`googlechromelabs.github.io` üzerindeki sürüm dizini bu ortamda kapalı,
  onun için URL doğrudan sürümden kuruluyor.)
- **Selenium 3.141.59 (2018)** en eski hareketli parça; `scalajs-env-selenium`
  1.1.1 onu getiriyor. Modern chromedriver ile çalışıyor (ölçüldü), ama bir
  gün sorun çıkarsa yükseltmek ayrı bir iş.
- **WebGL** başsız Chrome'da SwiftShader ile koşuyor; PIXI testleri geçiyor.
- Sürücü önbelleği `.gitignore`'da.
