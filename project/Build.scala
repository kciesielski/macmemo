import sbt._
import Keys._

object BuildSettings {
  val ScalaVersion = "2.11.1"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "com.softwaremill.macmemo",
    version := "0.1",
    scalacOptions ++= Seq(),
    scalaVersion := ScalaVersion,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M1" cross CrossVersion.full)
  )
}

object MacMemoBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "root",
    file(".")
  ) aggregate macros

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % ScalaVersion,
        "com.google.guava" % "guava" % "13.0.1",
        "com.google.code.findbugs" % "jsr305" % "1.3.+",
        "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"))
  )

  // Enabling debug project-wide. Can't find a better way to pass options to scalac.
  System.setProperty("macmemo.debug", "true")
}
