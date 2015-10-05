import scala.collection.immutable.Vector
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.math

class Neighbour(idnum: Int, numOfPeers: Int, topology: String) {

    val size = math.round(math.cbrt(numOfPeers)).toInt
    val neighbour = buildNeighbour()

    def getRandomName(): String = {
        var i = idnum
        while (i == idnum) {
          i = Random.nextInt(numOfPeers)
        }
        s"../peer$i"
    }

    def getNeighbourName(): String = {
        val i = neighbour(Random.nextInt(neighbour.length))
        s"../peer$i"
    }

    val getName: () => String = {
        topology match {
            case "full" => getRandomName

            case _ => getNeighbourName
        }
    }

    def buildNeighbour(): Vector[Int] = {
        var buffer = new ListBuffer[Int]
        topology match {
            case "full" => buildFull(buffer)

            case "line" => buildLine(buffer)

            case "ring" => buildRing(buffer)

            case "3D" => build3D(buffer)

            case "imp3D" => build3D(buffer)
                            buildRandom(buffer)

            case _ => throw new NotImplementedError(s"$topology not yet implemented")
        }
        buffer.toVector
    }

    def buildFull(buffer: ListBuffer[Int]) = Nil

    def buildLine(buffer: ListBuffer[Int]) = {
        if (idnum > 0) {
            buffer += idnum - 1
        }
        if (idnum < numOfPeers - 1) {
            buffer += idnum + 1
        }
    }

    def buildRing(buffer: ListBuffer[Int]) = {
        buildLine(buffer)
        if (idnum == 0) {
            buffer += numOfPeers - 1
        } else if (idnum == numOfPeers - 1) {
            buffer += 0
        }
    }

    def build3D(buffer: ListBuffer[Int]) = {
        val z = idnum / size / size
        val y = (idnum - z * size * size) / size
        val x = idnum % size
        if (x > 0) {
            buffer += idnum - 1
        }
        if (x < size -1) {
            buffer += idnum + 1
        }
        if (y > 0) {
            buffer += idnum - size
        }
        if (y < size -1) {
            buffer += idnum + size
        }
        if (z > 0) {
            buffer += idnum - size * size
        }
        if (z < size -1) {
            buffer += idnum + size * size
        }
    }

    def buildRandom(buffer: ListBuffer[Int]) = {
        var i = idnum
	    do {
            i = Random.nextInt(numOfPeers)
        } while (buffer.contains(i) || idnum == i)
        buffer += i
    }
}
