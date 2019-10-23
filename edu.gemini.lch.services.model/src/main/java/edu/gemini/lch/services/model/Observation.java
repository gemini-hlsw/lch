package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * Transfer data object for observations.
 */
@XmlRootElement
@XmlType(propOrder = {"id", "targets"})
public class Observation {

    private String id;
    private List<ObservationTarget> targets;

    public Observation(String id, List<ObservationTarget> targets) {
        this.id = id;
        this.targets = targets;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the one and only science target for this observation.
     * Each observation must have a science target.
     * @return
     */
    @XmlTransient
    public ObservationTarget getScienceTarget() {
        for (ObservationTarget target : getTargets()) {
            if (target.isScience()) {
                return target;
            }
        }
        throw new RuntimeException("observations must have a science target (type=\"Base\")");
    }

    /**
     * Gets all non-science targets for this observation ordered by their types.
     * This list can be empty.
     * @return
     */
    @XmlTransient
    public List<ObservationTarget> getNonScienceTargets() {
        List<ObservationTarget> targets = new ArrayList<>();
        for (ObservationTarget target : getTargetsSortedByType()) {
            if (!target.isScience()) {
                targets.add(target);
            }
        }
        return Collections.unmodifiableList(targets);
    }

    /**
     * Gets all unique laser targets for this observation.
     * There must be at least one laser target.
     * @return
     */
    @XmlTransient
    public Set<LaserTarget> getLaserTargets() {
        Set<LaserTarget> targets = new HashSet<>();
        for (ObservationTarget target : getTargetsSortedByType()) {
            targets.add(target.getLaserTarget());
        }
        return Collections.unmodifiableSet(targets);
    }


    /**
     * Gets all targets for this observation in alphabetical order of their types.
     * This list can be empty.
     * @return
     */
    @XmlTransient
    public List<ObservationTarget> getTargetsSortedByType() {
        List<ObservationTarget> targets = new ArrayList<>(this.targets);
        Collections.sort(targets, (a, b) -> a.getType().compareTo(b.getType()));
        return Collections.unmodifiableList(targets);
    }

    @XmlElementWrapper(name = "targets")
    @XmlElement(name = "target")
    public List<ObservationTarget> getTargets() {
        if (targets == null) targets = new ArrayList<>();
        return targets;
    }

    // needed for JAXB
    public Observation() {
        this.id = "";
    }
}
