package BakgroundManager

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.*

class MaskedBackgroundUpdater(
    var updateRate : Double,
    val imageSize : Pair<Int,Int>,
    val rateUpdate : (Double, Long) -> Double,
    val historySize : Int,
    val maxHistorySize : Int,
    val deviationThreshold : Int,
    val background : Mat = Mat.zeros(imageSize.first, imageSize.second, CvType.CV_8UC1)){

    init {
        require(maxHistorySize >= historySize)
    }

    private var imageCount = 0L
    private val pixelHistory = Array(imageSize.first){Array(imageSize.second){ArrayDeque<Double>()}}
    private val math = StandardDeviation()

    fun feed(img : Mat, historyMask : Mat, noUpdateMask : Mat = Mat.ones(img.rows(), img.cols(), CvType.CV_8UC1)){

        var rIndex = 0
        var cIndex = 0

        for(index in 0 until imageSize.first * imageSize.second) {

            rIndex = index / imageSize.second
            cIndex = index % imageSize.second

            if(noUpdateMask[rIndex,cIndex][0] == 0.0){
                //pixelHistory[rIndex][cIndex].clear()
                continue
            }

            val imageValue = img[rIndex,cIndex][0]

            if(historyMask[rIndex,cIndex][0] > 0){
                // il pixel appartiene alla maschera e non è cambiato troppo
                val oldValue = background[rIndex,cIndex][0]
                val difference = oldValue - imageValue
                val newValue = oldValue - updateRate * difference
                background.put(rIndex, cIndex, newValue)

                if(pixelHistory[rIndex][cIndex].isNotEmpty()) {
                    pixelHistory[rIndex][cIndex].clear()
                    //println("Clear $rIndex $cIndex")
                }
            }else{
                // il pixel non appartiene alla maschera, prima di aggiornare il background guardo la sua storia
                pixelHistory[rIndex][cIndex].add(imageValue)
                if(pixelHistory[rIndex][cIndex].size >= historySize){
                    // ho abbastanza campioni per calcolare le statistiche
                    math.clear()
                    val deviation = math.evaluate(pixelHistory[rIndex][cIndex].toDoubleArray())
                    // se la varianza è troppo alta non aggiorno, poi rimuovo gli elementi se ne ho troppi
                    if(deviation <= deviationThreshold){
                        // varianza accettabile, aggiorno il background
                        val mean = pixelHistory[rIndex][cIndex].average()
                        val oldValue = background[rIndex,cIndex][0]
                        val difference = oldValue - mean
                        val newValue = oldValue - 0.05 * difference
                        background.put(rIndex,cIndex,newValue)
                    }
                    if(pixelHistory[rIndex][cIndex].size >= maxHistorySize)
                        pixelHistory[rIndex][cIndex].remove()
                }
            }

        }

        // DEBUG
        //pixelHistory[210][280].forEach { print("$it ") }
        //math.clear()
        //print(" D: ${math.evaluate(pixelHistory[210][280].toDoubleArray())}")
        //println("")
        updateRate = rateUpdate(updateRate, ++imageCount)
    }
}