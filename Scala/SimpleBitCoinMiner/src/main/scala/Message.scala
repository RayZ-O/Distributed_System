import akka.actor.ActorRef

case object WorkerReady
case object WorkComplete
case class Job(jobId: Int, baseStr: String, suffixLen: Int, prefix: String)
case class Result(text: String, value: String)