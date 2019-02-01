package view

import controller.VideoController
import controller.VideoController.Companion.convertMatToImage
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import org.opencv.core.CvType
import org.opencv.core.Mat
import tornadofx.*
import java.lang.IllegalStateException
import java.util.*

class VideoUI: View() {

    private val controller : VideoController by inject()
    var imageFrameView : ImageView by singleAssign()
    var frameToSkipField : TextField by singleAssign()

    override val root =
        vbox {
            hbox {
                imageFrameView = imageview(convertMatToImage(
                    Mat.zeros(240,320,
                        CvType.CV_8U)) ?: throw IllegalStateException("Non sono riuscito a convertire l'immagine")
                )
            }
            hbox{
                button("Next frame").action {nextButtonListener.forEach { it.value() } }
                button("Play").action{
                    println(this@VideoUI)
                    println("${controller}")
                    playListener.forEach { it.value() }
                    play()
                }
                button("Skip to frame").action{ skipToListener.forEach { it.value() }}
                frameToSkipField = textfield ("",{})
            }
        }

    private val nextButtonListener = HashMap<UUID,() -> Unit>()
    private val skipToListener = HashMap<UUID,() -> Unit>()
    private val playListener = HashMap<UUID,() -> Unit>()

    fun subscribeNextButton(action : ()->Unit) : UUID {
        val uuid = UUID.randomUUID()
        nextButtonListener.put(uuid,action)
        return uuid
    }

    fun unsubscribeNextButton(id : UUID){
        nextButtonListener.remove(id)
    }

    fun play(){
        println("${playListener.size}")
        playListener.forEach { it.value() }
    }

    fun subscribePlay(action : ()->Unit) : UUID {
        val uuid = UUID.randomUUID()
        playListener.put(uuid,action)
        return uuid
    }

    fun unsubscribePlay(id : UUID){
        playListener.remove(id)
    }

    fun subscribeSkipTo(action : ()->Unit) : UUID {
        val uuid = UUID.randomUUID()
        skipToListener.put(uuid,action)
        return uuid
    }

    fun unsubscribeSkipTo(id : UUID){
        skipToListener.remove(id)
    }
}