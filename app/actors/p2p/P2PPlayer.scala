package actors.p2p

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import play.api.libs.json.{JsSuccess, JsValue, Json}

/**
  * Created by Sergio on 1/31/2017.
  */
class P2PPlayer(playerId: String, out: ActorRef, server: ActorRef) extends Actor {
  val p2pMessageWrites = Json.writes[P2PMessage]
  val p2pMessageReads = Json.reads[P2PMessage]

  override def receive: Receive = {
    case e: JsValue => Json.fromJson(e)[P2PMessage] match {
      case JsSuccess(msg, _) => server ! P2PServerMessage(playerId, msg)

      case e: P2PMessage => out ! Json.toJson[P2PMessage](e)
    }
  }

}

case class P2PDced()

case class P2PMessage(enemyId: String, messageType: String, sdp: Array[Byte], candidate: Array[Byte])

case class P2PServerMessage(playerId: String, p2pMessage: P2PMessage)
