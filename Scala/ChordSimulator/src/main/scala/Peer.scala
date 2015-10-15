import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorPath 
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Random
import scala.util.Success
import scala.util.Failure
import scala.collection.mutable.ArrayBuffer

case class NodeAndSucc(id: Int, succ: Tuple2[Int, ActorPath])
case class CloestPreceding(id: Int)
case object Join

class Peer(peerIp: String, peerPort: Int, id: Int, m: Int) extends Actor {
    
    val ip = peerIp
    val port = peerPort
    val chordId = id
    val fingerTable= new ArrayBuffer[FingerEntry]
    var successor: ActorPath  = _
    var predecessor: ActorPath  = _

    def receive = {
//        case CloestPreceding(id) =>
//            val ns = cloestPrecedingFinger(id)
//            sender ! NodeAndSucc(ns._1, ns._2)
        case Join =>
            buildInterval()
            println("chordId: " + chordId)
            fingerTable foreach { x => println(x.interval) }

        case _ =>
    }
    
    // n is current number of nodes inside the network
    def join(n: Int) = {
       buildInterval()
       if (n != 0) {
           val random = Random.nextInt(n)       	   
//           initFingerTable(random)
           // update others: move key in (predecessor, n] from successor
       } else {
           for (i <- 0 until m) {
               fingerTable(i).nodeId = chordId
               fingerTable(i).nodePath = self.path
           }
           predecessor = self.path
       }        
    }
    
//    def initFingerTable(n: Int) = {
//        val node = context.actorSelection(s"../peer$n")
//        successor = node.findSuccessor(fingerTable(0).interval._1)
//        fingerTable(0).nodeAddress = successor
//        fingerTable(0).nodeId = chordId
//        predecessor = successor.predecessor
//        for (i <- 0 until m - 1) {
//            val start = fingerTable(i + 1).interval._1
//            if (start < fingerTable(i).nodeId && start > chordId) {
//                fingerTable(i + 1).nodeId = fingerTable(i).nodeId
//                fingerTable(i + 1).nodeAddress = fingerTable(i).nodeAddress
//            } else {
//                successor = node.findSuccessor(fingerTable(i + 1).interval._1)
//                // fingerTable(i + 1).nodeId = TODO
//                fingerTable(i + 1).nodeAddress = successor
//            }
//        }
//    }

    // tested
    def buildInterval() = {
        val modNum = math.pow(2, m).toInt
        var start = (chordId + 1) % modNum
        for (i <- 1 to m) {
            val next =  (chordId + math.pow(2, i).toInt) % modNum
            fingerTable += new FingerEntry(start, next)
            start = next
        }
    }

    def findSuccessor(nodeId:Int): Tuple2[Int, ActorPath] = {
        var curNodeId = chordId
        var succNode = ( fingerTable(0).nodeId, fingerTable(0).nodePath )
        while (curNodeId <= nodeId || nodeId > succNode._1) { // nodeId not in (n, n.succesor]
            implicit val timeout = Timeout(Duration(500, "mills"))
            import context.dispatcher // implicit ExecutionContext for future
            val future = peer ? CloestPreceding(chordId)       
            future.onComplete {
                case Success(value) =>
                    value match {
                        case ns: NodeAndSucc =>
                            curNodeId = ns.id
                            succNode = ns.succ
                            
                        case _ =>
                    }
                case Failure(e) => e.printStackTrace
            }
        }
        succNode
    }

    def cloestPrecedingFinger(nodeId: Int): Tuple2[Int, ActorPath ] = {
        for (i <- m - 1 to 0) {
            val gtNodeId = fingerTable(i).nodeId
            if (chordId > gtNodeId && gtNodeId < nodeId) { // finger[i].node in (id, nodeId)
                return (gtNodeId, fingerTable(i).nodePath)
            }
        }
        (chordId, self.path)
        
    }
}

class FingerEntry(intv: Tuple2[Int,Int]) {
    var interval = intv
    var nodeId: Int = _
    var nodePath: ActorPath = _ // first node >= start
}
