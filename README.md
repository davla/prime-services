# prime-services
Prime-calculating services in REST and gRPC

## Build system

This project is built with sbt, mostly because I used it a little in the past
and I wanted to refresh my memory. :wink:

Gradle would have been a valid alternative, but I became quite confident with
it recently, and I really wanted to use something different.

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
