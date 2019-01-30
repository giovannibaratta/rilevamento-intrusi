package TestEnv

import BakgroundManager.BackgroundInitializator
import BakgroundManager.MaskedBackgroundUpdater
import ConvertMat2Image
import javafx.beans.value.ObservableValue
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import tornadofx.*
import utility.*
import javafx.scene.control.TextField
import java.util.*
import java.util.concurrent.Semaphore
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.concurrent.thread
import kotlin.math.abs

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

    var skip = false
    var skipTo = 5
    val sleep = true
    val sleepTime = 2000L
    val enableUI = true

    val nextFrameSemaphore = Semaphore(0)
    controller.subscribeNextButton {
        nextFrameSemaphore.release()
    }
    controller.subscribeSkipTo {
        skip = true
        skipTo = controller.frameToSkip.toInt()
        nextFrameSemaphore.release()
        //for(i in 0 until controller.frameToSkip.toInt())
        //    nextFrameSemaphore.release()
    }

    var frameCounter = 0

    controller.subscribePlay {
        for(i in frameCounter until frames.size-1)
            nextFrameSemaphore.release()
    }

    //val pixelTracking = Array(503){0}
    //val backTracking = Array(503){0}

    val initialBackground = BackgroundInitializator.initializeWithMode(frames.subList(0,65)) //BackgroundInitializator.initializeWithMean(frames.subList(0,100))
    //val staticBackground = BackgroundInitializator.initializeWithMode(frames)

    val histSize = 12

    val backgroundManager = MaskedBackgroundUpdater(

        Pair(240,320),
        0.3,
        //{r,c -> max(1.0/Math.pow(c+1.0,0.8),0.015) },
        {r,c -> 0.3},
        0.05,
        {r,c -> 0.05},
        histSize,
        histSize+1,
        9,
        initialBackground)

    val kernelEllipse  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(5.0,5.0))
    val kernelDilation = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(11.0,11.0))





    var previousFrameWithGaussian = Triple(frames[0],frames[0],frames[0])

    thread {
        Thread.sleep(2000)

        controller.initialize()

        val closeAfterOpen = Mat()
        val openedDiff = Mat()
        val dilation = Mat()
        var reference = initialBackground

        val semaphore = Semaphore(0)

        var similarMask : Mat
        var frameToFrameSimiliraty1 : Mat
        var frameToFrameSimiliraty2 : Mat
        var frameToFrameSimilarity3 : Mat

        val similarityThreshold = 10


        frames.forEach {

            
            val currentFrame = it
            val currentWithGaussian = Mat()
            Imgproc.GaussianBlur(currentFrame,currentWithGaussian,Size(3.0,3.0),1.2)

            /** CALCOLABILI IN PARALLELO **/
            //thread {
                similarMask = currentFrame.computeMask { rIndex, cIndex, pixelValue ->
                    abs(pixelValue - reference[rIndex, cIndex][0]) < similarityThreshold
                }
            //    semaphore.release()
            //}

            //thread {
                frameToFrameSimiliraty1 = currentWithGaussian.computeMask{ rIndex, cIndex, pixelValue ->
                    abs(pixelValue - previousFrameWithGaussian.first[rIndex, cIndex][0]) < 8
                }
            //    semaphore.release()
            //}

            //thread {
                frameToFrameSimiliraty2 = currentWithGaussian.computeMask{ rIndex, cIndex, pixelValue ->
                    abs(pixelValue - previousFrameWithGaussian.second[rIndex, cIndex][0]) < 10
                }
            //    semaphore.release()
            //}

            //thread {
                frameToFrameSimilarity3 = currentWithGaussian.computeMask{ rIndex, cIndex, pixelValue ->
                    abs(pixelValue - previousFrameWithGaussian.third[rIndex, cIndex][0]) < 15
                }
            //    semaphore.release()
            //}

            //semaphore.acquire()
            //semaphore.acquire()
            //semaphore.acquire()
            //semaphore.acquire()
            /** **/

            //val originalFrameToFrame = Mat()



            //val f1f2f3 = frameToFrameSimiliraty1.combine(frameToFrameSimiliraty2.combine(frameToFrameSimilarity3))
            //f1f2f3.areaOpening(800).convertTo(originalFrameToFrame, CvType.CV_8U)
            //val combinedMask = Mat()


            val areopeningSimilar = similarMask.areaOpening(400, false)

            val kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(3.0,3.0))
            val kernelBig = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(17.0, 17.0))
            //Imgproc.morphologyEx(originalFrameToFrame, combinedMask,Imgproc.MORPH_ERODE, kernel)
            val rectKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT,Size(5.0,5.0))
            val frame1Morph = Mat()
            val frame2Morph = Mat()
            val frame3Morph = Mat()
            Imgproc.morphologyEx(frameToFrameSimiliraty1, frame1Morph,Imgproc.MORPH_CLOSE, rectKernel)
            Imgproc.morphologyEx(frameToFrameSimiliraty2, frame2Morph,Imgproc.MORPH_CLOSE, rectKernel)
            Imgproc.morphologyEx(frameToFrameSimilarity3, frame3Morph,Imgproc.MORPH_CLOSE, rectKernel)
            val union = frame1Morph.combine(frame2Morph.combine(frame3Morph))
            val unionOpen = Mat()
            Imgproc.morphologyEx(union, unionOpen,Imgproc.MORPH_OPEN,kernel)
            val unionDilation = Mat()
            Imgproc.morphologyEx(unionOpen, unionDilation, Imgproc.MORPH_ERODE, kernelBig)

                if( !(skip && frameCounter < skipTo) ){

                //Imgproc.GaussianBlur(proc, output, Size(13.0,13.0),1.5)

                val diff = reference.grayDifferenceThresholding(currentFrame, 30)

                Imgproc.morphologyEx(diff, openedDiff, Imgproc.MORPH_OPEN, kernelEllipse)
                Imgproc.morphologyEx(openedDiff, closeAfterOpen, Imgproc.MORPH_CLOSE, kernelEllipse)

                Imgproc.morphologyEx(closeAfterOpen, dilation, Imgproc.MORPH_DILATE, kernelDilation)

                // TEST

                val areaOpening = Mat()
                diff.areaOpening(125/*350*/).convertTo(areaOpening,CvType.CV_8U)
                val testkernel  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(7.0,7.0))
                val testkerne2  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS,Size(11.0,11.0))
                val test = Mat()
                val int1 = Mat()
                val int2 = Mat()
                Imgproc.morphologyEx(areaOpening, int1, Imgproc.MORPH_CLOSE, testkernel)
                Imgproc.morphologyEx(int1, int2, Imgproc.MORPH_OPEN, testkernel)
                Imgproc.morphologyEx(int2, test, Imgproc.MORPH_DILATE, testkerne2)

                // FINE TEST

                //val blurred = Mat()
                //val edges = Mat()
                //Imgproc.GaussianBlur(currentFrame, blurred, Size(11.0, 11.0), 2.5)
                //Imgproc.Canny(blurred, edges, 25.0, 5.0)


                //val morphMask = closeAfterOpen.computeMask { rIndex, cIndex, value -> value > 0.0 }
                //val masked = edges.applyMask(morphMask)

                controller.view.view1.image = ConvertMat2Image(currentFrame)
                //controller.view.view2.image = ConvertMat2Image(output)
                controller.view.view2.image = ConvertMat2Image(reference)

                //controller.view.view3.image = ConvertMat2Image(combinedMask.label())


                //controller.view.view4.image = ConvertMat2Image(similarMask)


                //controller.view.view5.image = ConvertMat2Image(diff)


                //controller.view.view4.image = ConvertMat2Image(diff)
                controller.view.view3.image = ConvertMat2Image(diff)

                    // test

                    //val openSimilarity = Mat()
                    //similarMask.are

                // TEST
                    val referenceWithGaussian = Mat()
                    Imgproc.GaussianBlur(reference,referenceWithGaussian,Size(3.0,3.0),1.2)
                    val gaussianDiff = Mat()
                    referenceWithGaussian.grayDifferenceThresholding(currentWithGaussian, 30).areaOpening(20,false).convertTo(gaussianDiff,CvType.CV_8U)
                    val dilationOnDiff = Mat()

                    val crossKernel  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(5.0,5.0))
                    Imgproc.morphologyEx(gaussianDiff,dilationOnDiff,Imgproc.MORPH_DILATE,crossKernel)
                    val openDilationOnDiff = Mat()
                    dilationOnDiff.areaOpening(450, false).convertTo(openDilationOnDiff,CvType.CV_8U)
                    val aBitOfDilation = Mat()
                    val crossKernelBig  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(9.0,9.0))
                    Imgproc.morphologyEx(openDilationOnDiff,aBitOfDilation,Imgproc.MORPH_DILATE,crossKernelBig)
                    //

                controller.view.view4.image = ConvertMat2Image(gaussianDiff)

                //controller.view.view4.image = ConvertMat2Image(dilation.label())

                controller.view.view5.image = ConvertMat2Image(it.applyMaskNoBlack(aBitOfDilation))


                //controller.view.view5.image = ConvertMat2Image(test.label())
                //println("Calcolo")

                //pixelTracking[frameCounter] = currentFrame[210,280][0].toInt()
                //backTracking[frameCounter] = reference[210,280][0].toInt()

                if(enableUI) {
                    nextFrameSemaphore.acquire()
                }else{
                    if(sleep)
                        Thread.sleep(sleepTime)
                }

            }

            if(frameCounter > 3)
                backgroundManager.feed(currentFrame, areopeningSimilar/*similarMask*/,similarityThreshold, unionDilation/*ncombinedMask*/)
            reference = backgroundManager.background
            previousFrameWithGaussian = Triple(currentWithGaussian.clone(), previousFrameWithGaussian.first, previousFrameWithGaussian.second)
            frameCounter++

            if(skip && frameCounter == skipTo)
                skip = false

            println("Frame $frameCounter")
            //println("Rate : ${backgroundManager.updateRate}")
        }

    }

    launch<Test2>(args)

    //for(i in 0 until frameCounter)
    //    println("Frame $i p:${pixelTracking[i]} b:${backTracking[i]}")
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
    var textField : TextField by singleAssign()

    override val root =
        vbox {
            hbox {
                view1 = imageview(controller.get1())
                view2 = imageview(controller.get2())
                view3 = imageview(controller.get3())
                view4 = imageview(controller.get4())
                view5 = imageview(controller.get5())
            }
            hbox{
                button("Next frame").action { controller.nextButton() }
                button("Play").action{controller.play()}
                button("Skip to").action{controller.skipTo()}
                textField = textfield ("",{})
            }
        }

}

class TestController2 : Controller(){

    val view : TestGUI2 by inject()
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

    fun subscribeNextButton(action : ()->Unit) : UUID{
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

    fun subscribePlay(action : ()->Unit) : UUID{
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

    fun subscribeSkipTo(action : ()->Unit) : UUID{
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

}
