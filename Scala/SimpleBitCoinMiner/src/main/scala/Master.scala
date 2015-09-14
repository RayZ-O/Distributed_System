import akka.actor.Actor
import akka.actor.ActorRef
import scala.math.BigInt

class Master(baseStr: String, workUnit: BigInt, workSize: BigInt, numZeros: Int) extends Actor {
    val workproducer = new WorkProducer(baseStr, workUnit, workSize, numZeros)
    import scala.collection.mutable.ArrayBuffer
    var workers = new ArrayBuffer[ActorRef]()
    var numJobs = workproducer.numJobs
    var finishedJobs = BigInt(1);
    def receive = {
          case ReadyToWork(nums) => 
              workers += sender
              val jobs = workproducer.nextWork(nums)
              if (jobs.length > 0) {
                sender ! Work(jobs)
              } 
          case WorkDone => 
              finishedJobs = finishedJobs + 1
              if (numJobs equals finishedJobs) {
                  workers foreach { w => w ! WorkComplete}
                  context.system.shutdown
              }
          case Result(res) =>
              println(res)              
    }
}

