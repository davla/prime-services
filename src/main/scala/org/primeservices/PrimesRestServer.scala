package org.primeservices

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import scala.util.Success
import scala.util.Failure

class PrimesRestRoutes(private val grpcClient: PrimesServiceClient) {
  def routes = pathPrefix("prime") {
    path(IntNumber) { upTo: Int =>
      get {
        onSuccess(grpcClient.getPrimesUpTo(PrimesRequest(upTo))) { response =>
          complete(response.primes.mkString(sep = ","))
        }
      }
    }
  }
}

object PrimesRestRoutes {
  def apply(grpcClient: PrimesServiceClient): PrimesRestRoutes =
    new PrimesRestRoutes(grpcClient)

  def apply(implicit system: ActorSystem[Nothing]): PrimesRestRoutes =
    apply(PrimesGrpcClient(system))
}

object PrimesRestServer {
  private def startHttpServer(
      routes: Route
  )(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8081).bind(routes)
    futureBinding.onComplete {
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
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      // val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      // context.watch(userRegistryActor)
      implicit val sys = context.system

      startHttpServer(PrimesRestRoutes(sys).routes)

      Behaviors.empty
    }
    implicit val system = ActorSystem[Nothing](rootBehavior, "PrimesRestServer")
  }
}
