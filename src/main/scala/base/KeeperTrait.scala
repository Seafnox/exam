package base

import akka.actor._
import akka.util.Timeout
import akka.pattern.{ask,pipe}
import ui.GUI
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
  val startNodeCount = 2
  var nodeCounter = 0
  // список нод, для опроса
  var nodeList: MutableList[ActorRef] = MutableList.empty
  // счётчики для кэша
  var counters: mutable.Map[ActorRef, Int] = mutable.Map.empty

  // запуск без стартера
  // создаём ноды
  (1 to startNodeCount).foreach{_ => this.addNode() }
  updateNodeCounters()
  // говорим системе не падать

  // добавляем ноду, оповещаем всех о новой ноде
  def addNode(): Unit = {
    log.info("add node")
    nodeList.synchronized {
      val newNode = context.actorOf(Props(new Node(nodeList)), "Node" + nodeCounter.toString)
      nodeCounter += 1
      nodeList.foreach{ node => node ! AddNode(newNode) }
      nodeList += newNode
    }
  }

  def removeNodeCounter(node: ActorRef): Unit = {
    counters.synchronized {
      counters -= node
    }
  }

  // удаляем ноду и предупреждаем всех
  def removeNode(): Unit = {
    log.info("remove node")
    nodeList.synchronized {
      val last = nodeList.last
      nodeList = nodeList.filterNot(_ == last)//.clone()
      nodeList.foreach{ node => node ! RemoveNode(last) }
      removeNodeCounter(last)
      last ! SetClose
      context.stop(last)
    }
  }

  // выставляем новую задержку всем нодам
  def setDelay(newDelay: Int): Unit = {
    log.info("set delay")
    nodeList.synchronized {
      nodeList.foreach { node => node ! SetDelay(newDelay)}
    }
  }

  // обновляем возвращённые данные счётчика
  def updateCounter(node:ActorRef, count:Int): Unit = {
    counters.synchronized {
      counters.update(node, count)
    }
  }

  // циклично обновляем данные счётчика для интерфейса
  def updateNodeCounters(): Unit = {
    nodeList.synchronized {
      context.system.scheduler.scheduleOnce(500 milliseconds)(this.updateNodeCounters())
      //nodeList.foreach{ node => node ? GetLastSendedMessCount }
      nodeList.foreach{ node =>
        val response = node ? LastMessCountRequest()
        response pipeTo self
      }
    }
  }

  def shutdown(): Unit = {
    nodeList.synchronized {
      nodeList.foreach { node =>
        node ! SetClose
      }
    context.stop(self)
  }
  }

  def receive = {
    case r: AddNode =>
      addNode()
    case r: RemoveNode =>
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