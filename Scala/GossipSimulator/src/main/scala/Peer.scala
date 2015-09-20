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
case object Stopped
case class Neighbour(neighbour: ActorRef)
case class Gossip(content: String)
case class PushSum(s: Double, w: Double)

class Peer(idnum: Int, threshold: Int) extends Actor {

    val peerId = idnum
    import scala.collection.mutable.ArrayBuffer
    var neighbours = new ArrayBuffer[ActorRef]

    var info = ""  // use for information propagation
    var s = idnum.toDouble
    var w = 0.0
    var ratio = 0.0
    var terminateCnt = 0;

    import context.dispatcher
    val tick = context.system.scheduler.schedule(500.millis, 500.millis, self, Tick)

    def receive = {
        case Gossip(content) =>
            println(s"[Start] Peer $peerId knows the roumor")
            info = content
            sender ! Confirm
            context.become(gossip)

        case ps: PushSum =>
            s += ps.s
            w += ps.w
            ratio = s / w
            sender ! Confirm
            context.become(pushsum)

        case Neighbour(ne) =>
            neighbours += ne

        case _ => // do nothing
    }

    def gossip: Receive = {
        case Tick =>
            propagate(Gossip(info))

        case Gossip(content) =>
            // println(s"Peer $peerId receive")
            sender ! Confirm
            if (terminateCnt < threshold) {
                terminateCnt += 1
            } else {
                println(s"[Stop] Peer $peerId stopped")
                tick.cancel()
                context.become(stop)
            }

        case Neighbour(ne) =>
            neighbours += ne
    }

    def pushsum: Receive = {
        case Tick =>
            propagate(PushSum(s / 2, w / 2))
            s /= 2
            w /= 2
            val newRatio = s.toDouble / w
            // println(s"Peer $peerId sum estimate $newRatio")
            if ((newRatio - ratio).abs < 1e-10) {
                terminateCnt += 1
            }
            if (terminateCnt >= 3) {
                println(s"[Stop] Peer $peerId stopped, sum is $ratio")
                tick.cancel()
                context.become(stop)
            }
            ratio = newRatio

        case ps: PushSum =>
            s += ps.s
            w += ps.w
            sender ! Confirm

        case _ => // do nothing
    }

    def stop: Receive = {
        case g: Gossip =>
            sender ! Stopped

        case ps: PushSum =>
            sender ! Stopped

        case Neighbour(ne) =>
            throw new NotImplementedError("Add neighbours to stopped peer not yet implemented")
    }

    def propagate(msg: Any): Unit = {
        val i = Random.nextInt(neighbours.length)
        implicit val timeout = Timeout(400.milliseconds)
        val future = ask(neighbours(i), msg)
        import context.dispatcher // implicit ExecutionContext for future
        future.onComplete {
            case Success(value) =>
                value match {
                    case Confirm => //println("Gossip success")
                    case Stopped =>
                        neighbours.remove(i)
                        if (neighbours.length == 0) {
                            context.become(stop)
                            println(s"[Stop] Peer $peerId is isolated")
                        }
                }
            case Failure(e) => e.printStackTrace
        }
    }

}


