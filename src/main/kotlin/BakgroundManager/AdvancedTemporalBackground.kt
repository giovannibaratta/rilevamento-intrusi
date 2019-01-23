package BakgroundManager

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.opencv.core.CvType
import org.opencv.core.Mat
import kotlin.math.min

class AdvancedTemporalBackground(var rate : Double,
                                 val imageSize : Pair<Int,Int>,
                                 val rateUpdate : (Double, Long) -> Double,
                                 val historySize : Int,
                                 val rateBoost : Double,
                                 val deviationThreshold : Int){


    val background = Mat.zeros(imageSize.first, imageSize.second, CvType.CV_8UC1)
    private var imageCount = 0L
    private val pixelHistory = Array(imageSize.first){Array(imageSize.second){DoubleArray(historySize){0.0}}}
    private val math = StandardDeviation()


    fun feed(img : Mat){

        var rIndex = 0
        var cIndex = 0
        var historyIndex = (imageCount%historySize).toInt()

        for(index in 0 until imageSize.first * imageSize.second) {

            rIndex = index / imageSize.second
            cIndex = index % imageSize.second

            var pixelRate = rate
            var imageValue = img[rIndex,cIndex][0]
            if(imageCount > historySize){
                math.clear()
                val mean = pixelHistory[rIndex][cIndex].average()
                val deviation = math.evaluate(pixelHistory[rIndex][cIndex],mean)
                if(deviation > deviationThreshold) {
                    pixelRate = min(rateBoost + rate, 1.0)
                    imageValue = mean
                    //println("Deviation $deviation")
                }
            }

            val oldValue = background[rIndex,cIndex][0]
            val difference = background[rIndex,cIndex][0] - imageValue


            val newValue = oldValue - pixelRate * difference
            background.put(rIndex,cIndex, newValue)
            pixelHistory[rIndex][cIndex][historyIndex] = img[rIndex,cIndex][0]
        }

        rate = rateUpdate(rate, ++imageCount)
    }
}