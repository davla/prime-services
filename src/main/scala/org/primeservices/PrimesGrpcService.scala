package org.primeservices

import akka.stream.Materializer
import scala.concurrent.Future

class PrimesGrpcService(private val computer: PrimesComputer)
    extends PrimesService {

  override def getPrimesUpTo(in: PrimesRequest): Future[PrimesResponse] =
    Future.successful(
      PrimesResponse(
        computer(in.upTo)
      )
    )
}

object PrimesGrpcService {
  def apply(computer: PrimesComputer = PrimesUpTo.sieveOfEratosthenes) =
    new PrimesGrpcService(computer)
}
