package org.primeservices

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

class PrimesGrpcServiceSpec
    extends AnyWordSpec
    with should.Matchers
    with MockFactory
    with ScalaFutures {

  implicit val ec = scala.concurrent.ExecutionContext.global

  "PrimesGrpcService" should {
    "reply to single requests" in {
      // Given
      val input = 5
      val primes = Vector(2, 3, 5)

      val computePrimes = mockFunction[Int, Seq[Int]]
      computePrimes expects (input) returning primes

      val service = PrimesGrpcService(computePrimes)
      val expected = PrimesResponse(primes)

      // When
      val reply = service.getPrimesUpTo(PrimesRequest(input))

      // Then
      reply.futureValue should equal(expected)
    }

    "report INVALID_ARGUMENT on invalid input" in {
      // Given
      val input = -10

      val computePrimes = mockFunction[Int, Seq[Int]]
      computePrimes expects (input) throws new IllegalArgumentException()

      val service = PrimesGrpcService(computePrimes)

      // When
      val reply = service.getPrimesUpTo(PrimesRequest(input))

      // Then
      val error = reply.failed.futureValue
      error shouldBe a[StatusRuntimeException]
      val errorCode =
        error.asInstanceOf[StatusRuntimeException].getStatus.getCode
      errorCode should equal(Status.Code.INVALID_ARGUMENT)
    }

    "forward any other the error thrown by prime computation" in {
      // Given
      val input = -10

      val computePrimes = mockFunction[Int, Seq[Int]]
      computePrimes expects (input) throws new RuntimeException()

      val service = PrimesGrpcService(computePrimes)

      // When
      val reply = service.getPrimesUpTo(PrimesRequest(input))

      // Then
      reply.failed.futureValue shouldBe a[RuntimeException]
    }
  }
}
