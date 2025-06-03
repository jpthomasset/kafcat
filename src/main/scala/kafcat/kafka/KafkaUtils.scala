package kafcat.kafka

import java.time.Instant

import cats.effect.IO
import cats.implicits._
import fs2.kafka._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs

object KafkaUtils {

  def settings[K, V](
    bootstrapServer: String,
    keydes: KeyDeserializer[IO, K],
    valuedes: ValueDeserializer[IO, V],
    offsetReset: AutoOffsetReset,
    saslPlainConfig: Option[SaslPlainConfig]
  ) = {
    val baseSettings = ConsumerSettings(keydes, valuedes)
      .withAutoOffsetReset(offsetReset)
      .withBootstrapServers(bootstrapServer)

    saslPlainConfig.foldLeft(baseSettings) { (settings, sasl) =>
      settings
        .withProperties(
          Map(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> "SASL_SSL",
            SaslConfigs.SASL_JAAS_CONFIG                 -> s"""org.apache.kafka.common.security.plain.PlainLoginModule required username="${sasl.username}" password="${sasl.password}";""",
            SaslConfigs.SASL_MECHANISM                   -> "PLAIN",
            CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG -> "use_all_dns_ips"
          )
        )
    }
  }

  def seekOffset(topic: String, since: Option[Instant], consumer: KafkaConsumer[IO, ?, ?]): IO[Unit] =
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

      case None => consumer.assign(topic)
    }

}
