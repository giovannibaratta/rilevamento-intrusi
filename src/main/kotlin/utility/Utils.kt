package utility

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object Utils{

    val labelColor = Array(1024){
        Triple(Math.random() * 255.0, Math.random() * 255.0, Math.random() * 255.0)
    }

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


fun Mat.applyMaskNoBlack(mask : Mat) : Mat{
    require(mask.rows() == this.rows() && mask.cols() == this.cols())
    val output = Utils.emptyMask(this.rows(), this.cols())
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        if (mask[rIndex, cIndex][0] > 0)
            output.put(rIndex, cIndex, this[rIndex, cIndex][0])
    }
    return output
}

/**
 * Calcola una maschera dell'immagine utilizzando la funzione di selezione dei pixel [isPixelMask]
 */
fun Mat.computeMask(isPixelMask : (rIndex : Int, cIndex : Int, value : Int)->Boolean) : Mat{
    val output = Mat.zeros(this.rows(), this.cols(), CvType.CV_8U)
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

/**
 * Se color = false restituisce un'immagine di tipo CV_32S altrimenti CV_8UC3
 */
fun Mat.label(color : Boolean = true) : Mat{
    val labeledImage = Mat()
    val numberOfLabels = Imgproc.connectedComponents(this,labeledImage)

    if(color == false) return labeledImage

    val coloredImage = Mat.zeros(labeledImage.rows(), labeledImage.cols(), CvType.CV_8UC3)
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        val label = labeledImage[rIndex, cIndex][0]
        if (label > 0) {
            val color = Utils.labelColor[label.toInt()]
            coloredImage.put(rIndex, cIndex, color.first, color.second, color.third)
        }
    }

    labeledImage.release()
    return coloredImage
}


fun Mat.combine(other : Mat) : Mat{
    val output = Mat(this.rows(), this.cols(), CvType.CV_8U)
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()){
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        if(this[rIndex,cIndex][0] == 0.0 || other[rIndex,cIndex][0] == 0.0)
            output.put(rIndex,cIndex,0.0)
        else
            output.put(rIndex, cIndex, 255.0)
    }
    return output
}

/**
 * L'immagine di input deve essere di tipo CV_8U (1 canale) O CV_8S.
 * L'immagine di output è di tipo CV_32S
 */
fun Mat.areaOpening(areaThreshold : Int, label : Boolean = true) : Mat{
    val labelStat = Mat()
    val labelCentroid = Mat()
    val labeledImage = Mat()
    val numberOfLabels = Imgproc.connectedComponentsWithStats(this,labeledImage,labelStat,labelCentroid)
    val labelToRemove = BooleanArray(numberOfLabels){false}

    labelToRemove[0] = true // background sempre rimosso
    for(labelIndex in 1 until numberOfLabels)
        if(labelStat[labelIndex, Imgproc.CC_STAT_AREA][0] < areaThreshold)
            labelToRemove[labelIndex] = true

    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        if(labelToRemove[labeledImage[rIndex,cIndex][0].toInt()])
            labeledImage.put(rIndex,cIndex,0.0)
        else{
            if(!label)
                labeledImage.put(rIndex,cIndex,255.0)
        }
    }

    labelStat.release()
    labelCentroid.release()
    return labeledImage
}

fun colorEdge(edges : Mat, mask : Mat, colorInMask : Triple<Double,Double,Double> = Triple(0.0,0.0,255.0), colorOutOfMask : Triple<Double,Double,Double> = Triple(255.0,255.0,255.0)) : Mat{
    val coloredEdges = Mat.zeros(edges.rows(), edges.cols(), CvType.CV_8UC3)
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until coloredEdges.rows() * coloredEdges.cols()) {
        rIndex = index / coloredEdges.cols()
        cIndex = index % coloredEdges.cols()

        if(edges[rIndex,cIndex][0] > 0) {
            if (mask[rIndex, cIndex][0] > 0) {
                coloredEdges.put(rIndex,cIndex,colorInMask.first,colorInMask.second,colorInMask.third)
            }else{
                coloredEdges.put(rIndex,cIndex,colorOutOfMask.first,colorOutOfMask.second,colorOutOfMask.third)
            }
        }
    }
    return coloredEdges
}

/*
fun Mat.frameStat() : FrameStatistic{
    Imgproc.contou
}*/

fun Mat.applyEdge(edges : Mat, mask : Mat, colorInMask : Triple<Double,Double,Double> = Triple(0.0,0.0,255.0)) : Mat{
    val imageWithEdges = Mat(this.rows(), this.cols(), CvType.CV_8UC3)
    var rIndex = 0
    var cIndex = 0
    for(index in 0 until this.rows() * this.cols()) {
        rIndex = index / this.cols()
        cIndex = index % this.cols()
        if(edges[rIndex,cIndex][0] > 0) {
            if (mask[rIndex, cIndex][0] > 0) {
                imageWithEdges.put(rIndex,cIndex,colorInMask.first,colorInMask.second,colorInMask.third)
            }else{
                val imageValue = this[rIndex,cIndex][0]
                imageWithEdges.put(rIndex,cIndex,imageValue,imageValue,imageValue)
            }
        }else{
            val imageValue = this[rIndex,cIndex][0]
            imageWithEdges.put(rIndex,cIndex,imageValue,imageValue,imageValue)
        }
    }
    return imageWithEdges
}