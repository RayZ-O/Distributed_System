import akka.actor.Actor
import akka.actor.ActorLogging
import scala.util.Random
import com.roundeights.hasher.Implicits._
 
class Miner extends Actor with ActorLogging{

    def receive = {
        case Job(baseStr, start, end, prefix) =>  
            log.info(s"Get job: start $start, end $end")
            var sha256 = baseStr.sha256
            var randomText = ""
            var i = start
            while (i < end) {
                randomText = baseStr ++ i.toString(36)
                sha256 = randomText.sha256
                if (sha256.hex.startsWith(prefix)) {
                    println(randomText +"\t" + sha256.hex)
                }
                i = i + 1
            }
            sender ! WorkDone
    }
}


