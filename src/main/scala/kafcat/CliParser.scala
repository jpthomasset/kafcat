package kafcat

import java.time.Instant

import scala.concurrent.duration._

import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.time._
import fastparse._
import fs2.kafka.AutoOffsetReset
import kafcat.predicate._
import kafcat.kafka.DeserializerType

object CliParser {

  case class CliArgument(
    topic: String,
    abortOnFailure: Boolean = false,
    quiet: Boolean = false,
    broker: String = "localhost:9092",
    registry: String = "localhost:9090",
    keyDeserializer: DeserializerType = DeserializerType.String,
    valueDeserializer: DeserializerType = DeserializerType.String,
    format: String = "%k => %v",
    predicate: Option[Predicate] = None,
    number: Option[Int] = None,
    skip: Option[Int] = None,
    skipNullValues: Boolean = false,
    timeout: Option[FiniteDuration] = None,
    offsetReset: AutoOffsetReset = AutoOffsetReset.Latest,
    since: Option[Instant] = None
  )

  val deserializerMap = Map(
    "string" -> DeserializerType.String,
    "long"   -> DeserializerType.Long,
    "avro"   -> DeserializerType.Avro,
    "raw"    -> DeserializerType.Raw
  )

  implicit val deserializerArgument: Argument[DeserializerType] = Argument.fromMap(
    "Deserializer",
    deserializerMap
  )

  val deserialierNames = "One of: \n" + deserializerMap.keys.map(" * " + _).mkString("\n")

  val topic             = Opts.argument[String]("topic")
  val abortOnFailure    = Opts.flag("abort", "Abort on failure", "a").orFalse
  val quiet             = Opts.flag("quiet", "Do not output failures to stderr", "q").orFalse
  val broker            = Opts.option[String]("broker", "Broker address and port", "b", "url").withDefault("localhost:9092")
  val registry          = Opts.option[String]("registry", "Registry URL", "r", "url").withDefault("localhost:9090")
  val keyDeserializer   = Opts
    .option[DeserializerType]("key-deserializer", s"Key deserializer. Default is string. $deserialierNames", "k")
    .withDefault(DeserializerType.String)
  val valueDeserializer = Opts
    .option[DeserializerType]("value-deserializer", s"Value deserializer. Default is string. $deserialierNames", "v")
    .withDefault(DeserializerType.String)

  val format = Opts
    .option[String](
      "format",
      """Output format with templating. Default is "%k => %v". Valid template variables are:
        | * %k Key
        | * %v Value
        | * %t Topic name
        | * %p Partition
        | * %o Offset
        | * %d Timestamp
        | * %h Headers""".stripMargin,
      "f"
    )
    .withDefault("%k => %v")

  val predicate = Opts
    .option[String](
      "predicate",
      """Predicate to filter records. You can use the following operators: ==, !=, ||, &&
        | Then you can use value/key field names and constants:
        | * value.field to extract a field from the value of the event
        | * key.field to extract a field from the key of the event
        | * topic
        | * partition
        | * offset
        | * "some string" to use a string constant
        | * 123.45 to use a number constant
        |Here are some examples:
        | * "value.id == 12"
        | * "key.id == 12"
        | * "value.sub.subage == 15"
        | * "value.sub.subname == 'subname' || key.id == 12"
        | * "topic == 'some topic' && value.id == 12"
        |""".stripMargin,
      "p"
    )
    .mapValidated(s =>
      fastparse.parse(s, PredicateParser.predicate(_)) match {
        case Parsed.Success(p, _) => Validated.valid(p)
        case f: Parsed.Failure    => Validated.invalid(NonEmptyList.of(f.msg))
      }
    )
    .orNone

  val number = Opts
    .option[Int]("number", "Take N records and quit", "n", "N")
    .orNone

  val skip = Opts
    .option[Int]("skip", "Skip N records and quit", "s", "N")
    .orNone

  val skipNullValues = Opts.flag("skip-null", "Skip records with null values").orFalse

  val timeout = Opts.option[Int]("timeout", "Timeout after N seconds", metavar = "N").map(_.seconds).orNone

  val offsetReset = Opts
    .option[String](
      "offset-reset",
      "Offset reset strategy. One of earliest or latest. Default to latest",
      metavar = "strategy"
    )
    .mapValidated(_.toLowerCase() match {
      case "earliest" => Validated.valid(AutoOffsetReset.Earliest)
      case "latest"   => Validated.valid(AutoOffsetReset.Latest)
      case _          => Validated.invalid(NonEmptyList.of("Invalid offset reset strategy."))
    })
    .withDefault(AutoOffsetReset.Latest)

  val since = Opts.option[Instant]("since", "Start consuming from this timestamp (ISO Format)").orNone

  val parse: Opts[CliArgument] =
    (
      topic,
      abortOnFailure,
      quiet,
      broker,
      registry,
      keyDeserializer,
      valueDeserializer,
      format,
      predicate,
      number,
      skip,
      skipNullValues,
      timeout,
      offsetReset,
      since
    )
      .mapN(CliArgument.apply)

}
