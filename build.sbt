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
// Uzun süre burada PIXI 4 duruyordu: PixiUyum.beşVeÜstü her testte false
// kalıyor, yani doku dolgusu yolu -- bütün gradyanlar -- hiç koşmuyordu.
// BoyaTest'teki "test harnessi PIXI 4'e geri kaymamış" savı bunu çiviliyor.
jsDependencies += ProvidedJS / "pixi.min.js" % "test"
jsDependencies += ProvidedJS / "jsts.min.js" % "test"

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
