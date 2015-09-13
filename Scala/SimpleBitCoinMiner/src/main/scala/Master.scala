import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.ActorRef


class Master extends Actor {
    import scala.collection.mutable.HashSet
    var workers = new HashSet[ActorRef]()
    val workproducer = new WorkProducer("ruizhang;", 10, 3)
    def receive = {
          case WorkerReady => 
              workers + sender
              sender ! workproducer.nextJob

          case Result(text, value) => 
              println("\n" + text + " " + value + "\n")
              workers foreach { (worker: ActorRef) => worker ! WorkComplete }
              context.system.shutdown

          case _ => println("Unknown message")
    }
}

object Main extends App {
    val system = ActorSystem("BitcoinSystem")
    val master = system.actorOf(Props[Master], name = "master")
    val worker = system.actorOf(Props(classOf[Worker], master), name = "worker")
}
