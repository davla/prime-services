package org.primeservices

import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import io.grpc.Status
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class PrimesBackendSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with should.Matchers
    with MockFactory
    with LogCapturing {

  import PrimesBackend._

  "PrimesBackend" should {
    "reply to GetPrimes with Success on successful gRPC response" in {
      val input = 5
      val result = Vector(2, 3, 5)

      val backendClient = createTestProbe[PrimesBackend.Reply]()

      val grpcClient = mock[PrimesServiceClient]
      val getPrimesUpTo = grpcClient expects Symbol("getPrimesUpTo")
      getPrimesUpTo(PrimesRequest(input)) returns Future.successful(
        PrimesResponse(result)
      )

      val backend = spawn(PrimesBackend(grpcClient), "PrimesBackendSuccess")

      backend ! GetPrimes(input, backendClient.ref)

      backendClient.expectMessage(StatusReply.Success(result))
    }

    "reply to GetPrimes with Error on gRPC failures" in {
      val input = 5
      val failure = Status.INVALID_ARGUMENT.asRuntimeException()

      val backendClient = createTestProbe[PrimesBackend.Reply]()

      val grpcClient = mock[PrimesServiceClient]
      val getPrimesUpTo = grpcClient expects Symbol("getPrimesUpTo")
      getPrimesUpTo(PrimesRequest(input)) returns Future.failed(
        failure
      )

      val backend = spawn(PrimesBackend(grpcClient), "PrimesBackendFailure")

      backend ! GetPrimes(input, backendClient.ref)

      backendClient.expectMessage(StatusReply.error(failure))
    }
  }
}
