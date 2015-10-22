import akka.actor.{ Actor, Props, ActorPath }
import akka.util.Timeout
import akka.pattern.ask
import scala.math.BigInt
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.Await
import com.roundeights.hasher.Implicits._
import akka.actor.ActorSelection

import Peer.RemoteProcedureCall
import Peer.Procedure.JOIN

case class Build(num: Int)
case class Join(id: Int, path: ActorPath)
case object JoinComplete

class NetworkBuilder extends Actor {
    val m = 16
    import scala.collection.mutable.HashMap
    val idNameMap = new HashMap[Int, String]
    var nodes: Vector[Tuple2[Int, String]] = _
    var numNodes = 0
    var joinedCount = 0

    override def receive = {
        case Build(num) =>
            numNodes = num
            Peer.setMExponent(m)
            for (i <- 1 to num) {
                val name = s"peer$i"
                val id = (BigInt(name.sha1.hex.substring(0, 20), 16) % BigInt(2).pow(m)).toInt
                println(id)
                if (idNameMap.contains(id)) {
                    throw new IllegalArgumentException("ID collision! Please change m or reduce number of peers")
                } else {
                    idNameMap += (id->name)
                }
                context.actorOf(Props(classOf[Peer], id), s"peer$i")
            }
            nodes = idNameMap.toVector
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
                for (n <- nodes) {
                    context.actorSelection(n._2) ! Print
                    Thread.sleep(1000)
                }
                context.system.shutdown();

            }

        case _ =>
    }

}
