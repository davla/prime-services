package org.primeservices

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import io.grpc.Status

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
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

object PrimesGrpcServer {
  def main(args: Array[String]): Unit = {
    // Akka boot up code
    implicit val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    implicit val system =
      ActorSystem[Nothing](Behaviors.empty, "PrimesGrpcServer", conf)
    implicit val ec = system.executionContext

    val service = PrimesServiceHandler(PrimesGrpcService())

    val binding = Http().newServerAt("127.0.0.1", 8080).bind(service)
    binding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(
          s"gRPC server bound to ${address.getHostString}:${address.getPort}"
        )
      case Failure(ex) =>
        println(s"Failed to bind gRPC endpoint, terminating system: $ex")
        system.terminate()
    }
  }
}
