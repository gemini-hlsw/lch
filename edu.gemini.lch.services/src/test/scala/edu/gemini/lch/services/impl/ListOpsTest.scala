// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.lch.services.impl

import org.junit.Assert._
import org.junit.Test

final class ListOpsTest {

  import edu.gemini.lch.services.impl.list._

  def sequenceTest(maxStep: Int): (Int, Int) => Boolean =
    (a, b) => (b - a) <= maxStep

  @Test
  def emptyList(): Unit = {
    assertEquals(Nil, Nil.sequenceFilter(sequenceTest(10)))
  }

  @Test
  def singletonList(): Unit = {
    assertEquals(List(0), List(0).sequenceFilter(sequenceTest(0)))
  }

  @Test
  def collapseOne(): Unit = {
    assertEquals(List(0), List(0, 1).sequenceFilter(sequenceTest(10)))
  }

  @Test
  def collapseMany(): Unit = {
    assertEquals(List(0), List.range(0, 10).sequenceFilter(sequenceTest(10)))
  }

  @Test
  def keepNextToLast(): Unit = {
    // List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    // Here we keep 0 and 10 because including 11 would create a gap bigger than
    // 10.
    assertEquals(List(0, 10), List.range(0, 12).sequenceFilter(sequenceTest(10)))
  }

  @Test
  def bigJumps(): Unit = {
    assertEquals(List(0, 10, 20, 30), List(0, 10, 20, 30).sequenceFilter(sequenceTest(5)))
  }
}
