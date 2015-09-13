import akka.actor.ActorSystem
import akka.actor.Props
import scala.math.BigInt

object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("BitcoinSystem")
        val master = system.actorOf(Props(classOf[Master], "ruizhang;", BigInt(1000), BigInt(1000000), 3), name = "master")
        // val boss = system.actorOf(Props(classOf[Boss], master), name = "boss")
    }
   
}
