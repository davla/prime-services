package org.primeservices

object PrimesUpTo {
  // Courtesy of Wikipedia: https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes
  private def sieveOfEratosthenes(upTo: Int, divisor: Int, maybePrimes: Seq[Int]): Seq[Int] =
    if (divisor > floorSqrt(upTo)) {
      maybePrimes
    }
    else {
      val filteredPrimes = maybePrimes.filterNot(_ == divisor)
        .filterNot(_ % divisor == 0)
      filteredPrimes.headOption.map { newDivisor =>
        sieveOfEratosthenes(upTo, newDivisor, filteredPrimes)
      }.getOrElse(maybePrimes)
    }

  def sieveOfEratosthenes(upTo: Int): Seq[Int] =
    sieveOfEratosthenes(upTo, 2, (2 to upTo).toStream)
}
