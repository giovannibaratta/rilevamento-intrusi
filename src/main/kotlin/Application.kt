import BakgroundManager.SlidingBackground
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import tornadofx.*
import utility.grayDifferenceThresholding
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.concurrent.thread


fun main(args: Array<String>) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    println("Apertura video")



    val video = VideoCapture("/videoTest.avi")
    //val frames = mutableListOf<Mat>()
    val whiteImg = Mat(240,320,CvType.CV_8UC1)

    val proc = Mat(240,320,CvType.CV_8UC1)
    val frame = Mat()
    //println("Frame Type ${frame.type()}")
    //println("Open ? ${video.isOpened}")


    /*
    val start = System.currentTimeMillis()
    val back = BakgroundManager.BackgroundInitializator.initialize(frames)
    val end = System.currentTimeMillis()

    println("Time ${end-start}")
*/

    // STAMPA DI PROVA

    val controller = find<GUIController>()
    /*val trasf = ConvertMat2Image(back)
    if(trasf == null)
        throw Exception("fail")*/
    val converted = ConvertMat2Image(whiteImg)
    controller._image = converted
    controller._background = converted
    controller._difference = converted
    controller._edge = converted

    val background = SlidingBackground(40, Pair(240, 320))

    thread {
        Thread.sleep(2000)

        while(video.read(frame)) {
            Imgproc.cvtColor(frame, proc, Imgproc.COLOR_RGB2GRAY)
            val output = Mat()
            Imgproc.GaussianBlur(frame, output, Size(17.0,17.0),3.0)
            controller.view.imageView.image = ConvertMat2Image(proc)
            background.feed(output)
            val ref = background.background
            controller.view.backgroundView.image = ConvertMat2Image(ref)
            controller.view.differenceView.image = ConvertMat2Image(ref.grayDifferenceThresholding(output,20))
            val thresholdCanny = 10.0
            //Imgproc.blur(frame, output, Size(30.0, 30.0))
            Imgproc.Canny(output, frame,thresholdCanny, thresholdCanny * 3, 3, false)
            controller.view.edgeView.image = ConvertMat2Image(frame)
        }

        /*for (currentFrame in frames){
            val diff = ComputeDifference(back, currentFrame, 40)
            controller.view.view3.image = ConvertMat2Image(currentFrame)
            controller.view.differenceView.image = ConvertMat2Image(diff)
        }*/
    }

    launch<Application>(args)


    // PROVE SUI DATI DI MAT
    //val array = frame.get(0,0)
    //array.forEach { println(it) }


    println("Termino")
}




// converte un'immagine di opencv in un'immagine java
fun ConvertMat2Image(imgContainer : Mat) : Image? {
    val byteMatData = MatOfByte()
    //image formatting
    Imgcodecs.imencode(".jpg", imgContainer,byteMatData)
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


class Application : App(GUI::class)

class GUI: View() {

    private val controller : GUIController by inject()

    var backgroundView : ImageView by singleAssign()
    var imageView : ImageView by singleAssign()
    var differenceView : ImageView by singleAssign()
    var edgeView : ImageView by singleAssign()

    override val root = hbox {

        backgroundView = imageview(controller.getBackground())
        imageView = imageview(controller.getImage())
        differenceView = imageview(controller.getDifference())
        edgeView = imageview(controller.getEdge())
    }
}

class GUIController : Controller(){

    val view : GUI by inject()

    var _image : Image? = null
    var _background : Image? = null
    var _difference : Image? = null
    var _edge : Image? = null

    fun getImage() : Image {
        val out = _image
        if(out == null) throw Exception("Non init")
        return out
    }

    fun getBackground() : Image {
        val out = _background
        if(out == null) throw Exception("Non init")
        return out
    }

    fun getDifference() : Image {
        val out = _difference
        if(out == null) throw Exception("Non init")
        return out
    }

    fun getEdge() : Image {
        val out = _edge
        if(out == null) throw Exception("Non init")
        return out
    }
}

