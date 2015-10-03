import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import scala.concurrent.duration._

case object Tick
case class Gossip(content: String)
case class PushSum(s: Double, w: Double)

class Peer(idnum: Int, numOfPeers: Int, topology: String, master: ActorRef) extends Actor {

    val peerId = idnum
    // internal state
    var protocol = ""
    var info = ""  // use for information propagation
    var s = idnum + 1.0
    var w = 0.0
    // terminate condition variable
    var received = false
    var desolateCnt = 0
    var terminateCnt = 0
    // terminate threshold
    val gossipThreshold = 10
    val pushsumThreshold = 3
    val desolateThreshold = 20

    val neighbour = new Neighbour(idnum, numOfPeers, topology)

    import context.dispatcher
    val tick = context.system.scheduler.schedule(10.millis, 10.millis, self, Tick)

    def receive = {
        case Gossip(content) =>
            protocol = "gossip"
            info = content
            received = true
            context.become(gossip)

        case ps: PushSum =>
            protocol = "push-sum"
            s += ps.s
            w += ps.w
            s /= 2
            w /= 2
            context.actorSelection(neighbour.getName()) ! PushSum(s, w)
            context.become(pushsum)

        case _ => // do nothing
    }

    def gossip: Receive = {
        case Tick =>
            context.actorSelection(neighbour.getName()) ! Gossip(info)
            if (!received) {
                desolateCnt += 1
                checkTerminate(desolateCnt, desolateThreshold)
            } else {
                received = false;
                desolateCnt = 0;
            }

        case Gossip(content) =>
            received = true
            // println(s"Peer $peerId receive")
            terminateCnt += 1
            checkTerminate(terminateCnt, gossipThreshold)

        case _ => // do nothing
    }

    def checkTerminate(cnt: Int, threshold: Int) = {
        if (cnt >= threshold) {
            // println(s"[Stop] Peer $peerId terminate, info: $info, sum: ${s/w}")
            terminate()
        }
    }

    def pushsum: Receive = {
        case ps: PushSum =>
            val ratio = s / w
            s += ps.s
            w += ps.w
            val newRatio = s / w
            s /= 2
            w /= 2
            context.actorSelection(neighbour.getName()) ! PushSum(s, w)
            if ((newRatio - ratio).abs < 1e-10) {
                terminateCnt += 1
            } else {
                terminateCnt = 0
            }
            if (terminateCnt >= pushsumThreshold) {
                println(s"[Sum] Sum of range [1, $numOfPeers] ${s/w}")
                context.actorSelection(neighbour.getName()) ! Gossip("finished")
            }

        case Gossip(content) =>
            info = content
            terminateCnt = 0
            received = true
            context.become(gossip)

        case _ => //println("Unhandle message in pushsum")
    }

    def stop: Receive = {
        case _  => //println("Unhandle message in stop")
    }

    def terminate() = {
        master ! Stopped
        tick.cancel()
        context.become(stop)
    }
}



