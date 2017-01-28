package controllers

import javax.inject._

import actors._
import akka.actor.{ActorRef, ActorSystem}
import play.api.mvc._

import scala.concurrent.duration._
import akka.pattern.{ask}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.{Timeout}
import core.{AppState, Bullet, GameState, PlayerState}
import game.core.ProtoGameState

import scala.concurrent.{ExecutionContext}
import play.api.mvc.WebSocket.MessageFlowTransformer

import scala.collection.JavaConverters._


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(system: ActorSystem, implicit val mat: Materializer, implicit val execContext: ExecutionContext, state: AppState) extends Controller {

  implicit val timeout = Timeout(2 seconds)
  implicit val messageFlowTransformer = MessageFlowTransformer.byteArrayMessageFlowTransformer

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def socket(serverId: String, gameId: String) = WebSocket.acceptOrResult[Array[Byte], Array[Byte]] { request =>
    createSocket(serverId, gameId)
  }

  def createGame(serverId: String, name: String, maxPlayer: Int) = Action.async {
    for {
      server <- system.actorSelection("/user/" + s"$serverId").resolveOne()
      gameId <- server ? CreateGame(name, maxPlayer.toShort)
    } yield Created(gameId.asInstanceOf[String])
  }

  def joinGame(serverId: String, playerId: String, gameId: String) = Action.async {
    (for {
      server <- system.actorSelection("/user/" + s"$serverId").resolveOne()
      player <- (server ? GetPlayer(playerId)).mapTo[ActorRef]
      resp <- server ? JoinGame(gameId, playerId, player)
    } yield {
      resp match {
        case e: Boolean => Ok(e.toString)
      }
    }) recover {
      case e => Ok("false err")
    }
  }

  def startGame(serverId: String, gameId: String) = Action.async {
    for {
      server <- system.actorSelection("/user/" + s"$serverId").resolveOne()
    } yield {
      server ! StartGame(gameId)
      Ok
    }
  }

  def createSocket(serverId: String, playerId: String) = {

    for {
      server <- system.actorSelection("/user/" + s"$serverId").resolveOne()
      player <- (server ? GetPlayer(playerId)).mapTo[ActorRef]
    } yield {
      val source = Source.actorRef[GameState](100, OverflowStrategy.dropTail)
      val sink = Sink.asPublisher[GameState](false)
      val actor = source.toMat(sink)(Keep.both) run

      player ! SocketActor(actor._1)
      val in = Sink.actorRef(player, Disconected(playerId))
      /* val in = Sink.foreach[Array[Byte]] {
         msg =>
           println("closed")
           Json.parse(msg) match {
             case cmd: JsValue => Json.fromJson[Command](cmd) match {
               case JsSuccess(_cmd, _) => player ! PlayerCmd(playerId, _cmd)
               case _ => println("invalid command")
             }
             case _ =>
           }
       }*/

      val protobuffedSource = Source.fromPublisher(actor._2).map {
        case GameState(stateTime, players) =>
          val stateBuilder = ProtoGameState.State
            .newBuilder()
            .setStateTime(stateTime)
          val (playerStates, bullets) = players.partition {
            case e: PlayerState => true
            case e: Bullet => false
          }
          stateBuilder.addAllPlayers(playerStates.map(_.asInstanceOf[PlayerState]).map(ps => {
            ProtoGameState.State.Player.newBuilder()
              .setPlayerId(ps.playerId)
              .setPosX(ps.posX)
              .setPosY(ps.posY)
              .setViewOr(ps.viewOr)
              .setCurrWpn(ps.currWpn)
              .setHealth(ps.health).build()
          }).asJava)
          stateBuilder.addAllBullets(bullets.map(_.asInstanceOf[Bullet]).map(bs => {
            ProtoGameState.State.Bullet.newBuilder()
              .setOwnerId(bs.ownerId)
              .setPosX(bs.posX)
              .setPosY(bs.posY)
              .setDamage(bs.damage)
              .setBulletNum(bs.bulletNum)
              .build()
          }).asJava)
          stateBuilder.build().toByteArray
      }


      Either.cond(test = true, Flow.fromSinkAndSource(in, protobuffedSource), Forbidden)
    }


  }


}
