package controllers

import javax.inject._

import actors._
import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import play.api.mvc._

import scala.concurrent.duration._
import akka.pattern.{AskTimeoutException, ask}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.Timeout
import core.AppState
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(system: ActorSystem, implicit val mat: Materializer, implicit val execContext: ExecutionContext, state: AppState) extends Controller {

  implicit val timeout = Timeout(2 seconds)
  implicit val cmdReads = Json.reads[Command]

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def socket(serverId: String, gameId: String) = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
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
      resp <- server ? GetPlayer(playerId)
    } yield {
      val player = resp.asInstanceOf[ActorRef]
      val source = Source.actorRef(100, OverflowStrategy.dropTail)
      val sink = Sink.asPublisher(false)

      val actor = source.toMat(sink)(Keep.both) run

      player ! SocketActor(actor._1)

      val in = Sink.foreach[JsValue] {
        msg =>
          Json.fromJson[Command](msg) match {
            case JsSuccess(cmd, _) => player ! PlayerCmd(playerId, cmd)
            case _ =>
          }
      }
      Either.cond(test = true, Flow.fromSinkAndSource(in, Source.fromPublisher(actor._2)), Forbidden)
    }


  }


}
