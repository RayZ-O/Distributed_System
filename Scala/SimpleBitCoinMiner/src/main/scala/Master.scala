import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.Inbox
import akka.actor.ActorRef

import scala.sys

case class WorkerReady(worker: ActorRef)
case class WorkerComplete(text: String, value: String)
case class WorkerExceedDeadline(workerId: Int)
case class WorkerFailed(workerId: Int)

class Master extends Actor {
    var workerCnt = 0;

    import scala.collection.mutable.HashMap
    var workers = new HashMap[Int, ActorRef]()
    var workersJob = new HashMap[Int, Int]()

    val workproducer = context.actorOf(Props(classOf[WorkProducer], "ruizhang;", 10, 2), name = "workproducer")
    def receive = {
          case WorkerReady(worker) => 
              workerCnt = workerCnt + 1
              register(workerCnt, worker)
              sendJob(workerCnt, worker, Produce(-1))

          case WorkerExceedDeadline(workerId) => 
              sendJob(workerId, workers.get(workerId).get, Produce(workersJob.get(workerId).get))
         
          case WorkerFailed(workerId) => 
              sendJob(workerId, workers.get(workerId).get, ReProduce(workersJob.get(workerId).get)) // Max retry

          case WorkerComplete(text, value) => 
              println(text + " " + value)

          case _ => println("Unknown message")
    }

    def register(workerId: Int, worker: ActorRef) = {
        workers + (workerId -> worker)
        workersJob + (workerId -> 0)
    }

    def sendJob(workerId: Int, worker: ActorRef, produce: Any) = {
        import scala.concurrent.Await
        import scala.concurrent.Future
        import akka.pattern.ask
        import akka.util.Timeout
        import scala.concurrent.duration._
        implicit val timeout = Timeout(5 seconds)
        val future = workproducer ? produce
        val result = Await.result(future, timeout.duration).asInstanceOf[Job]         
        workersJob updated (workerId, result.jobId)
        worker ! result 
    }
}

object Main extends App {
    val system = ActorSystem("system")
    val master = system.actorOf(Props[Master], name = "master")
    val worker1 = system.actorOf(Props(classOf[Miner], master), name = "miner1")
    val worker2 = system.actorOf(Props(classOf[Miner], master), name = "miner2")
    val worker3 = system.actorOf(Props(classOf[Miner], master), name = "miner3")

    val inbox = Inbox.create(system)

    inbox.send(master, WorkerReady(worker1))
    inbox.send(master, WorkerReady(worker2))
    inbox.send(master, WorkerReady(worker3))
}
