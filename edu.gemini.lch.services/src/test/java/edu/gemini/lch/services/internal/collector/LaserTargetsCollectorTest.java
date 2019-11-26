package edu.gemini.lch.services.internal.collector;

import edu.gemini.lch.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test cases for grouping of nearby targets.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class LaserTargetsCollectorTest {

    @Test
    public void windowsAreOrdered() {
        RaDecLaserTarget target = new RaDecLaserTarget(null, 0.0, 0.0, Visibility.ALWAYS);
        ZonedDateTime t = ZonedDateTime.now();
        target.getPropagationWindows().add(new PropagationWindow(t.plusSeconds(2000), t.plusSeconds(2010)));
        target.getPropagationWindows().add(new PropagationWindow(t.plusSeconds(3000), t.plusSeconds(3010)));
        target.getPropagationWindows().add(new PropagationWindow(t.plusSeconds(1000), t.plusSeconds(1010)));
        target.getPropagationWindows().add(new PropagationWindow(t.plusSeconds(8000), t.plusSeconds(8010)));
        target.getPropagationWindows().add(new PropagationWindow(t.plusSeconds(5000), t.plusSeconds(5010)));

        // check that propagation windows are ordered (ascending by time)
        ZonedDateTime currentT;
        currentT = t;
        assertEquals(5, target.getPropagationWindows().size());
        for (PropagationWindow w : target.getPropagationWindows()) {
            assertTrue(w.getStart().isAfter(currentT));
            currentT = w.getStart();
        }

        // check that shuttering windows calculated from propagation windows are ordered (ascending by time)
        currentT = t; // reset
        assertEquals(4, target.getShutteringWindows().size());
        for (ShutteringWindow w : target.getShutteringWindows()) {
            assertTrue(w.getStart().isAfter(currentT));
            currentT = w.getStart();
        }

    }

    @Test
    public void doesCalculateNewCenter() {
        ObservationTarget t0 = new SiderealTarget("name0", "type", 10.0, 10.0);
        ObservationTarget t1 = new SiderealTarget("name1", "type", 20.0, 20.0);
        ObservationTarget t2 = new SiderealTarget("name2", "type", 15.0, 15.0);
        ObservationTarget t3 = new SiderealTarget("name3", "type", 11.0, 19.0);

        LaserTargetsCollector.Group g = new LaserTargetsCollector.Group(null, t0, Visibility.ALWAYS);
        assertEquals(10.0, g.getCenter().getRaDegrees(), 0.0001);
        assertEquals(10.0, g.getCenter().getDecDegrees(), 0.0001);

        /* The assumption is that for nearby targets we can just take the average of the different
           coordinates for the new center point in the middle of all targets added to the group. */
        g.add(t1);
        assertEquals(15.0, g.getCenter().getRaDegrees(), 0.0001);
        assertEquals(15.0, g.getCenter().getDecDegrees(), 0.0001);

        g.add(t2);
        assertEquals(15.0, g.getCenter().getRaDegrees(), 0.0001);
        assertEquals(15.0, g.getCenter().getDecDegrees(), 0.0001);

        g.add(t3);
        assertEquals(14.0, g.getCenter().getRaDegrees(), 0.0001);
        assertEquals(16.0, g.getCenter().getDecDegrees(), 0.0001);
    }

    @Test
    public void doesGroupProperly() {
        LaserTargetsCollector g = new LaserTargetsCollector(null, 1.0);

        g.addObservationTarget(new SiderealTarget("name", "type", 10.0, 10.5), Visibility.ALWAYS);
        g.addObservationTarget(new SiderealTarget("name", "type", 9.5, 10.9), Visibility.ALWAYS);
        g.addObservationTarget(new SiderealTarget("name", "type", 9.1, 10.0), Visibility.ALWAYS);

        g.addObservationTarget(new SiderealTarget("name", "type", 30.0, 10.0), Visibility.ALWAYS);
        g.addObservationTarget(new SiderealTarget("name", "type", 30.0, 10.2), Visibility.ALWAYS);
        g.addObservationTarget(new SiderealTarget("name", "type", 30.2, 9.3), Visibility.ALWAYS);

        g.addObservationTarget(new SiderealTarget("name", "type", 6.0, 19.0), Visibility.ALWAYS);
        g.addObservationTarget(new SiderealTarget("name", "type", 6.2, 18.9), Visibility.ALWAYS);

        g.addObservationTarget(new SiderealTarget("name", "type", 11.0, 19.0), Visibility.ALWAYS);

        assertEquals(4, g.getGroups().size());
    }

    @Test
    public void doesSetLaserTarget() {
        LaserTargetsCollector g = new LaserTargetsCollector(null, 1.0);

        // single target
        ObservationTarget t0 = new SiderealTarget("name", "type", 0.0, 0.0);
        // group of two targets
        ObservationTarget t1 = new SiderealTarget("name", "type", 2.0, 2.0);
        ObservationTarget t2 = new SiderealTarget("name", "type", 2.1, 1.9);

        g.addObservationTarget(t0, Visibility.ALWAYS);
        g.addObservationTarget(t1, Visibility.ALWAYS);
        g.addObservationTarget(t2, Visibility.ALWAYS);

        // we should have two groups
        assertEquals(2, g.getGroups().size());
        // all targets should have a laser target
        assertTrue(t0.getLaserTarget() != null);
        assertTrue(t1.getLaserTarget() != null);
        assertTrue(t2.getLaserTarget() != null);
        // targets in same group should have same laser target
        assertTrue(t1.getLaserTarget() == t2.getLaserTarget());
        // targets in different groups should have different laser targets
        assertTrue(t0.getLaserTarget() != t1.getLaserTarget());
    }
}