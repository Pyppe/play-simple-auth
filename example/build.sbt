name := """play-simple-auth-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  ws
//  "fi.pyppe" %% "play-simple-auth" % "0.1-SNAPSHOT" changing()
)
