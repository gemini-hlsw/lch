package edu.gemini.lch.model

import java.time.{Instant, ZonedDateTime}
import scala.math.Ordering.Implicits._

/**
 * An interval is a period of time between two instants.
 * Includes start, excludes end.
 * Requirement: end >= start.
 * They have fixed ms duration, i.e. end - start and are not comparable.
 * To compare intervals, you should compare their lengths, i.e. Durations.
 */
case class Interval private(start: Instant, end: Instant) {
  def overlaps(other: Interval): Boolean =
      start < other.end && other.start < end

  def overlap(interval: Interval): Interval = {
    if (!overlaps(interval)) return null
    val s = if (start > interval.start) start else interval.start
    val e = if (end < interval.end) end else interval.end
    new Interval(s, e)
  }

  def contains(instant: Instant): Boolean =
    start <= instant && instant <= end

  def abuts(interval: Interval): Boolean = if (interval == null) {
    val t = Instant.now
    t.equals(start) || t.equals(end)
  } else {
    interval.end.equals(start) || end.equals(interval.start)
  }

  def isAfter(instant: Instant): Boolean =
    start >= instant

  def isBefore(instant: Instant): Boolean =
    end <= instant

  // The length of the duration, in millisec.
  def length(): Long =
    end.toEpochMilli - start.toEpochMilli
}

object Interval {
  def apply(start: ZonedDateTime, end: ZonedDateTime): Interval =
    Interval(start.toInstant, end.toInstant)
  def apply(start: Instant, end: Instant): Interval = {
    assert(end >= start)
    Interval(start, end)
  }
}