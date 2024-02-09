package kafcat

import com.monovore.decline.Command
import kafcat.CliParser.CliArgument
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CliParserSpec extends AnyWordSpec with Matchers {
  "CliParser" should {
    val testParser = Command("test", "test")(CliParser.parse)

    "parse topic" in {
      val expected = Right(CliArgument("some-topic"))
      testParser.parse(List("some-topic")) should be(expected)
    }

    "require topic" in {
      testParser.parse(List()).isLeft should be(true)
    }

    "parse 'abort' flag" in {
      val expected = Right(CliArgument("some-topic", abortOnFailure = true))
      testParser.parse(List("--abort", "some-topic")) should be(expected)
      testParser.parse(List("-a", "some-topic")) should be(expected)
    }

    "parse 'quiet' flag" in {
      val expected = Right(CliArgument("some-topic", quiet = true))
      testParser.parse(List("--quiet", "some-topic")) should be(expected)
      testParser.parse(List("-q", "some-topic")) should be(expected)
    }

    "parse 'broker' flag" in {
      val expected = Right(CliArgument("some-topic", broker = "some-broker:9999"))
      testParser.parse(List("--broker", "some-broker:9999", "some-topic")) should be(expected)
      testParser.parse(List("-b", "some-broker:9999", "some-topic")) should be(expected)
    }

    "parse key deserializer option" in {
      CliParser.deserializerMap.foreach { case (k, v) =>
        val expected = Right(CliArgument("some-topic", keyDeserializer = v))
        testParser.parse(List("--key-deserializer", k, "some-topic")) should be(expected)
        testParser.parse(List("-k", k, "some-topic")) should be(expected)
      }
    }

    "parse value deserializer option" in {
      CliParser.deserializerMap.foreach { case (k, v) =>
        val expected = Right(CliArgument("some-topic", valueDeserializer = v))
        testParser.parse(List("--value-deserializer", k, "some-topic")) should be(expected)
        testParser.parse(List("-v", k, "some-topic")) should be(expected)
      }
    }

    "parse format option" in {
      val format   = "[partition %p offset %o at %d] %k => %h"
      val expected = Right(CliArgument("some-topic", format = format))
      testParser.parse(List("--format", format, "some-topic")) should be(expected)
      testParser.parse(List("-f", format, "some-topic")) should be(expected)
    }

    "parse predicate option" in {
      val predicateString = "field.id == 13"
      val predicate       = IsEqual(Field(List("field", "id")), NumberConstant(13))
      val expected        = Right(CliArgument("some-topic", predicate = Some(predicate)))

      testParser.parse(List("--predicate", predicateString, "some-topic")) should be(expected)
      testParser.parse(List("-p", predicateString, "some-topic")) should be(expected)
    }
  }
}
