import Dependencies._

ThisBuild / scalaVersion      := "3.3.1"
ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / organization      := "com.frenchcoder"
ThisBuild / organizationName  := "Jean-Pierre Thomasset"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
  .settings(
    name                 := "kafcat",
    maintainer           := "jpthomasset@gmail.com",
    libraryDependencies ++= Dependencies.libraries ++ Dependencies.testLibraries,
    buildInfoKeys        := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage     := "kafcat",
    Compile / run / fork := true,
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-Xmax-inlines:64",
      "-Wunused:all",
      "-feature",
      "-deprecation",
      "-Werror",
      "-unchecked"
    )
  )
