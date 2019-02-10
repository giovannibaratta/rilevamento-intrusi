package logic

import background.BackgroundInitializator
import background.MaskedBackgroundUpdater
import model.VideoModel
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import utility.*
import utility.multithread.WorkerManager
import kotlin.math.abs

class ChangeDetection(video: VideoModel) : ElaborationFunction{

    /* IMG DI SUPPORTO */
    private val closeFrameToFrame1Difference = Mat()
    private val closeFrameToFrame2Difference = Mat()
    private val closeFrameToFrame3Difference = Mat()
    private val noUpdateMask = Mat()
    private val dilationGuassianDifference = Mat()
    private val chagendMask = Mat()
    private val areaOpeningDilationGuassianDifference = Mat()
    private val gaussianDifference = Mat()
    private val referenceWithGaussian = Mat()
    private var reference : Mat
    private val currentFrameWithGaussian = Mat()
    private val closedChangedMask = Mat()

    /* KERNEL */
    private val noUpdateOpenKernel : Mat
    private val noUpdateErosionKernel : Mat
    private val frameToFrameKernel : Mat
    private val changeMaskFirstDilationKernel : Mat
    private val changeMaskSecondDilationKernel : Mat
    private val changedMaskClosingKernel : Mat

    /* PARAMETRI */
    private val guassianSizeCurrentFrame : Double
    private val guassianSigmaCurrentFrame : Double
    private val guassianSizeReference : Double
    private val guassianSigmaRefenrece : Double
    private val similarityThreshold : Int
    private val similarMaskAreaOpeningThreshold : Int
    private val differenceThresholding : Int
    private val differenceAreaOpeningThreshold : Int
    private val dilationGuassianDifferenceAreaOpeningThreshold : Int
    private val frameToFrame1Threshold : Int
    private val frameToFrame2Threshold : Int
    private val frameToFrame3Threshold : Int

    private val initialBackground : Mat
    private val backgroundUpdater : MaskedBackgroundUpdater
    private var previousFrameWithGaussian : Triple<Mat,Mat,Mat>
    private var frameCounter = 0L
    var frameStatistic = mutableListOf<FrameStatistic>()

    init {

        guassianSizeCurrentFrame = Params.GAUSSIAN_SIZE_CURRENT_FRAME.defaultValue.toDouble()
        guassianSigmaCurrentFrame = Params.GAUSSIAN_SIGMA_CURRENT_FRAME.defaultValue.toDouble()
        guassianSizeReference = Params.GUASSIAN_SIZE_REFERENCE.defaultValue.toDouble()
        guassianSigmaRefenrece = Params.GUASSIAN_SIGMA_REFERENCE.defaultValue.toDouble()
        similarityThreshold = Params.SIMILARITY_THRESHOLD.defaultValue.toInt()
        similarMaskAreaOpeningThreshold = Params.SIMILAR_MASK_AREA_OPENING_THRESHOLD.defaultValue.toInt()
        differenceAreaOpeningThreshold = Params.DIFFERENCE_AREA_OPENING_THRESHOLD.defaultValue.toInt()
        differenceThresholding = Params.DIFFERENCE_THRESHOLD.defaultValue.toInt()
        dilationGuassianDifferenceAreaOpeningThreshold = Params.DILATION_GUASSIAN_DIFFERENCE_AREA_OPENING_THRESHOLD.defaultValue.toInt()
        frameToFrame1Threshold = Params.FRAME_TO_FRAME_1_THRESHOLD.defaultValue.toInt()
        frameToFrame2Threshold = Params.FRAME_TO_FRAME_2_THRESHOLD.defaultValue.toInt()
        frameToFrame3Threshold = Params.FRAME_TO_FRAME_3_THRESHOLD.defaultValue.toInt()

        val changeMaskFirstDilationKernelSize = Params.CHANGE_MASK_DILATION_1_KERNEL_SIZE.defaultValue.toDouble()
        val changeMaskSecondDilationKernelSize = Params.CHANGE_MASK_DILATION_2_KERNEL_SIZE.defaultValue.toDouble()
        val frameToFrameKernelSize = Params.FRAME_TO_FRAME_KERNEL_SIZE.defaultValue.toDouble()
        val noUpdateOpenKernelSize = Params.NO_UPDATE_OPEN_KERNEL_SIZE.defaultValue.toDouble()
        val noUpdateErosionKernelSize = Params.NO_UPDATE_EROSION_KERNEL_SIZE.defaultValue.toDouble()
        val closingChangedMaskKernelSize = Params.CHANGE_MASK_CLOSING_KERNEL_SIZE.defaultValue.toDouble()

        changeMaskFirstDilationKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, Size(changeMaskFirstDilationKernelSize,changeMaskFirstDilationKernelSize))
        changeMaskSecondDilationKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, Size(changeMaskSecondDilationKernelSize,changeMaskSecondDilationKernelSize))
        frameToFrameKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, Size(frameToFrameKernelSize,frameToFrameKernelSize))
        noUpdateOpenKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, Size(noUpdateOpenKernelSize,noUpdateOpenKernelSize))
        noUpdateErosionKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, Size(noUpdateErosionKernelSize, noUpdateErosionKernelSize))
        changedMaskClosingKernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE,Size(15.0,15.0))

        initialBackground =
            BackgroundInitializator.initializeWithMode(video.frames.slice(0 until Params.INITIAL_FRAME.defaultValue.toInt()))

        backgroundUpdater = MaskedBackgroundUpdater(
            Pair(video.frames[0].rows(),video.frames[0].cols()),
            Params.SIMILAR_UPDATE_RATE.defaultValue.toDouble(),
            Params.HISTORY_UPDATE_RATE.defaultValue.toDouble(),
            Params.HISTORY_SIZE.defaultValue.toInt(),
            Params.DEVIATION_THRESHOLD.defaultValue.toInt(),
            initialBackground
            )

        previousFrameWithGaussian = Triple(video.frames[0],video.frames[0],video.frames[0])
        reference = initialBackground
    }

    override fun elaborate(video: VideoModel, frameToElaborate: Mat, params: Parameters): Mat {

        val guassianOfCurrentFrameJob = WorkerManager.execute<Unit> {
            Imgproc.GaussianBlur(frameToElaborate,currentFrameWithGaussian,Size(guassianSizeCurrentFrame,guassianSizeCurrentFrame),guassianSigmaCurrentFrame)
        }

        val guassianOfReferenceJob = WorkerManager.execute<Unit> {
            Imgproc.GaussianBlur(reference,referenceWithGaussian,Size(guassianSizeReference,guassianSizeReference),guassianSigmaRefenrece)
        }

        val similarMaskJob = WorkerManager.execute<Mat> {
            frameToElaborate.computeMask { rIndex, cIndex, pixelValue ->
                abs(pixelValue - reference[rIndex, cIndex][0]) < similarityThreshold
            }.areaOpening(similarMaskAreaOpeningThreshold,false)
        }


        //TODO("I 3 job possono essere eseguiti in un unico ciclo dando in uscita le tre immagini")
        val frameToFrameSimilarityJob1 = WorkerManager.execute<Unit> {
            guassianOfCurrentFrameJob.waitForResult()
            val fToFDifference = currentFrameWithGaussian.computeMask{ rIndex, cIndex, pixelValue ->
                abs(pixelValue - previousFrameWithGaussian.first[rIndex, cIndex][0]) < frameToFrame1Threshold
            }
            Imgproc.morphologyEx(fToFDifference, closeFrameToFrame1Difference, Imgproc.MORPH_CLOSE, frameToFrameKernel)
        }


        val frameToFrameSimilarityJob2 = WorkerManager.execute<Unit> {
            guassianOfCurrentFrameJob.waitForResult()
            val fToFDifference = currentFrameWithGaussian.computeMask{ rIndex, cIndex, pixelValue ->
                abs(pixelValue - previousFrameWithGaussian.second[rIndex, cIndex][0]) < frameToFrame2Threshold
            }

            Imgproc.morphologyEx(fToFDifference, closeFrameToFrame2Difference, Imgproc.MORPH_CLOSE, frameToFrameKernel)
        }

        val frameToFrameSimilarityJob3 = WorkerManager.execute<Unit> {
            guassianOfCurrentFrameJob.waitForResult()
            val fToFDifference = currentFrameWithGaussian.computeMask{ rIndex, cIndex, pixelValue ->
                abs(pixelValue - previousFrameWithGaussian.third[rIndex, cIndex][0]) < frameToFrame3Threshold
            }
            Imgproc.morphologyEx(fToFDifference, closeFrameToFrame3Difference, Imgproc.MORPH_CLOSE, frameToFrameKernel)
        }

        val objectContourJob = WorkerManager.execute<Mat> {
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            val contourImage = Mat()
            Imgproc.cvtColor(frameToElaborate,contourImage,Imgproc.COLOR_GRAY2RGB);
            guassianOfCurrentFrameJob.waitForResult()
            guassianOfReferenceJob.waitForResult()
            referenceWithGaussian.grayDifferenceThresholding(currentFrameWithGaussian, differenceThresholding)
                .areaOpening(differenceAreaOpeningThreshold,false).convertTo(gaussianDifference, CvType.CV_8U)
            Imgproc.morphologyEx(gaussianDifference,dilationGuassianDifference,Imgproc.MORPH_DILATE, changeMaskFirstDilationKernel)
            dilationGuassianDifference.areaOpening(dilationGuassianDifferenceAreaOpeningThreshold, false).convertTo(areaOpeningDilationGuassianDifference, CvType.CV_8U)
            Imgproc.morphologyEx(areaOpeningDilationGuassianDifference,chagendMask,Imgproc.MORPH_DILATE,changeMaskSecondDilationKernel)
            Imgproc.morphologyEx(chagendMask,closedChangedMask, Imgproc.MORPH_CLOSE, changedMaskClosingKernel)
            Imgproc.findContours(closedChangedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)
            val objects = mutableListOf<ObjectStat>()
            for(contourIndex in contours.indices){
                val color = Utils.labelColor[contourIndex]
                Imgproc.drawContours(contourImage,contours,contourIndex, Scalar(color.first,color.second,color.third),2)
                val area = Imgproc.contourArea(contours[contourIndex])
                val matOfPoint = MatOfPoint2f(*(contours[contourIndex].toArray()))
                objects.add(ObjectStat(area, Imgproc.arcLength(matOfPoint, true),
                    when{
                        area > 4000 -> ObjectStat.ObjectType.PERSON
                        else -> ObjectStat.ObjectType.OTHER
                    })
                )
            }
            frameStatistic.add(FrameStatistic(objects.toTypedArray()))
            contourImage
        }

        val noUpdateMaskJob = WorkerManager.execute<Unit> {
            frameToFrameSimilarityJob1.waitForResult()
            frameToFrameSimilarityJob2.waitForResult()
            frameToFrameSimilarityJob3.waitForResult()

            val union = closeFrameToFrame1Difference.combine(closeFrameToFrame2Difference)
                                                    .combine(closeFrameToFrame3Difference)

            val openUnion = Mat()
            Imgproc.morphologyEx(union, openUnion,Imgproc.MORPH_OPEN,noUpdateOpenKernel)
            Imgproc.morphologyEx(openUnion, noUpdateMask, Imgproc.MORPH_ERODE, noUpdateErosionKernel)
        }

        val updateBackgroundJob = WorkerManager.execute<Unit> {
            noUpdateMaskJob.waitForResult()
            val similarMask = similarMaskJob.waitForResult()
            guassianOfCurrentFrameJob.waitForResult()
            //previousFrameWithGaussian.third.release()
            previousFrameWithGaussian = Triple(currentFrameWithGaussian.clone(), previousFrameWithGaussian.first, previousFrameWithGaussian.second)
            if(frameCounter > 3)
                backgroundUpdater.feed(frameToElaborate,similarMask, similarityThreshold, noUpdateMask)
            reference = backgroundUpdater.background
            frameCounter++
        }

        updateBackgroundJob.waitForResult()
        println("Frame $frameCounter")
        return objectContourJob.waitForResult()
    }

    enum class Params(val defaultValue : String){
        HISTORY_SIZE("12"),
        DEVIATION_THRESHOLD("9"),
        SIMILAR_UPDATE_RATE("0.3"),
        HISTORY_UPDATE_RATE("0.05"),
        INITIAL_FRAME("65"),
        SIMILARITY_THRESHOLD("10"),
        GAUSSIAN_SIZE_CURRENT_FRAME("7"),
        GAUSSIAN_SIGMA_CURRENT_FRAME("1.2"),
        GUASSIAN_SIZE_REFERENCE("7"),
        GUASSIAN_SIGMA_REFERENCE("1.2"),
        SIMILAR_MASK_AREA_OPENING_THRESHOLD("400"),
        DIFFERENCE_AREA_OPENING_THRESHOLD("20"),
        DIFFERENCE_THRESHOLD("28"),
        DILATION_GUASSIAN_DIFFERENCE_AREA_OPENING_THRESHOLD("450"),
        CHANGE_MASK_DILATION_1_KERNEL_SIZE("5"),
        CHANGE_MASK_DILATION_2_KERNEL_SIZE("5"),
        CHANGE_MASK_CLOSING_KERNEL_SIZE("15"),
        FRAME_TO_FRAME_1_THRESHOLD("8"),
        FRAME_TO_FRAME_2_THRESHOLD("10"),
        FRAME_TO_FRAME_3_THRESHOLD("15"),
        FRAME_TO_FRAME_KERNEL_SIZE("5"),
        NO_UPDATE_OPEN_KERNEL_SIZE("3"),
        NO_UPDATE_EROSION_KERNEL_SIZE("17")
    }
}
