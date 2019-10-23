package edu.gemini.lch.services.internal.collector;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.ObservationTarget;
import edu.gemini.lch.model.RaDecLaserTarget;
import edu.gemini.lch.model.Visibility;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class to group targets according to their distances from each other.
 * For each group of targets which are not further away from each other than the maximal distance a new
 * center point is created which is the average of the coordinates of all targets in the groups (it is ok
 * to assume planar geometry for targets which are very close to each other).
 */
public class LaserTargetsCollector {

    private final LaserNight night;
    private final Set<Group> groups;
    private final Double maxDistance;

    public LaserTargetsCollector(LaserNight night, Double maxDistance) {
        this.night = night;
        this.groups = new HashSet<>();
        this.maxDistance = maxDistance;
    }

    /**
     * Adds an observation target and sets its laser target either to an already existing nearby one
     * or creates a new one and uses it for this observation target.
     * @param obsTarget
     * @param visibility
     */
    public void addObservationTarget(ObservationTarget obsTarget, Visibility visibility) {
        Group group = getClosestCoveringGroup(obsTarget);
        if (group != null) {
            group.add(obsTarget);
        } else {
            Group newGroup = new Group(night, obsTarget, visibility);
            groups.add(newGroup);
        }
    }

    Set<Group> getGroups() {
        return groups;
    }

    private Group getClosestCoveringGroup(ObservationTarget t) {
        Double closestDistance = Double.MAX_VALUE;
        Group closestGroup = null;
        for (Group group : groups) {
            Double distance = group.center.distanceTo(t.getDegrees1(), t.getDegrees2());
            if (distance < maxDistance && distance < closestDistance) {
                closestDistance = distance;
                closestGroup = group;
            }
        }
        return closestGroup;
    }

    static class Group {
        private final LaserNight night;
        private final Set<ObservationTarget> targets;
        private RaDecLaserTarget center;

        /**
         * Creates a new "nearby-group" with a single observation covered by a target that's equal
         * to the observations position. In theory we would have to recalculate the visibility for the new
         * center point but it is probably safe to assume that the difference for nearby targets is so small
         * that we can neglect that.
         */
        Group(LaserNight night, ObservationTarget target, Visibility visibility) {
            this.night = night;
            this.targets = new HashSet<>();
            this.center = new RaDecLaserTarget(night, target.getDegrees1(), target.getDegrees2(), visibility);
            this.targets.add(target);
            target.setLaserTarget(this.center);
        }

        RaDecLaserTarget getCenter() {
            return center;
        }

        Set<ObservationTarget> getObservationTargets() {
            return targets;
        }

        /**
         * Adds a new observation to this group and re-centers the group so that the new center equals the
         * "average" of all covered observations.
         */
        void add(ObservationTarget observationTarget) {
            // calculate new center as average of all positions
            Double raSum = 0.0;
            Double decSum = 0.0;
            targets.add(observationTarget);
            for (ObservationTarget ot : targets) {
                raSum += ot.getDegrees1();
                decSum += ot.getDegrees2();
            }
            Double centerRa = raSum / targets.size();
            Double centerDec = decSum / targets.size();

            // create new center and re-assign all already existing observation targets
            RaDecLaserTarget newCenter = new RaDecLaserTarget(night, centerRa, centerDec, center.getVisibility());
            for (ObservationTarget ot : targets) {
                ot.setLaserTarget(newCenter);
            }

            center = newCenter;
        }
    }
}
