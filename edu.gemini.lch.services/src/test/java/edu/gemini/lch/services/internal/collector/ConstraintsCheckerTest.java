package edu.gemini.lch.services.internal.collector;

import edu.gemini.odb.browser.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class ConstraintsCheckerTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    private static final ZonedDateTime before  = ZonedDateTime.parse("2012-08-05 10:00:00", formatter);
    private static final ZonedDateTime start   = ZonedDateTime.parse("2012-08-05 11:00:00", formatter);
    private static final ZonedDateTime between = ZonedDateTime.parse("2012-08-05 12:00:00", formatter);
    private static final ZonedDateTime end     = ZonedDateTime.parse("2012-08-05 13:00:00", formatter);
    private static final ZonedDateTime after   = ZonedDateTime.parse("2012-08-05 14:00:00", formatter);

    @Resource
    ConstraintsChecker constraintsChecker;

    @Test
    public void acceptsOverlappingWithStartOfNight() {
        Observation o = createObservationWithTimeConstraint(before, "2:00");
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsOverlappingWithEndOfNight() {
        Observation o = createObservationWithTimeConstraint(between, "2:00");
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsOverlappingWithNight() {
        Observation o = createObservationWithTimeConstraint(between, "0:30");
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void doesNotAcceptBeforeNight() {
        Observation o = createObservationWithTimeConstraint(before, "0:30");
        assertFalse(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void doesNotAcceptAfterNight() {
        Observation o = createObservationWithTimeConstraint(after, "0:30");
        assertFalse(constraintsChecker.passesTimeConstraints(o, start, end));
    }


    @Test
    public void acceptsRepeatedOverlappingWithStartOfNight() {
        Observation o = createObservationWithRepeatedTimeConstraint(before, "0:30", "3", "1:00");
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsRepeatedOverlappingWithEndOfNight() {
        Observation o = createObservationWithRepeatedTimeConstraint(between, "0:30", "3", "1:00");
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsRepeatedOverlappingWithNight() {
        Observation o = createObservationWithRepeatedTimeConstraint(before, "0:30", "5", "1:00");
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void doesNotAcceptRepeatedBeforeNight() {
        Observation o = createObservationWithRepeatedTimeConstraint(before.minusHours(5), "0:30", "5", "1:00");
        assertFalse(constraintsChecker.passesTimeConstraints(o, start, end));
    }


    @Test
    public void doesNotAcceptRepeatedAfterNight() {
        Observation o = createObservationWithRepeatedTimeConstraint(after, "0:30", "5", "1:00");
        assertFalse(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsLongWindow() {
        DateTime time  = formatter.parseDateTime("2013-10-11 00:00:00");
        DateTime start = formatter.parseDateTime("2013-10-16 18:00:00");
        DateTime end   = formatter.parseDateTime("2013-10-17 06:00:00");
        Observation o = createObservationWithTimeConstraint(time, "288:00"); // 288 hrs = 12 day window
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsForeverStartingBeforeNight() {
        // duration null = forever
        Observation o = createObservationWithTimeConstraint(before, null);
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void acceptsForeverStartingDuringNight() {
        // duration null = forever
        Observation o = createObservationWithTimeConstraint(between, null);
        assertTrue(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    @Test
    public void doesNotAcceptForeverStartingAfterNight() {
        // duration null = forever
        Observation o = createObservationWithTimeConstraint(after, null);
        assertFalse(constraintsChecker.passesTimeConstraints(o, start, end));
    }

    // ==== helpers ====

    private Observation createObservationWithRepeatedTimeConstraint(DateTime time, String duration, String times, String period) {
        TimingWindow w = new TimingWindow();
        w.setTime(formatter.print(time)+" UTC");
        w.setDuration(duration);
        TimingWindowRepeats repeats = new TimingWindowRepeats();
        repeats.setTimes(times);
        repeats.setPeriod(period);
        w.setRepeats(repeats);
        return createObservationWithTimeConstraint(w);
    }

    private Observation createObservationWithTimeConstraint(DateTime time, String duration) {
        TimingWindow w = new TimingWindow();
        w.setTime(formatter.print(time)+" UTC");
        w.setDuration(duration);
        return createObservationWithTimeConstraint(w);
    }

    private Observation createObservationWithTimeConstraint(TimingWindow w) {
        TimingWindowsNode node = new TimingWindowsNode();
        node.getTimingWindows().add(w);
        Conditions c = new Conditions();
        c.setTimingWindowsNode(node);
        Observation o = new Observation();
        o.setConditions(c);
        return o;
    }
}
