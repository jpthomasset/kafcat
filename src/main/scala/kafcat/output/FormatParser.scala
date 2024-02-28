package kafcat.output

import fastparse._
import fastparse.NoWhitespace._
import kafcat.predicate.{Field, StringConstant}
import kafcat.predicate.PredicateParser

object FormatParser {

  private def field[$: P]: P[Field]            = P("%" ~ PredicateParser.fieldPath.map(Field(_)))

  private def constant[$: P]: P[StringConstant] = P( CharsWhile(c => c != '%' && c != '\n').! ).map(StringConstant.apply)

  private def formatElement[$: P]: P[FormatElement] = P( constant | field )

  def formatParser[$: P]: P[Format] = P( formatElement.rep(1) ).map(xs => Format.apply(xs.toList))

}
