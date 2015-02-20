package base

import akka.actor.{ActorRef, ActorSystem, ActorLogging, Actor}
import scala.collection.mutable._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.pattern.{ask, pipe}

class Node(_nodeList: MutableList[ActorRef]) extends Actor with ActorLogging {
  // интервал выборки последних сообщений, которыми будет засорятся память, до момента очистки
  val lastRanger = 60
  // переменная, которая отвечает за поток
  val system = ActorSystem("system")
  // пока решение прекращения работы ноды по переменной, видел cancellable, но не разобрался
  var isActive = true
  // список нод, по которым происходит отправление сообщений, динамический
  var nodeList: MutableList[ActorRef] = _nodeList
  // задержка перед следующей массовой отправкой сообщений
  var delay: Int = 100
  // список, содержащий времена доставки сообщений, сами сообщения нам не нужны
  var sendedMessList: MutableList[Long] = MutableList.empty
  // переменная содержащая количество сообщений за интервал, обновляется при отправки сообщений другим нодам
  var lastSendedMessCount = 0
  // просто любое имя идентификации ноды
  var nodeName = this.toString
  // начать работу с отправки
  sendAll("bang!")

  // метод отправляет запрос на доставку сообщения в конкретную ноду и багополучно об этом забывает
  def send(node: ActorRef, text: String) = {
    // тут проблема
    node ! SendRequest(text)
    sendedMessList += System.currentTimeMillis / 1000
  }

  // метод делает массовую отправку сообщений во все известные ноды
  def sendAll(text: String): Unit = {
    if (isActive) {
      system.scheduler.scheduleOnce(delay milliseconds)(this.sendAll(text))
    }
    // запускаем обновление счётчика, чтобы не терзать процессор
    updateLastSendedMessCount()
    nodeList.foreach{node => send(node, text)}
  }

  // очищаем список от устаревших данных, захламляя память мёртвыми данными
  def removeOlderMessages() = {
    val timestamp: Long = System.currentTimeMillis / 1000
    sendedMessList = sendedMessList filter(_ >= timestamp - lastRanger)
  }

  // метод обновляет данные об отправленных сообщениях, пользуясь текущей меткой времени
  def updateLastSendedMessCount() = {
    val timestamp: Long = System.currentTimeMillis / 1000
    lastSendedMessCount = sendedMessList.filter(_ >= timestamp - lastRanger).size
    removeOlderMessages()    
  }

  // приём
  def receive = {
    // Выставляем новое значение задержки сообщений
    case r: SetDelay =>
      delay = r.value
    // добавляем новую ноду
    case r: AddNode =>
      nodeList += r.new_node
    // удаляем старую ноду
    case r: RemoveNode =>
      if (nodeList.contains(r.node)) {
        nodeList = nodeList filterNot(_ == r.node)
      }
    // гостевое сообщение от дружественной ноды
    case r: SendRequest =>
      // пока ничего не делаем
      log.info(s"Node $nodeName send $lastSendedMessCount until last second")
    // отдаём данные о последних секундах. это для интерфейса
    case r: GetLastSendedMessCount =>
      sender ! SetLastSendedMessCount(lastSendedMessCount)
    // ошибка какая-то
    case r: SetClose =>
      isActive = false
    case r =>
      log.warning(s"Unexpected: $r")
  }
}
