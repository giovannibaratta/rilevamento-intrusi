package logic

import model.VideoModel
import org.opencv.core.Mat

interface ElaborationFunction{
    fun elaborate(video : VideoModel, frameToElaborate : Mat, params : Parameters) : Mat
}