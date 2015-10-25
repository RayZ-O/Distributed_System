import akka.actor.{ Actor, Props, ActorPath, ActorSelection }
import scala.math.BigInt
import scala.util.Random

import com.roundeights.hasher.Implicits._
import ChordUtil._

case class Build(mode: String)

class NetworkBuilder(noNodes: Int, noRequests: Int) extends Actor {
    val numNodes = noNodes
    val numRequest = noRequests

    val m = 3
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
            println(s"$joinedCount peers joined")
            if (joinedCount < numNodes) {
                // report build progress
                Thread.sleep(1000)
                if (joinedCount % 1000 == 0) {
                    println(s"$joinedCount peers joined")
                }
                val random = Random.nextInt(joinedCount)
                val peer = context.actorSelection(nodes(joinedCount)._2)
                val path = ActorPath.fromString(self.path.toString + "/" + nodes(random)._2)
                peer ! ChordRequst.Join(NodeInfo(nodes(random)._1, path))
                joinedCount += 1
            } else {
                println(s"Build $numNodes peers complete")
                // all nodes start to send request
//                context.children foreach { _ ! Start }
                import context.dispatcher
                import scala.concurrent.duration._
                val tick = context.system.scheduler.schedule(1.second, 1.second, self, Tick)
            }

        case Tick => context.children foreach { _ ! ChordRequst.Print }

        case _ =>
    }
}


