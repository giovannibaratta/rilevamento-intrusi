package view

import controller.DebugVideoController
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import tornadofx.*

class DebugUI: View() {

    private val controller : DebugVideoController by inject()

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