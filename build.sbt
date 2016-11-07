import sbt._
import Keys._

  val ScalaVersion = "2.12.0"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "com.softwaremill.macmemo",
    version := "0.3",
    scalacOptions ++= Seq(),
    crossScalaVersions := Seq("2.11.8", "2.12.0"),
    scalaVersion := ScalaVersion,
    // Sonatype OSS deployment
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials   += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <scm>
        <url>git@github.com:kciesielski/macmemo.git</url>
        <connection>scm:git:git@github.com:kciesielski/macmemo.git</connection>
      </scm>
        <developers>
          <developer>
            <id>kciesielski</id>
            <name>Krzysztof Ciesielski</name>
          </developer>
        </developers>,
    licenses      := ("Apache2", new java.net.URL("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil,
    homepage      := Some(new java.net.URL("http://www.softwaremill.com")),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )


  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings
  ) aggregate macros

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % ScalaVersion,
        "com.google.guava" % "guava" % "13.0.1",
        "com.google.code.findbugs" % "jsr305" % "1.3.9",
        "org.scalatest" %% "scalatest" % "3.0.0" % "test"
      ),
      parallelExecution in Test := false,
      scalacOptions := Seq("-feature", "-deprecation"),
      testOptions in Test += Tests.Setup( () => System.setProperty("macmemo.debug", "true"))
    )
  )



