package kafcat.format

sealed trait Value

case class Field(path: List[String])     extends Value
sealed trait Constant                    extends Value
case class StringConstant(value: String) extends Constant
case class NumberConstant(value: Double) extends Constant