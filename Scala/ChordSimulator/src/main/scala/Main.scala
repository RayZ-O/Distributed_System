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
        
        system.actorOf(Props(classOf[Peer],"111", 10, 0, 3), s"peer0")
        system.actorOf(Props(classOf[Peer],"111", 10, 1, 3), s"peer1")
        system.actorOf(Props(classOf[Peer],"111", 10, 3, 3), s"peer3")
        
        
        val inbox = Inbox.create(system)
        inbox.send(netBuilder, Start(numOfPeers))
    }
}
