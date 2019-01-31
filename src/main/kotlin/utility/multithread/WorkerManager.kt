package utility.multithread

import java.util.*
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

object WorkerManager {

    private val availableWorker = ArrayDeque<Worker>()
    private val workingWorker = mutableListOf<Worker>()
    private val workQueue = ArrayDeque<Job<*>>()
    private val mutex = JvmType.Object("mutex")

    init {
        for(i in 0 until Runtime.getRuntime().availableProcessors()) {
            val worker = Worker(this::jobEnd)
            worker.start()
            availableWorker.add(worker)
        }
    }

    fun <T> execute(action : () -> T) : Result<T>{
        val result = Result.create<T>()
        assignJob(Job(action,result.second))
        return result.first
    }

    private fun jobEnd(worker : Worker){
        synchronized(mutex) {
            if(workQueue.isNotEmpty()){
                val job = workQueue.remove()
                worker.doJob(job)
            }else{
                availableWorker.add(worker)
            }
        }
    }

    private fun <T> assignJob(job : Job<T>){
        synchronized(mutex) {
            if (availableWorker.isNotEmpty()) {
                val worker = availableWorker.remove()
                worker.doJob(job)
                workingWorker.add(worker)
            }else{
                workQueue.add(job)
            }
        }
    }

}
