import scala.math.BigInt

class WorkProducer(baseStr: String, workUnit: BigInt, workSize: BigInt, numZeros: Int){
    val prefix = "0" * numZeros                
    var cur = BigInt(0)                               

    def nextWork(nums: Int): List[Job] = {
        import scala.collection.mutable.ListBuffer
        var tasks = ListBuffer[Job]()
        var end = cur + workUnit
        var i = 0
        while (i < nums && end < workSize) {
            tasks += Job(baseStr, cur, end, prefix)
            cur = end 
            end = cur + workUnit
            i = i + 1
        }
        if (i < nums && workSize - end > 0) {
            tasks += Job(baseStr, cur, workSize, prefix)
            cur = workSize
        }
        tasks.toList
    }

    def numJobs(): BigInt = {
        val i = workSize / workUnit
        if (workSize % workUnit == 0) i else i + 1
    }
}
