package org.primeservices

import akka.stream.Materializer
import io.grpc.Status

import scala.concurrent.Future
import scala.util.Try

class PrimesGrpcService(private val computer: PrimesComputer)
    extends PrimesService {

  override def getPrimesUpTo(in: PrimesRequest): Future[PrimesResponse] =
    Try(computer(in.upTo))
      .map { primes => Future.successful(PrimesResponse(primes)) }
      .recover(error => {
        val status = error match {
          case _ if error.isInstanceOf[IllegalArgumentException] =>
            Status.INVALID_ARGUMENT
              .augmentDescription(error.getMessage())
          case _ => Status.fromThrowable(error)
        }
        Future.failed(status.asRuntimeException())
      })
      .get
}

object PrimesGrpcService {
  def apply(computer: PrimesComputer = PrimesUpTo.sieveOfEratosthenes) =
    new PrimesGrpcService(computer)
}
