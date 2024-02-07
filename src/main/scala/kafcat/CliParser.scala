package kafcat

import cats.implicits._
import com.monovore.decline._

object CliParser {

  enum DeserializerType {
    case String
    case Long
    case Avro
    case Raw
  }

  case class CliArgument(
    topic: String,
    abortOnFailure: Boolean = false,
    quiet: Boolean = false,
    broker: String = "localhost:9092",
    groupId: String = "kafcat",
    registry: String = "localhost:9090",
    keyDeserializer: DeserializerType = DeserializerType.String,
    valueDeserializer: DeserializerType = DeserializerType.String,
    format: String = "%k => %v"
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
  val broker            = Opts.option[String]("broker", "Broker address and port", "b").withDefault("localhost:9092")
  val groupId           = Opts.option[String]("groupid", "Consumer Group ID", "g").withDefault("kafcat")
  val registry          = Opts.option[String]("registry", "Registry URL", "r").withDefault("localhost:9090")
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

  val parse: Opts[CliArgument] =
    (topic, abortOnFailure, quiet, broker, groupId, registry, keyDeserializer, valueDeserializer, format)
      .mapN(CliArgument.apply)

}
