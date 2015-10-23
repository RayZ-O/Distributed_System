import akka.actor.{ Actor, ActorRef }
import ChordUtil._
import Peer.StartFinder
import EndPoint.CLOSED
import EndPoint.OPEN

class Finder(rId: Int, rSender: ActorRef, rType: String) extends Actor {
    val requestId = rId
    val requestSender = rSender
    var curNode: NodeInfo = _

    override def receive = {
        case StartFinder(node) =>
            println("finder start")
            context.actorSelection(node.path) ! ChordRequst.ClosestPrecedingFinger(requestId)

        case ChordReply.ClosestPrecedingFinger(node) =>
            println("finder receive closest preceding finger")
            curNode = node
            context.actorSelection(node.path) ! ChordRequst.GetSuccessor

        case ChordReply.GetSuccessor(succ) =>
            println("finder receive get successor")
            if (Interval(curNode.id, succ.id, OPEN, CLOSED).contains(requestId)) {
                println("finder found right node")
                rType match {
                    case "successor" => requestSender ! ChordReply.FindSuccessor(succ)
                    case "predecessor" => requestSender ! ChordReply.FindPredecessor(curNode)

                    case _ => println("[ERROR] unhandle request in finder")
                }
                context.stop(self)
            } else {
                context.actorSelection(curNode.path) ! ChordRequst.ClosestPrecedingFinger(requestId)
            }

        case _ => println("unhandle message in predecessor finder")
    }
}
