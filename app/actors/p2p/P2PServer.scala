package actors.p2p

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

/**
  * Created by Sergio on 1/31/2017.
  */
class P2PServer extends Actor {

  var players: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty

  override def receive: Receive = {
    case AddPlayer(id, player) => players += (id -> player)

    case P2PDced(playerId) => players.remove(playerId)

    case msg: P2PServerMessage =>
      println(msg)
      players.get(msg.p2pMessage.enemyId.getOrElse("")) match {
        case Some(player) =>
          player forward P2PMessage(Some(msg.playerId), msg.p2pMessage.messageType, msg.p2pMessage.sdp, msg.p2pMessage.candidate)
        case _ => /*players.filter(_._1 != msg.playerId)
          .foreach(_._2 forward P2PMessage(Some(msg.playerId), msg.p2pMessage.messageType, msg.p2pMessage.sdp, msg.p2pMessage.candidate))*/
      }
  }
}

case class AddPlayer(playerId: String, player: ActorRef)

case class SendMessage()