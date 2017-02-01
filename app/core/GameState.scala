package core

import actors.PlayerCmd
import org.jbox2d.callbacks.{ContactImpulse, ContactListener}
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.shapes.{EdgeShape, PolygonShape}
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts.Contact
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

/**
  * Created by Sergio on 1/17/2017.
  */
case class GameState(var stateTime: Int, var players: ListBuffer[GameObject]) {
  val gravity = new Vec2(0.0f, 0.0f)
  val vel0 = new Vec2(0.0f, 0.0f)
  val world = new World(gravity)
  val velocityIterations = 6
  val positionIterations = 2
  var pBodies: HashMap[String, (Body, PlayerState)] = HashMap.empty
  var bBodies: ListBuffer[(Body, Bullet)] = ListBuffer.empty
  val worldSizeX = 40
  val worldSizeY = 40
  var bulletCounter = 0
  var score: HashMap[String, Int] = HashMap.empty[String, Int]
  var events: ListBuffer[GameEvent] = ListBuffer.empty

  def init() {
    world.setContactListener(new ContactListener() {
      override def postSolve(contact: Contact, impulse: ContactImpulse): Unit = {}

      override def endContact(contact: Contact): Unit = {}

      override def beginContact(contact: Contact): Unit = {
        val object1 = exctractGameObject(contact.getFixtureA.getBody.getUserData)
        val object2 = exctractGameObject(contact.getFixtureB.getBody.getUserData)
        object1.collide(object2) match {
          case Some(e) => events += e
          case _ => None
        }
      }

      def exctractGameObject(value: AnyRef): GameObject = {
        value match {
          case (_, player: GameObject) => player
          // case (_, bullet: Bullet) => bullet
          case _ => GameObject.getDummyObject
        }
      }

      override def preSolve(contact: Contact, oldManifold: Manifold): Unit = {}
    })
    //world size boundaries
    val upperRtCorner = new Vec2(0, 0)
    val upperLfCorner = new Vec2(0, worldSizeY)

    val lowerRtCorner = new Vec2(worldSizeX, 0)
    val lowerLfCorner = new Vec2(worldSizeX, worldSizeY)
    val boundaries = new BodyDef()
    boundaries.position.set(0, 0)
    boundaries.`type` = BodyType.STATIC
    val boundBody = world.createBody(boundaries)
    val boundShape = new EdgeShape()
    boundShape.set(upperRtCorner, upperLfCorner)
    boundBody.createFixture(boundShape, 0)
    boundShape.set(lowerRtCorner, upperRtCorner)
    boundBody.createFixture(boundShape, 0)
    boundShape.set(upperLfCorner, lowerLfCorner)
    boundBody.createFixture(boundShape, 0)
    boundShape.set(lowerLfCorner, lowerRtCorner)
    boundBody.createFixture(boundShape, 0)
    //end of boundaries
    pBodies = players.collect {
      case player: PlayerState =>
        createPlayer(player)
    }.foldLeft(HashMap.empty[String, (Body, PlayerState)]) { (i, body) =>
      i += (body._1.getUserData.asInstanceOf[(String, PlayerState)]._1 -> body)
    }
  }

  def createPlayer(playerState: PlayerState): (Body, PlayerState) = {
    val bd = new BodyDef()
    bd.position.set(5, 5)
    bd.`type` = BodyType.DYNAMIC
    bd.userData = (playerState.playerId, playerState)
    val ps = new PolygonShape()
    ps.setAsBox(0.5f, 0.5f)
    val fd = new FixtureDef()
    fd.shape = ps
    fd.density = 0.0f
    fd.friction = 0.0f
    fd.restitution = 0.0f
    val body = world.createBody(bd)
    body.createFixture(fd)
    body.setBullet(true)
    body -> playerState
  }

  def createPlayer(playerState: PlayerState, jip: Boolean): (Body, PlayerState) = {
    val (body, _) = createPlayer(playerState)
    if (jip) this.players += playerState
    if (jip) pBodies += playerState.playerId -> (body, playerState)
    body -> playerState
  }

  def removePlayer(playerId: String) {
    pBodies.remove(playerId) match {
      case Some(body) =>
        body._1.getWorld.destroyBody(body._1)
      case _ =>
    }
    players = players.filter(p => {
      p match {
        case e: PlayerState => e.playerId != playerId
        case _ => true
      }
    })
  }

  def applyCommand(playerCmd: PlayerCmd) {
    //apply movement
    val vec = new Vec2(playerCmd.cmd.xMv, playerCmd.cmd.yMv)
    pBodies.get(playerCmd.playerId) match {
      case Some(body) =>
        body._1.applyLinearImpulse(vec, body._1.getLocalCenter)
        //apply view orientation
        body._2.viewOr = if (playerCmd.cmd.xMv > 0) 1 else if (playerCmd.cmd.xMv < 0) 0 else body._2.viewOr
        //
        //
        // apply controls
        playerCmd.cmd.btns.foreach(btn => {
          if (btn.equals("shift")) {
            body._2.currWpn = body._2.currWpn match {
              case 1 => 2
              case 2 => 3
              case 3 => 1
              case _ => 1
            }
          }
          if (btn.equals("fire") && body._2.bulletTimer == 0) {
            body._2.bulletTimer = 5
            body._2.currWpn match {
              case 1 =>
                val bullet = createProjectile(body._1, body._2, 70, 0, 20)
                bBodies += bullet
                players += bullet._2
              case 2 =>
                val bullet1 = createProjectile(body._1, body._2, 60, 8, 10)
                val bullet2 = createProjectile(body._1, body._2, 60, -8, 10)
                bBodies += bullet1 += bullet2
                players += bullet1._2 += bullet2._2
              case 3 =>
                val bullet1 = createProjectile(body._1, body._2, 50, 15, 5)
                val bullet2 = createProjectile(body._1, body._2, 60, 0, 5)
                val bullet3 = createProjectile(body._1, body._2, 50, -15, 5)
                bBodies += bullet1 += bullet2 += bullet3
                players += bullet1._2 += bullet2._2 += bullet3._2
              case _ =>
            }

          }
        })
      case _ =>
    }
  }

  def applyCommands(playerCmds: List[PlayerCmd]) = {
    resetFlags(); //reset hit flag
    playerCmds.foreach(applyCommand)
    world.step(1 / 60f, velocityIterations, positionIterations)
    applyStateChange()
    decreaseTimers()
    events
  }

  def createProjectile(playerBody: Body, playerState: PlayerState, speedX: Short, speedY: Short, damage: Short): (Body, Bullet) = {
    val bd = new BodyDef()
    val (x, y) = playerState.viewOr match {
      case 1 => (playerBody.getPosition.x + 0.15f, playerBody.getPosition.y)
      case _ => (playerBody.getPosition.x - 0.15f, playerBody.getPosition.y)
    }
    bd.position.set(x, y)
    bd.`type` = BodyType.KINEMATIC
    val ps = new PolygonShape()
    ps.setAsBox(0.01f, 0.01f)
    val fd = new FixtureDef()
    fd.shape = ps
    fd.density = 0.0f
    fd.friction = 0.0f
    fd.restitution = 0.0f
    val bullet = Bullet(bulletCounter, playerBody.getPosition.x, playerBody.getPosition.y, damage, playerState.playerId)
    bd.userData = (bulletCounter, bullet)
    val body = world.createBody(bd)
    body.createFixture(fd)

    body.setLinearVelocity(playerState.viewOr match {
      case 1 => new Vec2(speedX, speedY)
      case _ => new Vec2(-speedX, -speedY)
    })
    bulletCounter += 1
    body -> bullet
  }

  def applyStateChange() {
    /*
      this should return events , such as , playerA just died , playerB killed PlayerA
      ,with some statistic data ioe:5 bullets fire by playerA, power up picked up by playerB
      */
    players foreach {
      case player: PlayerState =>
        pBodies.get(player.playerId) match {
          case Some(body) =>
            //i am 900% i can do this
            val posX = if ((body._1.getPosition.x < worldSizeX && body._1.getPosition.x > 0))
              body._1.getPosition.x -> false
            else {
              //body._1.setTransform(new Vec2(worldSizeX / 2, body._1.getPosition.y), body._1.getAngle)
              body._1.getPosition.x -> true
            }
            val posY = if (body._1.getPosition.y < worldSizeY && body._1.getPosition.y > 0)
              body._1.getPosition.y -> false
            else {
              body._1.setTransform(new Vec2(body._1.getPosition.x, worldSizeY / 2), body._1.getAngle)
              body._1.getPosition.y -> true
            }
            //check if out of bounds or dead and reset back to middle of screen
            if ((posX._2 || posY._2) && player.alive) {
              body._1.setTransform(new Vec2(worldSizeX / 2, worldSizeY / 2), body._1.getAngle)
            } else if (!player.alive) {
              body._1.setTransform(new Vec2(-15, -15), body._1.getAngle)
            } else {
              //inbounds and alive , set position
              player.posX = posX._1
              player.posY = posY._1
            }
            body._1.setLinearVelocity(vel0)

          case _ =>
        }


      case _ =>
    }
    val (delete, keep) = bBodies.partition(body =>
      body._2.destroy || body._1.getPosition.x > worldSizeX || body._1.getPosition.y > worldSizeY ||
        body._1.getPosition.x < 0 || body._1.getPosition.y < 0
    )
    bBodies = keep
    bBodies.par foreach (body => {
      body._2.posX = body._1.getPosition.x
      body._2.posY = body._1.getPosition.y
    })
    delete foreach (body => {
      body._1.m_world.destroyBody(body._1)
      players remove (players indexOf body._2)
    })
    //pool of bullets , move to an outside area, or flag as not to serialize
  }

  def decreaseTimers(): Unit = {
    players.par foreach (_.decreaseTimers)
  }

  def resetFlags() = {
    players.par foreach (_.resetFlags)
  }
}

abstract class GameObject {
  var posX: Float
  var posY: Float

  def collide(o: GameObject): Option[GameEvent]

  def decreaseTimers

  def resetFlags
}

abstract class GameEvent {
  var eventId: Int
}

case class PlayerKilledEvent(override var eventId: Int = 1, player: String, killedBy: String) extends GameEvent

object GameObject {


  implicit val playerStWrites = Json.writes[PlayerState]
  implicit val bulletStWrites = Json.writes[Bullet]
  implicit val gameObjectWrites = new Writes[GameObject] {
    override def writes(o: GameObject): JsValue = o match {
      case player: PlayerState => Json.toJson[PlayerState](player)
      case bullet: Bullet => Json.toJson[Bullet](bullet)
    }
  }

  def getDummyObject = {
    new GameObject {
      override var posX: Float = _
      override var posY: Float = _

      override def collide(o: GameObject) = {
        None
      }

      override def resetFlags: Unit = {}

      override def decreaseTimers = {}
    }
  }
}

case class PlayerState(playerId: String, var posX: Float, var posY: Float, var viewOr: Byte, var currWpn: Short, var health: Int)
  extends GameObject {
  var hitImmune = false
  var hitImmuneTimer = 0
  var bulletTimer = 0
  var hit = false
  var powerUp = 0
  var powerUpTimer = 30
  val _healt = health
  var alive = true
  var deadTimer = 0


  def resetHealth() {
    health = _healt
  }

  override def decreaseTimers = {
    if (hitImmuneTimer != 0) hitImmuneTimer -= 1 else this.hitImmune = false
    if (powerUpTimer != 0) powerUpTimer -= 1 else this.powerUp = 0
    if (bulletTimer != 0) bulletTimer -= 1 else this.bulletTimer = 0
    if (deadTimer != 0) deadTimer -= 1 else if (!this.alive) {
      this.alive = true
      this.resetHealth()
    }
  }

  override def resetFlags: Unit = {
    this.hit = false
  }

  override def collide(o1: GameObject): Option[GameEvent] = {
    o1 match {
      case bullet: Bullet => bullet collide this
      case _ => None
    }
  }


}

case class Bullet(bulletNum: Int, var posX: Float, var posY: Float, damage: Int, ownerId: String)
  extends GameObject {
  private var isDestroy = false

  def destroy(): Boolean = isDestroy

  override def decreaseTimers = {}

  override def resetFlags: Unit = {}

  override def collide(o: GameObject): Option[GameEvent] = {
    o match {
      case player: PlayerState if player.playerId != this.ownerId =>
        this.isDestroy = true
        if (!player.hitImmune && player.alive) {
          //println(s"reducing ${player.health} health by ${this.damage}")
          player.health -= this.damage
          //println(s"new health ${player.health}")
          player.health match {
            case a if a > 0 =>
              player.hitImmune = true
              player.hit = true
              player.hitImmuneTimer = 4 //game simulation cycles (currently 50 millis each)
              None
            case a if a <= 0 =>
              player.alive = false
              player.deadTimer = 30
              Some(PlayerKilledEvent(player = player.playerId, killedBy = ownerId))
            case _ => None
          }
        } else {
          None
        }

      case _ => None
    }
  }
}
