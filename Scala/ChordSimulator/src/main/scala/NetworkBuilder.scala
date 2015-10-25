import akka.actor.{ Actor, Props, ActorPath, ActorSelection }
import scala.util.Random

import ChordUtil._

case class Build(mode: String)

class NetworkBuilder(noNodes: Int, noRequests: Int) extends Actor {
    val numNodes = noNodes
    val numRequest = noRequests

    val m = 19
    import scala.collection.mutable.ArrayBuffer
    var nodes= ArrayBuffer.empty[Tuple2[Int, String]]
    var joinedCount = 0

    override def receive = {
        case Build(mode) =>
            val randomIds = Random.shuffle((0 until (math.pow(2, m).toInt)).toVector)
            for (i <- 0 until numNodes) {
                val id = randomIds(i)
                nodes += ((id, s"peer$id"))
            }
            mode match {
                case "normal" =>
                    Peer.setMExponent(m)
                    nodes foreach { node =>
                        context.actorOf(Props(classOf[Peer], node._1, numRequest), node._2)
                    }

                case "concurrent" =>
                    ConcurrentPeer.setMExponent(m)
                    nodes foreach { node =>
                        context.actorOf(Props(classOf[ConcurrentPeer], node._1, numRequest), node._2)
                    }
            }
            val firstPeer = context.actorSelection(nodes(0)._2)
            firstPeer ! ChordRequst.Join(NodeInfo(nodes(0)._1, null))
            joinedCount += 1

        case ChordReply.JoinComplete =>
            if (joinedCount < numNodes) {
                // report build progress
                if (joinedCount % 1000 == 0) {
                    println(s"$joinedCount peers joined")
                }
                val random = Random.nextInt(joinedCount)
                val peer = context.actorSelection(nodes(joinedCount)._2)
                val path = ActorPath.fromString(self.path.toString + "/" + nodes(random)._2)
                peer ! ChordRequst.Join(NodeInfo(nodes(random)._1, path))
                joinedCount += 1
            } else {
                println("Wating for finger table update...")
                Thread.sleep(5 * numNodes / 10000)
                println(s"Build $numNodes peers complete")
                // all nodes start to send request
                context.children foreach { _ ! ChordRequst.Start }
            }

        case _ =>
    }
}


