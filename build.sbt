lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  scalaOrganization := "org.typelevel",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.12.0",
  crossScalaVersions := Seq("2.11.8", scalaVersion.value),
  libraryDependencies ++= macroParadise(scalaVersion.value) ++ Seq(
    "org.scalacheck" %% "scalacheck"  % "1.13.4" % "test",
    "org.specs2"     %% "specs2-core" % "3.8.6"  % "test"
  )
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

lazy val commonSettings = Seq(
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", // 2 args
      "-feature",
      "-deprecation",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Yliteral-types"
      // "-Ywarn-value-discard"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
    publishArtifact in (Compile, packageDoc) := false
)

lazy val root = project.in(file("."))
  .settings(buildSettings)
  .aggregate(core, postgres, h2, mysql)
  // .settings(aggregate in amm := false)

lazy val core = project.in(file("modules/core"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-core",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.typelevel" %% "macro-compat"   % "1.1.1",
      "org.tpolecat"  %% "doobie-core"    % "0.3.1-M2"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val ammonite = project.in(file("modules/ammonite"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-ammonite",
    libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.6.0" cross CrossVersion.full
  )
  .dependsOn(core)

lazy val postgres = project.in(file("modules/postgres"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-postgres",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-postgres" % "0.3.1-M2"
    ),
    initialCommands := """
      |import scalaz._,Scalaz._
      |import scalaz.concurrent.Task
      |import doobie.imports._
      |import doobie.contrib.postgresql.pgtypes._
      |val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
      |import xa.yolo._
      |import org.postgis._
      |import org.postgresql.util._
      |import org.postgresql.geometric._
      |import doobie.tsql._
      |import shapeless._
      |//import doobie.tsql.amm._, pprint.TPrint, pprint.Config.Colors._
      |//def tp[A:TPrint](a:A) = println(pprint.tprint[A])
      """.stripMargin
  )
  .dependsOn(core)

lazy val h2 = project.in(file("modules/h2"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-h2",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.h2.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:h2:zip:./modules/h2/world.zip!/world",
      "-Xmacro-settings:doobie.user=",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-h2" % "0.3.1-M2"
    ),
    initialCommands := s"""
      |import doobie.tsql._, doobie.tsql.amm._, scalaz._, Scalaz._, doobie.imports._, shapeless._
      |""".stripMargin
  )
  .dependsOn(core)

lazy val mysql = project.in(file("modules/mysql"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    name := "doobie-tsql-mysql",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=com.mysql.cj.jdbc.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:mysql://localhost/world?useSSL=false&serverTimezone=UTC",
      "-Xmacro-settings:doobie.user=root",
      "-Xmacro-settings:doobie.password=",
      "-Xmacro-settings:doobie.checkParameters=false"
    ),
    libraryDependencies ++= Seq(
      "mysql" % "mysql-connector-java" % "6.0.2"
    ),
    initialCommands := s"""
      |import doobie.tsql._, doobie.tsql.amm._, scalaz._, Scalaz._, doobie.imports._, shapeless._
      |""".stripMargin
  )
  .dependsOn(core)


lazy val docs = project.in(file("modules/docs"))
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(tutSettings)
  .settings(
    name := "doobie-tsql-docs",
    scalacOptions ++= Seq(
      "-Xmacro-settings:doobie.driver=org.postgresql.Driver",
      "-Xmacro-settings:doobie.connect=jdbc:postgresql:world",
      "-Xmacro-settings:doobie.user=postgres",
      "-Xmacro-settings:doobie.password="
    ),
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-postgresql" % "0.3.1-M2"
    ),
    tutTargetDirectory := (baseDirectory in root).value,
    tut := {
      val data = tut.value
      data.map(_._2).foreach(println)
      // TODO: remove lines that look like `scala> tp(...)`
      data
    }
  )
  .dependsOn(postgres)
