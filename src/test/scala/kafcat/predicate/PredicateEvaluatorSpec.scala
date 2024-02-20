package kafcat.predicate

import com.sksamuel.avro4s.{AvroSchema, ToRecord}
import fs2.kafka.ConsumerRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PredicateEvaluatorSpec extends AnyWordSpec with Matchers {

  enum TestEnum {
    case One, Two, Three
  }

  case class TestSimpleEvent(id: Int, name: String, position: TestEnum = TestEnum.One)
  case class TestEvent(id: Int, name: String, sub: TestSubEvent)
  case class TestSubEvent(subname: String, subage: Int)

  val simpleEventToRecord = ToRecord.apply[TestSimpleEvent](AvroSchema[TestSimpleEvent])
  val eventToRecord       = ToRecord.apply[TestEvent](AvroSchema[TestEvent])

  val valueRecord =
    "PredicateEvaluator" should {
      "evaluate simple equality" in {
        val cr        = ConsumerRecord("topic", 0, 0, 1, "some event")
        val predicate = IsEqual(NumberConstant(12), NumberConstant(12))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with simple key" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate = IsEqual(Field(List("key")), NumberConstant(12))
        predicate.eval(cr) should be(true)
      }

      "evaluate inequality with simple key" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate = IsNotEqual(Field(List("key")), NumberConstant(12))
        predicate.eval(cr) should be(false)
      }

      "evaluate equality with simple value" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate = IsEqual(Field(List("value")), StringConstant("some event"))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with value field" in {
        val evt       = simpleEventToRecord.to(TestSimpleEvent(12, "some name"))
        val cr        = ConsumerRecord("topic", 0, 0, 12, evt)
        val predicate = IsEqual(Field(List("value", "id")), NumberConstant(12))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with non existent value field" in {
        val evt       = simpleEventToRecord.to(TestSimpleEvent(12, "some name"))
        val cr        = ConsumerRecord("topic", 0, 0, 12, evt)
        val predicate = IsEqual(Field(List("value", "toto")), NumberConstant(12))
        predicate.eval(cr) should be(false)
      }

      "evaluate equality with enum field" in {
        val evt       = simpleEventToRecord.to(TestSimpleEvent(12, "some name", TestEnum.Two))
        val cr        = ConsumerRecord("topic", 0, 0, 12, evt)
        val predicate = IsEqual(Field(List("value", "position")), StringConstant("Two"))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with value nested field" in {
        val evt       = eventToRecord.to(TestEvent(12, "some name", TestSubEvent("subname", 15)))
        val cr        = ConsumerRecord("topic", 0, 0, 12, evt)
        val predicate = IsEqual(Field(List("value", "sub", "subage")), NumberConstant(15))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with key field" in {
        val key       = simpleEventToRecord.to(TestSimpleEvent(12, "some name"))
        val cr        = ConsumerRecord("topic", 0, 0, key, "some event")
        val predicate = IsEqual(Field(List("key", "id")), NumberConstant(12))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with key nested field" in {
        val key       = eventToRecord.to(TestEvent(12, "some name", TestSubEvent("subname", 15)))
        val cr        = ConsumerRecord("topic", 0, 0, key, "some event")
        val predicate = IsEqual(Field(List("key", "sub", "subage")), NumberConstant(15))
        predicate.eval(cr) should be(true)
      }

      "evaluate equality with simple key or value" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate =
          Or(IsEqual(Field(List("value")), NumberConstant(12)), IsEqual(Field(List("key")), NumberConstant(12)))
        predicate.eval(cr) should be(true)
      }

      "evaluate false equality with simple key or value" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate =
          Or(IsEqual(Field(List("value")), NumberConstant(50)), IsEqual(Field(List("key")), NumberConstant(50)))
        predicate.eval(cr) should be(false)
      }

      "evaluate equality with simple key and value" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate =
          And(
            IsEqual(Field(List("value")), StringConstant("some event")),
            IsEqual(Field(List("key")), NumberConstant(12))
          )
        predicate.eval(cr) should be(true)
      }

      "evaluate false equality with simple key and value" in {
        val cr        = ConsumerRecord("topic", 0, 0, 12, "some event")
        val predicate =
          And(
            IsEqual(Field(List("value")), StringConstant("some event")),
            IsEqual(Field(List("key")), NumberConstant(13))
          )
        predicate.eval(cr) should be(false)
      }
    }
}
