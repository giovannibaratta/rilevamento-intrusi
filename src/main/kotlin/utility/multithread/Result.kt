package utility.multithread

import java.lang.IllegalStateException

class Result<T> private constructor(){

    private var _result : T? = null

    var isReady = false
        private set

    fun getResult() : T {
        if(!isReady) throw  IllegalStateException("Il risultato non è ancora pronto")
        return _result ?: throw  IllegalStateException("Il risultato non è nullo")
    }

    protected fun setResult(result : T){
        if(isReady) throw IllegalStateException("Risultato già settato")
        _result = result
        isReady = true
    }

    companion object {
        fun <T> create() : Pair<Result<T>,(T)->Unit>{
            val result = Result<T>()
            return Pair(result, result::setResult)
        }
    }
}