package kafcat

import cats.effect.IO
import fs2.kafka.Deserializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer => KafkaDeserializer, LongDeserializer => KafkaLongDeserializer}

trait DeserializerTag {
  type Type

  def build(): Deserializer[IO, Type]
}

object StringDeserializer extends DeserializerTag {
  type Type = Option[String]

  def build(): Deserializer[IO, Type] = Deserializer.lift { bytes =>
    IO(Option(bytes).map(String(_)))
  }
}

object LongDeserializer extends DeserializerTag {
  type Type = Long

  def build(): Deserializer[IO, Long] = {
    val longDeser = KafkaLongDeserializer()
    Deserializer.delegate[IO, Long](longDeser.asInstanceOf[KafkaDeserializer[Long]])
  }
}

case class AvroDeserializer(schemaRegistryUrl: String) extends DeserializerTag {
  type Type = GenericRecord

  def build(): Deserializer[IO, GenericRecord] = {
    val rawDeserializer = AvroSerdes.avroDeserializer(schemaRegistryUrl)
    Deserializer.delegate[IO, GenericRecord](rawDeserializer)
  }
}

//  val stringDeserializer: Deserializer[IO, Option[String]] =
//     Deserializer.lift { bytes =>
//       IO(Option(bytes).map(String(_)))
//     }
