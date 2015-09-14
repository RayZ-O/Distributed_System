import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.SmallestMailboxRoutingLogic

class Boss(master: ActorRef) extends Actor with ActorLogging{
    val numWorkers = 2 * Runtime.getRuntime().availableProcessors()
    log.info(s"Detected $numWorkers available processors")
    var router = {
        val routees = Vector.fill(numWorkers) {
            var r = context.actorOf(Props[Miner])
            context.watch(r)
            ActorRefRoutee(r)
        }
        Router(SmallestMailboxRoutingLogic(), routees)
    }

    master ! ReadyToWork(numWorkers)

    def receive = {
        case work: Work =>
            work.tasks foreach { t => router.route(t, context.self) }
        case WorkDone =>
            master ! ReadyToWork(1)
            master ! WorkDone
        case WorkComplete =>
            context.stop(self)
        case res: Result =>
            master ! res
        case Terminated(a) =>
            router = router.removeRoutee(a)
            val r = context.actorOf(Props[Miner])
            context.watch(r)
            router = router.addRoutee(r)
    }   
}