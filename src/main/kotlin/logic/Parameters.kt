package logic

import java.lang.IllegalStateException

class Parameters{

    private val params = HashMap<Int,String>()

    fun setParameter(key : Int, value : String){
        params[key] = value
    }

    fun getParameter(key : Int) : String =
        params[key] ?: throw IllegalStateException("Parametro non presente")


}