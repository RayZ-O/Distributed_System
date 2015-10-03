import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Inbox
import scala.math

object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("GossipSystem")
        if (args.length != 3) {
            println("usage: sbt \"run [num of peers][topology][algorithm]\"")
        }
        var numOfPeers = args(0).toInt
        val topology = args(1)
        val algorithm = args(2)

        if (topology == "3D" || topology == "imp3D") {
            numOfPeers = math.pow(math.round(math.cbrt(numOfPeers)), 3).toInt
        }

        val master = system.actorOf(Props(classOf[Master], numOfPeers), "master")

        for (i <- 0 until numOfPeers) {
            system.actorOf(Props(classOf[Peer], i, numOfPeers, topology, master), s"peer$i")
        }
        println(s"Created $numOfPeers Peers")

        val inbox = Inbox.create(system)
        val peer0 = system.actorFor("/user/peer0") // actorFor is deprecated
        inbox.send(master, Start)
        algorithm match {
            case "gossip" => inbox.send(peer0, Gossip("Hi from main"))
            case "push-sum" => inbox.send(peer0, PushSum(0, 1))
            case _ => throw new NotImplementedError("Algorithm not implemented")
        }
    }
}
