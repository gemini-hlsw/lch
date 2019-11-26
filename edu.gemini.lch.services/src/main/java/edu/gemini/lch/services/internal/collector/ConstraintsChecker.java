package edu.gemini.lch.services.internal.collector;

import edu.gemini.odb.browser.Observation;
import edu.gemini.odb.browser.TimingWindow;
import edu.gemini.odb.browser.TimingWindowRepeats;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper to check time constraints for observations.
 */
@Component
class ConstraintsChecker {

    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private static final PeriodFormatter periodFormatter =
            new PeriodFormatterBuilder().
                    appendHours().
                    appendSeparator(":").
                    appendMinutes().
                    toFormatter();

    /**
     * Checks if an observation can be observed between the given start and end time taking its timing
     * constraints into account. This is true if there is at least one timing window that overlaps with
     * the interval defined by start and end - or if there is no timing constraint at all.
     */
    boolean passesTimeConstraints(Observation o, ZonedDateTime start, ZonedDateTime end) {

        // no time constraints given ==> ok
        if (o.getConditions() == null || o.getConditions().getTimingWindowsNode() == null) {
            return true;
        }

        // if we have time constraints at least one of them needs to match
        for (TimingWindow w : o.getConditions().getTimingWindowsNode().getTimingWindows()) {
            if (passesWindow(w, start, end)) {
                return true;
            }
        }
        // none of the time constraints matches, i.e. we can not observe the target between start and end
        return false;
    }

    /**
     * Checks if a timing constraint overlaps with interval defined by start and end times.
     */
    private boolean passesWindow(TimingWindow w, ZonedDateTime nightStart, ZonedDateTime nightEnd) {
        Validate.isTrue(w.getTime().substring(20, 23).equals("UTC"));
        String s = w.getTime().substring(0, 19);
        ZonedDateTime start = ZonedDateTime.parse(s, dateTimeFormatter);

        if (w.getDuration() == null) {
            // duration is "forever"
            if (start.isBefore(nightEnd)) {
                return true;
            }  else {
                return false;
            }
        }

        ZonedDateTime end = start.plus(Period.parse(w.getDuration(), periodFormatter));
        Integer times = 1;
        Period period = null;
        if (w.getRepeats() != null) {
            TimingWindowRepeats r = w.getRepeats();
            times = Integer.parseInt(r.getTimes());
            period = Period.parse(r.getPeriod(), periodFormatter);
        }

        for (Integer i = 0; i < times; i++) {
            if (start.isBefore(nightEnd) &&
                    end.isAfter(nightStart)) {
                // the window overlaps with the start and end date of the night!
                return true;
            }
            if (period != null) {
                // if it's a repeated window, add period and try again...
                start = start.plus(period);
                end = end.plus(period);
            }
        }

        // none of the timing constraint windows overlaps with this night
        return false;
    }

}
