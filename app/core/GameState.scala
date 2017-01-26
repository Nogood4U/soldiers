package core

import actors.PlayerCmd
import org.jbox2d.callbacks.{ContactImpulse, ContactListener}
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.shapes.{EdgeShape, PolygonShape}
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts.Contact
import play.api.libs.json.{JsValue, Json, Writes}

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
  var pBodies: Map[String, (Body, PlayerState)] = Map.empty
  var bBodies: ListBuffer[(Body, Bullet)] = ListBuffer.empty
  val worldSizeX = 20
  val worldSizeY = 15
  var bulletCounter = 0

  def init() {
    world.setContactListener(new ContactListener() {
      override def postSolve(contact: Contact, impulse: ContactImpulse): Unit = {}

      override def endContact(contact: Contact): Unit = {}

      override def beginContact(contact: Contact): Unit = {
        val object1 = exctractGameObject(contact.getFixtureA.getBody.getUserData())
        val object2 = exctractGameObject(contact.getFixtureB.getBody.getUserData())
        object1.collide(object2)
        //  println(s"object ${contact.getFixtureA.getBody.getUserData} colided with ${contact.getFixtureB.getBody.getUserData}")
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
    val upperRtCorner = new Vec2(-1.0f, -1.0f)
    val upperLfCorner = new Vec2(-1.0f, worldSizeY)

    val lowerRtCorner = new Vec2(worldSizeX, -1.0f)
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
        val bd = new BodyDef()
        bd.position.set(5, 5)
        bd.`type` = BodyType.DYNAMIC
        bd.userData = (player.playerId, player)
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
        body -> player

    }.foldLeft(Map.empty[String, (Body, PlayerState)]) { (i, body) =>
      i + (body._1.getUserData.asInstanceOf[(String, PlayerState)]._1 -> body)
    }
  }

  def applyCommand(playerCmd: PlayerCmd) {
    //aply movement
    val vec = new Vec2(playerCmd.cmd.xMv, playerCmd.cmd.yMv)
    val body = pBodies.get(playerCmd.playerId).get
    body._1.applyLinearImpulse(vec, body._1.getLocalCenter)
    //apply view orientation
    body._2.viewOr = if (playerCmd.cmd.xMv > 0) 1 else if (playerCmd.cmd.xMv < 0) 0 else body._2.viewOr
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
      if (btn.equals("fire")) {
        val bullet = createProyectile(body._1, body._2, 60, 0)
        bBodies += bullet
        players += bullet._2
      }
    })
    //
  }

  def applyCommands(playerCmds: List[PlayerCmd]) {
    playerCmds.foreach(applyCommand)
    world.step(1 / 60f, velocityIterations, positionIterations)
    applyStateChange()
  }

  def createProyectile(playerBody: Body, playerState: PlayerState, speedX: Short, speedY: Short) = {
    val bd = new BodyDef()
    val (x, y) = playerState.viewOr match {
      case 1 => (playerBody.getPosition.x + 0.5f, playerBody.getPosition.y)
      case _ => (playerBody.getPosition.x - 0.5f, playerBody.getPosition.y)
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
    val bullet = Bullet(bulletCounter, playerBody.getPosition.x, playerBody.getPosition.y, 10 * playerState.currWpn, playerState.playerId)
    bd.userData = (bulletCounter, bullet)
    val body = world.createBody(bd)
    body.createFixture(fd)

    body.setLinearVelocity(playerState.viewOr match {
      case 1 => new Vec2(speedX, 0)
      case _ => new Vec2(-speedX, 0)
    })
    bulletCounter += 1
    body -> bullet
  }

  def applyStateChange() {
    players.foreach(player => {
      player match {
        case player: PlayerState => {
          val body = pBodies.get(player.playerId).get
          player.posX = if (body._1.getPosition.x < worldSizeX && body._1.getPosition.x > 0)
            body._1.getPosition.x
          else {
            body._1.setTransform(new Vec2(worldSizeX / 2, body._1.getPosition.y), body._1.getAngle)
            body._1.getPosition.x
          }
          player.posY = if (body._1.getPosition.y < worldSizeY && body._1.getPosition.y > 0)
            body._1.getPosition.y
          else {
            body._1.setTransform(new Vec2(body._1.getPosition.x, worldSizeY / 2), body._1.getAngle)
            body._1.getPosition.y
          }
          body._1.setLinearVelocity(vel0)
        }
        case _ => {
        }
      }

    })
    val (delete, keep) = bBodies.partition(body => {
      body._2.destroy || body._1.getPosition.x > worldSizeX || body._1.getPosition.y > worldSizeY ||
        body._1.getPosition.x < 0 || body._1.getPosition.y < 0
    })
    bBodies = keep
    bBodies.foreach(body => {
      body._2.posX = body._1.getPosition.x
      body._2.posY = body._1.getPosition.y
    })
    delete.foreach(body => {
      body._1.m_world.destroyBody(body._1)
      players remove (players indexOf body._2)
    })
    //pool of bullets , move to an outside area, or flag as not to serialize
  }
}

abstract class GameObject {
  var posX: Float
  var posY: Float

  def collide(o: GameObject)
}


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

      override def collide(o: GameObject): Unit = {
        println("dummy called..")
      }
    }
  }
}

case class PlayerState(playerId: String, var posX: Float, var posY: Float, var viewOr: Byte, var currWpn: Short, var health: Int)
  extends GameObject {
  var immune = false

  override def collide(o1: GameObject): Unit = {
    o1 match {
      case bullet: Bullet => bullet.collide(this)
      case _ =>
    }
  }
}

case class Bullet(bulletNum: Int, var posX: Float, var posY: Float, damage: Int, ownerId: String)
  extends GameObject {
  private var isDestroy = false

  def destroy = isDestroy

  override def collide(o: GameObject): Unit = {
    o match {
      case player: PlayerState if player.playerId != this.ownerId =>
        this.isDestroy = true
        //println(s"reducing ${player.health} health by ${this.damage}")
        player.health -= this.damage
        //println(s"new health ${player.health}")
        player.immune = true

      case _ =>
    }
  }
}
