package kafcat.predicate
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import kafcat.format._

class PredicateParserSpec extends AnyWordSpec with Matchers {
  import kafcat.ParserSpecUtils.checkParser

  "PredicateParser" should {
    "parse a double quoted string constant" in {
      val input    = "\"some string\""
      val expected = StringConstant("some string")

      checkParser(input, PredicateParser.doubleQuotedStringConstant(_), expected)
    }

    "parse a single quoted string constant" in {
      val input    = "'some string'"
      val expected = StringConstant("some string")

      checkParser(input, PredicateParser.singleQuotedStringConstant(_), expected)
    }

    "parse a number constant" in {
      val input    = "123.45"
      val expected = NumberConstant(123.45)

      checkParser(input, PredicateParser.numberConstant(_), expected)
    }

    "parse a simple equality" in {
      val input    = "12 == 13"
      val expected = IsEqual(NumberConstant(12), NumberConstant(13))

      checkParser(input, PredicateParser.predicate(_), expected)
    }

    "parse a field equality with number" in {
      val input    = "field.id == 13"
      val expected = IsEqual(Field(List("field", "id")), NumberConstant(13))

      checkParser(input, PredicateParser.predicate(_), expected)
    }

    "parse a field equality with string" in {
      val input    = "field.id == \"some string\""
      val expected = IsEqual(Field(List("field", "id")), StringConstant("some string"))

      checkParser(input, PredicateParser.predicate(_), expected)
    }

    "parse a simple inequality" in {
      val input    = "12 != 13"
      val expected = IsNotEqual(NumberConstant(12), NumberConstant(13))

      checkParser(input, PredicateParser.predicate(_), expected)
    }

    // "parse a simple >" in {
    //   val input    = "12 > 13"
    //   val expected = IsGreaterThan(NumberConstant(12), NumberConstant(13))

    //   checkParser(input, PredicateParser.predicate(_), expected)
    // }

    // "parse a simple >=" in {
    //   val input    = "12 >= 13"
    //   val expected = IsGreaterThanOrEqual(NumberConstant(12), NumberConstant(13))

    //   checkParser(input, PredicateParser.predicate(_), expected)
    // }

    // "parse a simple <" in {
    //   val input    = "12 < 13"
    //   val expected = IsLessThan(NumberConstant(12), NumberConstant(13))

    //   checkParser(input, PredicateParser.predicate(_), expected)
    // }

    // "parse a simple <=" in {
    //   val input    = "12 <= 13"
    //   val expected = IsLessThanOrEqual(NumberConstant(12), NumberConstant(13))

    //   checkParser(input, PredicateParser.predicate(_), expected)
    // }

    "parse a simple ||" in {
      val input    = "12 == 13 || 14 == 15"
      val expected =
        Or(IsEqual(NumberConstant(12), NumberConstant(13)), IsEqual(NumberConstant(14), NumberConstant(15)))

      checkParser(input, PredicateParser.predicate(_), expected)
    }

    "parse a simple &&" in {
      val input    = "12 == 13 && 14 == 15"
      val expected =
        And(IsEqual(NumberConstant(12), NumberConstant(13)), IsEqual(NumberConstant(14), NumberConstant(15)))

      checkParser(input, PredicateParser.predicate(_), expected)
    }
  }
}
