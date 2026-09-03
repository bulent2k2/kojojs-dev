// Faz 0 PoC: yamalı Türkçe derleyici (kojo/scala-tr) + sbt 1 + Scala.js 1.x
// birlikte çalışıyor mu? Bkz. ../../oneri-scala-2.13.md Bölüm 4, Faz 0.
//
// KOJO_SCALA_TR ile yamalı pack dizini gösterilir; verilmezse kojo klonunun
// bu deponun yanında olduğu varsayılır (kojo/scala-tr/build/pack).
enablePlugins(ScalaJSPlugin)

name := "faz0-yamali-derleyici"
scalaVersion := "2.13.18"

scalaHome := Some(
  file(sys.env.getOrElse("KOJO_SCALA_TR", "../../../kojo/scala-tr/build/pack")).getAbsoluteFile
)

scalaJSUseMainModuleInitializer := true
