package controller

import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import logic.ElaborationFunction
import logic.Parameters
import model.VideoModel
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class VideoController(private val videoModel : VideoModel,
                      private val elaborationFunction : ElaborationFunction,
                      private val elaborationParameters : Parameters){

    @FXML
    private lateinit var imageView : ImageView
    @FXML
    private lateinit var playButton : Button
    @FXML
    private lateinit var skipToButton : Button
    @FXML
    private lateinit var nextFrameButton : Button
    @FXML
    private lateinit var frameToSkipField : TextField

    private var frameToSkip = 0
    private var frameCounter = 0

    val defaultImage = convertMatToImage(Mat.zeros(videoModel.frames[0].rows(),videoModel.frames[0].cols(),CvType.CV_8U)) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine")

    @FXML
    private fun initialize(){
        playButton.setOnAction { play() }
        skipToButton.setOnAction { skipTo() }
        nextFrameButton.setOnAction { nextButton() }
        frameToSkipField.textProperty().addListener { observable, oldValue, newValue ->
            try{
                frameToSkip = newValue.toInt()
            }catch (e : java.lang.Exception){

            }
        }
        setFrameInView(defaultImage)
    }

    private fun nextButton(){
        val elabImage = elaborationFunction.elaborate(videoModel, videoModel.frames[frameCounter], elaborationParameters)
        setFrameInView(convertMatToImage(elabImage) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine"))
        elabImage.release()
        frameCounter++
    }

    private fun play(){
        thread {
            for (frameIndex in frameCounter until videoModel.frames.size - 1) {
                val elabImage =
                    elaborationFunction.elaborate(videoModel, videoModel.frames[frameCounter], elaborationParameters)
                setFrameInView(
                    convertMatToImage(elabImage)
                        ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine")
                )
                frameCounter++
            }
        }
    }

    private fun skipTo(){
        thread {
            val frameToStop = frameToSkip
            if (frameToStop > frameCounter) {
                for (frameIndex in frameCounter until frameToStop) {
                    val elabImage =
                        elaborationFunction.elaborate(
                            videoModel,
                            videoModel.frames[frameCounter],
                            elaborationParameters
                        )
                    setFrameInView(
                        convertMatToImage(elabImage)
                            ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine")
                    )
                    frameCounter++
                }
            }
        }
    }


    private fun setFrameInView( img : Image ){
        imageView.image = img
    }

    companion object {
        /*TODO("Cercare versioen ottimizzata") */
        private fun convertMatToImage(matImage : Mat) : Image? {
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
        val elabImage = elaborationFunction.elaborate(videoModel, videoModel.frames[0], elaborationParameters)
        setFrameInView(convertMatToImage(elabImage) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine"))
        frameCounter = 1
    }
}