package kafcat

import java.time.Instant

import cats.Show
import cats.effect.{ExitCode, IO}
import cats.effect.std.Console
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.kafka._
import kafcat.CliParser.DeserializerType
import kafcat.Fs2Pipes.NoMoreEventException
import org.apache.kafka.common.TopicPartition

object App
    extends CommandIOApp(
      name = kafcat.BuildInfo.name,
      header = "Consume events from a Kafka topic and print them to stdout",
      version = kafcat.BuildInfo.version
    ) {

  def getDeserializerTag(arg: DeserializerType, cliArgs: CliParser.CliArgument): DeserializerTag =
    arg match {
      case DeserializerType.String => StringDeserializer
      case DeserializerType.Long   => LongDeserializer
      case DeserializerType.Avro   => AvroDeserializer(cliArgs.registry)
      case DeserializerType.Raw    => RawDeserializer
    }

  def getShow(tag: DeserializerTag): Show[tag.Type] = tag match {
    case RawDeserializer =>
      Show
        .show[RawDeserializer.Type](xs => s"Array(${xs.map("0x%02x".format(_)).mkString(", ")})")
        .asInstanceOf[Show[tag.Type]]

    case _ => Show.fromToString[tag.Type]
  }

  def customSettings[K, V](
    bootstrapServer: String,
    keydes: KeyDeserializer[IO, K],
    valuedes: ValueDeserializer[IO, V],
    offsetReset: AutoOffsetReset
  ) =
    ConsumerSettings(keydes, valuedes)
      .withAutoOffsetReset(offsetReset)
      .withBootstrapServers(bootstrapServer)

  def seekOffset(topic: String, since: Option[Instant], consumer: KafkaConsumer[IO, _, _]): IO[Unit] =
    since match {
      case Some(instant) =>
        val timestamp = instant.toEpochMilli

        for {
          _               <- consumer.assign(topic)
          partitions      <- consumer.partitionsFor(topic)
          partitionsWithTs = partitions.map(p => TopicPartition(p.topic(), p.partition()) -> timestamp).toMap
          offsets         <- consumer.offsetsForTimes(partitionsWithTs)
          seekPositions    = offsets.flatMap {
                               case (tp, Some(offset)) => List(tp -> offset)
                               case _                  => None
                             }
          _               <- seekPositions.toList.traverse((tp, offset) => consumer.seek(tp, offset.offset()))

        } yield ()

      case None => consumer.subscribeTo(topic)
    }

  def consumeToStdout(cliArgs: CliParser.CliArgument): IO[Unit] = {

    val k = getDeserializerTag(cliArgs.keyDeserializer, cliArgs)
    val v = getDeserializerTag(cliArgs.valueDeserializer, cliArgs)

    val showK = getShow(k)
    val showV = getShow(v)

    KafkaConsumer
      .stream(customSettings(cliArgs.broker, k.build, v.build, cliArgs.offsetReset))
      .evalTap(seekOffset(cliArgs.topic, cliArgs.since, _))
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
