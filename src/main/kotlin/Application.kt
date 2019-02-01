import controller.VideoController
import logic.ChangeDetection
import logic.Parameters
import model.VideoModel
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import scope.VideoControllerScope
import tornadofx.*
import view.VideoUI
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    val videoModel = VideoModel(loadVideo())
    val changedDetection = ChangeDetection(videoModel)
    val params = Parameters()
    /* PARAMETRI */
    val scope = VideoControllerScope(videoModel, changedDetection, params)
    val controller = find(VideoController::class, scope)
    thread {
        Thread.sleep(2000)
        controller.start()
    }
    launch<Application>(args)
}

class Application : App(VideoUI::class)

fun loadVideo() : Array<Mat>{
    val videoCapture = VideoCapture("/videoTest.avi")
    val frames = mutableListOf<Mat>()
    val frame = Mat()
    val grayFrame = Mat() /*= Mat(240,320, CvType.CV_8UC1)*/
    while(videoCapture.read(frame)){
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_RGB2GRAY)
        frames.add(grayFrame)
    }
    frame.release()
    return frames.toTypedArray()
}