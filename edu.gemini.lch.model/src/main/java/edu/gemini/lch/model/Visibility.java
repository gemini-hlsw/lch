package edu.gemini.lch.model;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.*;

/**
 * Representation of the visibility of a target during the course of a single laser night.
 * It stores the times when an object rises above and sets below the horizon and in addition to that it
 * stores the times when an object rises above and sets below the laser minimal altitude (configurable but
 * roughly between 30 and 40 degrees).
 */
public final class Visibility {

    public static final Visibility NEVER = new Visibility();
    public static final Visibility ALWAYS = new Visibility(DateTime.now().minusYears(4000), DateTime.now().plusYears(4000));

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
     * @param rises  time the object rises above a given altitiude
     * @param sets   time the object sets below a given altitude
     */
    public Visibility(final DateTime rises, final DateTime sets) {
        this(
            Optional.of(new RiseSet(rises.toDate(), sets.toDate())),
            Optional.of(new RiseSet(rises.toDate(), sets.toDate()))
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
            intervals.add(new Interval(rs.rises.getTime(), rs.sets.getTime()));
        } else {
            intervals.add(new Interval(night.getStart().getMillis(), rs.sets.getTime()));
            intervals.add(new Interval(rs.rises.getTime(), night.getEnd().getMillis()));
        }
        return Collections.unmodifiableList(intervals);
    }

    public Optional<Date> getRises() {
        return aboveHorizon.map(rs -> rs.rises);
    }

    public Optional<Date> getSets() {
        return aboveHorizon.map(rs -> rs.sets);
    }

    public Optional<Date> getRisesAboveLimit() {
        return aboveLimit.map(rs -> rs.rises);
    }

    public Optional<Date> getSetsBelowLimit() {
        return aboveLimit.map(rs -> rs.sets);
    }

    public Duration getMaxDurationAboveLimit(final LaserNight night) {
        return aboveLimit.map(rs -> getMaxDuration(night, rs)).orElse(Duration.ZERO);
    }

    private Duration getMaxDuration(final LaserNight night, final RiseSet rs) {

        if (rs.risesBeforeSets()) {
            return new Duration(rs.rises.getTime(), rs.sets.getTime());

        } else {
            final Duration d1 = new Duration(night.getStart().getMillis(), rs.sets.getTime());
            final Duration d2 = new Duration(rs.rises.getTime(), night.getEnd().getMillis());
            if (d1.isLongerThan(d2)) {
                return d1;
            } else {
                return d2;
            }
        }
    }

    public final static class RiseSet {
        final Date rises;
        final Date sets;
        public RiseSet(final Date rises, final Date sets) {
            Validate.notNull(rises);
            Validate.notNull(sets);
            this.rises = rises;
            this.sets  = sets;
        }
        boolean risesBeforeSets() {
            return rises.before(sets);
        }
    }
}
