import Dependencies._

ThisBuild / scalaVersion      := "3.3.1"
ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / organization      := "com.frenchcoder"
ThisBuild / organizationName  := "Jean-Pierre Thomasset"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

val VersionRegex = """([0-9]+)\.([0-9]+)\.([0-9]+)(.*)""".r

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .settings(
    name                      := "kafcat",
    maintainer                := "jpthomasset@gmail.com",
    dockerBaseImage           := "openjdk:17",
    dockerRepository          := Some("jpthomasset"),
    libraryDependencies ++= Dependencies.libraries ++ Dependencies.testLibraries,
    buildInfoKeys             := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage          := "kafcat",
    git.useGitDescribe        := true,
    git.gitTagToVersionNumber := {
      case VersionRegex(major, minor, patch, "") =>
        Some(s"$major.$minor.$patch")

      case VersionRegex(major, minor, patch, _) =>
        Some(s"$major.${minor.toInt + 1}.0")

      case _ => None
    },
    Compile / run / fork      := true,
    resolvers ++= Seq(
      "confluent".at("https://packages.confluent.io/maven/"),
      "confluent-avro".at("https://packages.confluent.io/maven/io/confluent/kafka-avro-serializer/")
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-Xmax-inlines:64",
      "-Wunused:all",
      "-feature",
      "-deprecation",
      "-Werror",
      "-unchecked"
    ),
    Universal / javaOptions += "--" // Make sure all arguments are passed to the app and not the launching script
  )
