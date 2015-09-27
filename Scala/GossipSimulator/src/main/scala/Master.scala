import akka.actor.Actor

case object Start

class Master(numOfPeers: Int) extends Actor {
    var count = 0
    var start = 0.0
    def receive = {
        case Start =>
            start = System.currentTimeMillis
        case Stopped =>
            count += 1
            if (count == numOfPeers) {
                val time = System.currentTimeMillis - start
       	        println(s"$numOfPeers peers spent $time ms")
	            Thread.sleep(1000)
                context.system.shutdown
            }

    }
}
