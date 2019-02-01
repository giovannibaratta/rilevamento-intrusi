package controller

import javafx.scene.image.Image
import model.VideoModel
import tornadofx.Controller
import view.DebugUI
import java.util.*

class DebugVideoController(private val video : VideoModel) : Controller(){

    val view : DebugUI by inject()
    val videoModel : VideoModel by inject()

    var _1 : Image? = null
    var _2 : Image? = null
    var _3 : Image? = null
    var _4 : Image? = null
    var _5 : Image? = null

    var frameToSkip = ""
        private set

    private val nextButtonListener = HashMap<UUID,() -> Unit>()
    private val skipToListener = HashMap<UUID,() -> Unit>()
    private val playListener = HashMap<UUID,() -> Unit>()

    fun initialize(){
        view.textField.textProperty().addListener { _, _, newValue -> frameToSkip = newValue }
    }

    fun subscribeNextButton(action : ()->Unit) : UUID {
        val uuid = UUID.randomUUID()
        nextButtonListener.put(uuid,action)
        return uuid
    }

    fun nextButton(){
        nextButtonListener.values.forEach { it() }
    }

    fun unsubscribeNextButton(id : UUID){
        nextButtonListener.remove(id)
    }

    fun subscribePlay(action : ()->Unit) : UUID {
        val uuid = UUID.randomUUID()
        playListener.put(uuid,action)
        return uuid
    }

    fun play(){
        playListener.values.forEach { it() }
    }

    fun unsubscribePlay(id : UUID){
        playListener.remove(id)
    }

    fun subscribeSkipTo(action : ()->Unit) : UUID {
        val uuid = UUID.randomUUID()
        skipToListener.put(uuid,action)
        return uuid
    }

    fun skipTo(){
        skipToListener.values.forEach { it() }
    }

    fun unsubscribeSkipTo(id : UUID){
        skipToListener.remove(id)
    }

    fun get1() : Image {
        val out = _1
        if(out == null) throw Exception("Non init")
        return out
    }

    fun get2() : Image {
        val out = _2
        if(out == null) throw Exception("Non init")
        return out
    }

    fun get3() : Image {
        val out = _3
        if(out == null) throw Exception("Non init")
        return out
    }

    fun get4() : Image {
        val out = _4
        if(out == null) throw Exception("Non init")
        return out
    }

    fun get5() : Image {
        val out = _5
        if(out == null) throw Exception("Non init")
        return out
    }

    fun start(){
        TODO("Start")
    }
}