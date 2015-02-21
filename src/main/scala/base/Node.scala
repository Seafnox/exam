package base

import akka.actor.{ActorRef, ActorSystem, ActorLogging, Actor}
import scala.collection.mutable._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

case class SendRequest(value: String)
case class AddNode(new_node: ActorRef)
case class RemoveNode(node: ActorRef)
case class LastMessCountPush(node: ActorRef, count: Int)
case class LastMessCountRequest()
case class SetDelay(value: Int)
case class GetResponse(key: Option[String])
case class SetClose()
import akka.pattern.{ask, pipe}

class Node(_nodeList: MutableList[ActorRef]) extends Actor with ActorLogging {
  // интервал выборки последних сообщений, которыми будет засорятся память, до момента очистки
  val lastRanger : Long = 1 * 1000
  // пока решение прекращения работы ноды по переменной, видел cancellable, но не разобрался
  var isActive = true
  // список нод, по которым происходит отправление сообщений, динамический
  var nodeList: MutableList[ActorRef] = _nodeList
  // задержка перед следующей массовой отправкой сообщений
  var delay: Int = 400
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
    sendedMessList += System.currentTimeMillis
  }

  // метод делает массовую отправку сообщений во все известные ноды
  def sendAll(text: String): Unit = {
    if (isActive) {
      context.system.scheduler.scheduleOnce(delay milliseconds)(this.sendAll(text))
    }
    // запускаем обновление счётчика, чтобы не терзать процессор
    updateLastSendedMessCount()
    nodeList.foreach{node => send(node, text)}
  }

  // очищаем список от устаревших данных, захламляя память мёртвыми данными
  def removeOlderMessages() = {
    val diff: Long = System.currentTimeMillis - lastRanger
    sendedMessList = sendedMessList.filter(_ >= diff).clone()
  }

  // метод обновляет данные об отправленных сообщениях, пользуясь текущей меткой времени
  def updateLastSendedMessCount() = {
    removeOlderMessages()
    val timestamp: Long = System.currentTimeMillis
    lastSendedMessCount = sendedMessList.count(_ >= timestamp - lastRanger)
  }

  // приём
  def receive = {
    // Выставляем новое значение задержки сообщений
    case r: SetDelay =>
      delay = r.value
      log.info("delay setted " + delay)
    // добавляем новую ноду
    case r: AddNode =>
      nodeList += r.new_node
      log.info("add New Node " + r.new_node.toString())
    // удаляем старую ноду
    case r: RemoveNode =>
      if (nodeList.contains(r.node)) {
        nodeList = nodeList.filterNot(_ == r.node).clone()
      }
      log.info("remove Node " + r.node.toString())
    // гостевое сообщение от дружественной ноды
    case r: SendRequest =>
      // пока ничего не делаем
      // log.info(s"Node $nodeName send $lastSendedMessCount until last second")
    // отдаём данные о последних секундах. это для интерфейса
    case r: LastMessCountRequest =>
      //log.info(sender + " " + lastSendedMessCount)
      sender ! LastMessCountPush(self, lastSendedMessCount)
    case r: SetClose =>
      isActive = false
      log.info("Shut down node")
      context.stop(self)
    // ошибка какая-то
    case r =>
      log.warning(s"Unexpected: $r")
  }
}
