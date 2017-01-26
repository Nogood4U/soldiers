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


fork in run := true