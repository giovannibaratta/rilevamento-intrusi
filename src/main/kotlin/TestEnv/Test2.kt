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
import utility.*
import java.util.concurrent.Semaphore
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


    //val pixelTracking = Array(503){0}
    //val backTracking = Array(503){0}

    val initialBackground = BackgroundInitializator.initializeWithMean(frames.subList(0,50))
    //val staticBackground = BackgroundInitializator.initializeWithMode(frames)

    val histSize = 10

    val backgroundManager = MaskedBackgroundUpdatervar(
        0.6,
        Pair(240,320),
        //{r,c -> max(1.0/Math.pow(c+1.0,0.8),0.015) },
        {r,c -> 0.25},
        histSize,
        histSize+1,
        7,
        initialBackground)

    val kernelEllipse  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(5.0,5.0))
    val kernelDilation = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(11.0,11.0))

    var frameCounter = 0

    val skip = true
    val skipTo = 5

    var previousFrame = Triple(frames[0],frames[0],frames[0])

    thread {
        Thread.sleep(2000)

        val closeAfterOpen = Mat()
        val openedDiff = Mat()
        val dilation = Mat()
        var reference = initialBackground

        val semaphore = Semaphore(0)

        var similarMask = Mat()
        var frameToFrameSimiliraty1 = Mat()
        var frameToFrameSimiliraty2 = Mat()
        var frameToFrameSimilarity3 = Mat()


        frames.forEach {

            
            val currentFrame = it
            //Imgproc.GaussianBlur(it,currentFrame,Size(3.0,3.0),1.5)

            /** CALCOLABILI IN PARALLELO **/
            thread {
                similarMask = currentFrame.computeMask { rIndex, cIndex, pixelValue ->
                    abs(pixelValue - reference[rIndex, cIndex][0]) < 10
                }
                semaphore.release()
            }

            thread {
                frameToFrameSimiliraty1 = currentFrame.computeMask{ rIndex, cIndex, pixelValue ->
                    abs(pixelValue - previousFrame.first[rIndex, cIndex][0]) < 13
                }
                semaphore.release()
            }

            thread {
                frameToFrameSimiliraty2 = currentFrame.computeMask{ rIndex, cIndex, pixelValue ->
                    abs(pixelValue - previousFrame.second[rIndex, cIndex][0]) < 15
                }
                semaphore.release()
            }

            thread {
                frameToFrameSimilarity3 = currentFrame.computeMask{ rIndex, cIndex, pixelValue ->
                    abs(pixelValue - previousFrame.third[rIndex, cIndex][0]) < 25
                }
                semaphore.release()
            }

            semaphore.acquire()
            semaphore.acquire()
            semaphore.acquire()
            semaphore.acquire()
            /** **/

            val originalFrameToFrame = frameToFrameSimiliraty1.combine(frameToFrameSimiliraty2.combine(frameToFrameSimilarity3)).areaOpening(400)
            val combinedMask = Mat()

            val kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(3.0,3.0))
            Imgproc.morphologyEx(originalFrameToFrame, combinedMask,Imgproc.MORPH_ERODE, kernel)

            if( !(skip && frameCounter < skipTo) ){

                //val extractedSimilar = currentFrame.applyMask(similarMask)

                //Imgproc.GaussianBlur(proc, output, Size(13.0,13.0),1.5)

                // dinamico

                // statico

                val diff = reference.grayDifferenceThresholding(currentFrame, 20)

                Imgproc.morphologyEx(diff, openedDiff, Imgproc.MORPH_OPEN, kernelEllipse)
                Imgproc.morphologyEx(openedDiff, closeAfterOpen, Imgproc.MORPH_CLOSE, kernelEllipse)

                Imgproc.morphologyEx(closeAfterOpen, dilation, Imgproc.MORPH_DILATE, kernelDilation)
                //Imgproc.morphologyEx(detailOpen, detailCloseAfterOpen,Imgproc.MORPH_OPEN, kernelDetail)

                // TEST

                val areaOpening = diff.areaOpening(350)
                val testkernel  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(7.0,7.0))
                val testkerne2  = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS,Size(11.0,11.0))
                val test = Mat()
                val int1 = Mat()
                val int2 = Mat()
                Imgproc.morphologyEx(areaOpening, int1, Imgproc.MORPH_CLOSE, testkernel)
                Imgproc.morphologyEx(int1, int2, Imgproc.MORPH_OPEN, testkernel)
                Imgproc.morphologyEx(int2, test, Imgproc.MORPH_DILATE, testkerne2)

                // FINE TEST

                val blurred = Mat()
                val edges = Mat()
                //Imgproc.GaussianBlur(currentFrame, blurred, Size(11.0, 11.0), 2.5)
                //Imgproc.Canny(blurred, edges, 25.0, 5.0)


                //val morphMask = closeAfterOpen.computeMask { rIndex, cIndex, value -> value > 0.0 }
                //val masked = edges.applyMask(morphMask)

                controller.view.view1.image = ConvertMat2Image(currentFrame)
                //controller.view.view2.image = ConvertMat2Image(output)
                controller.view.view2.image = ConvertMat2Image(reference)

                controller.view.view3.image = ConvertMat2Image(diff)

                // TEST
                val prova = Mat()
                val ch1 = Mat(test.rows(), test.cols(), CvType.CV_8U)
                for(rIndex in 0 until test.rows()){
                    for(cIndex in 0 until test.cols()){
                        ch1.put(rIndex,cIndex,test[rIndex,cIndex][0])
                    }
                }

                val stat = Mat()
                val centroid = Mat()
                val nanosS = System.nanoTime()
                val numberOfLabels = Imgproc.connectedComponentsWithStats(ch1,prova,stat,centroid)
                val nanosE = System.nanoTime()
                println("time = ${nanosE-nanosS} number $numberOfLabels")
                for(lIndex in 0 until numberOfLabels){
                    println("Area ${stat.get(lIndex, Imgproc.CC_STAT_AREA)[0]}")
                }
                /*for(rIndex in 0 until test.rows()){
                    for(cIndex in 0 until test.cols()){
                        if(prova[rIndex,cIndex][0] > 0)
                        prova.put(rIndex,cIndex,prova[rIndex,cIndex][0] + 50)
                    }
                }*/

                // FINE TEST
                controller.view.view4.image = ConvertMat2Image(prova)


                //controller.view.view5.image = ConvertMat2Image(diff)


                //controller.view.view4.image = ConvertMat2Image(diff)
                //controller.view.view3.image = ConvertMat2Image(closeAfterOpen)
                //controller.view.view4.image = ConvertMat2Image(closeAfterOpen)

                //controller.view.view4.image = ConvertMat2Image(dilation.label())
                controller.view.view5.image = ConvertMat2Image( test.label())


                //controller.view.view5.image = ConvertMat2Image(test.label())
                //println("Calcolo")

                //pixelTracking[frameCounter] = currentFrame[210,280][0].toInt()
                //backTracking[frameCounter] = reference[210,280][0].toInt()

            }

            backgroundManager.feed(currentFrame, similarMask, combinedMask)
            reference = backgroundManager.background
            previousFrame = Triple(currentFrame.clone(), previousFrame.first, previousFrame.second)
            frameCounter++

            println("Frame $frameCounter")
            //println("Rate : ${backgroundManager.updateRate}")
            //Thread.sleep(50)
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
