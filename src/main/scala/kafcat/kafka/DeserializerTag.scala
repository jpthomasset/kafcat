package kafcat.kafka

import cats.Show
import cats.effect.IO
import fs2.Stream
import fs2.kafka.Deserializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{
  Deserializer => KafkaDeserializer,
  LongDeserializer => KafkaLongDeserializer
}
import fs2.compression.Compression

enum DeserializerType {
  case String
  case Long
  case Avro
  case Raw
  case GzipString
}

trait DeserializerTag {
  type Type

  def build: Deserializer[IO, Type]
}

object DeserializerTag {
  def getDeserializerTag(arg: DeserializerType, schemaRegistryUrl: String): DeserializerTag =
    arg match {
      case DeserializerType.String     => StringDeserializer
      case DeserializerType.Long       => LongDeserializer
      case DeserializerType.Avro       => AvroDeserializer(schemaRegistryUrl)
      case DeserializerType.Raw        => RawDeserializer
      case DeserializerType.GzipString => GZipStringDeserializer
    }

  def getShow(tag: DeserializerTag): Show[tag.Type] = tag match {
    case RawDeserializer =>
      Show
        .show[RawDeserializer.Type](xs => s"Array(${xs.map("0x%02x".format(_)).mkString(", ")})")
        .asInstanceOf[Show[tag.Type]]

    case _ => Show.fromToString[tag.Type]
  }
}

object StringDeserializer extends DeserializerTag {
  type Type = Option[String]

  def build: Deserializer[IO, Type] = Deserializer.lift { bytes =>
    IO(Option(bytes).map(String(_)))
  }
}

object LongDeserializer extends DeserializerTag {
  type Type = Long

  def build: Deserializer[IO, Long] = {
    val longDeser = KafkaLongDeserializer()
    Deserializer.delegate[IO, Long](longDeser.asInstanceOf[KafkaDeserializer[Long]])
  }
}

object RawDeserializer extends DeserializerTag {
  type Type = Array[Byte]

  def build: Deserializer[IO, Array[Byte]] = Deserializer.lift { bytes =>
    IO(bytes)
  }
}

object GZipStringDeserializer extends DeserializerTag {
  type Type = Option[String]

  def build: Deserializer[IO, Type] = Deserializer.lift { bytes =>
    IO(Option(bytes)).flatMap {
      case Some(data) =>
        inflateGzip(data).map(inflated => Some(String(inflated)))
      case None       => IO.pure(None)
    }
  }

  private def inflateGzip(
    bytes: Array[Byte]
  ): IO[Array[Byte]] =
    Stream
      .emits[IO, Byte](bytes)
      .through(Compression.forSync[IO].gunzip())
      .flatMap(_.content)
      .compile
      .to(Array)
}

case class AvroDeserializer(schemaRegistryUrl: String) extends DeserializerTag {
  type Type = GenericRecord

  def build: Deserializer[IO, GenericRecord] = {
    val rawDeserializer = AvroSerdes.avroDeserializer(schemaRegistryUrl)
    Deserializer.delegate[IO, GenericRecord](rawDeserializer)
  }

}
