package actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}

import scala.collection.mutable.HashMap

/**
  * Created by Sergio on 1/17/2017.
  */
class GameServer extends Actor {

  var rooms: HashMap[(String, String), ActorRef] = HashMap.empty
  var players: HashMap[String, ActorRef] = HashMap.empty

  override def receive = {
    case CreateGame(name: String, maxPlayer: Short) => rooms.find(room => room._1._2 == name) match {
      case Some(elm) => sender ! elm._1._1

      case None =>
        //create game , send id to sender
        val roomId = UUID.randomUUID().toString
        val (id, room) = (roomId, name) -> context.actorOf(Props(new GameRoom(name, maxPlayer, self)), name)
        rooms += (id -> room)
        sender ! roomId
        room ! StartRoom //auto start any game

    }

    case GetPlayer(playerId, create) => players.get(playerId) match {
      case Some(player) if !create => sender ! player
      case None if create =>
        val newPlayer = context.actorOf(Props(new Player(playerId)), playerId)
        players(playerId) = newPlayer
        println(s"creating player ${playerId}")
        sender ! newPlayer
      case _ =>

    }

    case CreatePlayer(playerId) => players.get(playerId) match {
      case Some(_) => sender ! CreateFailure
      case None =>
        val newPlayer = context.actorOf(Props(new Player(playerId)), playerId)
        players(playerId) = newPlayer
        println(s"creating player ${playerId}")
        sender ! CreateSuccess(newPlayer)
      case _ =>

    }


    case StartGame(gameId) => rooms.find((data) => {
      gameId.equals(data._1._1)
    }) match {
      case Some(elm) => elm._2 ! StartRoom
      case None =>
    }

    case msg@JoinGame(gameId, _, _) => rooms.find(room => room._1._1 == gameId) match {
      case Some(elm) =>
        //send actorRef
        println(s"forwarding msg to ${elm._2}")
        elm._2 forward msg

      case None =>
        sender ! false

    }
    case msg: RoomList => {
      sender ! RoomList(rooms.keySet.toList)
    }

    case e: Disconected => players.remove(e.playerId)
  }
}

case class StartGame(gameID: String)

case class CreateGame(name: String, maxPlayer: Short)

case class EndGame(gameID: String)

case class GetPlayer(playerId: String, create: Boolean)

case class CreatePlayer(playerId: String)

case class CreateSuccess(player: ActorRef)

case class CreateFailure()

case class PlayerList(gameID: String)

case class RoomList(rooms: List[(String, String)])

case class JoinGame(gameId: String, playerId: String, player: ActorRef)
