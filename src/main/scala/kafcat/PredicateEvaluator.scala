package kafcat

import fs2.kafka.ConsumerRecord

extension (p: Predicate) {
  def eval(record: ConsumerRecord[_, _]): Boolean = p match {
    case IsEqual(left, right) =>
      val x = getValue(record, left) == getValue(record, right)
      // println(s"IsEqual: $x")
      x

    case _ => false
  }

  private def getValue(record: ConsumerRecord[_, _], value: Value): Option[Constant] =
    value match {
      // case Field(path) =>
      case c: Constant => Some(c)
      case _           => None

    }
}
