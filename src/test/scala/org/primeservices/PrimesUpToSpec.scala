package org.primeservices

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

import scala.language.postfixOps
import org.scalatest.matchers.BeMatcher
import org.scalatest.matchers.MatchResult

class SieveOfEratosthenesSpec extends AnyWordSpec
  with ScalaCheckPropertyChecks
  with should.Matchers
{
  /*
   * This could be an implicit, since it's used in the majority of tests.
   * However, I find that implicits tend to cause unreadable and spaghetti
   * code, hence I prefer not to use them.
   */
  private val strictlyPositiveInts = Gen.choose(1, 150_000)

  // Courtesy of Wikipedia: https://en.wikipedia.org/wiki/Primality_test#Simple_methods
  private val prime = new BeMatcher[Int] {
      def apply(n: Int) = MatchResult(
        (2 to floorSqrt(n)).forall (n % _ != 0),
        s"$n is not prime",
        s"$n is prime",
      )
    }

  "sieveOfEratosthenes" should {
    /*
     * This could be made into a property test by checking that the correct
     * number of primes is returned. However, according to Wikipedia
     * (https://en.wikipedia.org/wiki/Prime-counting_function), counting the
     * number of primes up to an upper bound is not a trivial task, making it
     * unsuited for checking purposes.
     */
    "return all the prime numbers less than or equal to the upper bound" in {
      val expected = Stream(2, 3, 5, 7, 11, 13, 17, 19, 23, 29)
      PrimesUpTo.sieveOfEratosthenes(30) should equal (expected)
    }

    "only return prime numbers" in {
      forAll (strictlyPositiveInts) { (n: Int) =>
        val primes = PrimesUpTo.sieveOfEratosthenes(n)
        every (primes) shouldBe prime
      }
    }

    "only return numbers less than or equal to the upper bound" in {
      forAll (strictlyPositiveInts) { (n: Int) =>
        val primes = PrimesUpTo.sieveOfEratosthenes(n)
        every (primes) should be <= n
      }
    }

    "only return positive numbers" in {
      forAll (strictlyPositiveInts) { (n: Int) =>
        val primes = PrimesUpTo.sieveOfEratosthenes(n)
        every (primes) should be > 0
      }
    }
  }
}
