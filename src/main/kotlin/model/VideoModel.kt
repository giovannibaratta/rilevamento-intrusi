package model

import org.opencv.core.Mat
import tornadofx.ViewModel

data class VideoModel(val frames : Array<Mat>) : ViewModel(){

}