package edu.gemini.lch.model;

import org.apache.commons.lang.Validate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Representation of the visibility of a target during the course of a single laser night.
 * It stores the times when an object rises above and sets below the horizon and in addition to that it
 * stores the times when an object rises above and sets below the laser minimal altitude (configurable but
 * roughly between 30 and 40 degrees).
 */
public final class Visibility {

    public static final Visibility NEVER = new Visibility();
    public static final Visibility ALWAYS = new Visibility(ZonedDateTime.now().minusYears(4000), ZonedDateTime.now().plusYears(4000));

    private final Optional<RiseSet> aboveHorizon;
    private final Optional<RiseSet> aboveLimit;

    /**
     * Creates an empty visibility object which is used to signal that an object is NOT visible at all.
     */
    private Visibility() {
        aboveHorizon = Optional.empty();
        aboveLimit   = Optional.empty();
    }

    /**
     * Creates a visibility window for an object that has rise and set times.
     * @param rises  time the object rises above a given altitude
     * @param sets   time the object sets below a given altitude
     */
    public Visibility(final ZonedDateTime rises, final ZonedDateTime sets) {
        this(
            Optional.of(new RiseSet(rises.toInstant(), sets.toInstant())),
            Optional.of(new RiseSet(rises.toInstant(), sets.toInstant()))
        );
    }

    public Visibility(final Optional<RiseSet> aboveHorizon, final Optional<RiseSet> aboveLimit) {
        this.aboveHorizon = aboveHorizon;
        this.aboveLimit   = aboveLimit;
    }

    public boolean isVisible() {
        return aboveHorizon.isPresent();
    }

    public List<Interval> getVisibleIntervalsDuring(final BaseLaserNight night) {
        return aboveHorizon.map(rs -> getVisibleIntervalsDuring(night, rs)).orElse(Collections.emptyList());
    }

    public List<Interval> getVisibleIntervalsAboveLimitDuring(final BaseLaserNight night) {
        return aboveLimit.map(rs -> getVisibleIntervalsDuring(night, rs)).orElse(Collections.emptyList());
    }

    private List<Interval> getVisibleIntervalsDuring(final BaseLaserNight night, final RiseSet rs) {
        final List<Interval> intervals = new ArrayList<>();
        if (rs.risesBeforeSets()) {
            intervals.add(new Interval(rs.rises, rs.sets));
        } else {
            intervals.add(new Interval(night.getStart().toInstant(), rs.sets));
            intervals.add(new Interval(rs.rises, night.getEnd().toInstant()));
        }
        return Collections.unmodifiableList(intervals);
    }

    public Optional<Instant> getRises() {
        return aboveHorizon.map(rs -> rs.rises);
    }

    public Optional<Instant> getSets() {
        return aboveHorizon.map(rs -> rs.sets);
    }

    public Optional<Instant> getRisesAboveLimit() {
        return aboveLimit.map(rs -> rs.rises);
    }

    public Optional<Instant> getSetsBelowLimit() {
        return aboveLimit.map(rs -> rs.sets);
    }

    public Duration getMaxDurationAboveLimit(final LaserNight night) {
        return aboveLimit.map(rs -> getMaxDuration(night, rs)).orElse(Duration.ZERO);
    }

    private Duration getMaxDuration(final LaserNight night, final RiseSet rs) {

        if (rs.risesBeforeSets()) {
            return Duration.ofMillis(rs.sets.toEpochMilli() - rs.rises.toEpochMilli());
        } else {
            final Duration d1 = Duration.ofMillis(night.getStart().toInstant().toEpochMilli() - rs.sets.toEpochMilli());
            final Duration d2 = Duration.ofMillis(rs.rises.toEpochMilli() - rs.sets.toEpochMilli());
            return d1.compareTo(d2) > 0 ? d1 : d2;
        }
    }

    public final static class RiseSet {
        final Instant rises;
        final Instant sets;
        public RiseSet(final Instant rises, final Instant sets) {
            Validate.notNull(rises);
            Validate.notNull(sets);
            this.rises = rises;
            this.sets  = sets;
        }
        boolean risesBeforeSets() {
            return rises.isBefore(sets);
        }
    }
}
