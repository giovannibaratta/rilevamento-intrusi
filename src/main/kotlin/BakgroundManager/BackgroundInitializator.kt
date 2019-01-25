package BakgroundManager

import org.opencv.core.CvType
import org.opencv.core.Mat
import kotlin.math.max
import kotlin.math.roundToInt

object BackgroundInitializator{

    // assumiamo che ci sia un solo canale e facciamo la media
    fun initializeWithMean(frames : List<Mat>) : Mat{
        println("Numero di frames : ${frames.size}")

        val matrix = Array<Array<Int>>(frames[0].rows()) { Array<Int>(frames[0].cols()) {0} }
        frames.forEach {
            for (r in 0 until it.rows()){
                for(c in 0 until it.cols()){
                    matrix[r][c] += it[r,c][0].toInt()
                }
            }
        }
        val background = Mat(frames[0].rows(), frames[0].cols(), CvType.CV_8UC1)
        matrix.forEachIndexed { indexR, ints ->
            ints.forEachIndexed { indexC, value ->
                background.put(indexR,indexC, value/frames.size.toDouble())
            }
        }
        return background
    }

    fun initializeWithMode(frames : List<Mat>) : Mat{

        require(frames.size > 0)

        val mode = Array(frames[0].rows()){Array(frames[0].cols()){HashMap<Int,Int>()}}

        frames.forEach {
            for (r in 0 until it.rows()){
                for(c in 0 until it.cols()){
                    val imageValue : Int = it[r,c][0].toInt()
                    if(mode[r][c].containsKey(imageValue)){
                        mode[r][c].put(imageValue, mode[r][c][imageValue]!! + 1 )
                    }else{
                        mode[r][c].put(imageValue,1)
                    }
                }
            }
        }

        val background = Mat(frames[0].rows(), frames[0].cols(), CvType.CV_8UC1)
        mode.forEachIndexed { indexR, ints ->
            ints.forEachIndexed { indexC, value ->

                val sortedValue = mode[indexR][indexC].map { Pair(it.key, it.value) }.sortedByDescending { it.second }


                val perc = max((sortedValue.size * 20.0/100.0).roundToInt(),1)
                var mean = 0.0
                var total = 0
                for(i in 0 until perc){
                    mean += sortedValue[i].first * sortedValue[i].second
                    total += sortedValue[i].second
                }

                //val value = mode[indexR][indexC].maxBy { it.value }!!.key
                background.put(indexR,indexC, mean/total)
            }
        }
        return background
    }

}