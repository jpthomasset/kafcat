package kafcat.format

type FormatElement = StringConstant | Field

case class Format(elements: List[FormatElement])




