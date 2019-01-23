package utility

import org.opencv.core.CvType
import org.opencv.core.Mat


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
