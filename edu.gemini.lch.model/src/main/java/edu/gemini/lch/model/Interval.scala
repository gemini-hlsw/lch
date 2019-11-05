package edu.gemini.lch.model

import java.time.{Instant, ZonedDateTime}

/**
 * An interval is a period of time between two instants.
 * Includes start, excludes end.
 * Requirement: end >= start.
 * They have fixed ms duration, i.e. end - start and are not comparable.
 * To compare intervals, you should compare their lengths, i.e. Durations.
 */
case class Interval(start: Instant, end: Instant) {
  def overlaps(other: Interval): Boolean = {
    val thisStart = start.toEpochMilli
    val thisEnd   = end.toEpochMilli

    // If null, assume now and check if now falls in the represented interval.
    if (other == null) {
      val now: Instant = Instant.now()
      thisStart < now.toEpochMilli && now.toEpochMilli < thisEnd
    }
    else {
      val otherStart = other.start.toEpochMilli
      val otherEnd   = other.end.toEpochMilli
      thisStart < otherEnd && otherStart < thisEnd
    }
  }

  def overlap(interval: Interval): Interval = {
    if (!overlaps(interval)) return null
    val s = if (start.toEpochMilli > interval.start.toEpochMilli) start else interval.start
    val e = if (end.toEpochMilli < interval.end.toEpochMilli) end else interval.end
    Interval(start, end)
  }

  def abuts(interval: Interval): Boolean = if (interval == null) {
    val t = Instant.now
    t.equals(start) || t.equals(end)
  } else {
    interval.end.equals(start) || end.equals(interval.start)
  }

  def contains(instant: Instant): Boolean =
    instant.toEpochMilli >= start.toEpochMilli && instant.toEpochMilli <= end.toEpochMilli

  def isAfter(instant: Instant): Boolean =
    start.toEpochMilli >= instant.toEpochMilli

  def isBefore(instant: Instant): Boolean =
    end.toEpochMilli <= instant.toEpochMilli
}

object Interval {
  def apply(start: ZonedDateTime, end: ZonedDateTime): Interval =
    Interval(start.toInstant, end.toInstant)
}