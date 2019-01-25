package TestEnv

import BakgroundManager.AdvancedTemporalBackground
import BakgroundManager.BackgroundInitializator
import BakgroundManager.MaskedBackgroundUpdatervar
import ConvertMat2Image
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import tornadofx.*
import utility.applyMask
import utility.computeMask
import utility.grayDifferenceThresholding
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

fun main(args: Array<String>) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    println("Apertura video")

    val video = VideoCapture("/videoTest.avi")
    val frames = mutableListOf<Mat>()
    val frame = Mat()
    while(video.read(frame)){
        val proc = Mat(240,320, CvType.CV_8UC1)
        // converto in grigio
        Imgproc.cvtColor(frame, proc, Imgproc.COLOR_RGB2GRAY)
        frames.add(proc)
    }

    val whiteImg = Mat(240,320, CvType.CV_8UC1)
    val controller = find<TestController2>()
    val converted = ConvertMat2Image(whiteImg)
    controller._1 = converted
    controller._2 = converted
    controller._3 = converted
    controller._4 = converted
    controller._5 = converted

    val backgroundManager = MaskedBackgroundUpdatervar(
        0.6,
        Pair(240,320),
        //{r,c -> max(1.0/Math.pow(c+1.0,0.8),0.015) },
        {r,c -> 0.4},
        10,
        20,
        15)

    val initialBackground = BackgroundInitializator.initializeWithMean(frames.subList(0,50))
    //val staticBackground = BackgroundInitializator.initializeWithMode(frames)

    val kernelEllipse  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(7.0,7.0))

    var frameCounter = 0

    thread {
        Thread.sleep(2000)

        val closeAfterOpen = Mat()
        val openedDiff = Mat()
        var reference = initialBackground

        frames.forEach {

            val similarMask = it.computeMask { rIndex, cIndex, pixelValue ->
                abs(pixelValue - reference[rIndex, cIndex][0]) < pixelValue * 8.0/100
            }

            //val extractedSimilar = it.applyMask(similarMask)


            //Imgproc.GaussianBlur(proc, output, Size(13.0,13.0),1.5)
            backgroundManager.feed(it, similarMask)

            // dinamico
            reference = backgroundManager.background
            // statico

            val diff = reference.grayDifferenceThresholding(it,25)

            Imgproc.morphologyEx(diff, openedDiff,Imgproc.MORPH_OPEN, kernelEllipse)
            Imgproc.morphologyEx(openedDiff, closeAfterOpen,Imgproc.MORPH_CLOSE, kernelEllipse)

            val blurred = Mat()
            val edges = Mat()
            Imgproc.GaussianBlur(it,blurred,Size(11.0,11.0),2.5)
            Imgproc.Canny(blurred, edges,25.0,5.0)


            val morphMask = closeAfterOpen.computeMask { rIndex, cIndex, value -> value > 0.0 }
            val masked = edges.applyMask(morphMask)

            controller.view.view1.image = ConvertMat2Image(it)
            //controller.view.view2.image = ConvertMat2Image(output)
            controller.view.view2.image = ConvertMat2Image(reference)
            controller.view.view3.image = ConvertMat2Image(diff)
            //controller.view.view4.image = ConvertMat2Image(diff)
            //controller.view.view3.image = ConvertMat2Image(closeAfterOpen)
            controller.view.view4.image = ConvertMat2Image(closeAfterOpen)
            controller.view.view5.image = ConvertMat2Image(masked)

            frameCounter++
            println("Rate : ${backgroundManager.updateRate}")
            //Thread.sleep(50)
        }

    }

    launch<Test2>(args)

    println("Termino")
}


class Test2 : App(TestGUI2::class)

class TestGUI2: View() {

    private val controller : TestController2 by inject()

    var view1 : ImageView by singleAssign()
    var view2 : ImageView by singleAssign()
    var view3 : ImageView by singleAssign()
    var view4 : ImageView by singleAssign()
    var view5 : ImageView by singleAssign()

    override val root = hbox {
        view1 = imageview(controller.get1())
        view2 = imageview(controller.get2())
        view3 = imageview(controller.get3())
        view4 = imageview(controller.get4())
        view5 = imageview(controller.get5())
    }
}

class TestController2 : Controller(){

    val view : TestGUI2 by inject()
    var _1 : Image? = null
    var _2 : Image? = null
    var _3 : Image? = null
    var _4 : Image? = null
    var _5 : Image? = null

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
}
