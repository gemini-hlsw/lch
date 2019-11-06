package edu.gemini.lch.services;

import edu.gemini.lch.data.fixture.DatabaseFixture;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.Observation;
import edu.gemini.lch.model.ObservationTarget;
import edu.gemini.odb.browser.OdbBrowser;
import edu.gemini.odb.browser.QueryResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static edu.gemini.lch.model.ObservationTarget.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class LaserNightUpdaterTest extends DatabaseFixture {

    @Resource
    private LaserNightService laserNightService;
    @Resource
    private OdbBrowser odbBrowser;

    @Test
    public void canReplace() throws Exception {
        QueryResult queryResult = odbBrowser.query("simpleResult.xml");
        ZonedDateTime now = ZonedDateTime.now().plusDays(1);
        ZonedDateTime day = ZonedDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 12, 0, 0, 0, ZoneId.of("UTC"));
        LaserNight newNight = laserNightService.createAndPopulateLaserNight(day, queryResult, new QueryResult());

        // no prm sent yet, all targets should simply be replaced
        LaserNight updatedNight = laserNightService.updateLaserNight(newNight, queryResult, new QueryResult());
        assertTrue(updatedNight.getObservations().size() > 0);
        assertTrue(updatedNight.getLaserTargets().size() > 0);
    }

    @Test
    public void canUpdate() throws Exception {
        LaserNight night = createLaserNightForUpdate("updateStart.xml");
        QueryResult queryResult = odbBrowser.query("updateStart.xml");
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        // since we update with the original data all observations should have state "OK"
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                assertEquals(State.OK, t.getState());
            }
        }
    }

    @Test
    public void canUpdateWithChanged() {
        LaserNight night = createLaserNightForUpdate("updateStart.xml");
        QueryResult queryResult = odbBrowser.query("updateWithChanged.xml");
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        int added = 0;
        int removed = 0;
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                assertTrue(t.getState() == State.OK || t.getState() == State.ADDED || t.getState() == State.REMOVED);
                if (t.getState() == State.ADDED) added++;
                if (t.getState() == State.REMOVED) removed++;
            }
        }
        assertEquals(1, added);
        assertEquals(1, removed);
    }

    @Test
    public void canUpdateWithAdded() {
        LaserNight night = createLaserNightForUpdate("updateStart.xml");
        QueryResult queryResult = odbBrowser.query("updateWithAdded.xml");
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        int added = 0;
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                assertTrue(t.getState() == State.OK || t.getState() == State.ADDED);
                if (t.getState() == State.ADDED) added++;
            }
        }
        assertEquals(1, added);
    }

    @Test
    public void canUpdateWithAddedTwice() {
        LaserNight night = createLaserNightForUpdate("updateStart.xml");
        QueryResult queryResult = odbBrowser.query("updateWithAdded.xml");

        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        int added = 0;
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                assertTrue(t.getState() == State.OK || t.getState() == State.ADDED);
                if (t.getState() == State.ADDED) added++;
            }
        }
        assertEquals(1, added);
    }

    @Test
    public void canUpdateWithRemoved() {
        LaserNight night = createLaserNightForUpdate("updateStart.xml");
        QueryResult queryResult = odbBrowser.query("updateWithRemoved.xml");
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        int removed = 0;
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                assertTrue(t.getState() == State.OK || t.getState() == State.REMOVED);
                if (t.getState() == State.REMOVED) removed++;
            }
        }
        assertEquals(2, removed);
    }

    @Test
    public void canUpdateWithRemovedTwice() {
        LaserNight night = createLaserNightForUpdate("updateStart.xml");
        QueryResult queryResult = odbBrowser.query("updateWithRemoved.xml");

        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        int removed = 0;
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                assertTrue(t.getState() == State.OK || t.getState() == State.REMOVED);
                if (t.getState() == State.REMOVED) removed++;
            }
        }
        assertEquals(2, removed);
    }

    @Test
    public void canUpdateFullSemesterWithoutChanges() {
        LaserNight night = createLaserNightForUpdate("queryResult2012B.xml");
        QueryResult queryResult = odbBrowser.query("queryResult2012B.xml");

        LaserNight updated = laserNightService.loadLaserNight(night.getId());
        laserNightService.updateLaserNight(updated, queryResult, new QueryResult());

        assertEquals(night.getObservations().size(), updated.getObservations().size());
        assertEquals(night.getLaserTargets().size(), updated.getLaserTargets().size());
        assertEquals(0, updated.getUncoveredObservations().size());
        for (Observation o : updated.getObservations()) {
            assertEquals(o.getTargets().size(), countStates(o, State.OK));
        }

    }

    @Test
    public void canUpdateFullSemesterWithChanges() {
        LaserNight night = createLaserNightForUpdate("queryResult2012B.xml");
        QueryResult queryResult = odbBrowser.query("queryResult2012BWithChanges.xml");

        LaserNight updated = laserNightService.loadLaserNight(night.getId());
        laserNightService.updateLaserNight(updated, queryResult, new QueryResult());

        // Expected changes:
        // 1) Q-68-110: fully deleted, expect two targets marked as REMOVED
        // 2) Q-68-111: Altair AOWFS has been edited, change is small enough that existing laser target is still valid
        // 3) Q-5555-444: New program/observation which introduces a new laser target

        assertEquals(night.getObservations().size() + 1, updated.getObservations().size());
        assertEquals(night.getLaserTargets().size() + 1, updated.getLaserTargets().size());

        // -- check removal of Q-68-110
        Observation Q_68_110 = findObservation(updated, "GN-2012B-Q-68-110");
        assertEquals(2, Q_68_110.getTargets().size());
        assertEquals(2, countStates(Q_68_110, State.REMOVED));
        assertEquals(2, countTransmitted(Q_68_110));            // make sure laser target is not touched

        // -- check changes in GN-2012B-Q-68-111
        // Note: change translates to original target being removed and new one added, there is one more target now!
        Observation Q_68_111 = findObservation(updated, "GN-2012B-Q-68-111");
        assertEquals(3, Q_68_111.getTargets().size());
        assertEquals(1, countStates(Q_68_111, State.OK));
        assertEquals(1, countStates(Q_68_111, State.ADDED));
        assertEquals(1, countStates(Q_68_111, State.REMOVED));
        assertEquals(3, countTransmitted(Q_68_111));            // make sure laser target is reused!

        // -- check changes in Q-5555-444 (new program and new laser target)
        Observation Q_5555_444 = findObservation(updated, "GN-2012B-Q-5555-444");
        assertEquals(3, Q_5555_444.getTargets().size());
        assertEquals(3, countStates(Q_5555_444, State.ADDED));
        assertEquals(0, countTransmitted(Q_5555_444));           // make sure there is a new laser target
        assertEquals(1, updated.getUncoveredObservations().size());
        assertEquals(1, updated.getUntransmittedLaserTargets().size());

    }

    @Test
    public void canUpdateFullSemesterWithChangesTwice() {
        LaserNight night = createLaserNightForUpdate("queryResult2012B.xml");
        QueryResult queryResult = odbBrowser.query("queryResult2012BWithChanges.xml");
        LaserNight updated = laserNightService.loadLaserNight(night.getId());
        laserNightService.updateLaserNight(updated, queryResult, new QueryResult());
        laserNightService.updateLaserNight(updated, queryResult, new QueryResult());
    }

    @Test
    public void doesUpdateStatesAndTargetsAfterResendingPRMs() {
        // setting up the test: create a night and update it
        Long nightId = createLaserNightForUpdate("queryResult2012B.xml").getId();
        LaserNight night = laserNightService.loadLaserNight(nightId);
        QueryResult queryResult = odbBrowser.query("queryResult2012BWithChanges.xml");
        laserNightService.updateLaserNight(night, queryResult, new QueryResult());

        // this is what we expect after an update with the data provided.
        LaserNight updated1 = laserNightService.loadLaserNight(nightId);    // reload night to see actual changes in DB
        assertTrue(countStates(updated1, State.ADDED) > 0);
        assertTrue(updated1.getUntransmittedLaserTargets().size() > 0);

        // now resend prms for the updated night
        laserNightService.sendPrm(updated1);

        // this is what we expect after PRMs have been resent: no unsent targets, no "ADDED" observations.
        // Note that we can still have "REMOVED" ones, but those are not relevant. The ADDED ones however will be
        // removed before each update in order to detect additional observations properly. Removing observations
        // with targets that have already been sent can result in loosing propagation windows that have been
        // received from LCH (see bug LCH-311).
        LaserNight updated2 = laserNightService.loadLaserNight(nightId);    // reload night to see actual changes in DB
        assertTrue(countStates(updated2, State.ADDED) == 0);                // NO added!
        assertTrue(updated2.getUntransmittedLaserTargets().size() == 0);    // NO unsent targets!

    }

    // --- some trivial helpers for analysing the data

    private Observation findObservation(LaserNight night, String observationId) {
        for (Observation observation : night.getObservations()) {
            if (observation.getObservationId().equals(observationId)) {
                return observation;
            }
        }
        throw new RuntimeException("observation " + observationId + " not found");
    }

    private int countStates(LaserNight night, State s) {
        int count = 0;
        for (Observation o : night.getObservations()) {
            count += countStates(o, s);
        }
        return count;
    }

    private int countStates(Observation o, State s) {
        Integer count = 0;
        for (ObservationTarget t : o.getTargets()) {
            if (t.getState() == s) count++;
        }
        return count;
    }

    private int countTransmitted(Observation o) {
        Integer count = 0;
        for (ObservationTarget t : o.getTargets()) {
            if (t.getLaserTarget().isTransmitted()) {
                count++;
            }
        }
        return count;
    }

    private LaserNight createLaserNightForUpdate(String file) {
        // DO NOT CHANGE THE DATE!
        // Changing the date will impact the test by changing the visibility of targets etc.!
        ZonedDateTime day = ZonedDateTime.of(2012, 11, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

        QueryResult queryResult = odbBrowser.query(file);
        LaserNight night = laserNightService.createAndPopulateLaserNight(day, queryResult, new QueryResult());
        night = laserNightService.updateLaserNight(night, queryResult, new QueryResult());
        laserNightService.sendPrm(night); // send PRMs in order to force update instead of just replacing the data!
        return night;
    }
}
