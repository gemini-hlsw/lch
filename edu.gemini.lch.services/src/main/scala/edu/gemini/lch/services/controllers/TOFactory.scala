package edu.gemini.lch.services.controllers

import edu.gemini.lch.model._
import edu.gemini.lch.services.VisibilityCalculator
import edu.gemini.lch.services.model.{LaserTarget => LaserTargetTO, ShutteringWindow => ShutteringWindowTO, ClearanceWindow => ClearanceWindowTO, Visibility => VisibilityTO, Coordinates => CoordinatesTO }
import edu.gemini.lch.services.model.Visibility.{Interval => IntervalTO}
import jsky.coords.WorldCoords


import scala.collection.JavaConversions._
import org.joda.time.Interval

/**
 * Factory to create transfer objects.
 */
object TOFactory {

  def toLaserTargetTOs(night: BaseLaserNight, calculator: VisibilityCalculator, laserTargets: Set[LaserTarget]): Set[LaserTargetTO] =
    laserTargets.map { lt => toLaserTargetTO(night, calculator, lt) }

  def toLaserTargetTO(night: BaseLaserNight, calculator: VisibilityCalculator, lt: LaserTarget): LaserTargetTO = {
    val v = calculator.calculateVisibility(new WorldCoords(lt.getDegrees1, lt.getDegrees2))
    val c = toCoordinatesTO(lt)
    val t = new LaserTargetTO(lt.getId, c)
    t.getClearanceWindows.addAll(toClearanceWindowTOs(lt.getPropagationWindows.toList).toList)
    t.getShutteringWindows.addAll(toShutteringWindowTOs(lt.getShutteringWindows.toList).toList)
    t.setVisibility(toVisibilityTO(night, v))
    t
  }

  def toLaserTargetTO(night: BaseLaserNight, lt: LaserTarget): LaserTargetTO = {
    val c = toCoordinatesTO(lt)
    val t = new LaserTargetTO(lt.getId, c)
    t.getClearanceWindows.addAll(toClearanceWindowTOs(lt.getPropagationWindows.toList).toList)
    t.getShutteringWindows.addAll(toShutteringWindowTOs(lt.getShutteringWindows.toList).toList)
    t.setVisibility(toVisibilityTO(night, lt.getVisibility))
    t
  }

  def toLaserTargetTO(night: BaseLaserNight, lt: LaserTarget, clearance: List[PropagationWindow], shuttering: List[ShutteringWindow]): LaserTargetTO = {
    val c = toCoordinatesTO(lt)
    val t = new LaserTargetTO(lt.getId, c)
    t.getClearanceWindows.addAll(toClearanceWindowTOs(clearance).toList)
    t.getShutteringWindows.addAll(toShutteringWindowTOs(shuttering).toList)
    t.setVisibility(toVisibilityTO(night, lt.getVisibility))
    t
  }

  def toClearanceWindowTOs(windows: List[PropagationWindow]): List[ClearanceWindowTO] =
    windows map {
      w =>
        new ClearanceWindowTO(w.getStart.toDate, w.getEnd.toDate)
    }

  def toShutteringWindowTOs(windows: List[ShutteringWindow]): List[ShutteringWindowTO] =
    windows map {
      case w: BlanketClosure => new ShutteringWindowTO(w.getStart.toDate, w.getEnd.toDate, true)
      case w: ShutteringWindow => new ShutteringWindowTO(w.getStart.toDate, w.getEnd.toDate, false)
    }

  def toVisibilityTO(night: BaseLaserNight, visibility: Visibility) = {
    val aboveHorizon = visibility getVisibleIntervalsDuring (night) map (i => toIntervalTO(i))
    val aboveLimit = visibility getVisibleIntervalsAboveLimitDuring(night) map (i => toIntervalTO(i))
    new VisibilityTO(aboveHorizon, aboveLimit)
  }

  def toIntervalTO(interval: Interval) =
    new IntervalTO(interval.getStart.toDate, interval.getEnd.toDate)

  def toCoordinatesTO(lt: LaserTarget) =
    lt match {
      case lt : RaDecLaserTarget => new CoordinatesTO(CoordinatesTO.RADEC, lt.getDegrees1, lt.getDegrees2)
      case lt : AzElLaserTarget  => new CoordinatesTO(CoordinatesTO.AZEL,  lt.getDegrees1, lt.getDegrees2)
    }

  def toCoordinatesTO(ot: ObservationTarget) =
    ot match {
      case ot : SiderealTarget    => new CoordinatesTO(CoordinatesTO.RADEC, ot.getDegrees1, ot.getDegrees2)
      case ot : NonSiderealTarget => new CoordinatesTO(CoordinatesTO.RADEC, ot.getDegrees1, ot.getDegrees2)
      case ot : EngineeringTarget => new CoordinatesTO(CoordinatesTO.AZEL,  ot.getDegrees1, ot.getDegrees2)
    }
}
