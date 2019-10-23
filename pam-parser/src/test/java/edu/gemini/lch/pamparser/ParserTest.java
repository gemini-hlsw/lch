package edu.gemini.lch.pamparser;

import edu.gemini.lch.model.PropagationWindow;
import edu.gemini.lch.model.Site;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Simple test class to check if parser works as expected.
 */
public class ParserTest {

    @Test
    public void canParseRaDecTarget() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/singleRaDecTarget.txt")) {
            Response response = Response.parseResponse(is);
            Target target = response.getTargets().get(0);
            // we expect one target with 7 windows
            assertTrue(target instanceof RaDecTarget);
            assertEquals(1, response.getTargets().size());
            assertEquals(7, response.getWindowsForTarget(target).size());

            // check target
            //assertEquals("J2000", ((RaDecTarget) target).getCatalogDate());
            assertEquals(333.319, ((RaDecTarget) target).getRaDegrees(), 0.00001);
            assertEquals(-1.768, ((RaDecTarget) target).getDecDegrees(), 0.00001);

            // check first window (all times in file from LCH are UT / GMT)
            // start = 2011 May 19 (139) 2204 55  // end = 2011 May 20 (140) 0526 56
            PropagationWindow window = response.getWindowsForTarget(target).get(0);
            assertEquals(new DateTime(2011, 5, 19, 22, 4, 55, DateTimeZone.UTC), window.getStart().toDateTime(DateTimeZone.UTC));
            assertEquals(new DateTime(2011, 5, 20, 5, 26, 56, DateTimeZone.UTC), window.getEnd().toDateTime(DateTimeZone.UTC));

        }
    }

    @Test
    public void canParseEmptyFile() throws Exception {
        // no targets, should never happen, but better to be safe than sorry
        try (InputStream is = getClass().getResourceAsStream("/emptyNorthRaDec.txt")) {
            // we expect an empty response
            Response response = Response.parseResponse(is);
            assertEquals(0, response.getTargets().size());
        }
    }

    @Test
    public void canParseEmptyRaDecTarget() throws Exception {
        // targets without any windows show percentage as ".00" instead of "0.00" which is a special case..
        try (InputStream is = getClass().getResourceAsStream("/emptyRaDecTarget.txt")) {
            // we expect one target with 0 windows
            Response response = Response.parseResponse(is);
            assertEquals(1, response.getTargets().size());

            Target target = response.getTargets().get(0);
            assertTrue(target instanceof RaDecTarget);
            assertEquals(0, response.getWindowsForTarget(target).size());
        }
    }

    @Test
    public void canParseAzTarget() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/singleAzTarget.txt")) {
            // we expect one target with 13 windows
            Response response = Response.parseResponse(is);
            assertEquals(1, response.getTargets().size());

            Target target = response.getTargets().get(0);
            assertTrue(target instanceof AzAltTarget);
            assertEquals(13, response.getWindowsForTarget(target).size());

            // check target
            assertEquals(270.0, ((AzAltTarget) target).getAzDegrees(), 0.00001);
            assertEquals(20.0, ((AzAltTarget) target).getAltDegrees(), 0.00001);
        }
    }

    @Test
    public void canParseSouthRaDecFile() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/responseSouthRaDec.txt")) {
            Response response = Response.parseResponse(is);
            // check site
            assertEquals(Site.SOUTH, response.getSite());
            // check mission start / end from header
            //Mission Start Date/Time (UTC):   2011 May 19 22:04:55
            //Mission Stop  Date/Time (UTC):   2011 May 20 11:14:28
            assertEquals(new DateTime(2011, 5, 19, 22, 4, 55, DateTimeZone.UTC), response.getMissionStart().toDateTime(DateTimeZone.UTC));
            assertEquals(new DateTime(2011, 5, 20, 11, 14, 28, DateTimeZone.UTC), response.getMissionEnd().toDateTime(DateTimeZone.UTC));
            assertEquals(new Integer(139), response.getJDay());
            // we expect 84 targets
            assertEquals(84, response.getTargets().size());
        }
    }

    @Test
    public void canParseNorthRaDecFile() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/responseNorthRaDec.txt")) {
            Response response = Response.parseResponse(is);
            // check site
            assertEquals(Site.NORTH, response.getSite());
            // check mission start / end from header
            //Mission Start Date/Time (UTC):   2011 May 30 05:05:12
            //Mission Stop  Date/Time (UTC):   2011 May 30 15:33:38
            assertEquals(new DateTime(2011, 5, 30, 5, 5, 12, DateTimeZone.UTC), response.getMissionStart().toDateTime(DateTimeZone.UTC));
            assertEquals(new DateTime(2011, 5, 30, 15, 33, 38, DateTimeZone.UTC), response.getMissionEnd().toDateTime(DateTimeZone.UTC));
            assertEquals(new Integer(150), response.getJDay());
            // we expect 95 targets
            assertEquals(95, response.getTargets().size());
        }
    }


    @Test
    public void canParseSouthAzimuthFile() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/responseSouthAzimuth.txt")) {
            Response response = Response.parseResponse(is);
            assertEquals(new Integer(111), response.getJDay());
            // we expect 29 targets
            assertEquals(29, response.getTargets().size());
        }
    }

    @Test
    public void canParseNorthAzimuthFile() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/responseNorthAzimuth.txt")) {
            Response response = Response.parseResponse(is);
            assertEquals(new Integer(150), response.getJDay());
            // we expect 1 target
            assertEquals(1, response.getTargets().size());
        }
    }

    // test some files which have caused problems in the past
    // this includes changes in the header and other unexpected stuff that made the parser fail
    // (for example on March 22 2013 LCH fixed a typo in the header from STATEGIC to STRATEGIC which
    // caused the parser to fail, because this string was part of a literal - bonk!)
    @Test
    public void canParse() throws Exception {
        canParse("/PAM-test-2013-055-RaDec.txt");
        canParse("/PAM-test-2013-055-AzEl.txt");
        canParse("/PAM-test-2013-082-RaDec.txt");
        canParse("/PAM-test-2013-083-RaDec.txt");
        canParse("/PAM-test-2013-083-AzEl.txt");
        canParse("/PAM-test-2013-084-RaDec.txt");
        canParse("/PAM-test-2013-084-AzEl.txt");
        canParse("/PAM-test-2013-087-RaDec.txt");
        canParse("/PAM-test-2013-087-AzEl.txt");
    }

    private void canParse(String name) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(name)) {
            Response response = Response.parseResponse(is);
            assertNotNull(response.getMissionStart());
            assertNotNull(response.getMissionEnd());
            assertNotNull(response.getMissionId());
            assertNotNull(response.getReportTime());
            assertNotNull(response.getSite());
            assertTrue(response.getTargets().size() > 0);
        }
    }

}
