import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.Props

class Worker(master: ActorRef) extends Actor with ActorLogging{
    val workExecutor = context.watch(context.actorOf(Props[Miner], name = "miner"))
    master ! WorkerReady
    
    def receive = {
        case job: Job =>
            workExecutor ! job
        case result: Result =>
            master ! result
        case WorkComplete =>
            context.stop(self)
    }   
}