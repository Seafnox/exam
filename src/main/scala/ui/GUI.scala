package ui

import akka.actor.ActorRef
import base.{Node, AddNode, RemoveNode, KeeperTrait}
import scala.swing._

/**
 * Created by Кирилл on 20.02.2015.
 */

class GUI(keeper: KeeperTrait) extends MainFrame {
  title = "Визуальная оболочка мониторинга удалённо запущенных приложений"
  val box = new BoxPanel(Orientation.Vertical)
  val sliderLabelText:String = "Ползунок отвечает за задержку между пакетами сообщений в потоке. Текущая задержка "
  val sliderLabel:Label = new Label(sliderLabelText + "100 миллесекунд")
  box.contents += sliderLabel
  box.contents += new Slider {
    min = 10
    max = 100
    value = 100
    labels = Map(0 -> new Label("0"),50 -> new Label("50"),100 -> new Label("100"))
    paintLabels = true
  }
  var nodeLabelsWrapper = new BoxPanel(Orientation.Horizontal)
  box.contents += nodeLabelsWrapper
  box.contents += new Label("")
  box.contents += new BoxPanel(Orientation.Horizontal) {
    contents += Button("Add Node") { addNode() }
    contents += Swing.HGlue
    contents += Button("Remove Node") { removeNode() }
  }
  contents = box

  def addNode(): Unit = {
    keeper.sender().tell(AddNode,keeper.sender())
  }

  def removeNode(): Unit = {
    keeper.sender().tell(RemoveNode,keeper.sender())
  }

  def updateCounters(): Unit = {
    val counters = keeper.counters
    nodeLabelsWrapper.contents.clear()
    counters.foreach{counter =>
      nodeLabelsWrapper.contents += new Label("Node" + counter._1.toString() + " - " + counter._2)
    }
    nodeLabelsWrapper.repaint()
  }
}
