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

jsDependencies += ProvidedJS / "pixi.min.js" % "test"
jsDependencies += ProvidedJS / "jsts.min.js" % "test"

// Selenium tabanlı tarayıcı testleri (varsayılan). Saf mantık testleri için:
//   sbt 'set Test/jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()' \
//       'set jsDependencies := Seq()' 'testOnly *TurkishStdlib*'
val capabilities = new org.openqa.selenium.chrome.ChromeOptions()
Test / jsEnv := new SeleniumJSEnv(capabilities, SeleniumJSEnv.Config().withKeepAlive(false))
