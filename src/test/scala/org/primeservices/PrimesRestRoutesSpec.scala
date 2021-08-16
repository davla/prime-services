package org.primeservices

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.proxy.MockFactory
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes

class PrimesRestRoutesSpec
    extends AnyWordSpec
    with BeforeAndAfterEach
    with should.Matchers
    with MockFactory
    with ScalaFutures
    with ScalatestRouteTest {

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  // lazy val testKit = ActorTestKit()
  // implicit def typedSystem = testKit.system
  // override def createActorSystem(): akka.actor.ActorSystem =
  //   testKit.system.classicSystem

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  // val userRegistry = testKit.spawn(UserRegistry())
  // lazy val routes = new UserRoutes(userRegistry).userRoutes

  // use the json formats to marshal and unmarshall objects in the test
  // import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  // import JsonFormats._

  "PrimesRestRoutes" should {
    "respond to GET /prime/<number>" in {
      val input = 5
      val result = Vector(2, 3, 5)

      val grpcClient = mock[PrimesServiceClient]
      val getPrimesUpTo = grpcClient expects Symbol("getPrimesUpTo")
      getPrimesUpTo(PrimesRequest(input)) returns Future.successful(
        PrimesResponse(result)
      )

      val routes = PrimesRestRoutes(grpcClient).routes

      Get(s"/prime/$input") ~> routes ~> check {
        status should be(StatusCodes.OK)
        entityAs[String] should be("2,3,5")
      }
    }

    "return errors on invalid input" in {
      val input = 0

      val grpcClient = mock[PrimesServiceClient]
      val getPrimesUpTo = grpcClient expects Symbol("getPrimesUpTo")
      getPrimesUpTo(PrimesRequest(input)) returns Future.failed(
        new IllegalArgumentException()
      )

      val routes = PrimesRestRoutes(grpcClient).routes

      Get(s"/prime/$input") ~> routes ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }
  }
}
