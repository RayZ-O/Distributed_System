import akka.actor.Actor
import scala.collection.mutable.HashMap


class WorkProducer(baseStr: String, suffixLen: Int, numZeros: Int){
    // character set to compose the suffix
    val charset = "abcdefghijklmnopqrstuvwxyz0123456789"
    val size = charset.length
    val prefix = "0" * numZeros
    // current random string length
    var curLen = suffixLen
    // current job Id                   
    var curId = 0;                                

    def nextJob() : Job = {
        val job = Job(curId, baseStr + charset.charAt(curId % size), curLen, prefix)
        curId = curId + 1
        // increase random string length if all characters in the char set 
        // have been used
        if (curId % size == 0) {
            curLen = curLen + 1
        }
        job
    }
}