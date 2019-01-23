package TestEnv

import BakgroundManager.AdvancedTemporalBackground
import BakgroundManager.TemporalBackground
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
import utility.grayDifferenceThresholding
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

fun main(args: Array<String>) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    println("Apertura video")



    val video = VideoCapture("/videoTest.avi")
    val whiteImg = Mat(240,320, CvType.CV_8UC1)
    val proc = Mat(240,320, CvType.CV_8UC1)
    val frame = Mat()
    val controller = find<TestController>()
    val converted = ConvertMat2Image(whiteImg)
    controller._1 = converted
    controller._2 = converted
    controller._3 = converted
    controller._4 = converted
    controller._5 = converted

    val pixelTracking = Array(503){0}
    val backTracking = Array(503){0}

    //val backgroundManager = TemporalBackground(1.0, {r,c -> max(1.0/(Math.pow(c+1.0,0.8)),0.015) },Pair(240,320))

    val backgroundManager = AdvancedTemporalBackground(1.0, Pair(240,320),{r,c -> max(1.0/Math.pow(c+1.0,0.8),0.015) },55,0.35, 30)
    val kernel  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(7.0,7.0))
    //val allKernel = Mat.ones(Size(27.0,27.0),CvType.CV_32F)
    var frameCounter = 0

    thread {
        Thread.sleep(2000)

        while(video.read(frame)) {
            // converto in grigio
            Imgproc.cvtColor(frame, proc, Imgproc.COLOR_RGB2GRAY)
            val output = proc //Mat()
            //Imgproc.GaussianBlur(proc, output, Size(13.0,13.0),1.5)

            controller.view.view1.image = ConvertMat2Image(proc)
            pixelTracking[frameCounter] = output[169,214][0].toInt()
            backgroundManager.feed(output)
            controller.view.view2.image = ConvertMat2Image(output)
            val ref = backgroundManager.background
            backTracking[frameCounter] = ref[169,214][0].toInt()
            //val closing = Mat()
            //val diff = ref.grayDifferenceThresholding(output, 20)
            controller.view.view3.image = ConvertMat2Image(ref)
            //Imgproc.morphologyEx(diff,closing,Imgproc.MORPH_CLOSE,allKernel)
            //controller.view.view3.image = ConvertMat2Image(closing)
            val diff = ref.grayDifferenceThresholding(output,35)
            controller.view.view4.image = ConvertMat2Image(diff)
            val closingDiff = Mat()

            Imgproc.morphologyEx(diff, closingDiff,Imgproc.MORPH_OPEN, kernel)
            controller.view.view5.image = ConvertMat2Image(closingDiff)
            frameCounter++
            println("Rate : ${backgroundManager.rate}")
            //Thread.sleep(50)
        }

        for(i in 0 until frameCounter){
            println("Frame $i p:${pixelTracking[i]} b:${backTracking[i]}")
        }

        /*for (currentFrame in frames){
            val diff = ComputeDifference(back, currentFrame, 40)
            controller.view.view3.image = ConvertMat2Image(currentFrame)
            controller.view.differenceView.image = ConvertMat2Image(diff)
        }*/
    }

    launch<Test>(args)

    println("Termino")
}


class Test : App(TestGUI::class)

class TestGUI: View() {

    private val controller : TestController by inject()

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

class TestController : Controller(){

    val view : TestGUI by inject()
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
