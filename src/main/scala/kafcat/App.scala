package kafcat

import cats.effect.{ExitCode, IO}
import cats.effect.std.Console
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.kafka._
import kafcat.Fs2Pipes.NoMoreEventException
import kafcat.kafka._
import kafcat.predicate.eval

object App
    extends CommandIOApp(
      name = kafcat.BuildInfo.name,
      header = "Consume events from a Kafka topic and print them to stdout",
      version = kafcat.BuildInfo.version
    ) {

  def consumeToStdout(cliArgs: CliParser.CliArgument): IO[Unit] = {

    val k = DeserializerTag.getDeserializerTag(cliArgs.keyDeserializer, cliArgs.registry)
    val v = DeserializerTag.getDeserializerTag(cliArgs.valueDeserializer, cliArgs.registry)

    val showK = DeserializerTag.getShow(k)
    val showV = DeserializerTag.getShow(v)

    KafkaConsumer
      .stream(KafkaUtils.settings(cliArgs.broker, k.build, v.build, cliArgs.offsetReset, cliArgs.saslPlainConfig))
      .evalTap(KafkaUtils.seekOffset(cliArgs.topic, cliArgs.since, _))
      .records
      .through(Fs2Pipes.timeoutWhenNoEvent(cliArgs.timeout))
      .through(Fs2Pipes.skipNullValues(cliArgs.skipNullValues))
      .filter(cr => cliArgs.predicate.map(_.eval(cr.record)).getOrElse(true))
      .through(Fs2Pipes.take(cliArgs.number))
      .foreach(cr => IO.println(RecordFormater.format(cr.record, cliArgs.format)(showK, showV)))
      .compile
      .drain
      .recoverWith { case NoMoreEventException(s) => Console[IO].errorln(s) }
  }

  override def main: Opts[IO[ExitCode]] =
    CliParser.parse.map(consumeToStdout(_).as(ExitCode.Success))
}
