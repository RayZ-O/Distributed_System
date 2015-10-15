import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Inbox

object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("ChordSystem")
        if (args.length < 2) {
            println("usage: sbt \"run [num of peers][num of request]\"")
        }
        val numOfPeers = args(0).toInt
        val numOfReqs = args(1).toInt     
        val netBuilder = system.actorOf(Props[NetworkBuilder])
        val m = 10
        for (i <- 0 until numOfPeers) {
            system.actorOf(Props(classOf[Peer], m), s"peer$i")
        }           
        
        val inbox = Inbox.create(system)
        inbox.send(netBuilder, Start(numOfPeers))
    }
}
