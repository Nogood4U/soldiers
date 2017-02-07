import play.sbt.{PlayCommands, PlayScala}
import sbtprotobuf.{ProtobufPlugin => PB}

/*
PB.protobufSettings

sourceDirectory in PB.protobufConfig := (sourceDirectory in Compile) (_ / "core" / "proto").value
//unmanagedResourceDirectories in Compile += (sourceDirectory in PB.protobufConfig).value
//javaSource in PB.protobufConfig := (sourceDirectory in Compile) (_ / "core" / "generated").value
//cleanFiles += (javaSource in PB.protobufConfig).value

val cleanAllProtoCompiled = TaskKey[Unit]("cleanProto", "Deletes files produced by the build, such as generated sources, compiled classes, and task caches.")
cleanAllProtoCompiled := sbt.Defaults.doClean(Seq((javaSource in PB.protobufConfig).value), Seq())

compile in Compile <<= (compile in Compile) dependsOn cleanAllProtoCompiled


*/
val copyProto = Def.task {

  val protoDir = (sourceDirectory in Compile) (_ / "core" / "proto").value
  val assetDir = (baseDirectory in Compile) (_ / "public" / "proto").value
  IO.copyDirectory(protoDir, assetDir, true)

  val protoCompDir = (sourceManaged in Compile) (_ / "compiled_protobuf" / "game" / "core").value
  println(protoCompDir)
  val protoSrc = (sourceDirectory in Compile) (_ / "game" / "core").value
  IO.createDirectory(protoSrc)
  IO.copyDirectory(protoCompDir, protoSrc, true)
  IO.delete(protoCompDir)
  println(protoSrc)

  Seq.empty[File]
}

//sourceGenerators in Runtime += copyProto.taskValue
//sourceGenerators in Compile += copyProto.taskValue
name := """soldiers"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

// https://mvnrepository.com/artifact/org.jbox2d/jbox2d-library
libraryDependencies += "org.jbox2d" % "jbox2d-library" % "2.2.1.1"
libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.1.0"

fork in run := true