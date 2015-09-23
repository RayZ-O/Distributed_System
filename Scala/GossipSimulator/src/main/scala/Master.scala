import akka.actor.Actor


class Master(numOfPeers: Int) extends Actor {
    var count = 0
    def receive = {
        case Stopped =>
            count += 1
            if (count == numOfPeers) {
                context.system.shutdown
            }
    }
}
