package org

package object primeservices {
  type PrimesComputer = Int => Seq[Int]

  def floorSqrt(n: Int) = math.sqrt(n).floor.toInt
}
