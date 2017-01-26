package actors

import akka.actor.{Actor, ActorRef}
import core.GameState

import scala.collection.mutable.ListBuffer

/**
  * Created by Sergio on 1/17/2017.
  */
class GameTimer extends Actor {
  var lastSampledTime = 0l
  var players: List[(String, ActorRef)] = _
  var state: GameState = _
  var commands: ListBuffer[PlayerCmd] = ListBuffer.empty

  override def receive = {
    case StartTimer(players, tick, _state) => {
      this.players = players
      state = _state;
      state.init
    } //execute game Logic , resend message , wait for time-span the re-execute logic ?
    case Update => {
      state.stateTime += 1
      state.applyCommands(commands.toList)
      commands = ListBuffer.empty
      players.foreach(player => player._2 ! GameUpdate(state))
    }

    case cmd: PlayerCmd => {
      commands += cmd
    }
  }
}

case class StartTimer(players: List[(String, ActorRef)], refreshRate: Short, initialState: GameState)

case class Update()