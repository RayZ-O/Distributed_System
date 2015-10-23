import akka.actor.{ Actor, ActorRef, ActorLogging }
import ChordUtil._
import Peer.StartFinder
import EndPoint.CLOSED
import EndPoint.OPEN

class Finder(rId: Int, rSender: ActorRef, rType: String) extends Actor with ActorLogging {
    val requestId = rId
    val requestSender = rSender
    var curNode: NodeInfo = _
    var numHops = 1

    override def receive = {
        case StartFinder(node) =>
            context.actorSelection(node.path) ! ChordRequst.ClosestPrecedingFinger(requestId)

        case ChordReply.ClosestPrecedingFinger(node) =>
            curNode = node
            context.actorSelection(curNode.path) ! ChordRequst.GetSuccessor

        case ChordReply.GetSuccessor(succ) =>
            numHops += 1
            if (Interval(curNode.id, succ.id, OPEN, CLOSED).contains(requestId)) {
                rType match {
                    case "successor" => requestSender ! ChordReply.FindSuccessor(succ, numHops)
                    case "predecessor" => requestSender ! ChordReply.FindPredecessor(curNode)
                    case t => log.warning(s"Unregconized request type $t in finder")
                }
                context.stop(self)
            } else {
                context.actorSelection(curNode.path) ! ChordRequst.ClosestPrecedingFinger(requestId)
            }

        case msg => log.warning(s"Unhandle message $msg in finder of ${context.parent.path.name} when finding $requestId")
    }
}
