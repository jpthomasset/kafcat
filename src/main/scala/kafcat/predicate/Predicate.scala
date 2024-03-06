package kafcat.predicate

import kafcat.format.Value

sealed trait Predicate

case class IsEqual(left: Value, right: Value)     extends Predicate
case class IsNotEqual(left: Value, right: Value)  extends Predicate
// case class IsGreaterThan(left: Value, right: Value)        extends Predicate
// case class IsGreaterThanOrEqual(left: Value, right: Value) extends Predicate
// case class IsLessThan(left: Value, right: Value)           extends Predicate
// case class IsLessThanOrEqual(left: Value, right: Value)    extends Predicate
case class Or(left: Predicate, right: Predicate)  extends Predicate
case class And(left: Predicate, right: Predicate) extends Predicate
