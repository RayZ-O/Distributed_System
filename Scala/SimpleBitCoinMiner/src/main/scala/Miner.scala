import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import scala.util.Random
import com.roundeights.hasher.Implicits._

case class Job(text: String, prefix: String) 
case class Message(msg: String)
 
class Worker extends Actor {
    def receive = {
          case Job(text, prefix) => mine(text, prefix)
          case _                 => println("What's the job?")
    }

    def mine(text: String, prefix: String) = {
        var sha256 = text.sha256
        var randomText = ""
        while(!sha256.hex.startsWith(prefix)){
            randomText = text ++ Random.alphanumeric.take(10).mkString
            sha256 = randomText.sha256
        }
        println(randomText + " " + sha256.hex)
        context.parent ! Message("found")
    }
}

class Master extends Actor {
    val child = context.actorOf(Props[Worker], name = "worker")
    def receive = {
          case Job(text, prefix) => child ! Job(text, prefix)
          case Message(msg)      => println(msg) //exit
          case _                 => println("huh?")
    }
}

object Main extends App {
    val system = ActorSystem("system")
    val master = system.actorOf(Props[Master], name = "master")
    master ! Job("ruizhang;a", "000")
}

