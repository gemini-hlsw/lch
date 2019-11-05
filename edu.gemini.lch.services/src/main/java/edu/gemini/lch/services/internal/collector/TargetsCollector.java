package edu.gemini.lch.services.internal.collector;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.*;
import edu.gemini.lch.model.Observation;
import edu.gemini.lch.services.*;
import edu.gemini.lch.services.impl.VisibilityCalculatorImpl;
import edu.gemini.lch.services.util.StringLogger;
import edu.gemini.odb.browser.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.HorizonsDesignation;
import jsky.coords.WorldCoords;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * A class for digesting observations from a query result.
 */
@Component
@Scope("prototype")
public class TargetsCollector {

    private static final Logger LOGGER = Logger.getLogger(TargetsCollector.class.getName());

    @Resource
    private ConfigurationService configurationService;
    @Resource
    private HorizonsService horizonsService;
    @Resource
    private ConstraintsChecker constraintsChecker;

    private final LaserNight night;
    private final Set<Observation> observations;

    private Double maxDistance;
    private Duration minVisibilityDuration;
    private LaserTargetsCollector laserTargets;
    private VisibilityCalculator visibilityCalculator;
    private StringLogger messages;


    TargetsCollector(LaserNight night) {
        this.night = night;
        this.observations = new HashSet<>();
    }

    @PostConstruct
    private void init() {
        // We want to use the same configuration values throughout the use of this collector object,
        // therefore we read them once after object creation and store them.

        ZonedDateTime earliest = night.getStart();
        ZonedDateTime latest = night.getEnd();

        double altitude = configurationService.getDouble(Configuration.Value.VISIBILITY_MIN_ALTITUDE);
        this.maxDistance = configurationService.getDouble(Configuration.Value.NEARBY_GROUP_MAX_DISTANCE);
        this.visibilityCalculator = new VisibilityCalculatorImpl(night.getSite(), earliest, latest, altitude);
        this.minVisibilityDuration = Duration.ofMinutes(configurationService.getInteger(Configuration.Value.VISIBILITY_MIN_DURATION)*60*1000);

        // This helper class will keep track of nearby targets and create unique laser targets for them.
        // The laser targets are directly added to the observation targets that use them and will be stored
        // when storing the observation targets.
        this.laserTargets = new LaserTargetsCollector(night, maxDistance);

        // prepare a string logger to get info/error messages during the processing of all the targets
        // this is important for example if a non-sidereal target can not be found in the Horizons database
        this.messages = new StringLogger(TargetsCollector.class);
    }

    public void addScienceTargets(QueryResult queryResult) {
        if (queryResult.getProgramsNode() != null && queryResult.getProgramsNode().getPrograms() != null) {
            addTargets(queryResult.getProgramsNode().getPrograms(), false);
        }
    }

    public void addEngineeringTargets(QueryResult queryResult) {
        if (queryResult.getProgramsNode() != null && queryResult.getProgramsNode().getPrograms() != null) {
            addTargets(queryResult.getProgramsNode().getPrograms(), true);
        }
    }

    private void addTargets(Collection<Program> programs, boolean isEngineering) {
        for (Program p : programs) {
            if (p.getObservationsNode() == null || p.getObservationsNode().getObservations() == null) {
                continue;
            }

            for (edu.gemini.odb.browser.Observation o : p.getObservationsNode().getObservations()) {
                if (o.getTargetsNode() == null || o.getTargetsNode().getTargets() == null) {
                    continue;
                }

                if (!passesAllChecks(o)) {
                    continue;
                }

                final Observation observation;
                if (isEngineering) {
                    observation = new EngineeringObservation(o.getId());
                } else {
                    observation = new ScienceObservation(o.getId());
                }

                for (Object t : o.getTargetsNode().getTargets()) {
                    if (t instanceof Sidereal) {
                        WorldCoords c = new WorldCoords(((Sidereal)t).getHmsDms().getRa(), ((Sidereal)t).getHmsDms().getDec());
                        try {
                            Visibility visibility = visibilityCalculator.calculateVisibility(c);
                            if (visibility.isVisible() && visibility.getMaxDurationAboveLimit(night).isLongerThan(minVisibilityDuration)) {
                                if (isEngineering) {
                                    observation.getTargets().add(createEngineeringSidereal((Sidereal) t, c, visibility));
                                } else {
                                    observation.getTargets().add(createScienceSidereal((Sidereal) t, c, visibility));
                                }
                            } else {
                                LOGGER.debug("Sidereal target: skipped position " + c + " for target \"" + ((Sidereal) t).getName() + "\" because it is not visible at all or not visible long enough");
                            }
                        } catch (Exception e) {
                            // TODO: how to deal with targets that rise or set multiple times, this can happen when creating test data covering 24 hrs (will not happen for real data)
                            LOGGER.error(String.format("Could not add sidereal target at RA=%s Dec=%s (this should only happen when creating test data!)", c.getRA(), c.getDec()), e);
                        }
                    } else if (t instanceof NonSidereal) {
                        try {
                            observation.getTargets().addAll(createNonSidereal((NonSidereal) t));
                        } catch (Exception e) {
                            // TODO: how to deal with targets that rise or set multiple times, this can happen when creating test data covering 24hrs (will not happen for real data)
                            LOGGER.error(String.format("Could not add non sidereal target %s (this should only happen when creating test data!)", ((NonSidereal) t).getName()), e);
                        }
                    } else {
                        throw new RuntimeException("unknown target type" + t);
                    }
                }

                // only add this observation if the science target has not been filtered
                // (e.g. because it is not visible or for some other reason)
                if (hasScienceTarget(observation)) {
                    observations.add(observation);
                }
            }
        }
    }

    public void addTargets(List<EngTargetTemplate> engineeringTargets) {
        int i = 1;
        for (EngTargetTemplate e : engineeringTargets) {
            if (e.getActive()) {
                AzElLaserTarget engLaserTarget = new AzElLaserTarget(night, e.getAzimuth(), e.getAltitude());

                EngineeringTarget engTarget = new EngineeringTarget("Engineering", e.getAzimuth(), e.getAltitude());
                engTarget.setLaserTarget(engLaserTarget);

                Observation engineeringObs = new EngineeringObservation("Engineering-" + i++);
                engineeringObs.getTargets().add(engTarget);

                observations.add(engineeringObs);
            }
        }
    }

    public Set<Observation> getObservations() {
        return observations;
    }

    public String getMessages() {
        return messages.toString();
    }

    // -- HANDLE TARGETS

    public boolean hasScienceTarget(Observation o) {
        for (ObservationTarget target : o.getTargets()) {
            if (target.isScience()) {
                return true;
            }
        }
        return false;
    }


    // ================== SIDEREAL ============================================

    private ObservationTarget createScienceSidereal(Sidereal t, WorldCoords c, Visibility visibility) {
        SiderealTarget obsTarget = new SiderealTarget(t.getName(), t.getType(), c.getRaDeg(), c.getDecDeg());
        laserTargets.addObservationTarget(obsTarget, visibility);
        LOGGER.debug("Sidereal target: added position " + c + " for target \"" + t.getName() + "\"");
        return obsTarget;
    }



    // ================== ENGINEERING =========================================

    private ObservationTarget createEngineeringSidereal(Sidereal t, WorldCoords c, Visibility visibility) {
        EngineeringTarget obsTarget = new EngineeringTarget(t.getName(), c.getRaDeg(), c.getDecDeg());
        laserTargets.addObservationTarget(obsTarget, visibility);
        LOGGER.debug("Engineering target: added position " + c + " for target \"" + t.getName() + "\"");
        return obsTarget;
    }


    // ================== NON-SIDEREAL ========================================

    private Set<ObservationTarget> createNonSidereal(final NonSidereal t) {

        final Set<ObservationTarget> obsTargets = new HashSet<>();

        final Option<HorizonsDesignation> horizonsDesignation =
            ImOption.apply(t.getHorizonsObjectId())
                    .flatMap(h -> ImOption.fromScalaOpt(HorizonsDesignation.read(h)));

        final String queryString =
            horizonsDesignation.map(hd -> hd.queryString()).getOrElse("-1"); // in keeping with existing code

        final List<WorldCoords> coordinates =
            horizonsDesignation.map(hd -> horizonsService.getCoordinates(night, hd))
                               .getOrElse(Collections.emptyList());

        // check if horizons service returned something
        if (coordinates.size() == 0) {
            // could not find object in Horizons database (either not unique or did not exist)
            // create a place holder object at position (0,0) so that we can manually upload
            // positions if needed
            final WorldCoords coordinate = new WorldCoords(0, 0);
            final NonSiderealTarget target =
                    new NonSiderealTarget(
                            t.getName(),
                            t.getType(),
                            queryString,
                            coordinate.getRaDeg(),
                            coordinate.getDecDeg()
                    );
            laserTargets.addObservationTarget(target, Visibility.NEVER);
            obsTargets.add(target);
            LOGGER.info("Non-sidereal target: added place-holder position (0,0) for target \"" + t.getName() + "\" [" + queryString + "] which is unknown to Horizons.");
        } else {
            // we have at least one position for this object, go ahead and use the position(s) returned by Horizons
            for (final WorldCoords coordinate : coordinates) {
                final NonSiderealTarget target =
                        new NonSiderealTarget(
                                t.getName(),
                                t.getType(),
                                queryString,
                                coordinate.getRaDeg(),
                                coordinate.getDecDeg());
                final Visibility visibility = visibilityCalculator.calculateVisibility(coordinate);
                if (visibility.isVisible() && visibility.getMaxDurationAboveLimit(night).compareTo(minVisibilityDuration) > 0) {
                    laserTargets.addObservationTarget(target, visibility);
                    obsTargets.add(target);
                    LOGGER.debug("Non-sidereal target: Added position " + coordinate + " for target \"" + t.getName() + "\" [" + queryString + "]");
                } else {
                    LOGGER.debug("Non-sidereal target: Skipped position " + coordinate + " for target \"" + t.getName() + "\" [" + queryString + "] because it is not visible at all or not visible long enough.");
                }
            }
        }

        return obsTargets;
    }

    // ================================================================


    // -- FILTERING
    protected boolean passesAllChecks(edu.gemini.odb.browser.Observation o) {
        if (isTooTemplate(o)) {
            return false;
        }
        if (!constraintsChecker.passesTimeConstraints(o, night.getStart(), night.getEnd())) {
            return false;
        }

        return true;
    }

    /**
     * Checks for too templates which are defined as observations with a sidereal science target at (0.0,0.0).
     * These observations are mere placeholders, we don't need to create laser targets for them and can
     * safely ignore them.
     * @param o
     * @return
     */
    private boolean isTooTemplate(edu.gemini.odb.browser.Observation o) {
        for (Object target : o.getTargetsNode().getTargets()) {
            if (target instanceof Sidereal) {
                Sidereal s = (Sidereal) target;
                if (s.getType().equals("Base")) {
                    WorldCoords c = new WorldCoords(s.getHmsDms().getRa(), s.getHmsDms().getDec());
                    if (c.getDecDeg() == 0.0 && c.getRaDeg() == 0.0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
