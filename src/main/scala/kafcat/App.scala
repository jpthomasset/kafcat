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

  def consumeToStdout(cliArgs: CliParser.CliArgument): IO[Unit] = {
    import kafcat.eval

    val k = getDeserializerTag(cliArgs.keyDeserializer, cliArgs)
    val v = getDeserializerTag(cliArgs.valueDeserializer, cliArgs)

    val showK = getShow(k)
    val showV = getShow(v)

    KafkaConsumer
      .stream(customSettings(cliArgs.broker, cliArgs.groupId, k.build, v.build))
      .subscribeTo(cliArgs.topic)
      .records
      .filter(cr => cliArgs.predicate.map(_.eval(cr.record)).getOrElse(true))
      .foreach(cr => IO.println(RecordFormater.format(cr.record, cliArgs.format)(showK, showV)))
      .compile
      .drain
  }

  override def main: Opts[IO[ExitCode]] =
    CliParser.parse.map(consumeToStdout(_).as(ExitCode.Success))
}
