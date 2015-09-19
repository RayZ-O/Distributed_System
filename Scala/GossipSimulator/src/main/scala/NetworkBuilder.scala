import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer

class NetworkBuilder() {

    def build(network: String, peers: ArrayBuffer[ActorRef]) = {
        network match {
            case "full" => buildFull(peers)

            case "line" => buildLine(peers)

            case "3D" => build3DGrid(peers)

            case "imp3D" => buildImperfect3DGrid(peers)

            case "ring" => buildRing(peers)

            case _ => throw new NotImplementedError(s"Build $network not yet implemented")
        }
    }

    def buildFull(peers: ArrayBuffer[ActorRef]) = {
        for (i <- 0 until peers.length; j <- 0 until peers.length) {
            if (i != j) {
                peers(i) ! Neighbour(peers(j))
            }
        }
    }

    def buildLine(peers: ArrayBuffer[ActorRef]) = {
        val end = peers.length - 1
        for (i <- 1 until end; j <- 1 until end) {
            peers(i) ! Neighbour(peers(i - 1))
            peers(i) ! Neighbour(peers(i + 1))
        }
        peers(0) ! Neighbour(peers(1))
        peers(end) ! Neighbour(peers(end - 1))
    }

    def buildRing(peers: ArrayBuffer[ActorRef]) = {
        val end = peers.length - 1
        for (i <- 1 until end; j <- 1 until end) {
            peers(i) ! Neighbour(peers(i - 1))
            peers(i) ! Neighbour(peers(i + 1))
        }
        peers(0) ! Neighbour(peers(1))
        peers(0) ! Neighbour(peers(end))
        peers(peers.length - 1) ! Neighbour(peers(end - 1))
        peers(peers.length - 1) ! Neighbour(peers(0))
    }

    def build3DGrid(peers: ArrayBuffer[ActorRef]) = {
        // TODO
    }

    // Grid arrangement but one random other neighboor is selected
    // from the list of all actors (4+1 neighboors)
    def buildImperfect3DGrid(peers: ArrayBuffer[ActorRef]) = {
        // TODO
    }
}
