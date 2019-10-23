// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.lch.services.impl

import scala.annotation.tailrec

/**
 * Implements a sequence filtering operation that is needed to remove extra
 * ephemeris elements.
 */
final class ListOps[A](val self: List[A]) extends AnyVal {

  def sequenceFilter(f: (A, A) => Boolean): List[A] = {

    @tailrec
    def go(result: List[A], tentative: Option[A], next: List[A]): List[A] =
      (result, tentative, next) match {
        case (_,    _,       Nil  )            => result.reverse                 // termination
        case (Nil,  _,       n::ns)            => go(List(n),   None,    ns  )   // always accept the first element
        case (r::_, _,       n::ns) if f(r, n) => go(result,    Some(n), ns  )   // even if next is close, we may need it to avoid opening up a large gap
        case (r::_, None,    n::ns)            => go(n::result, None,    ns  )   // no tentative element, not close, so we need this element
        case (r::_, Some(t), _    )            => go(t::result, None,    next)   // next not close but add tentative to avoid opening a larger gap
      }

    go(Nil, None, self)
  }

}

trait ToListOps {
  implicit def ToListOps[A](l: List[A]): ListOps[A] =
    new ListOps[A](l)
}

object list extends ToListOps
