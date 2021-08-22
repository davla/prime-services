package org.primeservices

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.StatusReply
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

class PrimesRestRoutesSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with should.Matchers
    with ScalatestRouteTest
    with LogCapturing {

  lazy val testKit = ActorTestKit()

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  val typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

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
      val primesBackend =
        testKit.spawn(Behaviors.monitor(probe.ref, backendBehavior))

      val routes = PrimesRestRoutes(primesBackend.ref)

      Get(s"/prime/$input") ~> routes ~> check {
        status should be(StatusCodes.OK)
        entityAs[String] should be("2,3,5")
      }
    }

    "reject GET /prime/<number> with ValidationRejection for invalid input" in {
      val input = 0
      val errorMsg = "Invalid input: 0"

      val backendBehavior = Behaviors.receiveMessage[PrimesBackend.Method] {
        case PrimesBackend.GetPrimes(upTo, replyTo) =>
          upTo should be(0)
          replyTo ! StatusReply.Error(new IllegalArgumentException(errorMsg))
          Behaviors.same
      }
      val probe = testKit.createTestProbe[PrimesBackend.Method]()
      val primesBackend =
        testKit.spawn(Behaviors.monitor(probe.ref, backendBehavior))

      val routes = PrimesRestRoutes(primesBackend.ref)

      Get(s"/prime/$input") ~> routes ~> check {
        rejection shouldBe a[ValidationRejection]
      }
    }

    "respond to GET /prime/<number> with InternalServerError for other errors" in {
      val input = 0

      val backendBehavior = Behaviors.receiveMessage[PrimesBackend.Method] {
        case PrimesBackend.GetPrimes(upTo, replyTo) =>
          upTo should be(0)
          replyTo ! StatusReply.Error(new RuntimeException())
          Behaviors.same
      }
      val probe = testKit.createTestProbe[PrimesBackend.Method]()
      val primesBackend =
        testKit.spawn(Behaviors.monitor(probe.ref, backendBehavior))

      val routes = PrimesRestRoutes(primesBackend.ref)

      Get(s"/prime/$input") ~> routes ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "reject any other route with an empty rejection set (Resouce not found)" in {
      val primesBackend = testKit.createTestProbe[PrimesBackend.Method]()

      val routes = PrimesRestRoutes(primesBackend.ref)

      Get(s"/route") ~> routes ~> check {
        rejections shouldBe empty
      }
    }
  }
}
