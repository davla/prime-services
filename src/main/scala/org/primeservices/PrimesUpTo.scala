package org.primeservices

object PrimesUpTo {
  /*
   * Courtesy of Wikipedia: https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes
   *
   * No input validation occurs in this method, since it's private and in a
   * small context like this it's possible to keep track of the internal calls.
   * Furthermore, it improves performances, since this method is recursive.
   *
   * This method is not tail-recursive. If efficiency is really a concern, we
   * could make this method concurrent, or use an imperative implementation.
   */
  private def sieveOfEratosthenes(
      upTo: Int,
      divisor: Int,
      maybePrimes: Seq[Int]
  ): Seq[Int] =
    if (divisor > floorSqrt(upTo)) {
      maybePrimes
    } else {
      val filteredPrimes =
        maybePrimes.filter(n => n == divisor || n % divisor != 0)
      filteredPrimes
        .find(_ > divisor)
        .map { newDivisor =>
          sieveOfEratosthenes(upTo, newDivisor, filteredPrimes)
        }
        .getOrElse(filteredPrimes)
    }

  def sieveOfEratosthenes(upTo: Int): Seq[Int] =
    if (upTo < 2)
      throw new IllegalArgumentException(
        s"upTo must be greater than 2: $upTo passed"
      )
    else
      sieveOfEratosthenes(upTo, 2, (2 to upTo).to(LazyList))
}
