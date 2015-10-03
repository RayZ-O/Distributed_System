import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import scala.concurrent.duration._

case object Tick
case object Convergent
case class Gossip(content: String)
case class PushSum(s: Double, w: Double)

class Peer(idnum: Int, numOfPeers: Int, topology: String, master: ActorRef) extends Actor {

    val peerId = idnum
    // internal state
    var info = ""  // use for information propagation
    var s = idnum + 1.0
    var w = 1.0
    var ratio = 0.0
    // terminate condition variable
    var desolateCnt = 0
    var terminateCnt = 0
    // terminate threshold
    val gossipThreshold = 10
    val pushsumThreshold = 10
    val desolateThreshold = 20

    val neighbour = new Neighbour(idnum, numOfPeers, topology)

    import context.dispatcher
    val tick = context.system.scheduler.schedule(10.millis, 10.millis, self, Tick)

    def receive = {
        case Gossip(content) =>
            info = content
            terminateCnt += 1
            context.become(gossip)

        case ps: PushSum =>
            s += ps.s
            w += ps.w
            ratio = s / w
            context.become(pushsum)

        case _ => // do nothing
    }

    def gossip: Receive = {
        case Tick =>
            context.actorSelection(neighbour.getName()) ! Gossip(info)

        case Gossip(content) =>
            // println(s"Peer $peerId receive")
            terminateCnt += 1
            checkTerminate(terminateCnt, gossipThreshold)

        case Convergent =>
            desolateCnt += 1
            checkTerminate(desolateCnt, desolateThreshold)

        case _ => // do nothing
    }

    def pushsum: Receive = {
        case Tick =>
            s /= 2
            w /= 2
            context.actorSelection(neighbour.getName()) ! PushSum(s, w)

        case ps: PushSum =>
            s += ps.s
            w += ps.w
            val newRatio = s / w
            terminateCnt = if ((newRatio - ratio).abs < 1e-10) terminateCnt + 1 else 0
            if (terminateCnt >= pushsumThreshold) {
                // println(s"[Sum] Sum of range [1, $numOfPeers] ${s/w}")
                terminate()
            }
            ratio = newRatio

        case Convergent =>
            desolateCnt += 1
            checkTerminate(desolateCnt, desolateThreshold)

        case _ => //println("Unhandle message in pushsum")
    }

    def stop: Receive = {
        case go: Gossip => sender ! Convergent

        case ps: PushSum => sender ! Convergent

        case _  => //println("Unhandle message in stop")
    }

    def checkTerminate(cnt: Int, threshold: Int) = {
        if (cnt >= threshold) {
            // println(s"[Stop] Peer $peerId terminate, info: $info, average: ${s/w}")
            terminate()
        }
    }

    def terminate() = {
        master ! Stopped
        tick.cancel()
        context.become(stop)
    }
}



