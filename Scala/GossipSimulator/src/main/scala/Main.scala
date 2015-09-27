import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Inbox
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer


object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("GossipSystem")
        if (args.length != 3) {
            println("usage: sbt \"run [num of peers][topology][algorithm]\"")
        }
        val threshold = 10
        val numOfPeers = args(0).toInt
        val topology = args(1)
        val algorithm = args(2)

        val master = system.actorOf(Props(classOf[Master], numOfPeers), "master")

        var peers = new ArrayBuffer[ActorRef]
        for (i <- 1 to numOfPeers) {
            peers += system.actorOf(Props(classOf[Peer], i, threshold, master), s"peer$i")
        }
        println(s"Created $numOfPeers Peers")

        val networkBuilder = new NetworkBuilder()
        networkBuilder.build(topology, peers)
        println("Build network complete")

        val inbox = Inbox.create(system)
        inbox.send(master, Start)
        algorithm match {
            case "gossip" => inbox.send(peers(0), Gossip("Hi from main"))
            case "push-sum" => inbox.send(peers(0), PushSum(0, 1))
            case _ => throw new NotImplementedError("Algorithm not implemented")
        }
    }
}
