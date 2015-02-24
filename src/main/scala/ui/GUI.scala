package ui

import java.util
import javax.swing.table.DefaultTableModel

import akka.actor.ActorRef
import base._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.swing._
import scala.swing.event.ValueChanged

/**
 * Created by Кирилл on 20.02.2015.
 */

class GUI(keeper: ActorRef) extends MainFrame {
  println("Start compiling gui")
  title = "Визуальная оболочка мониторинга удалённо запущенных приложений"
  val box = new BoxPanel(Orientation.Vertical)
  val sliderLabelText:String = "Ползунок отвечает за задержку между пакетами сообщений в потоке. Текущая задержка "
  val sliderLabel:Label = new Label(sliderLabelText + "100 миллесекунд")
  box.contents += new FlowPanel {
    contents += sliderLabel
  }
  // ползунок, для выбора задержки, между пакетами сообщений
  val slider: Slider = new Slider {
    min = 10
    max = 100
    value = 100
    labels = Map(0 -> new Label("0"),50 -> new Label("50"),100 -> new Label("100"))
    paintLabels = true
  }
  box.contents += slider
  // таблица, в которой будут выводиться результаты
  var nodeTable = new Table() {
    rowHeight = 25
    showGrid = true
    ignoreRepaint = false
    val dtm: DefaultTableModel = new DefaultTableModel(null, Array[Any]("Column 1","Column 2").asInstanceOf[Array[Object]])
    dtm.setRowCount(2)
    model = dtm
  }
  box.contents += nodeTable
  // кнопки для работы с нодами (добавить и удалить)
  box.contents += new BoxPanel(Orientation.Horizontal) {
    contents += Button("Add Node") { addNode() }
    contents += Swing.HGlue
    contents += Button("Remove Node") { removeNode() }
  }
  contents = box

  // просим приложение слушать слайдер
  listenTo(slider)

  reactions += {
    case ValueChanged(`slider`) =>
      println("Your slider is now: " + slider.value)
      if (slider.value <= 100 && slider.value >= 10) {
        // отсылаем данные о изменениях скорости ползунка
        keeper.tell(SetDelay(slider.value), null)
      }
      sliderLabel.text = sliderLabelText + slider.value + " миллесекунд"
  }

  def addNode(): Unit = {

    println("Push CreateNode")
    // отсылаем команду на создание ноды
    keeper.tell(CreateNode(),null)
  }

  def removeNode(): Unit = {

    println("Push DestroyNode")
    // отсылаем команду на удаление ноды
    keeper.tell(DestroyNode(),null)
  }

  // функция перерисовки данных таблицы
  def updateCounters(counters: mutable.Map[ActorRef, Int]): Unit = {
    val cells = counters.size
    //println("Cells = " + cells)
    nodeTable.dtm.setRowCount(cells)
    nodeTable.model = nodeTable.dtm
    // двойной репейнт по идее не нужен
    nodeTable.repaint()
    var inc = 0
    counters.foreach{counter =>
      //println("update row " + inc)
      nodeTable.update( inc, 0, counter._1)
      nodeTable.update(inc, 1, counter._2)
      inc+=1
    }
    nodeTable.repaint()
  }

  // операция закрытия приложения, вызывает самоубийство кипера
  override def closeOperation() {
    keeper.tell(SetClose(), null)
    sys.exit(0)
  }

  println("End compiling gui")
}
