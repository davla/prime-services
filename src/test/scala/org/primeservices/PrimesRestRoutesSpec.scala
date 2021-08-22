package org.primeservices

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.proxy.MockFactory
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import org.scalatest.BeforeAndAfterAll

class PrimesRestRoutesSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with should.Matchers
    with ScalaFutures
    with ScalatestRouteTest {

  lazy val testKit = ActorTestKit()

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  val typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

//   implicit val executionContext = typedSystem.executionContext
  implicit val scheduler = typedSystem.scheduler

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "PrimesRestRoutes" should {
    "respond to GET /prime/<number>" in {
      val input = 5
      val result = Vector(2, 3, 5)

      val backendBehavior = Behaviors.receiveMessage[PrimesBackend.Method] {
        case PrimesBackend.GetPrimes(upTo, replyTo) =>
          upTo should be(5)
          replyTo ! StatusReply.Success(result)
          Behaviors.same
      }
      val probe = testKit.createTestProbe[PrimesBackend.Method]()
      val primesBackend = testKit.spawn(Behaviors.monitor(probe.ref, backendBehavior))

      val routes = PrimesRestRoutes(primesBackend.ref)

      Get(s"/prime/$input") ~> routes ~> check {
        status should be(StatusCodes.OK)
        entityAs[String] should be("2,3,5")
      }
    }

    "return errors on invalid input" in {
      val input = 0

      val backendBehavior = Behaviors.receiveMessage[PrimesBackend.Method] {
        case PrimesBackend.GetPrimes(upTo, replyTo) =>
          upTo should be(0)
          replyTo ! StatusReply.Error(new IllegalArgumentException())
          Behaviors.same
      }
      val probe = testKit.createTestProbe[PrimesBackend.Method]()
      val primesBackend = testKit.spawn(Behaviors.monitor(probe.ref, backendBehavior))

      val routes = PrimesRestRoutes(primesBackend.ref)

      Get(s"/prime/$input") ~> routes ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }
  }
}
