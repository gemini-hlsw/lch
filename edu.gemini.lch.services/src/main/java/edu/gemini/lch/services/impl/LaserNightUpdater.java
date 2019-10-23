package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.*;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.HorizonsService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.EmailService;
import edu.gemini.lch.services.internal.collector.TargetsCollector;
import edu.gemini.odb.browser.QueryResult;
import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

import static edu.gemini.lch.model.ObservationTarget.*;

/**
 * The laser night updater takes care of updating all targets for the night and comparing the current laser targets
 * with the ones in the ODB and tracking the state of the different targets.
 */
@Component
@Scope("prototype")
public class LaserNightUpdater {

    @Resource
    private SessionFactory sessionFactory;
    @Resource
    private ConfigurationService configurationService;
    @Resource
    private LaserNightService laserNightService;
    @Resource
    private HorizonsService horizonsService;
    @Resource
    private EmailService emailService;
    @Resource
    private Factory factory;


    private Double maxDistance = null;

    public LaserNightUpdater() {
    }

    @PostConstruct
    private void init() {
        maxDistance = configurationService.getDouble(Configuration.Value.NEARBY_GROUP_MAX_DISTANCE);
    }

    public void replaceObservations(LaserNight night, QueryResult scienceTargets, QueryResult engineeringTargets) {
        night.getObservations().clear();
        Set<Observation> newObservations = getObservationsForNight(night, scienceTargets, engineeringTargets);
        night.getObservations().addAll(newObservations);
    }

    /**
     * Marks all laser targets of a night as sent.
     * @param night
     */
    public static void markAllLaserTargetsAsSent(LaserNight night) {
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                if (!t.getLaserTarget().isTransmitted()) {
                    t.getLaserTarget().setTransmitted(true);
                }
            }
        }
    }

    /**
     * Incorporates all added observation targets into the set of targets for this night by changing their state
     * from ADDED to OK. From this moment these observation targets and their laser targets are part of the happy
     * targets family of this night. Only targets with transmitted laser targets can be incorporated.
     * @param night
     */
    public static void incorporateAddedObservationTargets(LaserNight night) {
        for (Observation o : night.getObservations()) {
            for (ObservationTarget t : o.getTargets()) {
                if (t.getState() == State.ADDED) {
                    Validate.isTrue(t.getLaserTarget().isTransmitted());
                    t.setState(State.OK);
                }
            }
        }
    }

    public void updateObservations(LaserNight night, QueryResult scienceTargets, QueryResult engineeringTargets) {
        Set<Observation> newObservations = getObservationsForNight(night, scienceTargets, engineeringTargets);
        resetForComparison(night);
        compare(night.getObservations(), newObservations);

        // last step: recycle laser targets for added observation targets
        recycleLaserTargets(night.getLaserTargets(), night.getObservations());

    }

    private Set<Observation> getObservationsForNight(LaserNight night, QueryResult scienceTargets, QueryResult engineeringTargets) {
        // collect all targets form query results
        TargetsCollector collector = factory.createTargetsCollector(night);
        collector.addScienceTargets(scienceTargets);
        collector.addEngineeringTargets(engineeringTargets);

        // add enigneering targets from templates
        List<EngTargetTemplate> templates = laserNightService.getEngineeringTargetsForSite(night.getSite());
        collector.addTargets(templates);

        // check if there were problems we want to report by email (all log entries with level info or higher)
        // NOTE: in theory this email should also be configurable like other warnings, but I am taking a shortcut here
        // if ever the need arises for having this configurable do it the same way as it is done for other warnings
        if (!collector.getMessages().isEmpty()) {
            emailService.sendInternalEmail(
                "LTTS: WARNING: Problems during creation/update of night " + night.getStart().withZone(DateTimeZone.UTC).toString("yyyy-MM-dd (DDD) z"),
                "Problems occurred during the creation/updating process of night\n\n"+
                "  " + night.getStart().withZone(DateTimeZone.UTC).toString("yyyy-MM-dd (DDD) z")+ "\n" +
                "  " + night.getStart().toString("yyyy-MM-dd (DDD) z")+ "\n\n" +
                collector.getMessages());
        }

        // and return the whole shebang
        Set<Observation> observations = collector.getObservations();
        return observations;
    }

    private void resetForComparison(LaserNight night) {
        // reset means: bring everything back in state we had when PRM was sent
        // -> all we have to do is to delete all observations with state "ADDED"
        // by removing all not-incorporated targets from the last comparison run we can just rerun the comparison
        Set<Observation> observationsToRemove = new HashSet<>();
        for (Observation o : night.getObservations()) {
            Set<ObservationTarget> targetsToRemove = new HashSet<>();
            for (ObservationTarget t : o.getTargets()) {
                if (t.getState() == State.ADDED) {
                    // TODO: as a quick fix for LCH-242 don't remove laser target ...
                    // a laser target can be used by several observation targets therefore we must not
                    // delete it here! this will result in orphans, make sure orphans are deleted
                    t.setLaserTarget(null);
                    targetsToRemove.add(t);
                }
            }
            o.getTargets().removeAll(targetsToRemove);

            // also remove observation if all targets have been removed
            if (o.getTargets().isEmpty()) {
                observationsToRemove.add(o);
            }
        }

        night.getObservations().removeAll(observationsToRemove);
    }

    private void compare(Set<Observation> oldObservations, Set<Observation> newObservations) {
        Map<String, Observation> newTargetsMap = getObservationsMap(newObservations);

        Set<Observation> oldFound = new HashSet<>();
        Set<Observation> newFound = new HashSet<>();

        for (Observation oldObservation : oldObservations) {
            Observation newObservation = newTargetsMap.get(oldObservation.getObservationId());
            if (newObservation != null) {
                compareObservations(oldObservation, newObservation);
                oldFound.add(oldObservation);
                newFound.add(newObservation);
            }
        }

        // find observations in old without corresponding observation in new (removed)
        // and observations in new without corresponding observation in old (added)
        Set<Observation> removedSet = new HashSet<>(oldObservations);
        Set<Observation> addedSet   = new HashSet<>(newObservations);
        removedSet.removeAll(oldFound);
        addedSet.removeAll(newFound);
        // for entirely removed observations mark all targets as removed
        for (Observation removed : removedSet) {
            for (ObservationTarget t : removed.getTargets()) {
                t.setState(State.REMOVED);
            }
        }
        // for completely new observations mark all targets as added and add the new observations
        for (Observation added : addedSet) {
            for (ObservationTarget t : added.getTargets()) {
                t.setState(State.ADDED);
            }
        }
        oldObservations.addAll(addedSet); // add new observations
    }

    /**
     * Compare all observation targets from the night with the new ones from the ODB.
     * All observation targets of the night for which no target with the same coordinates can be found in the new list
     * will be marked as REMOVED, all targets in the new list for which no target with same coordinates can be found
     * in the list of existing targets will be added to the night and marked as ADDED.
     * @param oldObs
     * @param newObs
     */
    private void compareObservations(Observation oldObs, Observation newObs) {

        Set<ObservationTarget> oldFound = new HashSet<>();
        Set<ObservationTarget> newFound = new HashSet<>();

        for (ObservationTarget oldT : oldObs.getTargets()) {
            for (ObservationTarget newT : newObs.getTargets()) {

                if (haveSameType(oldT, newT) && haveSameCoordinates(oldT, newT)) {
                    oldT.setState(ObservationTarget.State.OK);
                    oldFound.add(oldT);
                    newFound.add(newT);
                }

            }
        }

        // find targets in old without correspondence in new (removed)
        // and new ones without corresponding elements in old (added)
        Set<ObservationTarget> removedSet = new HashSet<>(oldObs.getTargets());
        Set<ObservationTarget> addedSet   = new HashSet<>(newObs.getTargets());
        removedSet.removeAll(oldFound);
        addedSet.removeAll(newFound);
        // mark missing old ones as removed
        for (ObservationTarget removed : removedSet) {
            removed.setState(ObservationTarget.State.REMOVED);
        }
        // mark new ones as added and add them to the observation
        for (ObservationTarget added : addedSet) {
            added.setState(ObservationTarget.State.ADDED);
        }
        oldObs.getTargets().addAll(addedSet);
    }
    
    private boolean haveSameType(ObservationTarget t1, ObservationTarget t2) {
        return t1.getType().equals(t2.getType());
    }

    private boolean haveSameCoordinates(ObservationTarget t1, ObservationTarget t2) {
        // Be careful with error ranges: targets with coordinates that have been stored in the database are
        // rounded differently from the ones that are read from the ODB! Allow quite some error margin here..
        if (Math.abs(t1.getDegrees1() - t2.getDegrees1()) > 0.00001) {
            return false;
        }
        if (Math.abs(t1.getDegrees2() - t2.getDegrees2()) > 0.00001) {
            return false;
        }
        return true;
    }

    /**
     * Checks for all new observation targets if they can reuse one of the already existing laser targets.
     * @param laserTargets
     * @param observations
     */
    private void recycleLaserTargets(Set<LaserTarget> laserTargets, Set<Observation> observations) {
        for (Observation o : observations) {
            for (ObservationTarget t : o.getTargets()) {
                if (t.getState() == State.ADDED) {
                    LaserTarget recycled = findBestTransmittedLaserTarget(laserTargets, t);
                    if (recycled != null) {
                        // replace the laser target in this new observation target by one that has already been
                        // transmitted to LCH (in that case we don't have an additional laser target to think about)
                        t.setLaserTarget(recycled);
                    }
                }
            }
        }
    }

    private LaserTarget findBestTransmittedLaserTarget(Set<LaserTarget> laserTargets, ObservationTarget t) {
        Double minDistance = Double.MAX_VALUE;
        LaserTarget closest = null;
        for (LaserTarget lt : laserTargets) {
            // only look at already transmitted laser targets! if possible we want to use those
            if (lt.isTransmitted()) {
                Double distance = lt.distanceTo(t.getDegrees1(), t.getDegrees2());
                if (distance < maxDistance && distance < minDistance) {
                    closest = lt;
                }
            }
        }
        return closest;
    }

    private Map<String, Observation> getObservationsMap(Set<Observation> targets) {
        Map<String, Observation> map = new HashMap<>();
        for (Observation t : targets) {
            map.put(t.getObservationId(), t);
        }
        return map;
    }
}
