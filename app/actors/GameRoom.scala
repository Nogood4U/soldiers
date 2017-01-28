package actors

import akka.actor.{Actor, ActorRef, Props}
import core.{GameState, PlayerState}

import scala.collection.mutable.{HashMap, ListBuffer}
import scala.concurrent.duration._

/**
  * Created by Sergio on 1/17/2017.
  */
class GameRoom(name: String, maxPlayer: Short, server: ActorRef) extends Actor {
  var timer: ActorRef = _
  var players: HashMap[String, ActorRef] = HashMap.empty
  var started: Boolean = false

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive = {

    case msg@JoinGame(_, playerId, player) =>
      if (started) {
        players += playerId -> player
        timer ! JoinInProgress(playerId, PlayerState(playerId, 0, 0, 1, 1, 100), player)
        player ! msg
        sender ! true
      } else {
        players += playerId -> player
        player ! msg
        sender ! true
      }
    case e: Disconected => if (started) {
      players.remove(e.playerId)
      timer ! e
      server ! e
    }

    case cmd: PlayerCmd => timer ! cmd //maybe extra processing here ? not likely

    case StartRoom => if (!started) {
      timer = context.actorOf(Props(new GameTimer))
      timer ! StartTimer(players.toList, 50, GameState(0, players.to[ListBuffer].map(pl => PlayerState(pl._1, 0, 0, 1, 1, 100))))
      context.system.scheduler.schedule(4 seconds, 50 millis, timer, Update)
      started = !started
    }
  }
}

case class StartRoom(players: List[ActorRef], refreshRate: Short)


