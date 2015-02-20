package base

import ui.GUI

// визуальный кипер
class Keeper extends KeeperTrait {
  // подключаем гуя
  // старым дедовским костылём преодолеваем приграду
  val gui = new GUI(this)
  override def updateNodeCounters(): Unit = {
    super.updateNodeCounters()
    // вывод на экран
  }

}
