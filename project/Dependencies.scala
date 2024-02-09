import sbt._

object Dependencies {

  object Versions {
    lazy val catsEffect                 = "3.5.3"
    lazy val catsEffectTestingScalatest = "1.5.0"
    lazy val decline                    = "2.4.1"
    lazy val fs2                        = "3.9.3"
    lazy val fs2Kafka                   = "3.2.0"
    lazy val scalatest                  = "3.2.17"
    lazy val confluent                  = "7.5.1"
    lazy val log4cats                   = "2.6.0"
    lazy val logback                    = "1.4.14"
    lazy val fastparse                  = "3.0.2"
    lazy val avro4s                     = "5.0.9"
  }

  lazy val libraries: Seq[ModuleID] = Seq(
    "co.fs2"          %% "fs2-core"                % Versions.fs2,
    "co.fs2"          %% "fs2-io"                  % Versions.fs2,
    "ch.qos.logback"   % "logback-classic"         % Versions.logback,
    "com.github.fd4s" %% "fs2-kafka"               % Versions.fs2Kafka,
    "com.monovore"    %% "decline-effect"          % Versions.decline,
    "org.typelevel"   %% "cats-effect"             % Versions.catsEffect,
    "io.confluent"     % "kafka-schema-serializer" % Versions.confluent,
    "io.confluent"     % "kafka-avro-serializer"   % Versions.confluent,
    "com.lihaoyi"     %% "fastparse"               % Versions.fastparse
  )

  lazy val testLibraries: Seq[ModuleID] = Seq(
    "org.scalatest"      %% "scalatest"                     % Versions.scalatest                  % Test,
    "org.typelevel"      %% "cats-effect-testing-scalatest" % Versions.catsEffectTestingScalatest % Test,
    "com.sksamuel.avro4s" % "avro4s-core_3"                 % Versions.avro4s                     % Test // Only to generate avro schema and record
  )
}
