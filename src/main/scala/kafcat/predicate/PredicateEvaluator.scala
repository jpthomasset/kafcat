package kafcat.predicate

import scala.util.Try

import fs2.kafka.ConsumerRecord
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8

extension (p: Predicate) {
  def eval(record: ConsumerRecord[?, ?]): Boolean = p match {
    case IsEqual(left, right)              => getValue(record, left) == getValue(record, right)
    case IsNotEqual(left, right)           => getValue(record, left) != getValue(record, right)
    case IsGreaterThan(left, right)        =>
      (getValue(record, left), getValue(record, right)) match {
        case (Some(NumberConstant(l)), Some(NumberConstant(r))) => l > r
        case _                                                  => false
      }
    case IsGreaterThanOrEqual(left, right) =>
      (getValue(record, left), getValue(record, right)) match {
        case (Some(NumberConstant(l)), Some(NumberConstant(r))) => l >= r
        case _                                                  => false
      }
    case IsLessThan(left, right)           =>
      (getValue(record, left), getValue(record, right)) match {
        case (Some(NumberConstant(l)), Some(NumberConstant(r))) => l < r
        case _                                                  => false
      }
    case IsLessThanOrEqual(left, right)    =>
      (getValue(record, left), getValue(record, right)) match {
        case (Some(NumberConstant(l)), Some(NumberConstant(r))) => l <= r
        case _                                                  => false
      }

    case Or(left, right)  => left.eval(record) || right.eval(record)
    case And(left, right) => left.eval(record) && right.eval(record)

  }

  private def getValue(record: ConsumerRecord[?, ?], value: Value): Option[Constant] =
    value match {
      case f: Field    => getFieldValue(record, f)
      case c: Constant => Some(c)
    }

  private def getFieldValue(record: ConsumerRecord[?, ?], field: Field): Option[Constant] =
    field.path match {
      case "key" :: Nil       => getAsConstant(record.key)
      case "value" :: Nil     => getAsConstant(record.value)
      case "value" :: tail    => getFieldValueFromRecord(record.value, tail)
      case "key" :: tail      => getFieldValueFromRecord(record.key, tail)
      case "topic" :: Nil     => Some(StringConstant(record.topic))
      case "partition" :: Nil => Some(NumberConstant(record.partition.toDouble))
      case "offset" :: Nil    => Some(NumberConstant(record.offset.toDouble))
      case _                  => None
    }

  private def getFieldValueFromRecord(record: Any, path: List[String]): Option[Constant] =
    if (record.isInstanceOf[GenericRecord]) {
      val value = path.foldLeft[Option[Any]](Some(record)) { (r, f) =>
        r.flatMap {
          case r: GenericRecord => Try(r.get(f)).toOption
          case _                => None
        }
      }
      value.flatMap(getAsConstant)
    } else {
      None
    }

  private def getAsConstant(value: Any): Option[Constant] =
    value match {
      case i: Int                    => Some(NumberConstant(i))
      case l: Long                   => Some(NumberConstant(l.toDouble))
      case d: Double                 => Some(NumberConstant(d))
      case s: String                 => Some(StringConstant(s))
      case u: Utf8                   => Some(StringConstant(u.toString))
      case s: GenericData.EnumSymbol => Some(StringConstant(s.toString))
      case Some(x)                   => getAsConstant(x)
      case _                         => None
    }
}
