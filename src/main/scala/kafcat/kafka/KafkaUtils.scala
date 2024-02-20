package kafcat.kafka

import java.time.Instant

import cats.effect.IO
import cats.implicits._
import fs2.kafka._
import org.apache.kafka.common.TopicPartition

object KafkaUtils {

  def settings[K, V](
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

      case None => consumer.assign(topic)
    }

}
