import akka.actor.{ActorSystem, Props}
import base.{AddGUI, Keeper}
import ui.GUI

object Init {
  def main(args: Array[String]) {
    val system = ActorSystem("system")
    val keeper = system.actorOf(Props(new Keeper), "Keeper")
    val gui = new GUI(keeper)
    gui.visible = true
    keeper.tell(AddGUI(gui), null)
    println("End of main function")
    system.awaitTermination()
  }
}