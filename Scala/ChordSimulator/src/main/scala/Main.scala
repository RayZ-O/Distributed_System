import akka.actor.ActorSystem

object Main {
    def main(args: Array[String]): Unit = {
        val system = ActorSystem("ChordSystem")
        if (args.length < 2) {
            println("usage: sbt \"run [num of peers][num of request]\"")
        }
        val numOfPeers = args(0).toInt
        val numOfReqs = args(1).toInt     

    }
}
