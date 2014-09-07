import sbt._
import Keys._

object BuildSettings {
  val ScalaVersion = "2.11.1"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "com.softwaremill.macmemo",
    version := "0.3-SNAPSHOT",
    scalacOptions ++= Seq(),
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
        "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"),
      parallelExecution in Test := false
    )
  )

  // Enabling debug project-wide. Can't find a better way to pass options to scalac.
  System.setProperty("macmemo.debug", "true")
}
