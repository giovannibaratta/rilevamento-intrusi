package controller

import javafx.embed.swing.SwingFXUtils
import view.VideoUI
import javafx.scene.image.Image
import model.VideoModel
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import scope.VideoControllerScope
import tornadofx.Controller
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException
import javax.imageio.ImageIO

class VideoController : Controller(){

    override val scope = super.scope as VideoControllerScope
    private val view : VideoUI by inject()
    private val videoModel : VideoModel = scope.videoModel
    private val elaborationFunction = scope.elaborationFunction
    private val elaborationParameters = scope.params
    private var frameToSkip = "0"
    private var frameCounter = 0

    val defaultImage = convertMatToImage(Mat.zeros(videoModel.frames[0].rows(),videoModel.frames[0].cols(),CvType.CV_8U)) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine")

    fun initialize(){
        println("Init ${view}")
        view.frameToSkipField.textProperty().addListener { _, _, newValue -> frameToSkip = newValue }
        view.subscribeNextButton { nextButton() }
        view.subscribeSkipTo { skipTo() }
        view.subscribePlay { play() }

        println("c  ${this}")
        //val blackImage = Mat.zeros(videoModel.frames[0].rows(),videoModel.frames[0].cols(),CvType.CV_8U)
        //view.imageFrameView.image = convertMatToImage(blackImage)
        //view.imageFrameView.fitHeight = blackImage.rows().toDouble()
        //view.imageFrameView.fitHeight = blackImage.cols().toDouble()
        println("set image")
    }

    private fun nextButton(){
        val elabImage = elaborationFunction.elaborate(videoModel, videoModel.frames[frameCounter], elaborationParameters)
        setFrameInView(convertMatToImage(elabImage) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine"))
        frameCounter++
    }

    private fun play(){
        println("Prova")
        for(frameIndex in frameCounter until videoModel.frames.size-1){
            val elabImage = elaborationFunction.elaborate(videoModel, videoModel.frames[frameCounter], elaborationParameters)
            setFrameInView(convertMatToImage(elabImage) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine"))
            frameCounter++
        }
    }

    private fun skipTo(){
        val frameToStop = frameToSkip.toInt()
        if(frameToStop < frameCounter) return
        for(frameIndex in frameCounter until frameToStop){
            val elabImage = elaborationFunction.elaborate(videoModel, videoModel.frames[frameCounter], elaborationParameters)
            setFrameInView(convertMatToImage(elabImage) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine"))
            frameCounter++
        }
    }

    private fun setFrameInView( img : Image ){
        view.imageFrameView.image = img
    }

    companion object {
        /*TODO("Cercare versioen ottimizzata") */
        /*private*/ fun convertMatToImage(matImage : Mat) : Image? {
            val byteMatData = MatOfByte()
            //image formatting
            Imgcodecs.imencode(".jpg", matImage,byteMatData)
            // Convert to array
            val byteArray = byteMatData.toArray()
            var img : BufferedImage? = null
            try {
                val input = ByteArrayInputStream(byteArray)
                //load image
                img = ImageIO.read(input)
            } catch (e : Exception) {
                e.printStackTrace()
                return null
            }
            return SwingFXUtils.toFXImage(img, null)
        }
    }



    fun start(){
        initialize()
        val elabImage = elaborationFunction.elaborate(videoModel, videoModel.frames[0], elaborationParameters)
        setFrameInView(convertMatToImage(elabImage) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine"))
        frameCounter = 1
    }
}