package edu.gemini.lch.services.timeline

import java.time.{Instant, ZoneId, ZonedDateTime}

import edu.gemini.lch.model.{BlanketClosure, Interval, PropagationWindow, ShutteringWindow}
import java.util.Date

object PartType extends Enumeration {
  type PartType = Value
  val
  Day, CivilTwilight, NauticalTwilight, AstronomicalTwilight, Night,
  Visible, AboveLimit,
  Open, Closed = Value
}
import PartType._

/**
 */
class Part(t: Set[PartType], int: Interval)  {


  def this(t: Set[PartType], start: ZonedDateTime, end: ZonedDateTime) = this(t, Interval(start.toInstant, end.toInstant))

  def types = t
  def interval = int
  def start = int.start
  def end = int.end

  /** Gets overlapping part of this and other, if any. */
  def overlapping(other: Part): Option[Part] = {
    val overlap = interval.overlap(other.interval)
    if (overlap != null)                                            // TODO: how to deal nicely with null
      Some(Part(t++other.types, ZonedDateTime.ofInstant(overlap.start, ZoneId.systemDefault()), ZonedDateTime.ofInstant(overlap.end, ZoneId.systemDefault())))
    else
      None
  }

  /** Gets part of this before other, if any */
    // TODO-JODA
  def heading(other: Part): Option[Part] =
    if (start.isBefore(other.start)) Some(Part(t, start, other.start)) else None

  /** Gets part of this that is after other, if any */
  def tailing(other: Part): Option[Part] =
    if (end.isAfter(other.end)) Some(Part(t, other.end, end)) else None

  /** Combines two parts by returning the heading, overlapping and tailing parts. */
  def combine(other: Part): List[Part] =
    if (interval.overlaps(other.interval)) {
      List(heading(other), overlapping(other), tailing(other)) flatten
    } else {
      List(this)
    }

  /** Checks if two parts abut each other. */
  def abuts(other: Part) = interval abuts(other interval)

}

object Part {
  def apply(t: Set[PartType], start: ZonedDateTime, end: ZonedDateTime) =
    new Part(t, Interval(start, end))
  def apply(t: PartType, start: Instant, end: Instant) =
    new Part(Set(t), Interval(ZonedDateTime.ofInstant(start, ZoneId.systemDefault()), ZonedDateTime.ofInstant(end, ZoneId.systemDefault())))
  def apply(t: PartType, start: ZonedDateTime, end: ZonedDateTime) =
    new Part(Set(t), Interval(start, end))
}



class Timeline(parts: List[Part]) {

  def allParts = parts

  def addPropagationWindows(windows: List[PropagationWindow]) =
    add(windows map(w => Part(Set(Open), w.getStart, w.getEnd)))

  def addShutteringWindows(windows: List[ShutteringWindow]) =
    add(windows map(w => Part(Set(Closed), w.getStart, w.getEnd)))

  def addBlanketClosures(closures: List[BlanketClosure]) =
    add(closures map(w => Part(Set(Closed), w.getStart, w.getEnd)))

  def addPart(t: PartType, start: ZonedDateTime, end: ZonedDateTime) =
    add(Part(Set(t), start, end))

  def addPart(t: PartType, start: Instant, end: Instant) =
    add(Part(Set(t), ZonedDateTime.ofInstant(start, ZoneId.systemDefault()), ZonedDateTime.ofInstant(end, ZoneId.systemDefault())))

  def add(part: Part) =
    new Timeline(parts flatMap (p => p combine part))

  def add(moreParts: List[Part]): Timeline =
    new Timeline(add(parts, moreParts))

  private def add(s1: List[Part], s2: List[Part]): List[Part] =
    s2 match {
      case Nil => s1
      case p::tail => add(add(s1, tail), p)
    }

  private def add(s1: List[Part], p: Part): List[Part] =
    s1 match {
      case Nil => Nil
      case sp::tail => sp.combine(p)++add(tail, p)
    }

}

