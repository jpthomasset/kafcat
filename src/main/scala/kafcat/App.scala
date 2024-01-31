package kafcat

import cats.Show
import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.kafka._
import kafcat.CliParser.DeserializerType

object App
    extends CommandIOApp(
      name = kafcat.BuildInfo.name,
      header = "Consume events from a Kafka topic and print them to stdout",
      version = kafcat.BuildInfo.version
    ) {

  def getDeserializer(arg: DeserializerType, schemaRegistryUrl: String): DeserializerTag = arg match {
    case DeserializerType.String => StringDeserializer
    case DeserializerType.Long   => LongDeserializer
    case DeserializerType.Avro   => AvroDeserializer(schemaRegistryUrl)
  }

  def customSettings[K, V](
    bootstrapServer: String,
    groupId: String,
    keydes: KeyDeserializer[IO, K],
    valuedes: ValueDeserializer[IO, V]
  ) =
    ConsumerSettings(keydes, valuedes)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(bootstrapServer)
      .withGroupId(groupId)

  def consumerSettings[K, V](
    bootstrapServer: String,
    groupId: String,
    keydes: KeyDeserializer[IO, K],
    valuedes: ValueDeserializer[IO, V]
  ) =
    ConsumerSettings(keydes, valuedes)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(bootstrapServer)
      .withGroupId(groupId)

  def consumeToStdout[K, V](cliArgs: CliParser.CliArgument): IO[Unit] = {
    val k = getDeserializer(cliArgs.keyDeserializer, cliArgs.registry).build()
    val v = getDeserializer(cliArgs.valueDeserializer, cliArgs.registry).build()

    KafkaConsumer
      .stream(customSettings(cliArgs.broker, cliArgs.groupId, k, v))
      .subscribeTo(cliArgs.topic)
      .records
      .foreach(cr => printRecord(cr.record))
      .compile
      .drain
  }

  def printRecord[K, V](
    record: ConsumerRecord[K, V]
  )(implicit KS: Show[K] = Show.fromToString[K], VS: Show[V] = Show.fromToString[V]): IO[Unit] = {
    val k = Show[K].show(record.key)
    val v = Option(record.value).map(Show[V].show(_)).getOrElse("null")

    IO.println(s"$k => $v")
  }

  override def main: Opts[IO[ExitCode]] =
    CliParser.parse.map(consumeToStdout(_).as(ExitCode.Success))
}
