import akka.actor.ActorSystem
import akka.actor.Props
import scala.math.BigInt
import scala.sys

object Main {
    def main(args: Array[String]): Unit = {
        if (args.length == 0 || args.length > 3) {
            println("Usege: sbt \"run [Number of Zeros] [Work Unit(optional)] [Work Size(optional)]\"")
            sys.exit(0)
        }
        val numZeros = args(0).toInt
        val workUnit = if (args.length >=2) BigInt(args(1)) else BigInt("10000")
        val workSize = if (args.length >=3) BigInt(args(2)) else BigInt("1000000")
        val system = ActorSystem("BitcoinSystem")
        val master = system.actorOf(Props(classOf[Master], "ruizhang;", workUnit, workSize, numZeros), name = "master")
        val boss = system.actorOf(Props(classOf[Boss], master), name = "boss")
    }
}
