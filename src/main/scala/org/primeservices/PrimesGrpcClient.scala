package org.primeservices

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object PrimesGrpcClient {
  implicit val system =
    ActorSystem[Nothing](Behaviors.empty, "PrimesGrpcClient")

  def apply(implicit system: ActorSystem[Nothing]): PrimesServiceClient = {
    // Boot akka
    implicit val ec = system.executionContext

    val config = ConfigFactory.load().resolve()
    val grpcHost = config.getString("primes.grpc.host")
    val grpcPort = config.getInt("primes.grpc.port")

    // Configure the client by code:
    val clientSettings =
      GrpcClientSettings.connectToServiceAt(grpcHost, grpcPort).withTls(false)

    // Create a client-side stub for the service
    PrimesServiceClient(clientSettings)
  }
}
