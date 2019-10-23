package edu.gemini.lch.services;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.data.fixture.DatabaseFixture;
import edu.gemini.lch.model.LaserTarget;
import edu.gemini.lch.model.Site;
import edu.gemini.lch.pamparser.Response;
import edu.gemini.odb.browser.OdbBrowser;
import edu.gemini.odb.browser.QueryResult;
import jsky.plot.SunRiseSet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class LaserNightServiceTest extends DatabaseFixture {

    @Resource
    private LaserNightService laserNightService;
    @Resource
    private OdbBrowser odbBrowser;

    @Test
    public void canCreateLaserNight() throws Exception {
        QueryResult queryResult = odbBrowser.query("simpleResult.xml");
        DateTime now = new DateTime().plusDays(20);
        DateTime day = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 12, 0, 0);
        LaserNight newNight = laserNightService.createAndPopulateLaserNight(day);

        // this one lies way in the future, we don't expect it to be populated
        assertFalse(newNight.isTestNight());
        assertTrue(newNight.getLaserTargets().size() == 0);
        assertTrue(newNight.getObservations().size() == 0);
    }

    @Test
    public void canCreateAndPopulateLaserNight() throws Exception {
        QueryResult queryResult = odbBrowser.query("simpleResult.xml");
        DateTime now = new DateTime().plusDays(1);
        DateTime day = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 12, 0, 0);
        LaserNight newNight = laserNightService.createAndPopulateLaserNight(day, queryResult, new QueryResult());

        // this one is for tomorrow, we expect it to be populated with targets
        assertFalse(newNight.isTestNight());
        assertTrue(newNight.getLaserTargets().size() > 0);
        assertTrue(newNight.getObservations().size() > 0);
    }

    @Test
    public void canCreateTestNight() throws Exception {
        QueryResult queryResult = odbBrowser.query("simpleResult.xml");
        LaserNight testNight = laserNightService.createAndPopulateTestLaserNight(DateTime.now(), queryResult, new QueryResult());
        assertTrue(testNight.isTestNight());
        assertTrue(testNight.getLaserTargets().size() > 0);
        for (LaserTarget t : testNight.getRaDecLaserTargets()) {
            assertTrue(t.getPropagationWindows().size() > 0);
            assertTrue(t.getShutteringWindows().size() > 0);
        }
    }

    @Test
    public void doesHandlePamFilesCorrectly() throws Exception {
        QueryResult queryResult = odbBrowser.query("simpleResult.xml");
        DateTime today = new DateTime(2013, 7, 23, 5 , 9, 12, DateTimeZone.UTC);
        LaserNight night = laserNightService.createAndPopulateLaserNight(today, queryResult, new QueryResult());

        // ------ STEP ONE: Import PAM created at 2013 Jul 22 16:52:04 (Report time stamp)
        Response responseOlder = Response.parseResponse(getClass().getResourceAsStream("/simpleResultRaDecPAM_1.txt"));
        laserNightService.addAndReplacePropagationWindows(night, responseOlder);
        // reload night and do some checks
        LaserNight result = laserNightService.loadLaserNight(night.getId());
        for (LaserTarget target : result.getRaDecLaserTargets()) {
            // check that the timestamp is set properly
            assertEquals(responseOlder.getReportTime(), target.getWindowsTimestamp());
            // check that all windows from PAM files have been added (7 per target)
            assertEquals(7, target.getPropagationWindows().size());
        }

        // ------ STEP TWO: Import PAM created at 2013 Jul 22 17:52:04, i.e. 1 hr later than first one
        Response responseNewer = Response.parseResponse(getClass().getResourceAsStream("/simpleResultRaDecPAM_2.txt"));
        laserNightService.addAndReplacePropagationWindows(night, responseNewer);
        // reload night and do some checks, we expect old data has been updated with new data
        LaserNight result1 = laserNightService.loadLaserNight(night.getId());
        for (LaserTarget target : result1.getRaDecLaserTargets()) {
            // check that the timestamp is set properly
            assertEquals(responseNewer.getReportTime(), target.getWindowsTimestamp());
            // check that all windows from PAM files have been replaced (new: 6 per target)
            assertEquals(6, target.getPropagationWindows().size());
        }

        // ------ STEP THREE: Import PAM created at 2013 Jul 22 16:52:04, i.e. BEFORE last imported data
        laserNightService.addAndReplacePropagationWindows(night, responseOlder);
        // reload night and do some checks, we expect that data has not been changed, older data should be ignored!
        LaserNight result2 = laserNightService.loadLaserNight(night.getId());
        for (LaserTarget target : result2.getRaDecLaserTargets()) {
            // check that the timestamp is set properly
            assertEquals(responseNewer.getReportTime(), target.getWindowsTimestamp());
            // check that all windows from PAM are the same (still 6 per target)
            assertEquals(6, target.getPropagationWindows().size());
        }
    }

    @Test
    public void doesCreateSunRiseSetCalculatorProperly() {
        checkSunRiseSetCalculator(Site.NORTH);
        checkSunRiseSetCalculator(Site.SOUTH);
    }

    private void checkSunRiseSetCalculator(Site site) {

        // Make sure that the SunRiseSet calculator object is created properly. It must be fed with the
        // UTC time of 12 noon local time of the date it has to do the calculations for.

        DateTime first = new DateTime(2008,  1,  1, 0, 0, 0, site.getTimeZone());
        DateTime last  = new DateTime(2014, 12, 31, 0, 0, 0, site.getTimeZone());

        DateTime date = first;
        while (date.isBefore(last)) {

            // check that we create a sun calculator object that is properly set up for the given site and time
            SunRiseSet calc = ModelFactory.createSunCalculator(site, date);
            DateTime nextDate = date.plusDays(1).withTimeAtStartOfDay();

            DateTime sunsetDate  = new DateTime(calc.getSunset()).withZone(site.getTimeZone()).withTimeAtStartOfDay();
            DateTime sunriseDate = new DateTime(calc.getSunrise()).withZone(site.getTimeZone()).withTimeAtStartOfDay();
            assertEquals(date,     sunsetDate);  // sunset must be on same day (local time)
            assertEquals(nextDate, sunriseDate); // sunrise must be on next day (local time)

            date = nextDate;
        }


    }

}
