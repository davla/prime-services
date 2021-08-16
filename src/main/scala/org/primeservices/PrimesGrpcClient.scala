package org.primeservices

import akka.grpc.GrpcClientSettings
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors


object PrimesGrpcClient {
  implicit val system = ActorSystem[Nothing](Behaviors.empty, "PrimesGrpcClient")

  def apply(implicit system: ActorSystem[Nothing]): PrimesServiceClient = {
    // Boot akka
    implicit val ec = system.executionContext

    // Configure the client by code:
    val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 8080).withTls(false)

    // Create a client-side stub for the service
    PrimesServiceClient(clientSettings)
  }
}
