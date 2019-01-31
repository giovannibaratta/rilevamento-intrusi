package utility.multithread

import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

class Worker internal constructor(private val jobEnd : (Worker)->Unit) : Thread(){

    private var isWorking = false
    private val currentJob = ArrayDeque<Job<*>>()
    private val mutex = JvmType.Object("mutex")
    private val canIWork = Semaphore(0)
    private var stopToWork = false

    override fun run() {
        super.run()
        canIWork.acquire()
        while(!stopToWork){
            //val job =
            //if(job == null) throw IllegalStateException("Non è presente nessun lavoro")
            work(currentJob.remove() ?: throw IllegalStateException("Non è presente nessun lavoro"))
            synchronized(mutex){
                isWorking = false
            }
            jobEnd(this)
            canIWork.acquire()
        }
    }

    fun stopToWork() {
        stopToWork = true
        canIWork.release()
    }

    private fun <T> work(job : Job<T>){
        job.resultSetter(job.action())
    }

    fun <T> doJob(job : Job<T>){
        synchronized(mutex){
            if(isWorking) throw IllegalStateException("Il worker sta già lavorando")
            currentJob.add(job)
            canIWork.release()
        }
    }

}