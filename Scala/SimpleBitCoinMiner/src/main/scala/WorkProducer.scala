import akka.actor.Actor
import scala.collection.mutable.HashMap

case class Job(jobId: Int, baseStr: String, suffixLen: Int, prefix: String)
case class Produce(jobId: Int)
case class ReProduce(jobId: Int)

class WorkProducer(baseStr: String, suffixLen: Int, numZeros: Int) extends Actor {
    // character set to compose the suffix
    val charset = "abcdefghijklmnopqrstuvwxyz0123456789"
    val size = charset.length
    val prefix = "0" * numZeros
    // store current runing: job Id -> Job
    var currentWorks = new HashMap[Int, Job]()
    // current random string length
    var curLen = suffixLen
    // current job Id                   
    var curId = 0;                                

    def receive = {
        case Produce(jobId) => 
            if (currentWorks contains jobId) {
                currentWorks - jobId
            }            
            sender() ! createNewJob()
        // restart the work
        case ReProduce(jobId) =>
            sender() ! currentWorks.get(jobId).get
        // the required bitcoin found
        // case Finish =>  exit
    }

    def createNewJob() : Job = {
        val job = Job(curId, baseStr + charset.charAt(curId % size), curLen, prefix)
        currentWorks + (curId -> job)
        curId = curId + 1
        // increase random string length if all characters in the char set 
        // have been used
        if (curId % size == 0) {
            curLen = curLen + 1
        }
        job
    }
}