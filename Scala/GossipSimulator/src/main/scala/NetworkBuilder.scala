import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import scala.math.cbrt
import scala.util.Random
import scala.sys

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
         val size = checkCubic(peers.length)
        for(i <- 0 until size; j <- 0 until size; k <- 0 until size) {
            if (k > 0)        peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k - 1, j, i, size)))
            if (k < size - 1) peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k + 1, j, i, size)))
            if (j > 0)        peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j - 1, i, size)))
            if (j < size - 1) peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j + 1, i, size)))
            if (i > 0)        peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j, i - 1, size)))
            if (i < size - 1) peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j, i + 1, size)))
        }
    }

    // Grid arrangement but one random other neighboor is selected
    // from the list of all actors (6+1 neighboors)
    def buildImperfect3DGrid(peers: ArrayBuffer[ActorRef]) = {
        val size = checkCubic(peers.length)
        for(i <- 0 until size; j <- 0 until size; k <- 0 until size) {
            if (k > 0)        peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k - 1, j, i, size)))
            if (k < size - 1) peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k + 1, j, i, size)))
            if (j > 0)        peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j - 1, i, size)))
            if (j < size - 1) peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j + 1, i, size)))
            if (i > 0)        peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j, i - 1, size)))
            if (i < size - 1) peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(map3DTo1D(k, j, i + 1, size)))
            val random = Random.nextInt(peers.length)
            peers(map3DTo1D(k, j, i, size)) ! Neighbour(peers(random))
        }
    }

    // check is length a cubic number
    def checkCubic(length: Int): Int = {
        val size = cbrt(length)
        if (!size.isValidInt) {
            throw new IllegalArgumentException("Number of peers must be cubic number in 3D gird")
        } else {
            size.toInt
        }
    }

    // convert 3D position to 1D position
    def map3DTo1D(x: Int, y: Int, z: Int, size: Int): Int = {
        x + y * size + z * size * size
    }
}
