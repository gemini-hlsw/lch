package edu.gemini.lch.model

import java.time.Instant

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
}
