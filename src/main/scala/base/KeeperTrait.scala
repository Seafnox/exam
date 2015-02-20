package base

import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import scala.collection.mutable
import scala.collection.mutable.MutableList
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

// трейт с методами
// используем абстрактный класс, чтобы обойти проблему перерывания
trait KeeperTrait extends Actor with ActorLogging {
  // переменная важная - нужна только системе
  implicit val timeout = Timeout(5 seconds)
  // сколько запускаем нод изначально
  val startNodeCount = 10
  var nodeCounter = 0
  // список нод, для опроса
  var nodeList: MutableList[ActorRef] = MutableList.empty
  // счётчики для кэша
  var counters: mutable.Map[ActorRef, Int] = mutable.Map.empty
  // переменная, которая отвечает за поток
  val system = ActorSystem("system")

  // запуск без стартера
  // создаём ноды
  (1 to startNodeCount).foreach{_ => this.addNode() }
  updateNodeCounters()
  // говорим системе не падать
  system.awaitTermination()

  // добавляем ноду, оповещаем всех о новой ноде
  def addNode(): Unit = {
    val newNode = system.actorOf(Props(new Node(nodeList)), "Node" + nodeCounter.toString)
    nodeList.foreach{ node => node ! AddNode(newNode) }
    nodeList += newNode
  }

  // удаляем ноду и предупреждаем всех
  def removeNode(): Unit = {
    val last = nodeList.last
    nodeList = nodeList filterNot(_ == last)
    nodeList.foreach{ node => node ! RemoveNode(last) }
  }

  // выставляем новую задержку всем нодам
  def setDelay(newDelay: Int): Unit = {
    nodeList.foreach{ node => node ! SetDelay(newDelay) }
  }

  // обновляем возвращённые данные счётчика
  def updateCounter(node:ActorRef, count:Int): Unit = {
    counters.update(node, count)
  }

  // циклично обновляем данные счётчика для интерфейса
  def updateNodeCounters(): Unit = {
    system.scheduler.scheduleOnce(500 milliseconds)(this.updateNodeCounters())
    nodeList.foreach{ node => node ? GetLastSendedMessCount }
  }

  def receive = {
    case r: SetLastSendedMessCount =>
      updateCounter(sender, r.count)
    case r =>
      log.warning(s"Unexpected: $r")

  }
}