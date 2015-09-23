import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import scala.concurrent.duration._

case object Tick
case object Stopped
case class Neighbour(neighbour: ActorRef)
case class Gossip(content: String)
case class PushSum(s: Double, w: Double)

class Peer(idnum: Int, threshold: Int, master: ActorRef) extends Actor {

    val peerId = idnum
    import scala.collection.mutable.ArrayBuffer
    var neighbours = new ArrayBuffer[ActorRef]

    var received = false;
    var info = ""  // use for information propagation
    var s = idnum.toDouble
    var w = 1.0
    var terminateCnt = 0;

    import context.dispatcher
    val tick = context.system.scheduler.schedule(500.millis, 500.millis, self, Tick)

    def receive = {
        case Gossip(content) =>
            // println(s"[Start] Peer $peerId knows the roumor")
            info = content
            context.become(gossip)

        case ps: PushSum =>
            updateState(ps.s, ps.w)
            propagate(PushSum(s, w))
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
            if (terminateCnt < threshold) {
                terminateCnt += 1
            } else {
                println(s"[Stop] Peer $peerId stopped")
                master ! Stopped
                terminate()
            }

        case Stopped =>
            neighbours -= sender
            if (neighbours.length == 0) {
                master ! Stopped
                println(s"[Stop] Peer $peerId is isolated")
                terminate()
            }

        case _ => // do nothing
    }

    def pushsum: Receive = {
        case Tick =>
            // if (!received) {
            //     updateState(0, 0)
            //     self ! PushSum(s, w)
            // }
            // received = false

        case ps: PushSum =>
            received = true
            val ratio = s / w
            updateState(ps.s, ps.w)
            val newRatio = s / w
            propagate(PushSum(s, w))
            //println(s"Peer $peerId sum estimate $newRatio")
            if ((newRatio - ratio).abs < 1e-10) {
                terminateCnt += 1
            } else {
                terminateCnt = 0
            }
            if (terminateCnt >= 3) {
                println(s"[Stop] Peer $peerId stopped, sum is $ratio")
                master ! Stopped
                terminate()
            }


        case _ => // do nothing
    }

    def stop: Receive = {
        case g: Gossip => sender ! Stopped
        case _  => // do nothing
    }

    def propagate(msg: Any): Unit = {
        val i = Random.nextInt(neighbours.length)
        neighbours(i) ! msg
    }

    def terminate() = {
        tick.cancel()
        context.become(stop)
    }

    def updateState(ns: Double, nw: Double) {
        s += ns
        w += nw
        s /= 2
        w /= 2
    }
}


