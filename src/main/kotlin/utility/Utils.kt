package utility

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size

object Utils{

    /**
     * Crea una maschera vuota
     */
    fun emptyMask(rows : Int, cols : Int) : Mat{
            val mask = Mat(rows, cols, CvType.CV_32F)
            var rIndex = 0
            var cIndex = 0
            for(index in 0 until rows*cols){
                rIndex = index / cols
                cIndex = index % cols
                mask.put(rIndex,cIndex,-1.0)
            }
            return mask
        }
}

// TODO("da eliminare")
fun Mat.grayDifferenceThresholding(other : Mat, threshold: Int) : Mat{
    require(this.type() == CvType.CV_8UC1
            && other.type() == CvType.CV_8UC1
            && threshold > 0)

    val difference = Mat(this.rows(), this.cols(), CvType.CV_8UC1)

    for(r in 0 until this.rows())
        for(c in 0 until this.cols())
            when{
                Math.abs(this[r,c][0] - other[r,c][0]) > threshold
                    -> difference.put(r,c,255.0)
                else
                    -> difference.put(r,c,0.0)
            }
    return difference
}

// TODO("estendere il funzionamento a immagini non grayscale")
/**
 * Data un'immagine ed una maschera calcola una nuova immmagine dove sono presenti solo i pixel
 * dell'immagine originale dove la maschera ha valore
 */
fun Mat.applyMask(mask : Mat) : Mat{
    require(mask.rows() == this.rows() && mask.cols() == this.cols())
    val output = Utils.emptyMask(this.rows(), this.cols())
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        if (mask[rIndex, cIndex][0] >= 0)
            output.put(rIndex, cIndex, this[rIndex, cIndex][0])
    }
    return output
}

/**
 * Calcola una maschera dell'immagine utilizzando la funzione di selezione dei pixel [isPixelMask]
 */
fun Mat.computeMask(isPixelMask : (rIndex : Int, cIndex : Int, value : Int)->Boolean) : Mat{
    val output = Utils.emptyMask(this.rows(), this.cols())
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()){
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        if(isPixelMask(rIndex, cIndex, this[rIndex,cIndex][0].toInt()))
            output.put(rIndex,cIndex,255.0)
    }
    return output
}

fun Mat.label() : Mat{
    var labelCounter = 0
    val labeledImage = Mat.zeros(this.rows(), this.cols(),CvType.CV_8UC1)

    var rIndex = 0
    var cIndex = 0
    var leftPixelLabel = 0.0
    var topPixelLabel = 0.0
    val equivalence = HashSet<Pair<Int,Int>>()

    // first scan
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        leftPixelLabel = 0.0
        topPixelLabel = 0.0
        val pixelValue = this[rIndex,cIndex][0]
        // TODO("DA RIVEDERE PER INCLUDERE IL COLORE NERO")
        if(pixelValue <= 0) {
            labeledImage.put(rIndex, cIndex, 0.0)// pixe non appartenente al background
            continue
        }
        if(rIndex > 0)
            // guardo il pixel top
            topPixelLabel = labeledImage[rIndex-1,cIndex][0]
        if(cIndex > 0)
            leftPixelLabel = labeledImage[rIndex, cIndex-1][0]
        when{
            leftPixelLabel == 0.0 && topPixelLabel == 0.0 -> labeledImage.put(rIndex, cIndex, (++labelCounter).toDouble())
            leftPixelLabel != 0.0 && topPixelLabel == 0.0 -> labeledImage.put(rIndex, cIndex, leftPixelLabel)
            leftPixelLabel == 0.0 && topPixelLabel != 0.0 -> labeledImage.put(rIndex, cIndex, topPixelLabel)
            leftPixelLabel != 0.0 && topPixelLabel != 0.0 && leftPixelLabel == topPixelLabel -> labeledImage.put(rIndex, cIndex, topPixelLabel)
            leftPixelLabel != 0.0 && topPixelLabel != 0.0 && leftPixelLabel != topPixelLabel ->{
                labeledImage.put(rIndex, cIndex, leftPixelLabel)
                //if(leftPixelLabel < topPixelLabel)
                    equivalence.add(Pair(leftPixelLabel.toInt(), topPixelLabel.toInt()))
                //else
                  //  equivalence.add(Pair(topPixelLabel, leftPixelLabel))
            }
        }
    }

    // costruisci l'equivalence matrix
    val equivalenceMatrix = Array(labelCounter+1){Array(labelCounter+1){0}}
    equivalence.forEach {
        equivalenceMatrix[it.first][it.second] = 1
        equivalenceMatrix[it.second][it.first] = 1
    }
    for(i in 1 until equivalenceMatrix.size)
        equivalenceMatrix[i][i] = 1

    var changed = false

    do {
        changed = false
        // risolvi le equivalenze
        for (i in 1 until equivalenceMatrix.size) {
            for (j in 1 until equivalenceMatrix.size) {
                if (equivalenceMatrix[i][j] == 1 && i != j) {
                    for (k in 1 until equivalenceMatrix.size) {
                        val pre = equivalenceMatrix[i][k]
                        equivalenceMatrix[i][k] = equivalenceMatrix[i][k] or equivalenceMatrix[j][k]
                        if(pre !=  equivalenceMatrix[i][k])
                            changed = true
                    }
                }
            }
        }
        //println("step")
    }while(changed == true)

    val lutTable = HashMap<Int,Int>()

    for(i in 1 until equivalenceMatrix.size){
        lutTable[i] = i
    }

    for(i in 1 until equivalenceMatrix.size) {
        for (j in 1 until equivalenceMatrix.size) {
            if(equivalenceMatrix[i][j] == 1 && i != j){
                lutTable[j] = i
                for(k in 1 until equivalenceMatrix.size){
                    equivalenceMatrix[j][k] = 0
                }
            }
        }
    }


    val differentLabel = lutTable.values.toSet()
    //println("Number of label ${differentLabel.count()}")

    val color = HashMap<Int, Triple<Double,Double,Double>>()
    differentLabel.forEach {
        color.put(it, Triple(Math.random() * 255, Math.random() * 255 ,Math.random() * 255))
    }


    val colorImage = Mat.zeros(this.rows(), this.cols(),CvType.CV_8UC3)
    var labelValue = 0.0
    // second scan
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        labelValue = labeledImage[rIndex,cIndex][0]
        if(labelValue == 0.0) continue
        val labelColor = lutTable[labelValue.toInt()]!!
        colorImage.put(rIndex, cIndex, color[labelColor]!!.first, color[labelColor]!!.second, color[labelColor]!!.third)
    }

    return colorImage
}
