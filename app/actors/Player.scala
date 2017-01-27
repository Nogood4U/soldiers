package actors

import akka.actor.{Actor, ActorRef}
import core.{Bullet, GameState, PlayerState}
import play.api.libs.json.{JsValue, Json}


/**
  * Created by Sergio on 1/17/2017.
  */
class Player(id: String) extends Actor {

  var socketActor: ActorRef = _
  var roomActor: ActorRef = _

  override def receive = {
    case cmd: PlayerCmd => if (roomActor != null) {
      roomActor ! cmd
    }
    case _: JoinGame => roomActor = sender

    case SocketActor(sa) => {
      socketActor = sa
    }
    case msg@GameUpdate(state) => if (socketActor != null) socketActor ! state
  }
}


case class Command(xMv: Float, yMv: Float, viewOr: Byte, weapon: Short, btns: List[String])

case class PlayerCmd(playerId: String, cmd: Command)

case class SocketActor(socketActor: ActorRef)

case class GameUpdate(state: GameState)
