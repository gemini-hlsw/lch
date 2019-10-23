package edu.gemini.lch.services;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.Site;
import edu.gemini.lch.model.Visibility;
import edu.gemini.lch.services.impl.VisibilityCalculatorImpl;
import edu.gemini.odb.browser.HmsDms;
import jsky.coords.WorldCoords;
import org.joda.time.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Some tests to check if visibility is calculated correctly.
 * The results are validated using the service at http://catserver.ing.iac.es/staralt/.
 */
public class VisibilityCalculatorTest {

    private static final DateTime Day20120824 = new DateTime(2012,  8, 24, 0, 0);
    private static final DateTime Day20120912 = new DateTime(2012,  9, 12, 0, 0);
    private static final DateTime Day20121212 = new DateTime(2012, 12, 12, 0, 0);

    @Test
    public void checkTargetAt5Hrs() {
        LaserNight night = ModelFactory.createNight(Site.NORTH, Day20120824);
        VisibilityCalculator calculator = createCalculator(night, 0.0);

        WorldCoords c = new WorldCoords("5:00:00", "+19:49:26");
        Visibility v = calculator.calculateVisibility(c);
        List<Interval> visible = v.getVisibleIntervalsDuring(night);

        Assert.assertTrue(v.isVisible());
        Assert.assertEquals(1, visible.size());
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isAfter(new DateTime(2012, 8, 25, 0, 36, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isBefore(new DateTime(2012, 8, 25, 0, 38, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isAfter(new DateTime(2012, 8, 25, 6, 3, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isBefore(new DateTime(2012, 8, 25, 6, 5, 0, DateTimeZone.forID("HST"))));
    }

    @Test
    public void checkTargetAt10Hrs() {
        LaserNight night = ModelFactory.createNight(Site.NORTH, Day20120824);
        VisibilityCalculator calculator = createCalculator(night, 0.0);

        WorldCoords c = new WorldCoords("10:00:00", "+19:49:26");
        Visibility v = calculator.calculateVisibility(c);
        List<Interval> visible = v.getVisibleIntervalsDuring(night);

        Assert.assertTrue(v.isVisible());
        Assert.assertEquals(1, visible.size());
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isAfter(new DateTime(2012, 8, 25, 5, 33, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isBefore(new DateTime(2012, 8, 25, 5, 38, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isAfter(new DateTime(2012, 8, 25, 6, 3, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isBefore(new DateTime(2012, 8, 25, 6, 5, 0, DateTimeZone.forID("HST"))));
    }

    @Test
    public void checkTargetAt15Hrs() {

        LaserNight night = ModelFactory.createNight(Site.NORTH, Day20120824);
        VisibilityCalculator calculator = createCalculator(night, 0.0);

        WorldCoords c = new WorldCoords("15:00:00", "+19:49:26");
        Visibility v = calculator.calculateVisibility(c);
        List<Interval> visible = v.getVisibleIntervalsDuring(night);

        Assert.assertTrue(v.isVisible());
        Assert.assertEquals(1, visible.size());
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isAfter(new DateTime(2012, 8, 24, 18, 41, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isBefore(new DateTime(2012, 8, 24, 18, 43, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isAfter(new DateTime(2012, 8, 24, 23, 30, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isBefore(new DateTime(2012, 8, 24, 23, 50, 0, DateTimeZone.forID("HST"))));
    }

    @Test
    public void checkTargetAbove30() {

        LaserNight night = ModelFactory.createNight(Site.NORTH, Day20120912);
        VisibilityCalculator calculator = createCalculator(night, 30.0);

        WorldCoords c = new WorldCoords("04:11:04.76", "-01:54:09.78");
        Visibility v = calculator.calculateVisibility(c);
        List<Interval> visible = v.getVisibleIntervalsAboveLimitDuring(night);

        Assert.assertTrue(v.isVisible());
        Assert.assertEquals(1, visible.size());
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isAfter(new DateTime(2012, 9, 13, 1, 13, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isBefore(new DateTime(2012, 9, 13, 1, 15, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isAfter(new DateTime(2012, 9, 13, 6, 8, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isBefore(new DateTime(2012, 9, 13, 6, 10, 0, DateTimeZone.forID("HST"))));
    }

    @Test
    public void checkTargetXyz() throws Exception {

        LaserNight night = ModelFactory.createNight(Site.NORTH, Day20120912);
        VisibilityCalculator calculator = createCalculator(night, 30.0);

        HmsDms hmsDms = new HmsDms();
        hmsDms.setRa("04:11:04.76");
        hmsDms.setDec("-01:54:09.78");
        Visibility v = calculator.calculateVisibility(new WorldCoords(hmsDms.getRa(), hmsDms.getDec()));
        List<Interval> visible = v.getVisibleIntervalsAboveLimitDuring(night);

        Assert.assertTrue(v.isVisible());
        Assert.assertEquals(1, visible.size());
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isAfter(new DateTime(2012, 9, 13, 1, 13, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isBefore(new DateTime(2012, 9, 13, 1, 15, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isAfter(new DateTime(2012, 9, 13, 6, 8, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isBefore(new DateTime(2012, 9, 13, 6, 10, 0, DateTimeZone.forID("HST"))));
    }

    /**
     * Test case for the special case of an object that is visible twice during a night, i.e. it is visible at the
     * start of the night, then sets and rises again before the end of the night. This results in the end time
     * of the visibility being before the start time. The duration of such a visibility is defined as the longer
     * of the two visibility periods to make things a bit simpler. When using the start and end times of visibilities
     * the caller has to take care of making sure that the case of end time (set) before start time (rise) is
     * handled properly!
     * @throws Exception
     */
    @Test
    public void handlesVisibleTwiceCorrectly() throws Exception {

        LaserNight night = ModelFactory.createNight(Site.NORTH, Day20121212);
        VisibilityCalculator calculator = createCalculator(night, 0.0);

        HmsDms hmsDms = new HmsDms();
        hmsDms.setRa("18:00:38.29");
        hmsDms.setDec("10:33:47.97");
        Visibility v = calculator.calculateVisibility(new WorldCoords(hmsDms.getRa(), hmsDms.getDec()));
        List<Interval> visible = v.getVisibleIntervalsAboveLimitDuring(night);

        // This object is above 0 degrees from sunset until set time (17:44 - 19:09) and then again
        // from rise time until sunrise (06:37 - 06:48). The duration is the longer of the two visibility
        // periods which is about 1h 25m or 5100s. Note that the times and durations may vary slightly if
        // the approximation algorithm used to find the set and rise time is changed.

        long delta = 5000; // error tolerance
        Assert.assertTrue(v.isVisible());
        Assert.assertEquals(2, visible.size());

        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isAfter(new DateTime(2012, 12, 12, 17, 43, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getStart()).isBefore(new DateTime(2012, 12, 12, 17, 45, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isAfter(new DateTime(2012, 12, 12, 19, 8, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(0).getEnd()).isBefore(new DateTime(2012, 12, 12, 19, 10, 0, DateTimeZone.forID("HST"))));

        Assert.assertTrue(new DateTime(visible.get(1).getStart()).isAfter(new DateTime(2012, 12, 13, 6, 36, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(1).getStart()).isBefore(new DateTime(2012, 12, 13, 6, 38, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(1).getEnd()).isAfter(new DateTime(2012, 12, 13, 6, 47, 0, DateTimeZone.forID("HST"))));
        Assert.assertTrue(new DateTime(visible.get(1).getEnd()).isBefore(new DateTime(2012, 12, 13, 6, 49, 0, DateTimeZone.forID("HST"))));

        Assert.assertTrue(v.getMaxDurationAboveLimit(night).isLongerThan(new Duration(5070*1000-delta)));
        Assert.assertTrue(v.getMaxDurationAboveLimit(night).isShorterThan(new Duration(5070*1000+delta)));
    }

    private VisibilityCalculator createCalculator(LaserNight night, Double altitude) {
        return new VisibilityCalculatorImpl(night.getSite(), night.getStart(), night.getEnd(), altitude);
    }

}
