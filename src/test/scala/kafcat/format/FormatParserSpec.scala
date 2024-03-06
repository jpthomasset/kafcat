package kafcat.format

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormatParserSpec extends AnyWordSpec with Matchers {
  import kafcat.ParserSpecUtils.checkParser

  "FormatParser" should {
    "parse simple text" in {
      val input    = "some text"
      val expected = Format(List(StringConstant("some text")))

      checkParser(input, FormatParser.formatParser(_), expected)
    }

    "parse simple field name" in {
      val input    = "field"
      val expected = Field(List("field"))

      checkParser(input, FormatParser.field(_), expected)
    }

    "parse field path" in {
      val input    = "some.field"
      val expected = Field(List("some", "field"))

      checkParser(input, FormatParser.field(_), expected)
    }

    "parse field placeholder" in {
      val input    = "%field"
      val expected = Format(List(Field(List("field"))))

      checkParser(input, FormatParser.formatParser(_), expected)
    }

    "parse field with dot syntax" in {
      val input    = "%field.id"
      val expected = Format(List(Field(List("field", "id"))))

      checkParser(input, FormatParser.formatParser(_), expected)
    }

    "parse complex format" in {
      val input    = "some text: %field.id some more text=%another.field"
      val expected = Format(List(
        StringConstant("some text: "),
        Field(List("field", "id")),
        StringConstant(" some more text="),
        Field(List("another", "field"))
      ))

      checkParser(input, FormatParser.formatParser(_), expected)
    }
  }

}