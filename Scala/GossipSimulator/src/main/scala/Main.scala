import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Inbox
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer


object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("GossipSystem")

        val threshold = 10
        val numOfPeers = 10

        var peers = new ArrayBuffer[ActorRef]
        for (i <- 0 until numOfPeers) {
            peers += system.actorOf(Props(classOf[Peer], i, threshold), s"peer$i")
        }

        val networkBuilder = new NetworkBuilder()
        networkBuilder.build("ring", peers)

        val inbox = Inbox.create(system)
        inbox.send(peers(0), Gossip("Hi from main"))

    }
}
