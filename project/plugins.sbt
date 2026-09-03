// Faz 1 (bkz. oneri-scala-2.13.md): sbt 0.13 + Scala.js 0.6 -> sbt 1 + Scala.js 1.x.
// 1.20.2 seçildi: scalajs-compiler'ı hem 2.12.20 hem 2.13.18 için yayınlayan hat
// (Faz 2'de Scala sürümü değişirken Scala.js sürümü sabit kalabilsin diye).
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.2")

// jsDependencies (ProvidedJS pixi/jsts) Scala.js 1.x'te ayrı eklentiye taşındı
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")

// Test jsEnv'i (tarayıcı testleri) için Selenium ortamı
libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.1.1"
