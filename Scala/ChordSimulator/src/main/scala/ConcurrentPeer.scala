import akka.actor.{ Actor, ActorRef, Props, ActorLogging, Cancellable }

import scala.concurrent.duration._
import scala.util.Random

import com.roundeights.hasher.Implicits._
import ChordUtil._
import ChordUtil.EndPoint.{ CLOSED, OPEN }

class ConcurrentPeer(chordid: Int, noRequests: Int) extends Actor with ActorLogging {
    import ConcurrentPeer._
    import context.dispatcher
    // peer info
    val chordId = chordid
    val path = self.path.toString
    import scala.collection.mutable.ArrayBuffer
    val fingerTable = ArrayBuffer.empty[FingerEntry]
    var successor: NodeInfo  = _
    var predecessor: NodeInfo = _
    val selfNode = NodeInfo(chordId, self.path)

    var requestCount = 0
    val numRequests = noRequests
    val hopCounter = context.actorSelection("/user/hopcounter")
    var requestTick: Cancellable = _
    var stabilizedTick: Cancellable = _

    // helper for initialization
    var guider: NodeInfo = _
    var randomIdxs: Vector[Int] = _
    var curIdx = 0

    override def receive =  {
        case ChordRequst.Join(node) =>
            guider = node
            predecessor = null
            join()

        case ChordReply.FindSuccessor(node, num) =>
            successor = node
            fingerTable(0).node = node
            context.become(complete)
            context.parent ! ChordReply.JoinComplete
            randomIdxs = Random.shuffle((1 until m_exponent).toVector)
            stabilizedTick = context.system.scheduler.schedule(100.millisecond, 300.millisecond, self, StabilizedTick)

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- start]")
    }

    def complete: Receive = {
        case ChordRequst.FindSuccessor(id) => find(id, sender, -1)

        case ChordRequst.GetPredecessor => sender ! ChordReply.GetPredecessor(predecessor)

        case ChordRequst.GetSuccessor => sender ! ChordReply.GetSuccessor(successor)

        case ChordRequst.ClosestPrecedingFinger(id) =>
            sender ! ChordReply.ClosestPrecedingFinger(cloestPrecedingFinger(id))

        case ChordRequst.Notify(node) =>
            if (predecessor == null ||
              Interval(predecessor.id, chordId, OPEN, OPEN).contains(node.id)) {
                predecessor = node
            }

        case StabilizedTick =>
            context.actorSelection(successor.path) ! ChordRequst.GetPredecessor
            if (curIdx == randomIdxs.length) {
                randomIdxs = Random.shuffle((1 until m_exponent).toVector)
                curIdx = 0
            }
            val i = randomIdxs(curIdx)
            curIdx += 1
            find(fingerTable(i).interval.start, self, i)

        case ChordReply.GetPredecessor(node) =>
            if (node != null &&
              Interval(chordId, successor.id, OPEN, OPEN).contains(node.id)) {
                successor = node
                fingerTable(0).node = node
            }
            context.actorSelection(successor.path) ! ChordRequst.Notify(selfNode)



        case ChordReply.FixFinger(node, idx) => fingerTable(idx).node = node

        case ChordRequst.Start =>
            requestTick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)

        case Tick =>
            val randomStr = Random.nextString(10)
            val id = (BigInt(randomStr.sha1.hex, 16) % ringSize).toInt
            self ! ChordRequst.FindSuccessor(id)

        case ChordReply.FindSuccessor(succ, num) =>
            hopCounter ! Hops(num)
            requestCount += 1
            if (requestCount >= numRequests) {
                requestTick.cancel()
                hopCounter ! Finish
            }

        case ChordRequst.Print => printFingerTable()

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- complete]")
    }

    def join() = {
        buildInterval()
        if (guider.path != null) {
            context.actorSelection(guider.path) ! ChordRequst.FindSuccessor(fingerTable(0).interval.start)
        } else {
            successor = selfNode
            fingerTable(0).node = selfNode
            context.become(complete)
            context.parent ! ChordReply.JoinComplete
            randomIdxs = Random.shuffle((1 until m_exponent).toVector)
            stabilizedTick = context.system.scheduler.schedule(100.millisecond, 300.millisecond, self, StabilizedTick)
        }
    }

    def buildInterval() = {
        var start = (chordId + 1) % ringSize
        for (i <- 1 to m_exponent) {
        val next =  (chordId + math.pow(2, i).toInt) % ringSize
            fingerTable += FingerEntry(start, next)
            start = next
        }
    }

    def printFingerTable() = {
        if (fingerTable.length > 0) {
            for (i <- 0 until m_exponent) {
               println(s"$chordId--finger[$i]:" + fingerTable(i).interval + " node:" +  fingerTable(i).node +
                       " predecessor:" + predecessor + " successor:" + successor)
            }
        }
    }

    def find(rid: Int, sender: ActorRef, index: Int) = {
        if (Interval(chordId, successor.id, OPEN, CLOSED).contains(rid)) {
            if (index < 0) {
                sender ! ChordReply.FindSuccessor(successor, 1)
            } else {
                sender ! ChordReply.FixFinger(successor, index)
            }
        } else {
            val node = cloestPrecedingFinger(rid)
            val finder = context.actorOf(Props(classOf[Finder], rid, sender, "successor"))
            if (index < 0) {
                finder ! ChordRequst.StartFinder(node)
            } else {
                finder ! ChordRequst.FixFinger(node, index)
            }
        }
    }

    def cloestPrecedingFinger(nodeId: Int): NodeInfo = {
        for (i <- m_exponent - 1 to 0 by -1) {
            val gtNode = fingerTable(i).node
            // finger[i].node in (id, nodeId)
            if (gtNode != null &&
              Interval(chordId, nodeId, OPEN, OPEN).contains(gtNode.id)) {
                return gtNode
            }
        }
        selfNode
    }
}

object ConcurrentPeer  {
    var m_exponent = 0
    var ringSize = 0

    case object StabilizedTick;

    def setMExponent(m: Int) = {
        m_exponent = m
        ringSize = math.pow(2, m_exponent).toInt
    }

    def apply(id:Int, nr: Int): ConcurrentPeer = {
        new ConcurrentPeer(id, nr)
    }
}
