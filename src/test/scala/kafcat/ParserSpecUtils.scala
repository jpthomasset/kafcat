package kafcat

import fastparse._
import org.scalatest.matchers.should.Matchers

object ParserSpecUtils extends Matchers {
  def checkParser[T](input: String, parser: P[_] => P[T], expected: T) =
    parse(input, parser(_)) match {
      case Parsed.Success(value, _)    => value should be(expected)
      case f @ Parsed.Failure(_, _, _) => fail(s"Failed to parse input $input: ${f.msg}")
    }
}