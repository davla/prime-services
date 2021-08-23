package org.primeservices

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import io.grpc.Status

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

class PrimesGrpcService(private val computer: PrimesComputer)(implicit
    val ec: ExecutionContext
) extends PrimesService {

  override def getPrimesUpTo(in: PrimesRequest): Future[PrimesResponse] =
    Future(computer(in.upTo))
      .transform(
        s = PrimesResponse(_),
        f = {
          case ex: IllegalArgumentException =>
            Status.INVALID_ARGUMENT
              .augmentDescription(ex.getMessage())
              .asRuntimeException()
          case ex => Status.fromThrowable(ex).asRuntimeException()
        }
      )
}

object PrimesGrpcService {
  def apply(computer: PrimesComputer = PrimesUpTo.sieveOfEratosthenes)(implicit
      ec: ExecutionContext
  ) =
    new PrimesGrpcService(computer)(ec)
}

object PrimesGrpcServer {
  def apply(implicit
      system: ActorSystem[Nothing]
  ): Future[Http.ServerBinding] = {
    implicit val ec = system.executionContext

    val service = PrimesServiceHandler(PrimesGrpcService()(ec))

    val config = ConfigFactory.load().resolve()
    val interface = config.getString("primes.grpc.interface")
    val port = config.getInt("primes.grpc.port")
    val binding = Http().newServerAt(interface, port).bind(service)
    binding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }

    binding
  }

  def main(args: Array[String]): Unit = {
    implicit val conf = ConfigFactory
      // We need to make sure that HTTP/2 is enabled for gRPC to work
      .parseString("akka.http.server.preview.enable-http2 = on")
      .resolve()
      .withFallback(ConfigFactory.load().resolve())
    implicit val system: ActorSystem[Nothing] =
      ActorSystem[Nothing](Behaviors.empty, "PrimesGrpcServer", conf)

    apply
  }
}
