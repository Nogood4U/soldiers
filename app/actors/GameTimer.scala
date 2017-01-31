package actors

import akka.actor.{Actor, ActorRef}
import core.{GameState, PlayerKilledEvent, PlayerState}

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

/**
  * Created by Sergio on 1/17/2017.
  */
class GameTimer extends Actor {
  var lastSampledTime = 0l
  var players: HashMap[String, ActorRef] = HashMap.empty
  var state: GameState = _
  var commands: ListBuffer[PlayerCmd] = ListBuffer.empty

  override def receive = {
    case StartTimer(_players, _, _state) =>
      this.players ++= _players
      state = _state
      state.init()
    //execute game Logic , resend message , wait for time-span the re-execute logic ?
    case Update =>
      state.stateTime += 1
      state.applyCommands(commands.toList) foreach {
        case e: PlayerKilledEvent => {
          //process scoreboard Here
          state.score(e.killedBy) = state.score.getOrElse(e.killedBy, 0) + 1
        }
      }
      state.events = ListBuffer.empty //reset all events , events already dispatched to some event handler
      commands = ListBuffer.empty
      players.foreach(player => player._2 ! GameUpdate(state))


    case cmd: PlayerCmd =>
      commands += cmd


    case join: JoinInProgress =>
      println("adding player in progress")
      this.players += join.playerId -> join.player
      state.createPlayer(join.playerState, jip = true) //joined in progress

    case e: Disconected =>
      println(s"${e.playerId} disconnected removing player from game")
      players.remove(e.playerId)
      state.removePlayer(e.playerId)

  }
}

case class StartTimer(players: List[(String, ActorRef)], refreshRate: Short, initialState: GameState)

case class JoinInProgress(playerId: String, playerState: PlayerState, player: ActorRef)

case class Update()