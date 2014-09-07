import sbt._
import Keys._
import com.github.retronym.SbtOneJar

object SimpleAuthBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "fi.pyppe",
    version      := "0.1-SNAPSHOT",
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.11.1", "2.10.4"),
    crossVersion := CrossVersion.binary,
    exportJars   := true,
    homepage     := Some(url("https://github.com/Pyppe/play-simple-auth")),
    startYear    := Some(2014),
    description  := "Simple authentication (Facebook, Google, Twitter)"
  )

  val PlayVersion = "2.3.4"

  lazy val dependencies = Seq(
    "com.typesafe.play"          %% "play"         % PlayVersion % "provided",
    "com.typesafe.play"          %% "play-json"    % PlayVersion % "provided",
    "com.typesafe.play"          %% "play-ws"      % PlayVersion % "provided",

    // Testing:
    "org.specs2"                 %% "specs2"                % "2.3.12" % "test"
  )

  lazy val webResolvers = Seq(
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  lazy val root = Project(
    id = "play-simple-auth",
    base = file("."),
    settings = buildSettings ++
      Seq(libraryDependencies ++= dependencies, resolvers ++= webResolvers) ++
      SbtOneJar.oneJarSettings
  )

}

