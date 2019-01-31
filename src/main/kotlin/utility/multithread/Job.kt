package utility.multithread

data class Job<T>(val action : () -> T,
                  val resultSetter : (T) -> Unit)

/*



open class DogUp  {

}

open class Dog : DogUp() {

}

class DogSub : Dog(){

}

fun <T : Dog> p (x : T) : T{
    return x
}

fun test(){
    val x : DogSub = p(Dog())

}*/