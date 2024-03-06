package kafcat.format

import fastparse._
import fastparse.NoWhitespace._

object FormatParser {

  private def fieldName[$: P]: P[String]       = P(CharsWhile(c => c.isLetterOrDigit || c == '_').!)
  private def fieldPath[$: P]: P[List[String]] = P(fieldName ~ ("." ~ fieldPath).?).map(x => x._1 :: x._2.toList.flatten)
  def field[$: P]: P[Field]            = P(fieldPath.map(Field(_)))

  private def fieldPlaceholder[$: P]: P[Field]            = P("%" ~ fieldPath.map(Field(_)))

  private def constant[$: P]: P[StringConstant] = P( CharsWhile(c => c != '%' && c != '\n').! ).map(StringConstant.apply)

  private def formatElement[$: P]: P[FormatElement] = P( constant | fieldPlaceholder )

  def formatParser[$: P]: P[Format] = P( formatElement.rep(1) ).map(xs => Format.apply(xs.toList))

}
