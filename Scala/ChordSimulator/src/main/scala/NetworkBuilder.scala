import akka.actor.{ Actor, Props, ActorPath, ActorSelection }
import akka.util.Timeout
import akka.pattern.ask

import scala.math.BigInt
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.Await

import com.roundeights.hasher.Implicits._
import ChordUtil._

case object Build
case class Join(node: NodeInfo)
case object JoinComplete
case object Start
case object Print

class NetworkBuilder(noNodes: Int, noRequests: Int) extends Actor {
    val numNodes = noNodes
    val numRequest = noRequests
    val m = 16
    import scala.collection.mutable.ArrayBuffer
    var nodes= ArrayBuffer.empty[Tuple2[Int, String]]
    var joinedCount = 0

    override def receive = {
        case Build =>
            Peer.setMExponent(m)
            val randomIds = Random.shuffle((0 until (math.pow(2, m).toInt)).toVector)
            for (i <- 0 until numNodes) {
                val id = randomIds(i)
                context.actorOf(Props(classOf[Peer], id, numRequest), s"peer$id")
                nodes += ((id, s"peer$id"))
            }
            val firstPeer = context.actorSelection(nodes(0)._2)
            firstPeer ! Join(NodeInfo(nodes(0)._1, null))
            joinedCount += 1

        case JoinComplete =>
            if (joinedCount < numNodes) {
                val random = Random.nextInt(joinedCount)
                val peer = context.actorSelection(nodes(joinedCount)._2)
                val path = ActorPath.fromString(self.path.toString + "/" + nodes(random)._2)
                peer ! Join(NodeInfo(nodes(random)._1, path))
                joinedCount += 1
            } else {
                // all nodes start to send request
                context.children foreach { _ ! Start }
//                context.children foreach { _ ! Print }
            }

        case _ =>
    }
}


