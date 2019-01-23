package BakgroundManager

import org.opencv.core.CvType
import org.opencv.core.Mat

object BackgroundInitializator{

    // assumiamo che ci sia un solo canale e facciamo la media
    fun initialize(frames : List<Mat>) : Mat{
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

}