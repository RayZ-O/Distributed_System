import akka.actor.{ Actor, ActorRef }
import scala.math.BigInt

case class Hops(num: Int)
case object Finish

class HopCounter(noNodes: Int) extends Actor {
    val numNodes = noNodes
    var hopSum = 0.0
    var hopCount = 0.0
    var finishedCount = 0

    override def receive = {
        case Hops(num) =>
            hopSum += num
            hopCount += 1
        case Finish =>
            finishedCount += 1
            if (finishedCount == numNodes) {
                println(s"The average number of hops = ${hopSum / hopCount}")
                context.system.terminate()
            }

        case _ =>
    }
}
