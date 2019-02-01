import javafx.scene.Parent
import tornadofx.*
import kotlin.concurrent.thread

fun main(args : Array<String>) {
    val contorller = find(ContTest::class)
    thread {
        Thread.sleep(2000)
        contorller.prova()

    }
    launch<App2>(args)
}

class App2 : App(Test::class)

class Test : View(){
    override val root = vbox{
        button {  }.action { println("Premuto ${this@Test} ${this}") }
    }
}

class ContTest : Controller(){

    val view : Test by inject()


    fun prova(){
        println("c ${view}")
    }


}


