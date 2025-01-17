package edu.gemini.lch.services.util;


import edu.gemini.horizons.api.EphemerisEntry;
import edu.gemini.lch.model.*;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.HorizonsService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.VisibilityCalculator;
import edu.gemini.lch.services.internal.collector.TargetsCollector;
import edu.gemini.odb.browser.QueryResult;
import edu.gemini.spModel.core.HorizonsDesignation;
import jsky.coords.WorldCoords;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

/**
 * Testing the targets collector.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class TargetsCollectorTest implements BeanFactoryAware {

    @Resource
    private LaserNightService laserNightService;
    @Resource
    private ConfigurationService configurationService;
    @Resource
    private HorizonsService horizonsService;

    private BeanFactory beanFactory;


    @Test
    public void doesCollectVisibleTargets() throws JAXBException {
        // create laser night
        DateTime start = new DateTime(2012, 9, 1, 18, 0, 0);
        DateTime end = new DateTime(2012, 9, 2, 6, 0, 0);
        LaserNight night = new LaserNight(Site.NORTH,  start, end);

        // set targets collector up with mock visibility calculator
        Visibility alwaysVisible = new Visibility(new DateTime(night.getStart()), new DateTime(night.getEnd()));
        TargetsCollector collector = (TargetsCollector)beanFactory.getBean("targetsCollector", night);
        ReflectionTestUtils.setField(collector, "visibilityCalculator", createMockVisCalc(alwaysVisible));

        // add targets to collector
        QueryResult result = getQueryResult("/singleSiderealResult.xml");
        collector.addScienceTargets(result);

        // check result
        Set<Observation> observations = collector.getObservations();
        assertTrue(observations.size() > 0);
    }

    // REL-4634 demonstration test
    // It throws a NumberFormatException because timing windows have a repeat window of "forever"
    @Ignore
    @Test
    public void targetsWithForeverRepeatWindows() throws JAXBException {
        // create laser night
        DateTime start = new DateTime(2012, 9, 1, 18, 0, 0);
        DateTime end = new DateTime(2012, 9, 2, 6, 0, 0);
        LaserNight night = new LaserNight(Site.NORTH,  start, end);

        // set targets collector up with mock visibility calculator
        Visibility visibleTwice = new Visibility(new DateTime(2012,9,2,3,0,0), new DateTime(2012,9,1,22,0,0));
        TargetsCollector collector = (TargetsCollector)beanFactory.getBean("targetsCollector", night);
        ReflectionTestUtils.setField(collector, "visibilityCalculator", createMockVisCalc(visibleTwice));

        // add targets to collector
        QueryResult result = getQueryResult("/REL-4634.xml");
        collector.addScienceTargets(result);

        // check result
        Set<Observation> observations = collector.getObservations();
        assertTrue(observations.size() > 0);
    }

    @Test
    public void doesCollectVisibleTwiceTargets() throws JAXBException {
        // create laser night
        DateTime start = new DateTime(2012, 9, 1, 18, 0, 0);
        DateTime end = new DateTime(2012, 9, 2, 6, 0, 0);
        LaserNight night = new LaserNight(Site.NORTH,  start, end);

        // set targets collector up with mock visibility calculator
        Visibility visibleTwice = new Visibility(new DateTime(2012,9,2,3,0,0), new DateTime(2012,9,1,22,0,0));
        TargetsCollector collector = (TargetsCollector)beanFactory.getBean("targetsCollector", night);
        ReflectionTestUtils.setField(collector, "visibilityCalculator", createMockVisCalc(visibleTwice));

        // add targets to collector
        QueryResult result = getQueryResult("/singleSiderealResult.xml");
        collector.addScienceTargets(result);

        // check result
        Set<Observation> observations = collector.getObservations();
        assertTrue(observations.size() > 0);
    }

    @Test
    public void doesCollectNonSiderealWithoutEphemerides() throws JAXBException  {
        TargetsCollector c = collectNonSiderealTarget(createLaserNight(), new Visibility[] {});
        Set<Observation> observations = c.getObservations();
        assertEquals(1, observations.size());
    }
    @Test
    public void doesNotCollectNonVisibleNonSiderealTarget1() throws JAXBException  {
        // one ephemeris, not visible
        TargetsCollector c = collectNonSiderealTarget(createLaserNight(), new Visibility[] { Visibility.NEVER });
        Set<Observation> observations = c.getObservations();
        assertEquals(0, observations.size());
    }
    @Test
    public void doesNotCollectNonVisibleNonSiderealTarget2() throws JAXBException  {
        // two ephemerides, both not visible
        TargetsCollector c = collectNonSiderealTarget(createLaserNight(), new Visibility[] { Visibility.NEVER, Visibility.NEVER });
        Set<Observation> observations = c.getObservations();
        assertEquals(0, observations.size());
    }
    @Test
    public void doesCollectNonSiderealTarget1() throws JAXBException  {
        LaserNight night = createLaserNight();
        // one ephemerides visible all night
        TargetsCollector c = collectNonSiderealTarget(night, new Visibility[] { Visibility.ALWAYS });
        Set<Observation> observations = c.getObservations();
        assertEquals(1, observations.size());
        assertEquals(1, observations.iterator().next().getTargets().size());
    }
    @Test
    public void doesCollectNonSiderealTarget2() throws JAXBException  {
        LaserNight night = createLaserNight();
        // two ephemerides visible all night
        TargetsCollector c = collectNonSiderealTarget(night, new Visibility[] {
                new Visibility(night.getStart(), night.getEnd()),
                new Visibility(night.getStart(), night.getEnd())
        });
        Set<Observation> observations = c.getObservations();
        assertEquals(1, observations.size());
        assertEquals(2, observations.iterator().next().getTargets().size());
    }
    @Test
    public void doesCollectNonSiderealTarget3() throws JAXBException  {
        LaserNight night = createLaserNight();
        // two ephemerides, both visible at start and end of night (rise is after set)
        DateTime rise1 = night.getEnd().minusMinutes(90);
        DateTime set1  = night.getStart().plusMinutes(90);
        DateTime rise2 = night.getEnd().minusMinutes(60);
        DateTime set2  = night.getStart().plusMinutes(120);
        TargetsCollector c = collectNonSiderealTarget(night, new Visibility[] {
                new Visibility(rise1, set1),
                new Visibility(rise2, set2)
        });
        Set<Observation> observations = c.getObservations();
        assertEquals(1, observations.size());
        Observation o = observations.iterator().next();
        assertEquals(2, o.getTargets().size());
        for (ObservationTarget t : o.getTargets()) {
            Visibility v = t.getLaserTarget().getVisibility();
            assertTrue(v.isVisible());
            assertTrue(v.getRises().get().equals(rise1.toDate()) || v.getRises().get().equals(rise2.toDate()));
            assertTrue(v.getSets().get().equals(set1.toDate())   || v.getSets().get().equals(set2.toDate()));
        }
    }

    private LaserNight createLaserNight() {
        DateTime start = new DateTime(2012, 9, 1, 18, 0, 0);
        DateTime end = new DateTime(2012, 9, 2, 6, 0, 0);
        return new LaserNight(Site.NORTH,  start, end);
    }

    private TargetsCollector collectNonSiderealTarget(LaserNight night, Visibility[] visibilities) throws JAXBException {
        // set targets collector up with mock visibility calculator and horizons service
        TargetsCollector collector = (TargetsCollector)beanFactory.getBean("targetsCollector", night);
        NonSiderealTest test = new NonSiderealTest(night, visibilities);
        ReflectionTestUtils.setField(collector, "visibilityCalculator", test.createMockVisCalc());
        ReflectionTestUtils.setField(collector, "horizonsService", test.createMockHorizonsService());

        // add targets to collector
        QueryResult result = getQueryResult("/singleNonSiderealResult.xml");
        collector.addScienceTargets(result);

        // check result
        return collector;
    }

    @Test
    public void doesNotCollectInvisibleTargets() throws JAXBException {

        DateTime start = new DateTime(2012, 9, 1, 18, 0, 0);
        DateTime end = new DateTime(2012, 9, 2, 6, 0, 0);
        LaserNight night = new LaserNight(Site.NORTH,  start, end);

        TargetsCollector collector = (TargetsCollector)beanFactory.getBean("targetsCollector", night);
        ReflectionTestUtils.setField(collector, "visibilityCalculator", createMockVisCalc(Visibility.NEVER));

        QueryResult result = getQueryResult("/singleSiderealResult.xml");
        collector.addScienceTargets(result);

        Set<Observation> observations = collector.getObservations();
        assertEquals(0, observations.size());
    }

    // This test is no longer relevant since the simulator does not behave as the implementation due to changes
    // in the ODB browser.
    @Ignore
    public void doesWarnAboutUnknownNonSidereal() throws JAXBException {

        DateTime start = new DateTime(2012, 9, 1, 18, 0, 0);
        DateTime end = new DateTime(2012, 9, 2, 6, 0, 0);
        LaserNight night = new LaserNight(Site.NORTH,  start, end);

        TargetsCollector collector = (TargetsCollector)beanFactory.getBean("targetsCollector", night);
        ReflectionTestUtils.setField(collector, "visibilityCalculator", createMockVisCalc(Visibility.NEVER));

        QueryResult result = getQueryResult("/singleUnknownNonSiderealResult.xml");
        collector.addScienceTargets(result);

        Set<Observation> observations = collector.getObservations();
        assertEquals(1, observations.size());
        // use position 0,0 as placeholder for unknown non-sidereal targets
        assertEquals(new Double(0.0), observations.iterator().next().getScienceTarget().getDegrees1());
        assertEquals(new Double(0.0), observations.iterator().next().getScienceTarget().getDegrees2());
        // make sure that an error/info messages is written, this will result in an email to be sent out
        assertTrue(collector.getMessages().length() > 0);
    }

    // ==== helpers ====

    private QueryResult getQueryResult(String name) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(QueryResult.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        InputStream in = getClass().getResourceAsStream(name);
        QueryResult result = (QueryResult)unmarshaller.unmarshal(in);
        return result;
    }

    private VisibilityCalculator createMockVisCalc(Visibility visibility) {
        VisibilityCalculator calculator = mock(VisibilityCalculator.class);
        when(calculator.calculateVisibility((WorldCoords)anyObject())).thenReturn(visibility);
        return calculator;
    }

    private class NonSiderealTest {
        private final EphemerisEntry[] ephemerides;
        private final Visibility[] visibilities;

        public NonSiderealTest(LaserNight night, Visibility[] visibilities) {
            this.visibilities = visibilities;
            this.ephemerides = new EphemerisEntry[visibilities.length];

            if (visibilities.length > 0) {
                Duration between = new Duration(night.getDuration().getMillis() / (visibilities.length));
                DateTime dateTime = night.getStart();
                for (int i = 0; i < visibilities.length; i++) {
                    ephemerides[i] = new EphemerisEntry(dateTime.toDate(), new WorldCoords("0"+i+":00:00", "00:00:00"), .0, .0, .0, .0);
                    dateTime = dateTime.plus(between);
                }
            }
        }

        public HorizonsService createMockHorizonsService() {
            HorizonsService service = mock(HorizonsService.class);
            when(service.getCoordinates((LaserNight) anyObject(), (HorizonsDesignation) anyObject())).thenReturn(createHorizonsReply());
            return service;
        }

        public VisibilityCalculator createMockVisCalc() {
            VisibilityCalculator calculator = mock(VisibilityCalculator.class);
            if (visibilities.length == 0) {
                when(calculator.calculateVisibility((WorldCoords) anyObject())).
                        thenThrow(new IllegalArgumentException("too many calls"));
            }
            else if (visibilities.length == 1) {
                when(calculator.calculateVisibility((WorldCoords) anyObject())).
                        thenReturn(visibilities[0]).
                        thenThrow(new IllegalArgumentException("too many calls"));
            }
            else if (visibilities.length == 2) {
                when(calculator.calculateVisibility((WorldCoords) anyObject())).
                        thenReturn(visibilities[0]).
                        thenReturn(visibilities[1]).
                        thenThrow(new IllegalArgumentException("too many calls"));
            }
            else if (visibilities.length == 3) {
                when(calculator.calculateVisibility((WorldCoords) anyObject())).
                        thenReturn(visibilities[0]).
                        thenReturn(visibilities[1]).
                        thenReturn(visibilities[2]).
                        thenThrow(new IllegalArgumentException("too many calls"));
            }
            else {
                throw new IllegalArgumentException();
            }
            return calculator;
        }

        private List<WorldCoords> createHorizonsReply() {
            List<WorldCoords> reply = new ArrayList<>();
            for (EphemerisEntry e : ephemerides) {
                reply.add(e.getCoordinates());
            }
            return reply;
        }
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
