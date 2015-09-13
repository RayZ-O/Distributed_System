import akka.actor.Actor
import scala.util.Random
import com.roundeights.hasher.Implicits._
 
class Miner extends Actor {

    def receive = {
          case Job(jobId, baseStr, suffixLen, prefix) =>  
            var sha256 = baseStr.sha256
            var randomText = ""
            while(!sha256.hex.startsWith(prefix)){
                randomText = baseStr ++ Random.alphanumeric.take(suffixLen).mkString
                sha256 = randomText.sha256
            }
            sender ! Result(randomText, sha256.hex)
    }
}


