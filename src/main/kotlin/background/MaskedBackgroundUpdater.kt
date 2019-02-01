package background

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.*
import kotlin.math.abs

class MaskedBackgroundUpdater(
    val imageSize : Pair<Int,Int>,
    val updateRate : Double,
    val historyUpdateRate : Double,
    val historySize : Int,
    val deviationThreshold : Int,
    val background : Mat = Mat.zeros(imageSize.first, imageSize.second, CvType.CV_8UC1)){

    private val pixelHistory = Array(imageSize.first){Array(imageSize.second){ArrayDeque<Double>()}}
    private val math = StandardDeviation()

    fun feed(img : Mat, historyMask : Mat, similarityThreshold : Int, noUpdateMask : Mat = Mat.ones(img.rows(), img.cols(), CvType.CV_8UC1)){

        var rIndex = 0
        var cIndex = 0

        for(index in 0 until imageSize.first * imageSize.second) {

            rIndex = index / imageSize.second
            cIndex = index % imageSize.second

            if(noUpdateMask[rIndex,cIndex][0] == 0.0){
                continue
            }

            val imageValue = img[rIndex,cIndex][0]

            if(historyMask[rIndex,cIndex][0] > 0){
                // il pixel appartiene alla maschera di conseguenza significa che ha subito una leggera variazione rispetto al precedente
                val oldValue = background[rIndex,cIndex][0]
                val difference = imageValue-oldValue
                val newValue = oldValue + updateRate * difference
                background.put(rIndex, cIndex, newValue)

                val similarValue = pixelHistory[rIndex][cIndex].count { abs(it-imageValue) < similarityThreshold }
                if(similarValue > pixelHistory[rIndex][cIndex].size/2) {
                    pixelHistory[rIndex][cIndex].clear()
                }else {
                    pixelHistory[rIndex][cIndex].add(imageValue)
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
                        val difference = mean-oldValue
                        val newValue = oldValue + historyUpdateRate * difference
                        background.put(rIndex,cIndex,newValue)
                    }
                }
            }
            if(pixelHistory[rIndex][cIndex].size >= historySize)
                pixelHistory[rIndex][cIndex].remove()
        }

    }
}