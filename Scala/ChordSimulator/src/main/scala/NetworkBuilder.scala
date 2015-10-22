import akka.actor.{ Actor, Props, ActorPath, ActorSelection }
import akka.util.Timeout
import akka.pattern.ask

import scala.math.BigInt
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.Await

import com.roundeights.hasher.Implicits._

import Peer.RemoteProcedureCall
import Peer.Procedure.JOIN

case class Build(num: Int)
case class Join(id: Int, path: ActorPath)
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
            val randomIds = Random.shuffle((1 to (numNodes * 5)).toVector)
            for (i <- 0 until numNodes) {
                val id = randomIds(i)
                context.actorOf(Props(classOf[Peer], id), s"peer$id")
                nodes += ((id, s"peer$id"))
            }
            val firstPeer = context.actorSelection(nodes(0)._2)
            firstPeer ! RemoteProcedureCall(JOIN, List(nodes(0)._1, null))
            joinedCount += 1

        case JoinComplete =>
            if (joinedCount < numNodes) {
                val random = Random.nextInt(joinedCount)
                val peer = context.actorSelection(nodes(joinedCount)._2)
                val path = ActorPath.fromString(self.path.toString + "/" + nodes(random)._2)
                peer ! RemoteProcedureCall(JOIN, List(nodes(random)._1, path))
                joinedCount += 1
            } else {
                // TODO send request
//                for (n <- nodes) {
//                    context.actorSelection(n._2) ! Print
//                    Thread.sleep(1000)
//                }
                context.system.shutdown();

            }

        case _ =>
    }

}
