import akka.actor.{ Actor, ActorRef, ActorPath }

import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{ Random, Success, Failure }
import scala.collection.mutable.ArrayBuffer

case object Print;

class Peer(id: Int) extends Actor {
    val chordId = id
    val path = self.path.toString

    // import helper classes and enums
    import Peer._
    import Peer.Procedure._
    import Interval.EndPoint.CLOSED
    import Interval.EndPoint.OPEN

    val selfNode = Node(chordId, self.path)
    val fingerTable= new ArrayBuffer[FingerEntry]
    var successor: Node  = _
    var predecessor: Node = _

    override def receive = {
        case RemoteProcedureCall(procedure, args) =>
            println(self.path.name + " receive from " + sender.path)
            handleRemoteCall(procedure, args, sender)

        case Print =>
            printFingerTable()

        case _ =>
    }



    def call(node: Node, procedure: Procedure.Value, args: List[Any], ret: Boolean): Node = {
        if (node.id == chordId) {
            callLocal(procedure, args)
        } else {
            callRemote(node, procedure, args, ret)
        }
    }

    def callLocal(procedure: Procedure.Value, args: List[Any]): Node = {
//        println(self.path.name + " call local " + procedure)
        procedure match {
            case GET_SUCCESSOR => successor

            case GET_PREDECESSOR => predecessor

            case SET_SUCCESSOR =>
                successor = args(0).asInstanceOf[Node]
                null

            case SET_PREDECESSOR =>
                predecessor = args(0).asInstanceOf[Node]
                null

            case FIND_SUCCESSOR =>
                findSuccessor(args(0).asInstanceOf[Int])

            case FIND_PREDECESSOR =>
                findPredecessor(args(0).asInstanceOf[Int])

            case CLOSEST_PRECEDING_FINGER =>
                cloestPrecedingFinger(args(0).asInstanceOf[Int])

            case UPDATE_FINGER_TABLE =>
                updateFingerTable(args(0).asInstanceOf[Node], args(1).asInstanceOf[Int])
                null

            case NOTIFY =>
                notify(args(0).asInstanceOf[Node])
                null

            case _ => throw new NotImplementedError("Procedure call not implemented")
        }
    }

    def callRemote(node: Node, procedure: Procedure.Value, args: List[Any], ret: Boolean): Node = {
        val nodeSelection = context.actorSelection(node.path)
//      println("Calling " + procedure + " on " + node.path)
        if (ret) {
            try {
                implicit val timeout = Timeout(Duration(5000, "millis"))
                import context.dispatcher // implicit ExecutionContext for future
                val future = nodeSelection ? RemoteProcedureCall(procedure, args)
                val reply = Await.result(future, Duration(1000, "millis")).asInstanceOf[RemoteReply]
                reply.node
            } catch {
                case _ : Throwable =>
                    println(procedure + "Timeout")
                    throw new Exception()
                    null
            }
        } else {
            nodeSelection ! RemoteProcedureCall(procedure, args)
            null
        }
    }


    def handleRemoteCall(procedure: Procedure.Value, args: List[Any], sender: ActorRef) = {
        procedure match {
            case GET_SUCCESSOR => sender ! RemoteReply(successor)

            case GET_PREDECESSOR => sender ! RemoteReply(predecessor)

            case SET_SUCCESSOR => successor = args(0).asInstanceOf[Node]

            case SET_PREDECESSOR => predecessor = args(0).asInstanceOf[Node]

            case FIND_SUCCESSOR =>
                val succ = findSuccessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(succ)

            case FIND_PREDECESSOR =>
                val pred = findPredecessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(pred)

            case CLOSEST_PRECEDING_FINGER =>
                val node = cloestPrecedingFinger(args(0).asInstanceOf[Int])
                sender ! RemoteReply(node)

            case JOIN =>
                join(args(0).asInstanceOf[Int], args(1).asInstanceOf[ActorPath])
                Thread.sleep(2000)
                sender ! JoinComplete

            case UPDATE_FINGER_TABLE => updateFingerTable(args(0).asInstanceOf[Node], args(1).asInstanceOf[Int])

            case NOTIFY => notify(args(0).asInstanceOf[Node])

            case _ => throw new NotImplementedError("Remote procedure call not implemented")
        }
    }

    // n is current number of nodes inside the network
    def join(id: Int, path: ActorPath) = {
       buildInterval()
       if (path != null) {
           initFingerTable(Node(id, path))
           println(s"$chordId Init table complete")
           printFingerTable()
           updateOthers()
           // move key in (predecessor, n] from successor
       } else {
           for (i <- 0 until m_exponent) {
               fingerTable(i).node = selfNode
           }
           successor = selfNode
           predecessor = selfNode
       }
    }

    def initFingerTable(node: Node) = {
        fingerTable(0).node = call(node, FIND_SUCCESSOR, List(fingerTable(0).interval.start), true)
        successor = fingerTable(0).node
        predecessor = call(successor, GET_PREDECESSOR, null, true)
        call(successor, SET_PREDECESSOR, List(selfNode), false)
        call(predecessor, SET_SUCCESSOR, List(selfNode), false)
        for (i <- 0 until m_exponent - 1) {
            // finger[i+1].start in [chordId, finger[i].node)
            if (Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN).contains(fingerTable(i + 1).interval.start)) {
                fingerTable(i + 1).node = fingerTable(i).node
            } else {
                fingerTable(i + 1).node = call(node, FIND_SUCCESSOR, List(fingerTable(i + 1).interval.start), true)
            }
        }
    }

    // tested
    def buildInterval() = {
        val ringSize = math.pow(2, m_exponent).toInt
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

    def updateOthers() = {
        val ringSize = math.pow(2, m_exponent).toInt
        for (i <- 0 until m_exponent) {
            val k = chordId - math.pow(2, i).toInt
            val predSlotId = if (k >= 0) k else k + ringSize
            val pred = if (predSlotId == predecessor.id) predecessor else findPredecessor(predSlotId)
            println(s"update others i = $i, k = $k, predSlotId = $predSlotId, predNode = ${pred.id}")
            call(pred, UPDATE_FINGER_TABLE, List(selfNode, i), false)
        }
    }

    def updateFingerTable(s: Node, i: Int) = {
        println(s"node $chordId call update finger table")
        if (s.id != chordId &&
            Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN).contains(s.id)) {
            println(Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN) + " contains " + s.id)
            fingerTable(i).node = s
            if (i == 0) {
                successor = s
            }
            call(predecessor, UPDATE_FINGER_TABLE, List(s, i), false)
        }
    }

    // three following functions are for stabilization
    def stabilize() {
        val x = call(successor, GET_PREDECESSOR, null, true)
        if (Interval(chordId, successor.id, OPEN, OPEN).contains(x.id)) {
            successor = x
        }
        call(successor, NOTIFY, List(selfNode), false)
    }

    def notify(node: Node) = {
        if (predecessor == null || Interval(predecessor.id, chordId, OPEN, OPEN).contains(node.id)) {
            predecessor = node
        }
    }

    def fixFinger() {
        val i = Random.nextInt(fingerTable.length)
        fingerTable(i).node = findSuccessor(fingerTable(i).interval.start)
    }

    def findSuccessor(nodeId: Int): Node = {
        val node = findPredecessor(nodeId)
        val succNode = call(node, GET_SUCCESSOR, null, true)
        succNode
    }

    def findPredecessor(nodeId: Int): Node = {
        var curNode = selfNode
        var succNode = successor
        while (!Interval(curNode.id, succNode.id, OPEN, CLOSED).contains(nodeId)) { // nodeId not in (n, n.succesor]
            println(chordId + " find " + nodeId + " in " + Interval(curNode.id, succNode.id, OPEN, CLOSED))
           curNode = call(curNode, CLOSEST_PRECEDING_FINGER, List(nodeId), true)
           succNode = call(curNode, GET_SUCCESSOR, null, true)
        }
        curNode
    }

    def cloestPrecedingFinger(nodeId: Int): Node = {
        for (i <- m_exponent - 1 to 0 by -1) {
            val gtNode = fingerTable(i).node
            // finger[i].node in [id, nodeId)
            if (Interval(chordId, nodeId, CLOSED, OPEN).contains(gtNode.id)) {

                return gtNode
            }
        }
        selfNode
    }
}

object Peer  {
    case class RemoteProcedureCall(procedure: Procedure.Value, args: List[Any])
    case class RemoteReply(node: Node)
    var m_exponent = 0

    // enum for remote procedure call type
    object Procedure extends Enumeration {
        type Procedure = Value
        val GET_SUCCESSOR,
            GET_PREDECESSOR,
            SET_SUCCESSOR,
            SET_PREDECESSOR,
            FIND_SUCCESSOR,
            FIND_PREDECESSOR,
            CLOSEST_PRECEDING_FINGER,
            JOIN,
            UPDATE_FINGER_TABLE,
            NOTIFY = Value
    }

    class Node(nodeId: Int, nodePath: ActorPath) {
        val id = nodeId
        val path= nodePath
        override def toString() = {
            "ID=" + id //+ " Path=" + path
        }
    }

    object Node {
        def apply(nodeId: Int, nodePath: ActorPath): Node = {
            val node = new Node(nodeId, nodePath)
            node
        }
    }

    import Interval.EndPoint
    import Interval.EndPoint.CLOSED
    import Interval.EndPoint.OPEN

    class Interval(istart: Int, iend: Int, left: EndPoint.Value, right: EndPoint.Value) {
        val start = istart
        val end = iend
        val leftEnd = left
        val rightEnd = right

        override def toString() = {
            val prefix = if (left == OPEN) "(" else "["
            val suffix = if (right == OPEN) ")" else "]"
            prefix + s"$start, $end" + suffix
        }

        def contains(n: Int): Boolean = {
            if (start < end) {
                (if (left == OPEN) n > start else n >= start) && (if (right == OPEN) n < end else n <= end)
            } else {
                (if (left == OPEN) n > start else n >= start) || (if (right == OPEN) n < end else n <= end)
            }

        }
    }

    object Interval {
        def apply(start: Int, end: Int, left: EndPoint.Value, right: EndPoint.Value): Interval = {
            val interval = new Interval(start, end, left, right)
            interval
        }

        object EndPoint extends Enumeration {
             type EndPoint = Value
             val OPEN, CLOSED = Value
        }
    }

    class FingerEntry(start: Int, end: Int) {
        var interval = Interval(start, end, CLOSED, OPEN)
        var node: Node= _ // first node >= start
        override def toString() = {
            "Interval: " + interval + "\nNode: " + node
        }
    }

    object FingerEntry {
        def apply(start: Int, next: Int): FingerEntry = {
            val entry = new FingerEntry(start, next)
            entry
        }
        def apply(start: Int, next: Int, node: Node): FingerEntry = {
            val entry = new FingerEntry(start, next)
            entry.node = node
            entry
        }
    }

    def setMExponent(m: Int) = {
        m_exponent = m
    }

    def apply(id:Int, m: Int): Peer = {
        val peer = new Peer(id)
        peer
    }
}

