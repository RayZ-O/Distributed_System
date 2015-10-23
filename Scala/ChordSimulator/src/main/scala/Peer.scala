import akka.actor.{ Actor, ActorRef, ActorPath, Props }
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.TimeoutException
import ChordUtil._
import ChordUtil.EndPoint.CLOSED
import ChordUtil.EndPoint.OPEN
import Peer._

case object Print;


class Peer(id: Int) extends Actor {
    val chordId = id
    val path = self.path.toString


    // peer info
    val selfNode = NodeInfo(chordId, self.path)
    val fingerTable= new ArrayBuffer[FingerEntry]
    var successor: NodeInfo  = _
    var predecessor: NodeInfo = _
    // helper for initialization
    var guider: NodeInfo = _
    var initTableIndex = 0
    var updateTableIndex = 0

    override def receive = {
        case Join(node: NodeInfo) =>
            println(s"$chordId receive join")
            guider = node
            join()

//        case _ =>
    }

    def complete: Receive = {
        case ChordRequst.FindSuccessor(id) =>
            println(s"$chordId receive find successor")
            find(id, "successor", sender)

        case ChordRequst.FindPredecessor(id) =>
            println(s"$chordId receive find predecessor")
            find(id, "predecessor", sender)

        case ChordRequst.GetSuccessor =>
            sender ! ChordReply.GetSuccessor(successor)

        case ChordRequst.GetPredecessor =>
            sender ! ChordReply.GetPredecessor(predecessor)

        case ChordRequst.SetPredecessor(node) =>
            println(s"$chordId receive set predecessor")
            predecessor = node

        case ChordRequst.UpdateFingerTable(node, i) =>
            println(s"$chordId receive update finger table")
            updateFingerTable(node, i)

        case ChordRequst.ClosestPrecedingFinger(id) =>
            println(s"$chordId receive closest preceding finger")
            sender ! ChordReply.ClosestPrecedingFinger(cloestPrecedingFinger(id))

        case Print => printFingerTable()
    }

    def initNode: Receive = {
        case ChordReply.FindSuccessor(node) =>
            println(s"$chordId receive find successor reply")
            successor = node
            fingerTable(0).node = node
            context.actorSelection(successor.path) ! ChordRequst.GetPredecessor

        case ChordReply.GetPredecessor(pred) =>
            println(s"$chordId receive get predecessor reply")
            predecessor = pred
            context.actorSelection(successor.path) ! ChordRequst.SetPredecessor(selfNode)
            context.become(initFingerTable)
            self ! InitTable(0)
    }

    def initFingerTable: Receive = {
        case InitTable(i) =>
            if (i >= m_exponent - 1) {
                context.become(updateOthers)
                println(s"$chordId start update others")
                self ! UpdateTable(0)
            } else {
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
                        context.actorSelection(guider.path) ! ChordRequst.FindSuccessor(fingerTable(i + 1).interval.start)
                    }
                }
            }

        case ChordReply.FindSuccessor(node) =>
            println(s"$chordId receive init table find successor")
            fingerTable(initTableIndex).node = node
            self ! InitTable(initTableIndex)

//        case _ =>
    }

    def updateOthers: Receive = {
        case UpdateTable(i) =>
            println(s"$chordId receive update others $i")
            printFingerTable()
            if (i >= m_exponent) {
                context.become(complete)
                println(s"$chordId complete")
                context.parent ! JoinComplete
            } else {
                val k = chordId - math.pow(2, i).toInt
                val predSlotId = if (k >= 0) k else k + ringSize
                println(s"pred slot = $predSlotId")
                if (predSlotId == predecessor.id) {
                    context.actorSelection(predecessor.path) ! ChordRequst.UpdateFingerTable(selfNode, i)
                    self ! UpdateTable(i + 1)
                } else {
                    updateTableIndex = i
//                    find(predSlotId, "predecessor", self)
                    self ! ChordRequst.FindPredecessor(predSlotId)
                }
            }

        case ChordReply.FindPredecessor(node) =>
            println(s"$chordId receive update table find predecessor")
            context.actorSelection(node.path) ! ChordRequst.UpdateFingerTable(selfNode, updateTableIndex)
            self ! UpdateTable(updateTableIndex + 1)

        case ChordRequst.FindPredecessor(id) =>
            println(s"$chordId receive find predecessor")
            find(id, "predecessor", sender)

        case _ => println(s"[ERROR] $chordId Unhandle message in update others")

    }


    def find(rid: Int, rType: String, sender: ActorRef) = {
        if (Interval(chordId, successor.id, OPEN, CLOSED).contains(id)) {
            rType match {
                case "successor" => sender ! ChordReply.FindSuccessor(successor)
                case "predecessor" => sender ! ChordReply.FindPredecessor(selfNode)
            }
        }
        val node = cloestPrecedingFinger(id)
        val finder = context.actorOf(Props(classOf[Finder], id, sender, rType))
        finder ! StartFinder(node)
    }

    def join() = {
       buildInterval()
       println(s"$chordId build interval complete")
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
           context.parent ! JoinComplete
       }
    }

    // tested
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
        if (s.id != chordId &&
            Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN).contains(s.id)) {
//            println(Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN) + " contains " + s.id)
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

    case class StartFinder(node: NodeInfo)
    case class InitTable(index: Int)
    case class UpdateTable(index: Int)

    class FingerEntry(start: Int, end: Int) {
        var interval = Interval(start, end, CLOSED, OPEN)
        var node: NodeInfo= _ // first node >= start
        override def toString() = {
            "Interval: " + interval + "\nNode: " + node
        }
    }

    object FingerEntry {
        def apply(start: Int, next: Int): FingerEntry = {
            val entry = new FingerEntry(start, next)
            entry
        }
        def apply(start: Int, next: Int, node: NodeInfo): FingerEntry = {
            val entry = new FingerEntry(start, next)
            entry.node = node
            entry
        }
    }

    def setMExponent(m: Int) = {
        m_exponent = m
        ringSize = math.pow(2, m_exponent).toInt
    }

    def apply(id:Int, m: Int): Peer = {
        val peer = new Peer(id)
        peer
    }
}

