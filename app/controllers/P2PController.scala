package controllers

import javax.inject._

import actors.p2p.{AddPlayer, P2PDced, P2PPlayer, P2PServer}
import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * Created by Sergio on 1/31/2017.
  */
@Singleton
class P2PController @Inject()(system: ActorSystem, implicit val mat: Materializer, implicit val execContext: ExecutionContext) extends Controller {

  val server = system.actorOf(Props(new P2PServer), "p2pserver")

  def index(playerId: String) = Action {
    Ok(views.html.p2pIndex(playerId))
  }

  def socket(playerId: String) = WebSocket.accept[JsValue, JsValue] { request =>

    val source = Source.actorRef[JsValue](100, OverflowStrategy.dropTail)
    val sink = Sink.asPublisher[JsValue](false)
    val actor = source.toMat(sink)(Keep.both) run
    val playerP2p = system.actorOf(Props(new P2PPlayer(playerId, actor._1, server)))
    server ! AddPlayer(playerId, playerP2p)
    //player ! SocketActor(actor._1)
    val in = Sink.actorRef(playerP2p, P2PDced(playerId))
    Source.fromPublisher(actor._2)
    Flow.fromSinkAndSource(in, Source.fromPublisher(actor._2))
  }
}