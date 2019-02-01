package utility.multithread

import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

class Result<T> private constructor(){

    private val mutex = JvmType.Object("mutex")
    private val queue = ArrayDeque<Semaphore>()
    private var _result : T? = null

    var isReady = false
        private set

    fun getResult() : T {
        synchronized(mutex) {
            if (!isReady) throw  IllegalStateException("Il risultato non è ancora pronto")
            return _result ?: throw  IllegalStateException("Il risultato non è nullo")
        }
    }

    fun waitForResult() : T{
        val semaphore = Semaphore(0)
        synchronized(mutex){
            if(isReady) return _result ?: throw  IllegalStateException("Il risultato non è nullo")
            queue.add(semaphore)
        }
        semaphore.acquire()
        return _result ?: throw  IllegalStateException("Il risultato non è nullo")
    }

    protected fun setResult(result : T){
        synchronized(mutex) {
            if (isReady) throw IllegalStateException("Risultato già settato")
            _result = result
            isReady = true
            queue.forEach { it.release() }
        }
    }

    companion object {
        fun <T> create() : Pair<Result<T>,(T)->Unit>{
            val result = Result<T>()
            return Pair(result, result::setResult)
        }
    }
}