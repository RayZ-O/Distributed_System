import akka.actor.ActorPath

object ChordUtil {

    case object Tick

    // request messages
    object ChordRequst {
        // if sendHopCount = true, the number of hops that have to be traversed
        // to deliever a message will be sent to HopCounter
        case class FindSuccessor(id: Int)
        case class FindPredecessor(id: Int)
        case class ClosestPrecedingFinger(id: Int)
        case object GetSuccessor
        case object GetPredecessor
        case class SetPredecessor(node: NodeInfo)
        case class UpdateFingerTable(node: NodeInfo, index: Int)
        case class Notify(node: NodeInfo)
        case class StartFinder(node: NodeInfo)
        case class FixFinger(node: NodeInfo, index: Int)
        case class Join(guider: NodeInfo)
        case object Start
        case object Print
    }

    // reply messages
    object ChordReply {
        case class FindSuccessor(node: NodeInfo, numHops: Int)
        case class FindPredecessor(node: NodeInfo)
        case class ClosestPrecedingFinger(node: NodeInfo)
        case class GetSuccessor(node: NodeInfo)
        case class GetPredecessor(node: NodeInfo)
        case class FixFinger(node: NodeInfo, index: Int)
        case object JoinComplete
    }

    class FingerEntry(start: Int, end: Int) {
        import EndPoint.{ CLOSED, OPEN }
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

    class NodeInfo(nodeId: Int, nodePath: ActorPath) {
        val id = nodeId
        val path= nodePath
        override def toString() = {
            "ID=" + id + " Path=" + path
        }
    }

    object NodeInfo {
        def apply(nodeId: Int, nodePath: ActorPath): NodeInfo = {
            new NodeInfo(nodeId, nodePath)
        }
    }

    object EndPoint extends Enumeration {
         type EndPoint = Value
         val OPEN, CLOSED = Value
    }

    class Interval(istart: Int, iend: Int, left: EndPoint.Value, right: EndPoint.Value) {
        val start = istart
        val end = iend
        val leftEnd = left
        val rightEnd = right

        import EndPoint.OPEN

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
            new Interval(start, end, left, right)
        }
    }
}
