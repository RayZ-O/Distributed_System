import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Inbox
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer


object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("GossipSystem")

        val threshold = 3
        val numOfPeers = 1000

        val master = system.actorOf(Props(classOf[Master], numOfPeers), "master")

        var peers = new ArrayBuffer[ActorRef]
        for (i <- 1 to numOfPeers) {
            peers += system.actorOf(Props(classOf[Peer], i, threshold, master), s"peer$i")
        }
        println(s"Created $numOfPeers Peers")
        val networkBuilder = new NetworkBuilder()
        networkBuilder.build("line", peers)
        println("Build network complete")
        val inbox = Inbox.create(system)
//        inbox.send(peers(0), Gossip("Hi from main"))
        inbox.send(peers(0), PushSum(0, 0))
    }
}
