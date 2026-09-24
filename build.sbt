import org.scalajs.jsenv.selenium.SeleniumJSEnv

enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)

name := "Kojo Dev"
// 2.13.18: masaüstü kojo ile aynı sürüm (Faz 2) -- yamalı scala-tr derleyicisi
// bu sürümden üretiliyor. Bkz. oneri-scala-2.13.md
scalaVersion := "2.13.18"

// This is an application with a main method
scalaJSUseMainModuleInitializer := true

libraryDependencies ++= Seq(
  // 1.2.0: Scala.js 1.x destekleyen, .raw paketini hâlâ taşıyan son hat
  // (2.x'te raw kalkıyor; o sıçrama Faz 2'ye bırakıldı)
  "org.scala-js" %%% "scalajs-dom" % "1.2.0",
  // 3.0.x Scala.js 1.x için yok; 3.2.x'te FunSuite -> AnyFunSuite oldu
  "org.scalatest" %%% "scalatest" % "3.2.19" % "test"
)

// DİKKAT: src/test/resources/pixi.min.js SİTENİN sunduğu sürümle aynı olmalı
// (kojojs-editor/server/src/main/assets/javascript/pixi.min.js -- şu an 5.3.12).
// CI bunu lib/pixi5.min.js ile karşılaştırarak denetliyor (bkz.
// .github/workflows/uretecler.yml); o dosya bugün siteninkiyle bayt bayt aynı.
// Editör yükseltilirse ÜÇÜ birden tazelenmeli -- CI depolar arasını göremiyor.
// Uzun süre burada PIXI 4 duruyordu: PixiUyum.beşVeÜstü her testte false
// kalıyor, yani doku dolgusu yolu -- bütün gradyanlar -- hiç koşmuyordu.
// BoyaTest'teki "test harnessi PIXI 4'e geri kaymamış" savı bunu çiviliyor.
jsDependencies += ProvidedJS / "pixi.min.js" % "test"
jsDependencies += ProvidedJS / "jsts.min.js" % "test"
// libtess: kendini kesen yolların NON_ZERO dolgusu (bkz. Ucgenleyici.scala,
// oneri-kesisen-dolgu.md). lib/libtess.cat.js ile BAYT BAYT AYNI olmalı --
// CI bunu denetliyor (.github/workflows/uretecler.yml). KÜÇÜLTÜLMÜŞ yapıyı
// kullanmayın: window'a tek harfli 70 küresel ad bırakıyor.
jsDependencies += ProvidedJS / "libtess.cat.js" % "test"

// Selenium tabanlı tarayıcı testleri (varsayılan) -- PIXI/DOM isteyen resim,
// çarpışma ve prelude testleri ancak burada koşuyor.
//
// KOŞMANIN YOLU: ./test-tarayici.sh
// Betik Chrome'u bulup sürümünü okuyor ve EŞLEŞEN chromedriver'ı indirip
// -Dwebdriver.chrome.driver ile veriyor. Elle `sbt test` ancak sistemde
// Chrome ile aynı ana sürümden bir chromedriver PATH'teyse çalışır;
// uyuşmazlıkta ChromeDriver oturum açmayı reddediyor.
//
// Saf mantık testlerini tarayıcısız koşmak için (hızlı, ama PIXI isteyen
// paketler düşer):
//   sbt 'set Test/jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()' \
//       'set jsDependencies := Seq()' 'testOnly *TurkishStdlib*'
val capabilities = {
  val o = new org.openqa.selenium.chrome.ChromeOptions()
  // Chrome'un yeri: test-tarayici.sh KOJO_CHROME ile veriyor. Verilmezse
  // Selenium PATH'teki Chrome'u arar (eski davranış).
  sys.env.get("KOJO_CHROME").filter(_.nonEmpty).foreach(o.setBinary)
  // Başsız koşu varsayılan: sunucuda/konteynerde ekran yok. Pencereli koşmak
  // (hata ayıklarken) için KOJO_CHROME_PENCERELI=1.
  if (sys.env.get("KOJO_CHROME_PENCERELI").forall(_.isEmpty)) {
    o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage")
  }
  o
}
Test / jsEnv := new SeleniumJSEnv(capabilities, SeleniumJSEnv.Config().withKeepAlive(false))

// Takımlar SIRALI (#149 "takımlar arası", #155 incelemesinde yakalandı): sbt
// varsayılanı takımları paralel koşturuyor ve Scala.js'te bu, async savların
// AYNI olay döngüsünde iç içe geçmesi demek. ÜçgenlemeUyarısı'nın saati ve
// sayacı küresel: bir takım sahte saati takarken (okuma başına +30 ms) öteki
// takımın pompadan geçen gülü Eşik altı evresinde libtess'e girip not
// düşürüyordu -- "30 ms sürdü (21 nokta)", üç tam koşuda iki kez, tek başına
// koşan takımlarda hiç. Sıralı koşu ortak durumu bir takıma ayırıyor;
// dünyaya bağlı rapor durumu (#149) gelene dek bu, yazı turayı kapatan
// tek satır. Bedeli duvar süresi (async beklemeler artık örtüşmüyor).
Test / parallelExecution := false
