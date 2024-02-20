package kafcat.kafka

import cats.Show
import fs2.kafka._

object RecordFormater {
  def format[K, V](
    record: ConsumerRecord[K, V],
    format: String
  )(implicit KS: Show[K] = Show.fromToString[K], VS: Show[V] = Show.fromToString[V]): String = {
    val k = Show[K].show(record.key)
    val v = Option(record.value).map(Show[V].show(_)).getOrElse("null")

    val map = Map[String, String](
      "%k" -> k,
      "%v" -> v,
      "%t" -> record.topic,
      "%p" -> record.partition.toString,
      "%o" -> record.offset.toString,
      "%d" -> record.timestamp.toString,
      "%h" -> record.headers.toString
    )

    map.foldLeft(format) { case (acc, (k, v)) =>
      acc.replaceAll(k, v)
    }
  }
}
