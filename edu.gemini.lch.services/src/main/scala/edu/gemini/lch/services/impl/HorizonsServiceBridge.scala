// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.lch.services.impl

import java.util.Date

import edu.gemini.horizons.api.HorizonsException
import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.horizons.server.backend.HorizonsService2.{EphemerisEmpty, HS2, HorizonsError, ParseError}
import edu.gemini.lch.model.LaserNight
import edu.gemini.spModel.core.{Angle, Coordinates, Ephemeris, HorizonsDesignation, Site}
import jsky.coords.WorldCoords

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._


/**
 * Provides a Java-friendly API for ephemeris lookup, leaving the Spring service
 * kookiness to `HorizonsServiceImpl`.
 */
object HorizonsServiceBridge {

  val MaxElements: Int = 60 * 24 // minutes/day (1 element/min is horizons max)

  def lookupEphemeris(
    target: HorizonsDesignation,
    site:   Site,
    night:  LaserNight
  ): HS2[Ephemeris] =

    // Emphemeris library uses Date
    HorizonsService2.lookupEphemeris(
      target,
      site,
      Date.from(night.getStart.toInstant),
      Date.from(night.getEnd.toInstant),
      MaxElements
    )


  def unsafeLookupWorldCoords(
    target:  HorizonsDesignation,
    site:    Site,
    night:   LaserNight,
    maxStep: Angle
  ): List[WorldCoords] = {

    import edu.gemini.lch.services.impl.list._

    def toWorldCoords(e: Ephemeris): List[WorldCoords] =
      // Filter points but don't make a gap larger than step size if possible
      e.toList.map(_._2).sequenceFilter { (a, b) =>
        a.angularDistance(b) <= maxStep
      }.map { c =>
        new WorldCoords(c.ra.toDegrees, c.dec.toDegrees)
      }

    lookupEphemeris(target, site, night).run.unsafePerformIO match {
      case -\/(HorizonsError(e)) => throw e
      case -\/(ParseError(_, m)) => throw new HorizonsException("ParseError: " + m)
      case -\/(EphemerisEmpty)   => throw new HorizonsException("EphemerisEmpty")
      case \/-(e)                => toWorldCoords(e)
    }

  }


  @throws[HorizonsException]("if there is a problem executing the query")
  def unsafeLookupWorldCoordsForJava(
    target:        HorizonsDesignation,
    site:          Site,
    night:         LaserNight,
    maxStepArcsec: Int
  ): java.util.List[WorldCoords] =

    unsafeLookupWorldCoords(target, site, night, Angle.fromArcsecs(maxStepArcsec)).asJava

}
