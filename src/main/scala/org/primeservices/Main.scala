package org.primeservices

import scala.util.Success
import scala.util.Failure

object Main {
  def main(args: Array[String]): Unit = {
    implicit val executionContext = scala.concurrent.ExecutionContext.global

    args.headOption.getOrElse("all") match {
      case "grpc" => PrimesGrpcServer()
      case "rest" => PrimesRestServer()
      case "all" =>
        PrimesGrpcServer().onComplete {
          case Success(_) => PrimesRestServer()
          case Failure(_) => System.exit(1)
        }
    }
  }
}
