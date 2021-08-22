package org.primeservices

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import scala.util.Failure
import scala.util.Success
import akka.actor.typed.ActorRef
import scala.concurrent.ExecutionContext
import akka.pattern.StatusReply
import akka.util.Timeout

import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.duration._
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler

object PrimesBackend {
  type Reply = StatusReply[Seq[Int]]

  trait Method
  case class GetPrimes(upTo: Int, replyTo: ActorRef[Reply]) extends Method
  private case class Primes(primes: Seq[Int], replyTo: ActorRef[Reply]) extends Method
  private case class Error(reason: Throwable, replyTo: ActorRef[Reply]) extends Method

  object GetPrimes {
    def apply(upTo: Int) = (replyTo: ActorRef[Reply]) => new GetPrimes(upTo, replyTo)
  }

  def apply(grpcClient: PrimesServiceClient): Behavior[Method] =
    Behaviors.receive[Method] { (context, message) =>
      implicit val executionContext = context.executionContext

      message match {
        case GetPrimes(upTo, replyTo) =>
          val futurePrimes = grpcClient.getPrimesUpTo(PrimesRequest(upTo))
          context.pipeToSelf(futurePrimes) {
            case Success(PrimesResponse(primes, _)) => Primes(primes, replyTo)
            case Failure(ex) => Error(ex, replyTo)
          }

        case Primes(primes, replyTo) =>
          replyTo ! StatusReply.success(primes)

        case Error(reason, replyTo) =>
          replyTo ! StatusReply.error(reason)
      }

      Behaviors.same
    }

  def apply(implicit actorSystem: ActorSystem[Nothing]): Behavior[Method] =
    apply(PrimesGrpcClient(actorSystem))
}

object PrimesRestRoutes {
  import PrimesBackend._

  def apply(backend: ActorRef[PrimesBackend.Method])(implicit ec: ExecutionContext, scheduler: Scheduler) =
    pathPrefix("prime") {
      path(IntNumber) { upTo: Int =>
        get {
          implicit val timeout: Timeout = 5.seconds

          val response = backend.askWithStatus(GetPrimes(upTo)).map(_.mkString(sep = ","))
          complete(response)
        }
      }
    }
}

object PrimesRestServer {
  val httpServerActor = Behaviors.setup[Nothing] { context =>
    implicit val actorSystem = context.system
    implicit val executionContext = actorSystem.executionContext

    val primesBackend = context.spawn(PrimesBackend.apply, "PrimesBackendActor")
    val routes = PrimesRestRoutes(primesBackend)

    startHttpServer(routes)

    Behaviors.empty
  }

  private def startHttpServer(
      routes: Route
  )(implicit system: ActorSystem[_]): Unit = {
    implicit val executionContext = system.executionContext

    val config = ConfigFactory.load().resolve()
    val interface = config.getString("primes.rest.interface")
    val port = config.getInt("primes.rest.port")
    val futureBinding = Http().newServerAt(interface, port).bind(routes)

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
    ActorSystem[Nothing](httpServerActor, "PrimesRestServer")
  }
}
