package org.primeservices

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.ValidationRejection
import akka.pattern.StatusReply
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

object PrimesBackend {
  type Reply = StatusReply[Seq[Int]]

  trait Method
  case class GetPrimes(upTo: Int, replyTo: ActorRef[Reply]) extends Method
  private case class Primes(primes: Seq[Int], replyTo: ActorRef[Reply])
      extends Method
  private case class Error(reason: Throwable, replyTo: ActorRef[Reply])
      extends Method

  object GetPrimes {
    def apply(upTo: Int) = (replyTo: ActorRef[Reply]) =>
      new GetPrimes(upTo, replyTo)
  }

  private case class State(
      grpcClient: PrimesServiceClient,
      maxPendingRequests: Int,
      pendingRequestsCount: Int = 0
  )

  def apply(state: State): Behavior[Method] =
    Behaviors.receive[Method] { (context, message) =>
      implicit val executionContext = context.executionContext

      val State(grpcClient, maxPendingRequests, pendingRequestsCount) = state

      val updatedPendingRequestsCount = message match {
        case GetPrimes(upTo, replyTo) =>
          if (pendingRequestsCount + 1 > maxPendingRequests) {
            replyTo ! StatusReply.error(
              "Max number of pending requests reached"
            )
            pendingRequestsCount

          } else {
            val futurePrimes = grpcClient.getPrimesUpTo(PrimesRequest(upTo))
            context.pipeToSelf(futurePrimes) {
              case Success(PrimesResponse(primes, _)) => Primes(primes, replyTo)
              case Failure(ex)                        => Error(ex, replyTo)
            }
            pendingRequestsCount + 1
          }

        case Primes(primes, replyTo) =>
          replyTo ! StatusReply.success(primes)
          pendingRequestsCount - 1

        case Error(reason, replyTo) =>
          replyTo ! StatusReply.error(reason)
          pendingRequestsCount - 1
      }

      apply(state.copy(pendingRequestsCount = updatedPendingRequestsCount))
    }

  def apply(
      grpcClient: PrimesServiceClient,
      maxPendingRequests: Int
  ): Behavior[Method] =
    apply(State(grpcClient, maxPendingRequests))

  def apply(maxPendingRequests: Int)(implicit
      actorSystem: ActorSystem[Nothing]
  ): Behavior[Method] =
    apply(PrimesGrpcClient(actorSystem), maxPendingRequests)
}

object PrimesRestRoutes {
  import PrimesBackend._

  def apply(
      backend: ActorRef[PrimesBackend.Method]
  )(implicit ec: ExecutionContext, scheduler: Scheduler) =
    pathPrefix("prime") {
      path(IntNumber) { upTo: Int =>
        get {
          implicit val timeout: Timeout = 5.seconds

          onComplete(backend.askWithStatus(GetPrimes(upTo))) {
            case Success(primes) =>
              complete(primes.mkString(sep = ","))
            case Failure(ex: IllegalArgumentException) =>
              reject(
                new ValidationRejection(s"Invalid input ${upTo}", Some(ex))
              )
            case Failure(ex) => throw ex
          }
        }
      }
    }
}

object PrimesRestServer {
  val httpServerActor = Behaviors.setup[Nothing] { context =>
    implicit val actorSystem = context.system
    implicit val executionContext = actorSystem.executionContext

    val maxPendingGrpcRequests = ConfigFactory
      .load()
      .resolve()
      .getInt("primes.rest.max-pending-grpc-requests")
    val primesBackend =
      context.spawn(PrimesBackend(maxPendingGrpcRequests), "PrimesBackendActor")
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
