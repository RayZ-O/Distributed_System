import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorPath
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{ Random, Success, Failure }
import scala.collection.mutable.ArrayBuffer
import com.roundeights.hasher.Implicits._


class Peer(m: Int) extends Actor {
    val m_exponent = m
    val path = self.path.toStringWithoutAddress
    println(path)
    val chordId = (BigInt(path.sha1.hex, 16) % BigInt(2).pow(m_exponent)).toInt
    
    import Peer._
    val selfNode = Node(chordId, self.path)
    val fingerTable= new ArrayBuffer[FingerEntry]
    var successor: Node  = _
    var predecessor: Node = _
    
    def receive = {
        case RemoteProcedureCall(procedure, args) => handleRemoteCall(procedure, args, sender)

        case _ =>
    }
    
    import Peer.Procedure._
    
    def callRemote(node: Node, procedure: Procedure.Value, args: List[Any]): Node = {
        val nodeSelection = context.actorSelection(node.path)
        implicit val timeout = Timeout(Duration(500, "mills"))
        import context.dispatcher // implicit ExecutionContext for future
        val future = nodeSelection ? RemoteProcedureCall(procedure, args)      
        Await.result(future, timeout.duration).asInstanceOf[Node]
    }
    
    
    def handleRemoteCall(procedure: Procedure.Value, args: List[Any], sender: ActorRef) = {
        procedure match {       
            case GET_SUCCESSOR => sender ! RemoteReply(successor)                
                
            case GET_PREDECESSOR => sender ! RemoteReply(predecessor)
                
            case SET_PREDECESSOR => predecessor = args(0).asInstanceOf[Node]
            
            case FIND_SUCCESSOR =>  
                val succ = findSuccessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(succ)
                
            case FIND_PREDECESSOR => 
                val pred = findSuccessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(pred)
                
            case CLOSEST_PRECEDING_FINGER =>
                val node = findSuccessor(args(0).asInstanceOf[Int])
                sender ! RemoteReply(node)
                
            case JOIN => join(args(0).asInstanceOf[Int])
            
            case UPDATE_FINGER_TABLE => updateFingerTable(args(0).asInstanceOf[Node], args(1).asInstanceOf[Int])
            
            case NOTIFY => notify(args(0).asInstanceOf[Node])
        }        
    }
    
    // n is current number of nodes inside the network
    def join(n: Int) = {
       buildInterval()
       if (n != 0) {
           val random = Random.nextInt(n)          
//           initFingerTable(random)
           updateOthers()// update others: move key in (predecessor, n] from successor
       } else {
           for (i <- 0 until m_exponent) {
               fingerTable(i).node = selfNode
           }
           predecessor = selfNode
       }        
    }
    
    def initFingerTable(node: Node) = {  
        fingerTable(0).node = callRemote(node, FIND_SUCCESSOR, List(fingerTable(0).interval.start))
        predecessor = callRemote(successor, GET_PREDECESSOR, null)
        callRemote(successor, SET_PREDECESSOR, List(selfNode))
        for (i <- 0 until m_exponent - 1) {
            // TODO finger[i+1].start in [chordId, finger[i + 1].node]
            if (Interval(chordId, fingerTable(i).node.id).contains(fingerTable(i + 1).interval.start)) {
                fingerTable(i + 1).node = fingerTable(i).node
            } else {
                fingerTable(i + 1).node = callRemote(node, FIND_SUCCESSOR, List(fingerTable(i + 1).interval.start))
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
    
    def updateOthers() = {
        for (i <- 0 until m_exponent) {
            val pred = findPredecessor(chordId - math.pow(2, m_exponent).toInt)
            callRemote(pred, UPDATE_FINGER_TABLE, List(selfNode, i))
        }
    }
    
    def updateFingerTable(s: Node, i: Int) = {
        if (Interval(chordId, fingerTable(i).node.id).contains(s.id)) {
            fingerTable(i).node = s
            callRemote(predecessor, UPDATE_FINGER_TABLE, List(s, i))
        }
    }
    
    // three following functions are for stabilization
    def stabilize() {
        val x = callRemote(successor, GET_PREDECESSOR, null)
        if (Interval(chordId, successor.id).contains(x.id)) {
            successor = x
        }
        callRemote(successor, NOTIFY, List(selfNode))
    }
    
    def notify(node: Node) = {
        if (predecessor == null || Interval(predecessor.id, chordId).contains(node.id)) {
            predecessor = node            
        }
    }
    
    def fixFinger() {
        val i = Random.nextInt(fingerTable.length)
        fingerTable(i).node = findSuccessor(fingerTable(i).interval.start)
    }

    def findSuccessor(nodeId: Int): Node = {
        val node = findPredecessor(nodeId)
        successor
    }
    
    def findPredecessor(nodeId: Int): Node = {
        var curNode = selfNode
        var succNode = successor
        val modNum = math.pow(2, m_exponent).toInt
        // TODO succ id may equal to cur id
        while (!Interval(curNode.id, (succNode.id + 1) % modNum).contains(nodeId)) { // nodeId not in (n, n.succesor]
           curNode = callRemote(curNode, CLOSEST_PRECEDING_FINGER, List(nodeId))
           succNode = callRemote(curNode, GET_SUCCESSOR, null)
        }
        curNode
    }

    def cloestPrecedingFinger(nodeId: Int): Node = {
        for (i <- m_exponent - 1 to 0) {
            val gtNode = fingerTable(i).node
            // finger[i].node in (id, nodeId)
            if (Interval(chordId, nodeId).contains(gtNode.id)) {
                return gtNode
            }
        }
        selfNode
    }
}

object Peer { 
    case class RemoteProcedureCall(procedure: Procedure.Value, args: List[Any])
    case class RemoteReply(node: Node)

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
    
    class Interval(istart: Int, iend: Int) {
        val start = istart
        val end = iend
        override def toString() = {
            s"[$start, $end]"
        }
        
        def contains(n: Int): Boolean = {
            if (start == end) {        // the interval is the whole ring
                true
            } else if (start < end) {   // e.g. (2, 5)
                n > start && n < end
            } else {                    // e.g. (7, 3)
                n > end || n < start 
            }
        }
    }
    
    object Interval {
        def apply(start: Int, end: Int): Interval = {
            val interval = new Interval(start, end)
            interval
        }
    }
    
    class FingerEntry(start: Int, end: Int) {
        var interval = Interval(start, end)
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

    
    def apply(m_exponent: Int): Peer = {
        val peer = new Peer(m_exponent)
        peer
    }
}

