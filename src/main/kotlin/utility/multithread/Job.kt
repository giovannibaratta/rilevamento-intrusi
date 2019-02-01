package utility.multithread

data class Job<T>(val action : () -> T,
                  val resultSetter : (T) -> Unit)