package utility

data class FrameStatistic(
    val objectsStat : Array<ObjectStat>

){
    val numberOfObjects = objectsStat.size
}


data class ObjectStat(
    val area : Double,
    val perimeter : Double,
    val type : ObjectType
) {

    enum class ObjectType {
        PERSON,
        OTHER
    }
}

