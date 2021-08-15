package org.primeservices

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

class PrimesGrpcServiceSpec
    extends AnyWordSpec
    with BeforeAndAfterEach
    with should.Matchers
    with MockFactory
    with ScalaFutures {

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
  }
}
