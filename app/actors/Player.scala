package actors

import akka.actor.{Actor, ActorRef, PoisonPill}
import core.{Bullet, GameState, PlayerState}
import play.api.libs.json.{JsSuccess, JsValue, Json}


/**
  * Created by Sergio on 1/17/2017.
  */
class Player(id: String) extends Actor {

  var socketActor: ActorRef = _
  var roomActor: ActorRef = _
  implicit val cmdReads = Json.reads[Command]

  override def receive = {
    case msg: Array[Byte] =>
      val cmd = Json.parse(msg)

      Json.fromJson[Command](cmd) match {
        case JsSuccess(_cmd, _) => if (roomActor != null) roomActor ! PlayerCmd(id, _cmd)
        case _ => println("invalid command")
      }
    case cmd: PlayerCmd => if (roomActor != null) {
      roomActor ! cmd
    }
    case _: JoinGame => roomActor = sender

    case SocketActor(sa) => {
      socketActor = sa
    }
    case msg@GameUpdate(state) => if (socketActor != null) socketActor ! state

    case e: Disconected =>
      self ! PoisonPill
      if (roomActor != null) {
        roomActor ! e
      }
  }
}


case class Command(xMv: Float, yMv: Float, viewOr: Byte, weapon: Short, btns: List[String])

case class PlayerCmd(playerId: String, cmd: Command)

case class SocketActor(socketActor: ActorRef)

case class GameUpdate(state: GameState)

case class Disconected(playerId: String)
