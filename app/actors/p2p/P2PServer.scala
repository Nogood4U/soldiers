package actors.p2p

import akka.actor.{Actor, ActorRef}

/**
  * Created by Sergio on 1/31/2017.
  */
class P2PServer extends Actor {

  var players: Map[String, ActorRef] = Map.empty

  override def receive: Receive = {
    case AddPlayer(id, player) => players += (id -> player)

    case msg: P2PServerMessage => players.get(msg.p2pMessage.enemyId) match {
      case Some(player) =>
        player forward P2PMessage(msg.playerId, msg.p2pMessage.messageType, msg.p2pMessage.sdp, msg.p2pMessage.candidate)
      case _ => println(s"Got EnemyId: ${msg.p2pMessage.enemyId}")
    }
  }
}

case class AddPlayer(playerId: String, player: ActorRef)

case class SendMessage()