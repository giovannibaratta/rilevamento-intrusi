package BakgroundManager

import org.opencv.core.CvType
import org.opencv.core.Mat
import utility.grayDifferenceThresholding

class TemporalBackground(val initialRate : Double,
                         val rateUpdate : (Double, Long) -> Double,
                         val imageSize : Pair<Int,Int>){

    var rate = initialRate
        private set
    val background = Mat.zeros(imageSize.first, imageSize.second, CvType.CV_8UC1)
    private var imageCount = 0L

    fun feed(img : Mat){

        var rIndex = 0
        var cIndex = 0

        for(index in 0 until imageSize.first * imageSize.second) {

            rIndex = index / imageSize.second
            cIndex = index % imageSize.second

            val oldValue = background[rIndex,cIndex][0]
            val difference = background[rIndex,cIndex][0] - img[rIndex,cIndex][0]
            val newValue = oldValue - rate * difference
            background.put(rIndex,cIndex, newValue)
        }

        rate = rateUpdate(rate, ++imageCount)
    }

}