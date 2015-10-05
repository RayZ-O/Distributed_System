import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Inbox
import scala.math

object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("GossipSystem")
        if (args.length < 3) {
            println("usage: sbt \"run [num of peers][topology][algorithm]\"")
        }
        var numOfPeers = args(0).toInt
        val topology = args(1)
        val algorithm = args(2)
        val percent = if (args.length == 4) args(3).toDouble else 0.0


        if (topology == "3D" || topology == "imp3D") {
            numOfPeers = math.pow(math.round(math.cbrt(numOfPeers)), 3).toInt
        }

        val profiler = system.actorOf(Props(classOf[Profiler], numOfPeers, algorithm), "profiler")
        val failproducer = system.actorOf(Props[FailProducer], "failproducer")

        for (i <- 0 until numOfPeers) {
            system.actorOf(Props(classOf[Peer], i, numOfPeers, topology), s"peer$i")
        }
        println(s"Created $numOfPeers Peers")

        val inbox = Inbox.create(system)
        inbox.send(failproducer, Produce(numOfPeers, percent))

    }
}
