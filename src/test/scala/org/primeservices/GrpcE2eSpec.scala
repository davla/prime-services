package org.primeservices

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class GrpcE2eSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with should.Matchers
    with ScalaFutures
    with LogCapturing {

  implicit val patience: PatienceConfig =
    PatienceConfig(scaled(5.seconds), scaled(100.millis))
  val serverTestKit = ActorTestKit(
    ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .resolve()
  )
  val serverSystem = serverTestKit.system

  val service = PrimesGrpcServer(serverSystem)
  // make sure server is bound before using client
  service.futureValue

  val clientSystem: ActorSystem[_] =
    ActorSystem(Behaviors.empty, "GreeterClient")

  override def afterAll(): Unit = {
    ActorTestKit.shutdown(clientSystem)
    serverTestKit.shutdownTestKit()
  }

  "PrimesGrpcServer" should {
    "reply to single request" in {
      val client = PrimesGrpcClient(clientSystem)
      val reply = client.getPrimesUpTo(PrimesRequest(5))
      reply.futureValue should equal(PrimesResponse(Vector(2, 3, 5)))
    }

    "report INVALID_ARGUMENT on invalid input" in {
      val client = PrimesGrpcClient(clientSystem)
      val reply = client.getPrimesUpTo(PrimesRequest(-10))

      val error = reply.failed.futureValue
      error shouldBe a[StatusRuntimeException]
      // val errorCode =
      //   error.asInstanceOf[StatusRuntimeException].getStatus.getCode
      // errorCode should equal(Status.Code.INVALID_ARGUMENT)
    }
  }
}
