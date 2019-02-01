package scope

import logic.ElaborationFunction
import logic.Parameters
import model.VideoModel
import tornadofx.Scope

data class VideoControllerScope(
    val videoModel : VideoModel,
    val elaborationFunction: ElaborationFunction,
    val params : Parameters
) : Scope()