import akka.actor.{ Actor, ActorRef, ActorPath }

import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{ Random, Success, Failure }
import scala.collection.mutable.ArrayBuffer


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
    
    def receive = {
        case RemoteProcedureCall(procedure, args) =>         
            println(self.path.name + " receive from " + sender.path)
            handleRemoteCall(procedure, args, sender)

        case _ =>
    }
    
    
    
    def call(node: Node, procedure: Procedure.Value, args: List[Any], ret: Boolean): Node = {
        if (node.id == chordId) {
            callLocal(node, procedure, args)
        } else {
            callRemote(node, procedure, args, ret)
        }
    }
    
    def callLocal(node: Node, procedure: Procedure.Value, args: List[Any]): Node = {
        procedure match {  
            case GET_SUCCESSOR => successor
                
            case GET_PREDECESSOR => predecessor
                
            case SET_PREDECESSOR => 
                predecessor = args(0).asInstanceOf[Node]
                null
            
            case FIND_SUCCESSOR =>                 
                val succ = findSuccessor(args(0).asInstanceOf[Int])
                succ
                
            case FIND_PREDECESSOR =>                 
                val pred = findSuccessor(args(0).asInstanceOf[Int])
                pred
                
            case CLOSEST_PRECEDING_FINGER =>
                val node = findSuccessor(args(0).asInstanceOf[Int])
                node
            
            case UPDATE_FINGER_TABLE => 
                updateFingerTable(args(0).asInstanceOf[Node], args(1).asInstanceOf[Int])
                null
            
            case NOTIFY => 
                notify(args(0).asInstanceOf[Node])
                null
        }  
    }
    
    def callRemote(node: Node, procedure: Procedure.Value, args: List[Any], ret: Boolean): Node = {
        val nodeSelection = context.actorSelection(node.path)
        println("Calling " + procedure + " on " + node.path)
        if (ret) {
            implicit val timeout = Timeout(Duration(5000, "millis"))
            import context.dispatcher // implicit ExecutionContext for future
            val future = nodeSelection ? RemoteProcedureCall(procedure, args)
            Await.result(future, Duration(500, "millis")).asInstanceOf[Node]
        } else {
            nodeSelection ! RemoteProcedureCall(procedure, args)     
            null
        }        
    }
    
    
    def handleRemoteCall(procedure: Procedure.Value, args: List[Any], sender: ActorRef) = {
        procedure match {       
            case GET_SUCCESSOR => sender ! RemoteReply(successor)
                println("GET_SUCCESSOR Reply")
                
            case GET_PREDECESSOR => sender ! RemoteReply(predecessor)
                
            case SET_PREDECESSOR => predecessor = args(0).asInstanceOf[Node]
            
            case FIND_SUCCESSOR =>  
                println("FIND_SUCCESSOR Reply")
                val succ = findSuccessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(succ)
                
            case FIND_PREDECESSOR => 
                 println("FIND_PREDECESSOR Reply")
                val pred = findSuccessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(pred)
                
            case JOIN =>
                join(args(0).asInstanceOf[Int], args(1).asInstanceOf[ActorPath])
                printFingerTable()
                sender ! JoinComplete
                
            case CLOSEST_PRECEDING_FINGER =>
                val node = findSuccessor(args(0).asInstanceOf[Int])
                println("CLOSEST_PRECEDING_FINGER Reply")
                sender ! RemoteReply(node)
            
            case UPDATE_FINGER_TABLE => updateFingerTable(args(0).asInstanceOf[Node], args(1).asInstanceOf[Int])
            
            case NOTIFY => notify(args(0).asInstanceOf[Node])
        }        
    }
    
    // n is current number of nodes inside the network
    def join(id: Int, path: ActorPath) = {
       buildInterval()
       if (path != null) {
           initFingerTable(Node(id, path))
           updateOthers()// update others: move key in (predecessor, n] from successor
       } else {
           for (i <- 0 until m_exponent) {
               fingerTable(i).node = selfNode
           }
           successor = selfNode
           predecessor = selfNode
       }        
    }
    
    def initFingerTable(node: Node) = {  
        fingerTable(0).node = callRemote(node, FIND_SUCCESSOR, List(fingerTable(0).interval.start), true)
        successor = fingerTable(0).node
        predecessor = callRemote(successor, GET_PREDECESSOR, null, true)
        callRemote(successor, SET_PREDECESSOR, List(selfNode), false)
        for (i <- 0 until m_exponent - 1) {
            // TODO finger[i+1].start in [chordId, finger[i + 1].node]
            if (Interval(chordId, fingerTable(i).node.id, CLOSED, CLOSED).contains(fingerTable(i + 1).interval.start)) {
                fingerTable(i + 1).node = fingerTable(i).node
            } else {
                fingerTable(i + 1).node = callRemote(node, FIND_SUCCESSOR, List(fingerTable(i + 1).interval.start), true)
            }
        }
    }

    // tested
    def buildInterval() = {
        val modNum = math.pow(2, m_exponent).toInt
        var start = (chordId + 1) % modNum
        for (i <- 1 to m_exponent) {
            val next =  (chordId + math.pow(2, i).toInt) % modNum
            fingerTable += FingerEntry(start, next)
            start = next
        }
    }
    
    def printFingerTable() = {
        for (i <- 0 until m_exponent) {         
           println(s"finger[$i]:" + fingerTable(i).interval + " node:" +  fingerTable(i).node + 
                   " predecessor:" + predecessor + " successor:" + successor)
        }
    }
    
    def updateOthers() = {
        for (i <- 0 until m_exponent) {
            val pred = findPredecessor(chordId - math.pow(2, m_exponent).toInt)
            callRemote(pred, UPDATE_FINGER_TABLE, List(selfNode, i), false)
        }
    }
    
    def updateFingerTable(s: Node, i: Int) = {
        if (Interval(chordId, fingerTable(i).node.id, CLOSED, OPEN).contains(s.id)) {
            fingerTable(i).node = s
            callRemote(predecessor, UPDATE_FINGER_TABLE, List(s, i), false)
        }
    }
    
    // three following functions are for stabilization
    def stabilize() {
        val x = callRemote(successor, GET_PREDECESSOR, null, true)
        if (Interval(chordId, successor.id, OPEN, OPEN).contains(x.id)) {
            successor = x
        }
        callRemote(successor, NOTIFY, List(selfNode), false)
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
       
        // TODO succ id may equal to cur id
        while (!Interval(curNode.id, succNode.id, OPEN, CLOSED).contains(nodeId)) { // nodeId not in (n, n.succesor]
           curNode = call(curNode, CLOSEST_PRECEDING_FINGER, List(nodeId), true)
           succNode = call(curNode, GET_SUCCESSOR, null, true)
        }
        curNode
    }

    def cloestPrecedingFinger(nodeId: Int): Node = {
        for (i <- m_exponent - 1 to 0) {
            val gtNode = fingerTable(i).node
            // finger[i].node in (id, nodeId)
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
            "ID=" + id + " Path=" + path 
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
            val prefix = if (left== OPEN) "(" else "["
            val suffix = if (right== OPEN) ")" else "]"
            prefix + s"$start, $end" + suffix
        }
        
        def contains(n: Int): Boolean = {
            if (start <= end) {               
                (if (left== OPEN) n > start else n >= start) && (if (right== OPEN) n < end else n <= end)
            } else {
                (if (left== OPEN) n > start else n >= start) || (if (right== OPEN) n < end else n <= end)
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

