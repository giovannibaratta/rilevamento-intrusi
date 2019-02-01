import controller.VideoController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.stage.Stage
import logic.ChangeDetection
import model.VideoModel
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture



class VideoApplication : Application(){
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
            Application.launch(VideoApplication::class.java, *args)
        }
    }

    override fun stop() {
        super.stop()
        println("Termino")
    }

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        primaryStage.title = "Rilevamento intrusi"
        val loader = FXMLLoader(javaClass.getResource("VideoUI.fxml"))
        val videoModel = VideoModel(loadVideo())
        val controller = VideoController(videoModel, ChangeDetection(videoModel),  logic.Parameters())
        loader.setController(controller)
        val root : Parent = loader.load()
        val vbox : Parent = root.childrenUnmodifiable[0] as Parent
        val hbox : Parent = vbox.childrenUnmodifiable[1] as Parent
        val button  : Button = hbox.childrenUnmodifiable[0] as Button
        val buttonSize = button.prefHeight
        val scene = Scene(root, videoModel.frames[0].cols().toDouble(), videoModel.frames[0].rows()+ buttonSize)//+ Button.DE)
        primaryStage.isResizable = false
        primaryStage.scene = scene
        primaryStage.show()
        controller.start()
    }
}

fun loadVideo() : Array<Mat>{
    val videoCapture = VideoCapture("/videoTest.avi")
    val frames = mutableListOf<Mat>()
    val frame = Mat()
    val grayFrame = Mat() /*= Mat(240,320, CvType.CV_8UC1)*/
    while(videoCapture.read(frame)){
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_RGB2GRAY)
        frames.add(grayFrame.clone())
    }
    frame.release()
    return frames.toTypedArray()
}