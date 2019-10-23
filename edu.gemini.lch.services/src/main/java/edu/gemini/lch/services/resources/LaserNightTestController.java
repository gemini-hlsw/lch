package edu.gemini.lch.services.resources;

import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.model.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Test code.
 */
@Controller
@RequestMapping(value="/test/nights")
public class LaserNightTestController {

    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZone.UTC);

    @Resource
    private LaserNightService laserNightService;

    @RequestMapping(value="", method= RequestMethod.GET, produces="application/xml")
    @ResponseBody
    public NightFull getTestNight(@RequestParam(value = "date") String dateString, @RequestParam Boolean full, @RequestParam Double laserLimit) {
        DateTime date = formatter.parseDateTime(dateString).withTimeAtStartOfDay();

        Map<Long, edu.gemini.lch.services.model.LaserTarget> laserTargets = new HashMap<>();
        laserTargets.put(10L, new edu.gemini.lch.services.model.LaserTarget(10L, new Coordinates(Coordinates.RADEC,0.,0.)));
        laserTargets.put(13L, new edu.gemini.lch.services.model.LaserTarget(13L, new Coordinates(Coordinates.RADEC,0.,0.)));
        laserTargets.put(1121L, new edu.gemini.lch.services.model.LaserTarget(1121L, new Coordinates(Coordinates.RADEC,0.,0.)));
        laserTargets.put(1122L, new edu.gemini.lch.services.model.LaserTarget(1122L, new Coordinates(Coordinates.RADEC,0.,0.)));

        laserTargets.get(10L).setVisibility(createVisibility(date.plusHours(1), date.plusHours(10)));
        laserTargets.get(13L).setVisibility(createVisibility(date.plusHours(2), date.plusHours(9)));
        laserTargets.get(1121L).setVisibility(createVisibility2(date.plusHours(1), date.plusHours(4), date.plusHours(5), date.plusHours(7)));
        laserTargets.get(1122L).setVisibility(createVisibility2(date.plusHours(1), date.plusHours(4), date.plusHours(5), date.plusHours(7)));

        // GS-2012B-Q-10-56
        List<ClearanceWindow> clearanceWindows = new ArrayList<>();
        for (int i = -10; i < 10; i++) {
            ClearanceWindow w = new ClearanceWindow(date.plusHours(i).plusSeconds(3).toDate(),
                    date.plusHours(i+1).toDate());
            clearanceWindows.add(w);
        }
        List<ShutteringWindow> shutteringWindows = new ArrayList<>();
        for (int i = -10; i < 10; i++) {
            ShutteringWindow w = new ShutteringWindow(date.plusHours(i).toDate(), date.plusHours(i).plusSeconds(3).toDate());
            shutteringWindows.add(w);
        }
        edu.gemini.lch.services.model.LaserTarget laserTarget10 = laserTargets.get(10L);
        laserTarget10.getClearanceWindows().addAll(clearanceWindows);
        laserTarget10.getShutteringWindows().addAll(shutteringWindows);
        List<edu.gemini.lch.services.model.Observation> observations = new ArrayList<>();

        List<edu.gemini.lch.services.model.ObservationTarget> targets = new ArrayList<>();
        targets.add(new ObservationTarget("target10", "Base", laserTarget10));
        observations.add(new edu.gemini.lch.services.model.Observation("GS-2012B-Q-10-56", targets));

        // GS-2012B-Q-13-86
        clearanceWindows = new ArrayList<>();
        for (int i = -10; i < 10; i++) {
            ClearanceWindow w = new ClearanceWindow(date.plusHours(i).plusMinutes(30).plusSeconds(60).toDate(),
                    date.plusHours(i+1).plusMinutes(30).toDate());
            clearanceWindows.add(w);
        }
        shutteringWindows = new ArrayList<>();
        for (int i = -10; i < 10; i++) {
            ShutteringWindow w = new ShutteringWindow(date.plusHours(i).plusMinutes(30).toDate(),
                    date.plusHours(i).plusMinutes(30).plusSeconds(60).toDate());
            shutteringWindows.add(w);
        }
        edu.gemini.lch.services.model.LaserTarget laserTarget13 = laserTargets.get(13L);
        laserTarget13.getClearanceWindows().addAll(clearanceWindows);
        laserTarget13.getShutteringWindows().addAll(shutteringWindows);

        targets = new ArrayList<>();
        targets.add(new ObservationTarget("target13", "Base", laserTarget13));
        observations.add(new edu.gemini.lch.services.model.Observation("GS-2012B-Q-13-86",targets));


        // GS-2012B-Q-112-1
        clearanceWindows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ClearanceWindow w = new ClearanceWindow(date.plusHours(i).plusMinutes(13).plusSeconds(500).toDate(),
                    date.plusHours(i+1).plusMinutes(13).toDate());
            clearanceWindows.add(w);
        }
        shutteringWindows = new ArrayList<>();
        for (int i = -10; i < 10; i++) {
            ShutteringWindow w = new ShutteringWindow(date.plusHours(i).plusMinutes(13).toDate(),
                    date.plusHours(i).plusMinutes(13).plusSeconds(500).toDate());
            shutteringWindows.add(w);
        }
        edu.gemini.lch.services.model.LaserTarget laserTarget1121 = laserTargets.get(1121L);
        laserTarget1121.getClearanceWindows().addAll(clearanceWindows);
        laserTarget1121.getShutteringWindows().addAll(shutteringWindows);

        clearanceWindows = new ArrayList<>();
        for (int i = -6; i < 6; i++) {
            ClearanceWindow w = new ClearanceWindow(date.plusHours(i).plusMinutes(25).plusSeconds(300).toDate(),
                    date.plusHours(i+1).plusMinutes(25).toDate());
            clearanceWindows.add(w);
        }
        shutteringWindows = new ArrayList<>();
        for (int i = -6; i < 6; i++) {
            ShutteringWindow w = new ShutteringWindow(date.plusHours(i).plusMinutes(25).toDate(),
                    date.plusHours(i).plusMinutes(25).plusSeconds(300).toDate());
            shutteringWindows.add(w);
        }
        edu.gemini.lch.services.model.LaserTarget laserTarget1122 = laserTargets.get(1122L);
        laserTarget1122.getClearanceWindows().addAll(clearanceWindows);
        laserTarget1122.getShutteringWindows().addAll(shutteringWindows);

        targets = new ArrayList<>();
        targets.add(new ObservationTarget("target1121", "Base", laserTarget1121));
        targets.add(new ObservationTarget("target1122", "CWFS1", laserTarget1122));
        targets.add(new ObservationTarget("target1123", "CWFS2", laserTarget1122));
        targets.add(new ObservationTarget("target1124", "CWFS3", laserTarget1122));
        Observation observation = new edu.gemini.lch.services.model.Observation("GS-2012B-Q-112-1", targets);
        observations.add(observation);

        NightFull night = new NightFull();
        night.getObservations().addAll(observations);
        night.getLaserTargets().addAll(new ArrayList<>(laserTargets.values()));
        return night;
    }

    private Visibility createVisibility(DateTime start, DateTime end) {
        final Visibility.Interval i = new Visibility.Interval(start.toDate(), end.toDate());
        List<Visibility.Interval> aboveHorizon = new ArrayList<Visibility.Interval>() {{ add(i); }};
        List<Visibility.Interval> aboveLaLimit = new ArrayList<Visibility.Interval>() {{ add(i); }};
        return new Visibility(aboveHorizon, aboveLaLimit);
    }

    private Visibility createVisibility2(DateTime start1, DateTime end1, DateTime start2, DateTime end2) {
        final Visibility.Interval i1 = new Visibility.Interval(start1.toDate(), end1.toDate());
        final Visibility.Interval i2 = new Visibility.Interval(start2.toDate(), end2.toDate());
        List<Visibility.Interval> aboveHorizon = new ArrayList<Visibility.Interval>() {{ add(i1); add(i2);}};
        List<Visibility.Interval> aboveLaLimit = new ArrayList<Visibility.Interval>() {{ add(i1); add(i2);}};
        return new Visibility(aboveHorizon, aboveLaLimit);
    }
}
