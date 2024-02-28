package kafcat.output

import kafcat.predicate.{Field, StringConstant}

type FormatElement = StringConstant | Field

case class Format(elements: List[FormatElement])




