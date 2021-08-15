package org.primeservices

import org.scalacheck.Gen
import org.scalatest.matchers.BeMatcher
import org.scalatest.matchers.MatchResult
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait PrimesUpToBehavior {
  this: AnyWordSpec with ScalaCheckPropertyChecks with should.Matchers =>
  /*
   * This could be an implicit, since it's used in the majority of tests.
   * However, I find that implicits tend to cause unreadable and spaghetti
   * code, hence I prefer not to use them.
   */
  private val validInput = Gen.choose(2, 150_000)

  private val invalidInput = Gen.choose(Integer.MIN_VALUE, 1)

  // Courtesy of Wikipedia: https://en.wikipedia.org/wiki/Primality_test#Simple_methods
  private val prime = new BeMatcher[Int] {
    def apply(n: Int) = MatchResult(
      (2 to floorSqrt(n)).forall(n % _ != 0),
      s"$n is not prime",
      s"$n is prime"
    )
  }

  def primesUpTo(computer: PrimesComputer): Unit = {
    /*
     * This could be made into a property test by checking that the correct
     * number of primes is returned. However, according to Wikipedia
     * (https://en.wikipedia.org/wiki/Prime-counting_function), counting the
     * number of primes up to an upper bound is not a trivial task, making it
     * unsuited for checking purposes.
     */
    "return all the prime numbers less than or equal to the upper bound" in {
      val expected = LazyList(2, 3, 5, 7, 11, 13, 17, 19, 23, 29)
      computer(30) should equal(expected)
    }

    "only return prime numbers" in {
      forAll(validInput) { (n: Int) =>
        val primes = computer(n)
        every(primes) shouldBe prime
      }
    }

    "only return numbers less than or equal to the upper bound" in {
      forAll(validInput) { (n: Int) =>
        val primes = computer(n)
        every(primes) should be <= n
      }
    }

    "only return positive numbers" in {
      forAll(validInput) { (n: Int) =>
        val primes = computer(n)
        every(primes) should be > 0
      }
    }

    "throw exceptions on input less than two" in {
      forAll(invalidInput) { (n: Int) =>
        an[IllegalArgumentException] should be thrownBy computer(n)
      }
    }
  }
}

class SieveOfEratosthenesSpec
    extends AnyWordSpec
    with ScalaCheckPropertyChecks
    with should.Matchers
    with PrimesUpToBehavior {

  "sieveOfEratosthenes" should {
    behave like primesUpTo(PrimesUpTo.sieveOfEratosthenes)
  }
}
