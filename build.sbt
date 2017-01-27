import play.sbt.PlayScala
import sbtprotobuf.{ProtobufPlugin => PB}

PB.protobufSettings

sourceDirectory in PB.protobufConfig := (sourceDirectory in Compile) (_ / "core" / "proto").value
unmanagedResourceDirectories in Compile += (sourceDirectory in PB.protobufConfig).value
javaSource in PB.protobufConfig := (sourceDirectory in Compile) (_ / "core" / "generated").value
cleanFiles += (javaSource in PB.protobufConfig).value
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
libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value



fork in run := true



