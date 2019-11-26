package edu.gemini.lch.services.impl;

import edu.gemini.lch.model.*;
import edu.gemini.lch.services.AlarmService;
import edu.gemini.lch.services.LaserNightService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.ZonedDateTime;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class AlarmServiceTest {

    @Resource
    private LaserNightService laserNightService;

    @Resource
    private AlarmService alarmService;

    @Test
    public void detectsNightChanges() {

        LaserNight night1 = laserNightService.createLaserNight(ZonedDateTime.now());
        LaserNight night2 = laserNightService.createLaserNight(ZonedDateTime.now());
        LaserTarget t1 = new RaDecLaserTarget(night1, (double) 0, (double) 0, Visibility.ALWAYS);
        LaserTarget t2 = new RaDecLaserTarget(night1, (double) 0, (double) 0, Visibility.ALWAYS);

        AlarmService.Snapshot s1 = createAlarmSnapshot(night1, t1);
        AlarmService.Snapshot s2 = createAlarmSnapshot(night2, t2);

        Assert.assertFalse(s1.targetHasChanged(s1));

        Assert.assertTrue(s1.targetHasChanged(null));
        Assert.assertTrue(s1.targetHasChanged(s2));

    }


    @Test
    public void detectsTargetChanges() {

        LaserNight night = laserNightService.createLaserNight(ZonedDateTime.now());
        LaserTarget t1 = new RaDecLaserTarget(night, (double) 0, (double) 0, Visibility.ALWAYS);
        LaserTarget t2 = new RaDecLaserTarget(night, (double) 0, (double) 0, Visibility.ALWAYS);

        AlarmService.Snapshot s1 = createAlarmSnapshot(night, t1);
        AlarmService.Snapshot s2 = createAlarmSnapshot(night, t1);
        AlarmService.Snapshot s3 = createAlarmSnapshot(night, t2);

        Assert.assertFalse(s1.targetHasChanged(s1));
        Assert.assertFalse(s1.targetHasChanged(s2));

        Assert.assertTrue(s1.targetHasChanged(null));
        Assert.assertTrue(s1.targetHasChanged(s3));
    }

    private AlarmServiceImpl.Snapshot createAlarmSnapshot(LaserNight night, LaserTarget target) {
        return new AlarmServiceImpl.Snapshot(
                night,                          // night
                target,                         // target
                null,                           // earliest propagation
                null,                           // latest propagation
                null,                           // observations
                null,                           // error cone
                null,                           // distance
                null,                           // ltcs snapshot
                null,                           // epics snapshot
                null,                           // auto shutter
                null,                           // safety before
                null                            // safety after
        );
    }
}
