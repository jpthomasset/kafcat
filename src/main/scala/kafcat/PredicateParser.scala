package kafcat

import fastparse._
import fastparse.JavaWhitespace._

object PredicateParser {
  def fieldName[$: P]: P[String]       = P(CharsWhile(c => c.isLetterOrDigit || c == '_').!)
  def fieldPath[$: P]: P[List[String]] = P(fieldName ~ ("." ~ fieldPath).?).map(x => x._1 :: x._2.toList.flatten)
  def field[$: P]: P[Field]            = P(fieldPath.map(Field(_)))

  def stringConstant[$: P]: P[StringConstant] = P("\"" ~ CharsWhile(_ != '"').! ~ "\"").map(StringConstant(_))

  def numberConstant[$: P]: P[NumberConstant] =
    P(CharsWhile(c => c.isDigit || c == '.').!)
      .map(_.toDoubleOption)
      .collect { case Some(value) => NumberConstant(value) }

  def value[$: P]: P[Value] = P(stringConstant | numberConstant | field)

  def isEqual[$: P]: P[IsEqual]                           = P(value ~ "==" ~ value).map((left, right) => IsEqual(left, right))
  def isNotEqual[$: P]: P[IsNotEqual]                     = P(value ~ "!=" ~ value).map((left, right) => IsNotEqual(left, right))
  def isGreaterThan[$: P]: P[IsGreaterThan]               = P(value ~ ">" ~ value).map((left, right) => IsGreaterThan(left, right))
  def isGreaterThanOrEqual[$: P]: P[IsGreaterThanOrEqual] =
    P(value ~ ">=" ~ value).map((left, right) => IsGreaterThanOrEqual(left, right))
  def isLessThan[$: P]: P[IsLessThan]                     = P(value ~ "<" ~ value).map((left, right) => IsLessThan(left, right))
  def isLessThanOrEqual[$: P]: P[IsLessThanOrEqual]       =
    P(value ~ "<=" ~ value).map((left, right) => IsLessThanOrEqual(left, right))

  def or[$: P]: P[Or]   = P(expression ~ "||" ~ expression).map((left, right) => Or(left, right))
  def and[$: P]: P[And] = P(expression ~ "&&" ~ expression).map((left, right) => And(left, right))

  def expression[$: P]: P[Predicate] = P(
    isEqual | isNotEqual | isGreaterThan | isGreaterThanOrEqual | isLessThan | isLessThanOrEqual | or | and
  )

  def predicate[$: P]: P[Predicate] = P(
    (or | and | isEqual | isNotEqual | isGreaterThan | isGreaterThanOrEqual | isLessThan | isLessThanOrEqual | or | and) ~ End
  )
}
