import sbt._
import Keys._
import xerial.sbt.Sonatype.SonatypeKeys
import com.github.retronym.SbtOneJar

object SimpleAuthBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "fi.pyppe",
    version      := "1.0",
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.11.1", "2.10.4"),
    crossVersion := CrossVersion.binary,
    exportJars   := true,
    homepage     := Some(url("https://github.com/Pyppe/play-simple-auth")),
    startYear    := Some(2014),
    description  := "Simple authentication (Facebook, Github, Google, Linkedin, Twitter)"
  ) ++ Publish.settings

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

object Publish {
  lazy val settings = Seq(
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
      def nexusUrl(path: String) = s"https://oss.sonatype.org$path"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexusUrl("/content/repositories/snapshots"))
      else
        Some("releases" at nexusUrl("/service/local/staging/deploy/maven2"))
    },
    scmInfo := Some(ScmInfo(url("http://github.com/Pyppe/play-simple-auth"), "https://github.com/Pyppe/play-simple-auth.git")),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    licenses := Seq("The MIT License (MIT)" -> url("https://github.com/Pyppe/play-simple-auth/blob/master/LICENSE")),
    pomExtra := (
      <developers>
        <developer>
          <id>pyppe</id>
          <name>Pyry-Samuli Lahti</name>
          <url>http://www.pyppe.fi/</url>
        </developer>
      </developers>
    )
  ) ++ xerial.sbt.Sonatype.sonatypeSettings
}
