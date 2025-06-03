package kafcat.kafka

import scala.jdk.CollectionConverters.*

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.{AbstractKafkaSchemaSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Deserializer

object AvroSerdes {
  case class SerdesConfiguration(schemaRegistryUrl: String) {
    def asProps: java.util.Map[String, String] =
      Map[String, String](
        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl,
        AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS      -> "false"
      ).asJava

    // For testing purpose only
    def registryClient: Option[SchemaRegistryClient] = None
  }

  class GenericRecordDeserializer(configuration: SerdesConfiguration) extends Deserializer[GenericRecord] {
    private val inner = new KafkaAvroDeserializer(configuration.registryClient.orNull, configuration.asProps)

    override def configure(configs: java.util.Map[String, ?], isKey: Boolean): Unit =
      inner.configure(configs, isKey)

    override def deserialize(topic: String, data: Array[Byte]): GenericRecord =
      inner.deserialize(topic, data).asInstanceOf[GenericRecord]

    override def close(): Unit =
      inner.close()
  }

  def avroDeserializer(schemaRegistryUrl: String) = new AvroSerdes.GenericRecordDeserializer(
    AvroSerdes.SerdesConfiguration(schemaRegistryUrl)
  )
}
