import akka.actor.ActorPath

object ChordUtil {
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
    }

    object ChordReply {
        case class FindSuccessor(node: NodeInfo, numHops: Int)
        case class FindPredecessor(node: NodeInfo)
        case class ClosestPrecedingFinger(node: NodeInfo)
        case class GetSuccessor(node: NodeInfo)
        case class GetPredecessor(node: NodeInfo)
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
