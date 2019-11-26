package edu.gemini.lch.services.impl;

import edu.gemini.lch.model.Site;
import edu.gemini.lch.model.Visibility;
import edu.gemini.lch.services.ModelFactory;
import edu.gemini.lch.services.VisibilityCalculator;
import jsky.coords.WorldCoords;
import jsky.plot.util.SkyCalc;
import org.apache.commons.lang.Validate;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

/**
 */
public class VisibilityCalculatorImpl implements VisibilityCalculator {

    private static final Double horizon = .0;
    private final Double minAltitude;
    private final ZonedDateTime earliest;
    private final ZonedDateTime latest;
    private final SkyCalc skyCalc;

    public VisibilityCalculatorImpl(Site site, ZonedDateTime earliest, ZonedDateTime latest, Double minAltitude) {
        Validate.isTrue(!earliest.isAfter(latest), "earliest time must be before latest");
        this.minAltitude = minAltitude;
        this.earliest = earliest;
        this.latest = latest;
        this.skyCalc = ModelFactory.createSkyCalculator(site);
    }

    @Override
    public Double getHorizon() {
        return horizon;
    }

    @Override
    public Double getLaserLimit() {
        return minAltitude;
    }

    @Override
    public Visibility calculateVisibility(WorldCoords obj) {
        // TODO-JODA: Uses JSKY, so we must stick with Date.
        skyCalc.calculate(obj, Date.from(earliest.toInstant()));
        double altitudeAtStartOfNight = skyCalc.getAltitude();

        ZonedDateTime risesAboveHorizon = null;
        ZonedDateTime setsBelowHorizon = null;

        ZonedDateTime risesAboveLimit = null;
        ZonedDateTime setsBelowLimit = null;

        // ==
        // Find the places where the function of the altitude crosses the horizon and the minAltitude value.
        // The assumption is that our objects only set and rise at most once during the course of a night
        // (this should be valid because Gemini is not able to follow fast objects that set and rise
        // multiple times during one single night).
        final int STEP_WIDTH = 60; // define step width in seconds

        ZonedDateTime now = earliest;
        double lastAlt = altitudeAtStartOfNight;

        while (now.isBefore(latest)) {
            skyCalc.calculate(obj, Date.from(now.toInstant()));
            double nowAlt = skyCalc.getAltitude();

            // check for horizon
            if (lastAlt >= horizon && nowAlt < horizon) {
                Validate.isTrue(setsBelowHorizon == null, "object can not set below horizon twice during a night");
                setsBelowHorizon = now.minusSeconds(STEP_WIDTH/2);
            }
            if (lastAlt < horizon && nowAlt >= horizon) {
                Validate.isTrue(risesAboveHorizon == null, "object can not rise above horizon twice during a night");
                risesAboveHorizon = now.minusSeconds(STEP_WIDTH/2);
            }

            // check for limit
            if (lastAlt >= minAltitude && nowAlt < minAltitude) {
                Validate.isTrue(setsBelowLimit == null, "object can not set below limit twice during a night");
                setsBelowLimit = now.minusSeconds(STEP_WIDTH/2);
            }
            if (lastAlt < minAltitude && nowAlt >= minAltitude) {
                Validate.isTrue(risesAboveLimit == null, "object can not rise above limit twice during a night");
                risesAboveLimit = now.minusSeconds(STEP_WIDTH/2);
            }

            lastAlt = nowAlt;
            now = now.plusSeconds(STEP_WIDTH);
        }
        // == end of rise / set time approximation

        final Optional<Visibility.RiseSet> aboveHorizon = getRiseSet(risesAboveHorizon, setsBelowHorizon, altitudeAtStartOfNight > horizon);
        final Optional<Visibility.RiseSet> aboveLimit   = getRiseSet(risesAboveLimit, setsBelowLimit, altitudeAtStartOfNight > minAltitude);
        return new Visibility(aboveHorizon, aboveLimit);

    }

    private Optional<Visibility.RiseSet> getRiseSet(final ZonedDateTime risesAboveHorizon, final ZonedDateTime setsBelowHorizon, boolean visibleAtStartOfNight) {
        // 1) object does not cross min altitude once, so it is either above or below all the time
        if (risesAboveHorizon == null && setsBelowHorizon == null) {
            if (visibleAtStartOfNight) {
                // above minAltitude all the time : visible all the time
                return Optional.of(new Visibility.RiseSet(earliest.toInstant(), latest.toInstant()));
            } else {
                // below minAltitude all the time : never visible
                return Optional.empty();
            }

            // 2) altitude function of object crosses minAltitude once in downward direction (sets below minAltitude)
        } else if (setsBelowHorizon != null && risesAboveHorizon == null) {
            // object is visible at start of night and then sets
            return Optional.of(new Visibility.RiseSet(earliest.toInstant(), setsBelowHorizon.toInstant()));

            // 3) altitude function of object crosses minAltitude once in upward direction (rises above minAltitude)
        } else if (risesAboveHorizon != null && setsBelowHorizon == null) {
            // object rises during the night and is visible until the end of the night
            return Optional.of(new Visibility.RiseSet(risesAboveHorizon.toInstant(), latest.toInstant()));

            // 4) altitude function crosses minAltitude in both directions
        } else {
            return Optional.of(new Visibility.RiseSet(risesAboveHorizon.toInstant(), setsBelowHorizon.toInstant()));
        }
    }



}
