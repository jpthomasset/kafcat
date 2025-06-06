package kafcat

import java.time.Instant

import scala.concurrent.duration._

import com.monovore.decline.Command
import kafcat.CliParser.CliArgument
import kafcat.predicate.{Field, IsEqual, NumberConstant}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import kafcat.kafka.SaslPlainConfig

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

    "parse number option" in {
      val expected = Right(CliArgument("some-topic", number = Some(10)))

      testParser.parse(List("--number", "10", "some-topic")) should be(expected)
      testParser.parse(List("-n", "10", "some-topic")) should be(expected)
    }

    "parse skip option" in {
      val expected = Right(CliArgument("some-topic", skip = Some(10)))

      testParser.parse(List("--skip", "10", "some-topic")) should be(expected)
      testParser.parse(List("-s", "10", "some-topic")) should be(expected)
    }

    "parse skip null flag" in {
      val expected = Right(CliArgument("some-topic", skipNullValues = true))

      testParser.parse(List("--skip-null", "some-topic")) should be(expected)
    }

    "parse timeout option" in {
      val expected = Right(CliArgument("some-topic", timeout = Some(10.seconds)))

      testParser.parse(List("--timeout", "10", "some-topic")) should be(expected)
    }

    "parse since option" in {
      val expected = Right(CliArgument("some-topic", since = Some(Instant.parse("2024-02-15T08:05:20.341Z"))))

      testParser.parse(List("--since", "2024-02-15T08:05:20.341Z", "some-topic")) should be(expected)
    }

    "parse SASL Config" in {
      val expected = Right(CliArgument("some-topic", saslPlainConfig = Some(SaslPlainConfig("user", "pass"))))

      testParser.parse(List("--sasl-plain", "user:pass", "some-topic")) should be(expected)
    }
  }
}
