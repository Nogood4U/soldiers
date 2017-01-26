package core

import javax.inject.Inject

import actors.GameServer
import akka.actor.{ActorSystem, Props}

/**
  * Created by Sergio on 1/17/2017.
  */
class AppState @Inject()(system: ActorSystem){
 val ref = system.actorOf(Props(new GameServer),"server")
  println(ref)
}
