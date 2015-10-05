import akka.actor.Actor

case object Start
case object Stopped
case class FailedNum(num: Int)

class Profiler(numOfPeers: Int, algorithm: String) extends Actor {
    var count = 0
    var start = 0.0
    var total = numOfPeers

    def receive = {
        case Stopped =>
            count += 1
            if (count == total) {
                val time = System.currentTimeMillis - start
       	        println(s"$numOfPeers peers spent $time ms")
                Thread.sleep(2000)
                context.system.shutdown
            }
        case FailedNum(num) =>
            total -= num
            val peer0 = context.actorSelection("/user/peer0") // actorFor is deprecated
            start = System.currentTimeMillis
            algorithm match {
                case "gossip" => peer0 ! Gossip("Hi from main")
                case "push-sum" => peer0 !  PushSum(0, 0)
                case _ => throw new NotImplementedError("Algorithm not implemented")
            }

    }
}

