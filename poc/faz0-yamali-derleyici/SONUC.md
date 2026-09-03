# Faz 0 PoC sonucu: BAŞARILI ✅

Tarih: 2026-09-03. Ortam: OpenJDK 21, Node v22, sbt 1.10.11, Scala.js 1.20.2,
yamalı derleyici `kojo/scala-tr/build/pack` (2.13.18 + turkish-keywords.patch).

```
$ sbt run
[info] compiling 1 Scala source ...
[info] Fast optimizing ... faz0-yamali-derleyici-fastopt
[info] Running Dene.
merhaba, toplam=10 (on), yanlış=false
[success]
```

Kanıtlananlar (öneri Bölüm 4, Faz 0):

1. **Yamalı derleyici + `scalajs-compiler_2.13.18` (1.20.2) uyumlu** — eklenti
   yamalı `nsc.Global`'e sorunsuz takılıyor, Türkçe anahtar kelimeli kaynak
   (.sjsir üzerinden) JS'e derlenip Node'da koşuyor.
2. **sbt 1 + `scalaHome` yamalı pack ile çalışıyor** — zinc, 2.13.18 için
   önceden derlenmiş compiler-bridge kullandığından bridge'in yamalı
   derleyiciyle derlenmesi diye bir sorun da yok.
3. Maven Central'da `scalajs-compiler_2.13.18` şu Scala.js sürümlerinde var:
   1.20.1, 1.20.2, 1.21.0, 1.22.0.

Yol boyu öğrenilen iki ayrıntı:

- **Interpolasyonda `$anahtarkelime` olmaz:** `s"... $yanlış"` derlenmez —
  `$` sonrası tanımlayıcı beklenir, yamalı derleyici `yanlış`ı keyword (false)
  olarak lex eder. `${yanlış}` gerekir. Aynısı masaüstünde de geçerli olmalı;
  Türkçe eğitim malzemesinde bilinen bir kural olarak not edilmeye değer.
- **UTF-8 yereli şart:** POSIX (C) yerelli bir ortamda derleme
  `InvalidPathException: ... Dene$T?rk?eSelamc?.class` ile ölüyor — Türkçe
  adlı sınıfların .class dosya adları yazılamıyor. Çözüm: `LANG=C.UTF-8`
  (ve/veya `-Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8`). compilerServer
  konteynerine de bu ayar konmalı (fiddle'daki Türkçe adlı sınıflar bellek içi
  dosyalara yazılıyor ama aynı tuzak classpath/dosya yolu işlemlerinde her an
  çıkabilir).

## Yeniden üretmek

```sh
cd poc/faz0-yamali-derleyici
# kojo klonu ../../../kojo'da değilse: export KOJO_SCALA_TR=/yol/kojo/scala-tr/build/pack
LANG=C.UTF-8 sbt run
```
