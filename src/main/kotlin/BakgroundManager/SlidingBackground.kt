package BakgroundManager

import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.*

class SlidingBackground(val windowSize : Int, val imageSize : Pair<Int,Int>) {

    private var pixelMean = Array(imageSize.first * imageSize.second){0}
    private val emptyArray = Array(imageSize.first * imageSize.second){0}
    private var currentWindowsSize = 0
    private val nullBackground = Mat(imageSize.first, imageSize.second, CvType.CV_8UC1)
    private var history = ArrayDeque<Array<Int>>()


    var background : Mat = nullBackground
        get() {
            if(currentWindowsSize == 0) {
                return nullBackground
            }else{
                val outputMat = Mat(imageSize.first, imageSize.second, CvType.CV_8UC1)
                var rIndex = 0
                var cIndex = 0
                for(index in 0 until imageSize.first * imageSize.second){
                    rIndex = index / imageSize.second
                    cIndex = index % imageSize.second
                    outputMat.put(rIndex, cIndex, pixelMean[index].toDouble()/currentWindowsSize)
                }
                return outputMat
            }
        }

    fun feed(img : Mat){


        val newValue = Array(pixelMean.size){0}
        val valueToRemove = when(currentWindowsSize == windowSize){
            true -> {
                currentWindowsSize--
                history.remove()!!
            }
            else -> emptyArray
        }

        var rIndex = 0
        var cIndex = 0

        for(index in 0 until pixelMean.size) {

            rIndex = index / imageSize.second
            cIndex = index % imageSize.second

            newValue[index] = img[rIndex,cIndex][0].toInt()

            pixelMean[index] = pixelMean[index] + newValue[index] - valueToRemove[index]
        }

        history.add(newValue)
        currentWindowsSize++
    }


}