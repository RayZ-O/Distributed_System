import akka.actor.{ Actor, Props, ActorPath, ActorSelection }
import akka.util.Timeout
import akka.pattern.ask

import scala.math.BigInt
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.Await

import com.roundeights.hasher.Implicits._
import ChordUtil._

case class Build(num: Int)
case class Join(node: NodeInfo)
case object JoinComplete

class NetworkBuilder extends Actor {
    val m = 16
    import scala.collection.mutable.ArrayBuffer
    var nodes= ArrayBuffer[Tuple2[Int, String]]()
    var numNodes = 0
    var joinedCount = 0

    override def receive = {
        case Build(num) =>
            numNodes = num
            Peer.setMExponent(m)
            val randomIds = Random.shuffle((0 until (math.pow(2, m).toInt)).toVector)
            for (i <- 0 until numNodes) {
                val id = randomIds(i)
                context.actorOf(Props(classOf[Peer], id), s"peer$id")
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
                println(s"join ${nodes(joinedCount)._1}, guider is ${nodes(random)._1}")
                peer ! Join(NodeInfo(nodes(random)._1, path))
                joinedCount += 1
            } else {
                // TODO send request
//                context.actorSelection("user/networkbuilder/*") ! Print
                context.system.shutdown();
            }

        case _ =>
    }

}
