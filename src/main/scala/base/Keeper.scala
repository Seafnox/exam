package base

import akka.actor.ActorRef
import ui.GUI

case class AddGUI(gui : GUI)
case class CreateNode()
case class DestroyNode()
// визуальный кипер
class Keeper extends KeeperTrait {
  // подключаем гуя
  var gui : GUI = null

  def addGUI(gui_ : GUI): Unit = {
    log.info("Add GUI")
    gui = gui_
  }
  // старым дедовским костылём преодолеваем приграду
  override def updateNodeCounters(): Unit = {
    super.updateNodeCounters()
    // вывод на экран
    //println("GUI - " + gui)
    if (gui != null) {
      gui.updateCounters(counters)
    }
  }

  override def receive = {
    case r: AddGUI =>
      addGUI(r.gui)
    case r: CreateNode =>
      addNode()
    case r: DestroyNode =>
      removeNode()
    case r: SetDelay =>
      setDelay(r.value)
    case r: LastMessCountPush =>
      updateCounter(r.node, r.count)
    case r: SetClose =>
      shutdown()
    case r =>
      log.warning(s"Unexpected: $r")
  }
}
