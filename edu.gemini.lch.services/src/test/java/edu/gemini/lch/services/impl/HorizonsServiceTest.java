package edu.gemini.lch.services.impl;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.HorizonsService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.spModel.core.HorizonsDesignation;
import edu.gemini.spModel.core.HorizonsDesignation$;
import jsky.coords.WorldCoords;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Some tests to play around with the horizons service.
 * They need a connection to gnodb.hi.gemini.edu, ignore them in case that does not work for you.
 */
//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class HorizonsServiceTest {

    @Resource
    private LaserNightService laserNightService;

    @Resource
    private HorizonsService horizonsServiceReal;

    @Test
    public void canNotLookupUnknown() {
        LaserNight night = laserNightService.createLaserNight(ZonedDateTime.now());
        // This case really shouldn't happen since the HorizonsDesignation was
        // known to be valid via a past Horizons lookup. However, some ids have
        // changed in the past after the fact.
        final HorizonsDesignation id = HorizonsDesignation.read("Comet_unknownobject").get();
        try {
            horizonsServiceReal.getCoordinates(night, id); // not known
            Assert.fail("expected an exception because the object is not known");
        } catch (RuntimeException ex) {
            Assert.assertEquals("EphemerisEmpty", ex.getCause().getMessage());
        }
    }

    @Test
    public void canLookupUnique() {
        LaserNight night = laserNightService.createLaserNight(ZonedDateTime.now());
        final HorizonsDesignation id = HorizonsDesignation.read("AsteroidOld_134340").get();
        List<WorldCoords> coordinates = horizonsServiceReal.getCoordinates(night, id);
        Assert.assertTrue(coordinates.size() > 0);
    }

}
