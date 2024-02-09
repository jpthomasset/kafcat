package kafcat

sealed trait Predicate

sealed trait Value

case class Field(path: List[String])     extends Value
sealed trait Constant                    extends Value
case class StringConstant(value: String) extends Constant
case class NumberConstant(value: Double) extends Constant

case class IsEqual(left: Value, right: Value)              extends Predicate
case class IsNotEqual(left: Value, right: Value)           extends Predicate
case class IsGreaterThan(left: Value, right: Value)        extends Predicate
case class IsGreaterThanOrEqual(left: Value, right: Value) extends Predicate
case class IsLessThan(left: Value, right: Value)           extends Predicate
case class IsLessThanOrEqual(left: Value, right: Value)    extends Predicate
case class Or(left: Predicate, right: Predicate)           extends Predicate
case class And(left: Predicate, right: Predicate)          extends Predicate
