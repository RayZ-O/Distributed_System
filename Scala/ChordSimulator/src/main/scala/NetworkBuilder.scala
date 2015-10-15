import akka.actor.Actor
import com.roundeights.hasher.Implicits._
import scala.math

case class Start(numPeers: Int)

class NetworkBuilder extends Actor {
    
    val m = 20
    val idLength = 16
    
    def receive() = {
        case Start(num) =>
//            val IPList = generateIP(num)
//            val idList = IPList map { ip => BigInt(ip.sha1.hex.substring(0, idLength - 1), 16) }
//            idList foreach { println }
            context.actorSelection("../peer3") ! Join

        case _ => // TO DO
    }
    
    // generate IP in 10.0.0.0/8 to simulate real world node
    def generateIP(numPeers: Int): List[String] = {        
        import scala.collection.mutable.ListBuffer
        var ipList = new ListBuffer[String]
        var n = 0
        val prefix = "10."
        for (i <- 0 to 255; j <- 0 to 255; k <- 1 to 255) {
            ipList += prefix + i.toString + "." + j.toString + "." + k.toString
            n += 1
            if (n == numPeers) {
                return ipList.toList
            }
        }
        List.empty
    }
}