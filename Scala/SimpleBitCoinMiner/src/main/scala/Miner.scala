import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import com.roundeights.hasher.Implicits._
 
class Miner(master: ActorRef) extends Actor {

    def receive = {
          case Job(jobId, baseStr, suffixLen, prefix) => mine(baseStr, suffixLen, prefix)
          case _                 => println("What's the job?")
    }

    def mine(baseStr: String, suffixLen: Int, prefix: String) = {
        var sha256 = baseStr.sha256
        var randomText = ""
        while(!sha256.hex.startsWith(prefix)){
            randomText = baseStr ++ Random.alphanumeric.take(suffixLen).mkString
            sha256 = randomText.sha256
        }
        master ! WorkerComplete(randomText, sha256.hex)
    }
}


