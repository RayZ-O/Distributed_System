import akka.actor.Actor
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.util.Random
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await

case object Tick
case object Confirm
case class Neighbour(neighbour: ActorRef)
case class Gossip(content: String)
case object Stopped


class Peer(idnum: Int, threshold: Int) extends Actor {

    var state = ""
    var receiveCnt = 0;
    val id = idnum
    import scala.collection.mutable.ArrayBuffer
    var neighbours = new ArrayBuffer[ActorRef]

    import context.dispatcher
    val tick = context.system.scheduler.schedule(500.millis, 500.millis, self, Tick)

    def receive = {
        case Gossip(content) =>
            println(s"[Start] Peer $id knows the roumor")
            state = content
            sender ! Confirm
            context.become(gossip)

        case Neighbour(ne) =>
            neighbours += ne

        case _ => // do nothing
    }

    def gossip: Receive = {
        case Tick =>
            propogate()

        case Gossip(content) =>
            // println(s"Peer $id receive")
            sender ! Confirm
            if (receiveCnt < threshold) {
                receiveCnt += 1
            } else {
                println(s"[Stop] Peer $id stopped")
                tick.cancel()
                context.become(stop)
            }

        case Neighbour(ne) =>
            neighbours += ne
    }

    def stop: Receive = {
        case g: Gossip=>
            sender ! Stopped

        case Neighbour(ne) =>
            throw new NotImplementedError("Add neighbours to stopped peer not yet implemented")
    }

    def propogate(): Unit = {
        val i = Random.nextInt(neighbours.length)
        implicit val timeout = Timeout(400.milliseconds)
        val future = ask(neighbours(i), Gossip(state))
        import context.dispatcher // implicit ExecutionContext for future
        future.onComplete {
            case Success(value) =>
                value match {
                    case Confirm => //println("Gossip success")
                    case Stopped =>
                        neighbours.remove(i)
                        if (neighbours.length == 0) {
                            context.become(stop)
                            println(s"[Stop] Peer $id is isolated")
                        }
                }
            case Failure(e) => e.printStackTrace
        }
    }

}


