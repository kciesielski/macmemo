import sbt._
import Keys._

  val ScalaVersion = "2.12.4"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "com.softwaremill.macmemo",
    scalacOptions ++= Seq(),
    crossScalaVersions := Seq("2.11.8", "2.12.4"),
    scalaVersion := ScalaVersion,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

  lazy val root: Project = (project in file("."))
    .settings(publishArtifact := false, name := "macmemo")
    .settings(buildSettings)
    .aggregate(macros)

  lazy val macros: Project = (project in file("macros"))
    .settings(buildSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % ScalaVersion,
      "com.google.guava" % "guava" % "23.0",
      "com.google.code.findbugs" % "jsr305" % "3.0.2",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test"
      ),
    parallelExecution in Test := false,
    testOptions in Test += Tests.Setup( () => System.setProperty("macmemo.debug", "true"))
    ) ++ dependencyUpdatesSettings ++ ossPublishSettings
  )



