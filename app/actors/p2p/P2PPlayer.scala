package actors.p2p

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import play.api.libs.json.{JsString, JsSuccess, JsValue, Json}

/**
  * Created by Sergio on 1/31/2017.
  */
class P2PPlayer(playerId: String, out: ActorRef, server: ActorRef) extends Actor {
  implicit val p2pMessageWrites = Json.writes[P2PMessage]
  implicit val p2pMessageReads = Json.reads[P2PMessage]

  override def receive: Receive = {
    case e: JsValue => Json.fromJson[P2PMessage](e) match {
      case JsSuccess(msg, _) => server ! P2PServerMessage(playerId, msg)
      // println(s"Got message: ${msg}")
      case f => println(s"unsupported...got message ${e}")
      //  println(s"unsupported...got error ${f}")
    }
    case e: P2PMessage => out ! Json.toJson[P2PMessage](e)
    //  println(s"Sending message: ${e}")

    case e: P2PDced => server ! e
  }

}

case class P2PDced(playerId: String)

case class P2PMessage(enemyId: Option[String], messageType: String, sdp: JsValue, candidate: JsValue)

case class P2PServerMessage(playerId: String, p2pMessage: P2PMessage)
