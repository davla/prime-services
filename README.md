# prime-services
Prime-calculating services in REST and gRPC

This project includes a gRPC server that exposes methods to return a sequence
of prime numbers that are less than or equal to a given upper bound, and a
companion REST proxy.

## Build system

This project is built with sbt, mostly because I used it a little in the past
and I wanted to refresh my memory. :wink:

Gradle would have been a valid alternative, but I became quite confident with
it recently, and I really wanted to use something different.

## Development process

Since this is a solo project, I find it nonsense to use pull requests. However,
I'm still using git. :grin: This means that features are still developed in
their own branch, which are then merged into main by what I find most
appropriate based on the situation (merge commit, squash commit or
fast-forward).

## Project structure
The project comprises three different areas:
- the main domain logic (prime numbers computation)
- the gRPC service
- the REST proxy service

### Main domain logic
The main domain logic is implemented in the `PrimesUpTo` object.

The domain logic consists of returning a sequence of prime numbers that are
less than or equal to a given upper bound.

This logic has been split from the rest since:
- it is independent of the API chosen for the service
- it improves testability (less mocking, more focus on the logic, etc.)

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

The gRPC service is implemented in the `PrimesGrpcService` class. The service
is a standard ScalaPB service, processing requests in a serial fashion in the
same thread where the request is received. Errors from the main domain logic
are reported via `google.rpc.Status` protobuf message type, as defined in the
Google API protobuf Error model.

The gRPC server process is implemented in the `PrimesGrpcServer` object. The
server is an Akka HTTP server, where the glue code between that and the gRPC
service is provided by the Akka gRPC project.
