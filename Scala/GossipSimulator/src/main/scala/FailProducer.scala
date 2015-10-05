import akka.actor.Actor
import scala.util.Random

case class Produce(numOfPeers: Int, percent: Double)
case object Failed

class FailProducer extends Actor {

    var numFailNodes = 0

    def produce(numOfPeers: Int, percent: Double) = {
        numFailNodes = (numOfPeers * percent).toInt
        val nodes = Random.shuffle((1 until numOfPeers).toList)
        for (i <- 0 until numFailNodes) {
            context.actorSelection(s"../peer${nodes(i)}") ! Failed
        }
    }

    def receive() = {
        case Produce(num, percent) =>
            if (percent > 0.0) {
                produce(num, percent)
            }
            context.actorSelection("../profiler") ! FailedNum(numFailNodes)

        case _ => // nothing to do
    }

}
