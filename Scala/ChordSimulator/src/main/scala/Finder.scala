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
            context.actorSelection(node.path) ! ChordRequst.ClosestPrecedingFinger(requestId)

        case ChordReply.ClosestPrecedingFinger(node) =>
            curNode = node
            context.actorSelection(curNode.path) ! ChordRequst.GetSuccessor

        case ChordReply.GetSuccessor(succ) =>
            if (Interval(curNode.id, succ.id, OPEN, CLOSED).contains(requestId)) {
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
