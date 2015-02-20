import akka.actor.{ActorSystem, Props}
import base.{Node, Keeper}

object Init {
  def main(args: Array[String]) {
    val system = ActorSystem("system")
    val keeper = system.actorOf(Props(new Keeper), "Keeper")
    println("End of main function")
  }
}