# prime-services
Prime-calculating services in REST and gRPC

This project includes a gRPC server that exposes methods to return a sequence
of prime numbers that are less than or equal to a given upper bound, and a
companion REST proxy.

## Build and run

This project is built with sbt, mostly because I used it a little in the past
and I wanted to refresh my memory. :wink: Gradle would have been a valid
alternative, but I became quite confident with it recently, and I really wanted
to use something different.

Since this is a standard sbt project, the usual sbt commands will work. Just
for the sake of completeness, the most useful are listed here:
```sh
sbt compile   # compiles the source
sbt '~test'   # executes the tests whever the source files change
sbt run       # executes any main class available after compilation
```
Two classes in the project can be executed (i.e. have a static main method):
`org.primesservices.PrimesGrpcServer` and `org.primeservices.PrimesRestServer`.
They can be run individually by means of these two commands, respectively:
```sh
sbt 'runMain org.primesservices.PrimesGrpcServer'
sbt 'runMain org.primesservices.PrimesRestServer'
```

## Project structure
The project comprises three different areas:
- the main domain logic (prime numbers computation)
- the gRPC service
- the REST proxy service

While the main domain logic leverages solely the Scala standard library, both
the gRPC and the REST services are implemented using the plain Akka platform.

I purposefully chose not to use higher-level frameworks such as Logom, mostly
because I don't have the necessary knowledge in building microservices to
assess whether this type of frameworks would fit my use-case. In fact,
high-level frameworks tend to be more opinionated than generalist platforms
like Akka, making specific use-cases very simple to handle, but at the same
time making everything else extremely hard.

### Main domain logic
The main domain logic is implemented in the `PrimesUpTo` object.

The domain logic consists of returning a sequence of prime numbers that are
less than or equal to a given upper bound.

This logic has been split from the rest since:
- it is independent of the API chosen for the service
- it improves testability (less mocking, more focus on the logic, etc.)

Testing includes both property and scenario tests. The former has been chosen
where feasible, being this a mathematical type of logic: whenever scenario
tests have been written instead, the reasons are explained in a comment.

The code is organized so that it supports multiple implementation of the logic
itself. This is reflected in the test suite, which has been generalized into a
behavior trait, so as to ease code reuse.

However, there is currently only one implementation of the domain logic. This
is because I'm satisfied with the result, and there is no requirement to
optimize for efficiency. In such case, more efficient implementations could
still easily be added and tested.

#### Error handling

Errors in the domain logic are reported via exceptions. This controverse
decision reflects my "beliefs" on exceptions and partial computations.

My opinion on the topic can be phrased in many different ways, but shortly, I
don't think that input validation is a good reason to make a computation
partial.

In most cases invalid input can be considered an unverified precondition of a
function, rather than an intrinsic property of its semantics. In my view,
partiality should only be owed to the latter, hence the use of exceptions.

However, I do think as well that this approach is very not pragmatic:
exceptions are not part of a function's signature (:rage: Java's checked
exceptions :rage:), which makes them go unnoticed. In larger codebases,
exceptions flying around are mostly a cause of instability. Nonetheless, this
is not the case for this small project. :smirk:

### gRPC service

The protobuf contracts are defined in `primes.proto`. The gRPC service provides
a single method `GetPrimesUpTo` that responds with a list of prime numbers
given a request with an upper bound.

The gRPC service is implemented in the `PrimesGrpcService` class. The class
doesn't contain a full-blown HTTP/2 server that can be executed, but rather
only the gRPC service logic. This allows straightforward unit testing of the
gRPC API, without worrying about the HTTP/2 wrapping layer.

The service is a standard ScalaPB service, which processes each request as an
asynchronous task by means of scala `Future`s. This behavior could have been
wrapped in an actor-model pattern utilizing the same approach as the REST
service (described below), which is based on Akka's `ActorContext.pipeToSelf`.
However, this has not been done due to lack of time and close analogy with
another part of the system. The scatter-gather pattern was also considered as
an alternative. Nonetheless, it has not been chosen, as the pattern is mostly
advantageous for parallelized algorithms that can be partitioned across
different actors, which is not the case for our domain logic algorithm.

The gRPC service doesn't perform any input validation itself, delegating that
to the domain logic, in order not to duplicate the validation logic. Errors
from the main domain logic are reported via `google.rpc.Status` protobuf
message type, as defined in the Google API protobuf Error model.

The HTTP/2 server is implemented in the `PrimesGrpcServer` object. This is
where the main method resides, which starts an HTTP/2 server implemented via
the Akka HTTP library. The glue code between HTTP/2 and gRPC is provided
instead by the Akka gRPC project. This class is not directly unit tested, since
it's mostly made of boilerplate code, but rather end-to-end tested together
with the gRPC client.

The gRPC client is again a standard ScalaPB client, whose creation is wrapped
in the `PrimesGrpcClient` object. Similarly to `PrimesGrpcServer`, this object
consists mostly of boilerplate code, and is as such not unit tested, but rather
end-to-end tested together with the gRPC-HTTP/2 server.

### REST service

The REST service is implemented as an Akka HTTP server.

The routes are defined in the `PrimesRestRoutes` class. Each request is served
by sending the appropriate message to a single `PrimesBackend` actor (described
below) and then transforming its reply to make it fit the Akka HTTP model. No
input validation is performed in the REST service itself, so that the
validation logic is not duplicated. All the errors come from the
`PrimesBackend` actor, and are then mapped to appropriate HTTP status codes
(e.g. invalid inputs are reported as "401 Bad Request").

The `PrimesBackend` actor is a straightforward actor-model wrapper around the
future-based API exposed by the gRPC `PrimesServiceClient`. It mainly leverages
the Akka `ActorContext.pipeToSelf` method to receive the results from the gRPC
calls as messages, whose content is then forwarded as replies. The actor
doesn't spawn children to handle incoming messages, but rather executes the
processing directly. Indeed such processing consists mostly of task-based
asynchronous I/O operations, which can be executed directly in the actor system
execution context, without the need to wrap them into a child actor. In order
not to overload the gRPC server with requests, a simple client-side capping
mechanism on the number of pending requests has been implemented.

The REST routes are separated from the HTTP Server boilerplate code both to
clarify where the actual business logic is, and to ease unit testing: first, by
making it possible to test the REST logic directly, without having to deal with
the HTTP server glue code; second by allowing dependency injection of the gRPC
client, which eases the test fixture setup and the simulation of error
scenarios.

The HTTP server backing the routes is implemented in the `PrimesRestServer`
object. As mentioned before, it is powered by Akka HTTP. Once more, since it is
mostly glue code, the server itself has not been unit tested. However, unlike
the gRPC server, this server has not been end-to-end tested, as there's no
pre-made client available for testing and no need to implement one in the
system's own logic.

## Docker

In order to make it easier to run and test the services without having the
build tools installed on the local machine, a Docker image has been created.
The image contains the project compilation output ready to be executed, and
it's available on Docker Hub as [davla/prime-services](https://hub.docker.com/repository/docker/davla/prime-services).
The image is by no means intended for production use, mostly because I don't
have the knowledge to make it so. :grimacing:

A docker-compose file is also provided, as a form of "executable documentation"
on how containers should be created from the docker image to run and test the
services. There are three services defined in `docker-compose.yml`:

- `grpc` - the gRPC server
- `rest` - the rest server
- `test` - convenience service to run the tests

`grpc` and `rest` can be run together on the same host by means of
`docker-compose up`, and individually via `docker-compose run`. `test` is not
executed by default with `docker-compose up`, and it's meant to only be run
individually. It's worth mentioning that when running within docker, as opposed
to locally, some configuration parameters need to be different, and are thus
overridden via CLI arguments to the container command. This is shown in the
docker-compose file.

Both `Dockerfile` and `docker-compose.yml` have been written by hand, rather
than auto-generated via sbt plugins like `sbt-docker`. This is because I know
Docker well enough to be able to configure it by myself, and I see no point in
relaying the configuration through third-party integrations if I can carry out
the job efficiently myself. In general, I believe that if you use a tool
extensively in your daily job, you should spend the due time to learn to use it
directly, without relying on third-party integration with the rest of the
tooling you use.

## Development process

Since this is a solo project, I find it nonsense to use pull requests. However,
I'm still using git. :grin: This means that features are still developed in
their own branch, which are then merged into main by what I find most
appropriate based on the situation (merge commit, squash commit or
fast-forward).
