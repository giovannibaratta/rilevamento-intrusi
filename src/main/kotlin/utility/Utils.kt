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
