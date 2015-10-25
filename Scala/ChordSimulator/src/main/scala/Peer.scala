import akka.actor.{ Actor, ActorRef, ActorPath, Props, ActorLogging, Cancellable }
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Random
import scala.math.BigInt

import com.roundeights.hasher.Implicits._
import ChordUtil._
import ChordUtil.EndPoint.{ CLOSED, OPEN }

class Peer(chordid: Int, noRequests: Int) extends Actor with ActorLogging {
    import Peer._
    // peer info
    val chordId = chordid
    val path = self.path.toString
    import scala.collection.mutable.ArrayBuffer
    val fingerTable= ArrayBuffer.empty[FingerEntry]
    var successor: NodeInfo  = _
    var predecessor: NodeInfo = _
    val selfNode = NodeInfo(chordId, self.path)
    var requestCount = 0
    val numRequests = noRequests
    val hopCounter = context.actorSelection("/user/hopcounter")
    var tick: Cancellable = _

    // helper for initialization
    var guider: NodeInfo = _
    var initTableIndex = 0
    var updateTableIndex = 0



    override def receive = {
        case ChordRequst.Join(node: NodeInfo) =>
            guider = node
            join()

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- start]")
    }

    def complete: Receive = {
        case ChordRequst.FindSuccessor(id) => find(id, "successor", sender)

        case ChordRequst.FindPredecessor(id) => find(id, "predecessor", sender)

        case ChordRequst.GetSuccessor => sender ! ChordReply.GetSuccessor(successor)

        case ChordRequst.GetPredecessor => sender ! ChordReply.GetPredecessor(predecessor)

        case ChordRequst.SetPredecessor(node) => predecessor = node

        case ChordRequst.UpdateFingerTable(node, i) => updateFingerTable(node, i)

        case ChordRequst.ClosestPrecedingFinger(id) =>
            sender ! ChordReply.ClosestPrecedingFinger(cloestPrecedingFinger(id))

        case ChordRequst.Start =>
            import context.dispatcher
            tick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)

        case Tick =>
            val randomStr = Random.nextString(10)
            val id = (BigInt(randomStr.sha1.hex, 16) % ringSize).toInt
            self ! ChordRequst.FindSuccessor(id)

        case ChordReply.FindSuccessor(succ, num) =>
            hopCounter ! Hops(num)
            requestCount += 1
            if (requestCount >= numRequests) {
                tick.cancel()
                hopCounter ! Finish
            }

        case ChordRequst.Print => printFingerTable()

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- complete]")
    }

    def initNode: Receive = {
        case ChordReply.FindSuccessor(node, num) =>
            successor = node
            fingerTable(0).node = node
            context.actorSelection(successor.path) ! ChordRequst.GetPredecessor

        case ChordReply.GetPredecessor(pred) =>
            predecessor = pred
            context.actorSelection(successor.path) ! ChordRequst.SetPredecessor(selfNode)
            context.become(initFingerTable)
            self ! InitTable(0)

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- initNode]")
    }

    def initFingerTable: Receive = {
        case InitTable(i) =>
            if (i >= m_exponent - 1) {
                context.become(updateOthers)
                self ! UpdateTable(0)
            } else {
                // finger[i+1].start in [chordId, finger[i].node)
                if (Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN)
                        contains fingerTable(i + 1).interval.start) {
                    fingerTable(i + 1).node = fingerTable(i).node
                    self ! InitTable(i + 1)
                } else {
                    if (successor.id == predecessor.id &&   // only one existed node in the network
                      fingerTable(i + 1).interval.start != guider.id) {
                        fingerTable(i + 1).node = selfNode
                        self ! InitTable(i + 1)
                    } else {
                        initTableIndex = i + 1
                        context.actorSelection(guider.path) !
                        ChordRequst.FindSuccessor(fingerTable(i + 1).interval.start)
                    }
                }
            }

        case ChordReply.FindSuccessor(node, num) =>
            fingerTable(initTableIndex).node = node
            self ! InitTable(initTableIndex)

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- initFingerTable]")
    }

    def updateOthers: Receive = {
        case UpdateTable(i) =>
            if (i >= m_exponent) {
               context.become(complete)
               context.parent ! ChordReply.JoinComplete
            } else {
                val k = chordId - math.pow(2, i).toInt
                val predSlotId = if (k >= 0) k else k + ringSize
                if (predSlotId == predecessor.id) {
                    context.actorSelection(predecessor.path) ! ChordRequst.UpdateFingerTable(selfNode, i)
                    self ! UpdateTable(i + 1)
                } else {
                    updateTableIndex = i
                    find(predSlotId, "predecessor", self)
                }
            }

        case ChordReply.FindPredecessor(node) =>
            context.actorSelection(node.path) ! ChordRequst.UpdateFingerTable(selfNode, updateTableIndex)
            self ! UpdateTable(updateTableIndex + 1)

        case ChordRequst.ClosestPrecedingFinger(id) =>
            sender ! ChordReply.ClosestPrecedingFinger(cloestPrecedingFinger(id))

        case ChordRequst.UpdateFingerTable(node, i) => // nothing to do

        case msg => log.warning(s"Unhandle message $msg in peer$chordId [state <- updateOthers]")

    }


    def find(rid: Int, rType: String, sender: ActorRef) = {
        if (Interval(chordId, successor.id, OPEN, CLOSED).contains(rid)) {
            rType match {
                case "successor" => sender ! ChordReply.FindSuccessor(successor, 1)
                case "predecessor" => sender ! ChordReply.FindPredecessor(selfNode)
            }
        } else {
            val node = cloestPrecedingFinger(rid)
            val finder = context.actorOf(Props(classOf[Finder], rid, sender, rType))
            finder ! ChordRequst.StartFinder(node)
        }
    }

    def join() = {
       buildInterval()
       if (guider.path != null) {
           context.become(initNode)
           context.actorSelection(guider.path) ! ChordRequst.FindSuccessor(fingerTable(0).interval.start)
           // move key in (predecessor, n] from successor
       } else {
           for (i <- 0 until m_exponent) {
               fingerTable(i).node = selfNode
           }
           successor = selfNode
           predecessor = selfNode
           context.become(complete)
           context.parent ! ChordReply.JoinComplete
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

    def updateFingerTable(s: NodeInfo, i: Int) = {
        // s in [chordId, finger[i].node)
        if (Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN).contains(s.id) &&
         !Interval(chordId, fingerTable(i).interval.start, OPEN, OPEN).contains(s.id) &&
         s.id != chordId) {
            fingerTable(i).node = s
            if (i == 0) {
                successor = s
            }
            context.actorSelection(predecessor.path) ! ChordRequst.UpdateFingerTable(s, i)
        }
    }

    def cloestPrecedingFinger(nodeId: Int): NodeInfo = {
        for (i <- m_exponent - 1 to 0 by -1) {
            val gtNode = fingerTable(i).node
            // finger[i].node in (id, nodeId)
            if (Interval(chordId, nodeId, OPEN, OPEN).contains(gtNode.id)) {
                return gtNode
            }
        }
        selfNode
    }
}

object Peer  {
    var m_exponent = 0
    var ringSize = 0

    case class InitTable(index: Int)
    case class UpdateTable(index: Int)

    def setMExponent(m: Int) = {
        m_exponent = m
        ringSize = math.pow(2, m_exponent).toInt
    }

    def apply(id:Int, nr: Int): Peer = {
        new Peer(id, nr)
    }
}

