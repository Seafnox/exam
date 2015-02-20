package base

import akka.actor.ActorRef

case class SendRequest(value: String)
case class AddNode(new_node: ActorRef)
case class RemoveNode(node: ActorRef)
case class GetLastSendedMessCount()
case class SetLastSendedMessCount(count: Int)
case class SetDelay(value: Int)
case class GetResponse(key: Option[String])
case class SetClose()