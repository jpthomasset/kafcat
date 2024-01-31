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
  }
}
