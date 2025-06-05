package kafcat.predicate

import fastparse._
import fastparse.SingleLineWhitespace._

object PredicateParser {
  def alpha[$: P]     = P(CharIn("a-zA-Z"))
  def digit[$: P]     = P(CharIn("0-9"))
  def alphanum[$: P]  = P(alpha | digit)
  def fieldChar[$: P] = P(alphanum | "_")

  def fieldName[$: P]: P[String]       = P(fieldChar.repX(1).!)
  def fieldPath[$: P]: P[List[String]] = P(fieldName ~~ ("." ~~ fieldPath).?).map(x => x._1 :: x._2.toList.flatten)
  def field[$: P]: P[Field]            = P(fieldPath.map(Field(_))).log

  def doubleQuotedStringConstant[$: P]: P[StringConstant] =
    P("\"" ~~ CharsWhile(_ != '"').! ~~ "\"").map(StringConstant(_))

  def singleQuotedStringConstant[$: P]: P[StringConstant] =
    P("'" ~~ CharsWhile(_ != '\'').! ~~ "'").map(StringConstant(_))

  def numberConstant[$: P]: P[NumberConstant] =
    P(CharsWhile(c => c.isDigit || c == '.').!)
      .map(_.toDoubleOption)
      .collect { case Some(value) => NumberConstant(value) }

  def value[$: P]: P[Value] = P(doubleQuotedStringConstant | singleQuotedStringConstant | numberConstant | field)./.log

  def comparisonOperator[$: P]: P[String] = P("==" | "!=" | ">" | ">=" | "<" | "<=").!

  def comparison[$: P]: P[Predicate] =
    P(value ~/ comparisonOperator ~ value).flatMap {
      case (left, "==", right) => Pass(IsEqual(left, right))
      case (left, "!=", right) => Pass(IsNotEqual(left, right))
      case (left, ">", right)  => Pass(IsGreaterThan(left, right))
      case (left, ">=", right) => Pass(IsGreaterThanOrEqual(left, right))
      case (left, "<", right)  => Pass(IsLessThan(left, right))
      case (left, "<=", right) => Pass(IsLessThanOrEqual(left, right))
      case (_, op, _)          => Fail(s"Unsupported comparison operator: $op. ")
    }

  def booleanOperator[$: P]: P[String] = P("||" | "&&").!

  def booleanExpression[$: P]: P[Predicate] =
    P(expression ~/ booleanOperator ~ expression).flatMap {
      case (left, "||", right) => Pass(Or(left, right))
      case (left, "&&", right) => Pass(And(left, right))
      case (_, op, _)          => Fail(s"Unsupported boolean operator: $op. ")
    }

  def expression[$: P]: P[Predicate] = P(
    booleanExpression | comparison
  )

  def predicate[$: P]: P[Predicate] = P(
    expression ~ End
  )
}
