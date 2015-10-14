import akka.actor.Actor

import scala.math
import scala.collection.mutable.ArrayBuffer

import com.roundeights.hasher.Implicits._

case class NodeAndSucc(id: Int, succ: Int)
case class CloestPreceding(id: Int)

class Peer(peerIp: String, peerPort: Int, m: Int) extends Actor {
    val ip = peerIp
    val port = peerPort
    val chordId = ip.sha1 % m
    val fingerTable= new ArrayBuffer[FingerEntry]
    var successor = -1
    var predecessor = -1

    def receive = {
        case CloestPreceding(id) =>
            val ns = cloestPrecedingFinger(id)
            sender ! NodeAndSucc(ns._1, ns._2)

        case _ =>
    }

    def buildTable() = {
        val modNum = math.pow(2, m).toInt
        var start = chordId + 1 % modNum
        for (i <- 1 to m) {
            val gtNode = findGtNode(start)
            val next = chordId + math.pow(2, i).toInt % modNum
            fingerTable += new FingerEntry((start, next), gtNode)
            start = next
        }
    }

    def findSuccessor(nodeId: Int): Int = {
        var node = id
        var succ = fingerTable[0].node
        while (node <= nodeId || nodeId > succ)) { // nodeId not in (n, n.succesor]
            val peer = context.ActorSelection(s"/user/peer$node")
            implicit val timeout = Timeout(400.milliseconds)
            val future = ask(peer, CloestPreceding(id))
            import context.dispatcher // implicit ExecutionContext for future
            future.onComplete {
                case Success(value) =>
                    value match {
                        case ns: NodeAndSucc =>
                            node = ns.id
                            succ = ns.succ
                        case _ =>
                    }
                case Failure(e) => e.printStackTrace
            }
        }
        succ
    }

    def cloestPrecedingFinger(nodeId: Int): Tuple2[Int, Int] = {
        for (i <- m to 1) {
            val gtNode = fingerTable[i].node
            if (id > gtNode && gtNode < nodeId) { // finger[i].node in (id, nodeId)
                return (gtNode, fingerTable[i].successor)
            }
        }
        (id, fingerTable[0].node)
    }
}

class FingerEntry(intv: Tuple2, gtNode: Int) {
    var interval = intv
    var node = gtNode // first node >= start
}
