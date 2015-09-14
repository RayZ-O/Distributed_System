import scala.math.BigInt

sealed trait Message
case object WorkComplete extends Message
case class ReadyToWork(numWorks: Int) extends Message
case class Job(baseStr: String, start: BigInt, end: BigInt, prefix: String) extends Message
case class Work(tasks: List[Job]) extends Message
case object WorkDone extends Message
case class Result(str:String)
