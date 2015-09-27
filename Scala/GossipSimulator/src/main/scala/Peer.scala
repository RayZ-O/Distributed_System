import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import scala.concurrent.duration._

case object Tick
case object Confirm
case object Stopped
case class Neighbour(neighbour: ActorRef)
case class Gossip(content: String)
case class PushSum(s: Double, w: Double)

class Peer(idnum: Int, threshold: Int, master: ActorRef) extends Actor {

    val peerId = idnum
    import scala.collection.mutable.ArrayBuffer
    var neighbours = new ArrayBuffer[ActorRef]

    var info = ""  // use for information propagation
    var s = idnum.toDouble
    var w = 0.0
    var terminateCnt = 0;

    import context.dispatcher
    val tick = context.system.scheduler.schedule(10.millis, 10.millis, self, Tick)

    def receive = {
        case Gossip(content) =>
            info = content
            context.become(gossip)

        case ps: PushSum =>
            tick.cancel()
            s += ps.s
            w += ps.w
            s /= 2
            w /= 2
            val i = Random.nextInt(neighbours.length)
            neighbours(i) ! PushSum(s, w)
            context.become(pushsum)

        case Neighbour(ne) =>
            neighbours += ne

        case _ => // do nothing
    }

    def gossip: Receive = {
        case Tick =>
            val i = Random.nextInt(neighbours.length)
            neighbours(i) ! Gossip(info)

        case Gossip(content) =>
            // println(s"Peer $peerId receive")
            if (terminateCnt < threshold) {
                terminateCnt += 1
            } else {
                println(s"[Stop] Peer $peerId stopped")
                neighbours foreach { _ ! Stopped }
                master ! Stopped
                terminate()
            }

        case Stopped =>
            neighbours -= sender
            if (neighbours.length == 0) {
                println(s"[Stop] Peer $peerId is isolated")
                master ! Stopped
                terminate()
            }

        case _ => // do nothing
    }

    def pushsum: Receive = {
        case ps: PushSum =>
            val ratio = s / w
            s += ps.s
            w += ps.w
            val newRatio = s / w
            s /= 2
            w /= 2
            val i = Random.nextInt(neighbours.length)
            neighbours(i) ! PushSum(s, w)
            if ((newRatio - ratio).abs < 1e-10) {
                terminateCnt += 1
            } else {
                terminateCnt = 0
            }
            if (terminateCnt >= 3) {
                println(s"[Stop] Peer $peerId stopped, sum is $ratio")
                neighbours foreach { _ ! Stopped }
                master ! Stopped
                context.become(stop)
            }

        case Stopped =>
            println(s"[Stop] Peer $peerId stopped, sum is ${s/w}")
            neighbours foreach { _ ! Stopped }
            master ! Stopped
            context.become(stop)

        case _ => //println("Unhandle message in pushsum")
    }

    def stop: Receive = {
        case g: Gossip =>
            sender ! Stopped
        case _  => //println("Unhandle message in stop")
    }

    def terminate() = {
        tick.cancel()
        context.become(stop)
    }
}



