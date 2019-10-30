package edu.gemini.lch.model;

import javax.persistence.*;
import java.util.*;


/**
 * Named queries for observations.
 */
@NamedQueries({
        @NamedQuery(name = "observation.findForLaserTarget",
                query = "from Observation o " +
                        "where o.id in " +
                        "(select distinct ot.observationId from ObservationTarget as ot where ot.targetId = :targetId)"
        )
})

@Entity
@Table(name = "lch_observations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "observation_id")
    private Set<ObservationTarget> targets;

    /* make observation id accessible in HSQL named queries */
    @Column(name = "observation_id")
    private String observationId;

    protected Observation(String observationId) {
        this.targets = new HashSet<>();
        this.observationId = observationId;
    }

    /**
     * Gets the unique database id for this object.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the observation id for this observation, e.g. GN-2012B-Q-23-34.
     */
    public String getObservationId() {
        return observationId;
    }

    /**
     * Returns the list of targets for this observation.
     * Add or remove targets from this collection to change the actual data in the database.
     */
    public Set<ObservationTarget> getTargets() {
        return targets;
    }

    public ObservationTarget getScienceTarget() {
        return getTargets()
                .stream()
                .filter(ObservationTarget::isScience)
                .findAny()
                .orElseThrow(() -> new RuntimeException("observation must have a science target"));
    }

    /**
     * Returns an unmodifiable list of the targets sorted by their type: first the base (science), then the
     * guide stars and user types in alphabetical order, similar types are sorted by their database id to
     * guarantee same ordering every time the data is displayed.
     */
    public List<ObservationTarget> getTargetsSortedByType() {
        List<ObservationTarget> sortedTargets = new ArrayList<>(targets);
        sortedTargets.sort((t0, t1) -> {
            if (t0.getType().equals(t1.getType())) {
                if (t0.getId() == null || t1.getId() == null) {
                    // note: if objects haven't been persisted yet their ids will be null; in order
                    // to make tests run on un-persisted objects we have to take that into account
                    return t0.hashCode() - t1.hashCode();
                } else {
                    return t0.getId().compareTo(t1.getId());
                }
            } else if (t0.isScience()) {
                return -1;
            } else if (t1.isScience()) {
                return 1;
            } else {
                return t0.getType().compareTo(t1.getType());
            }
        });
        return Collections.unmodifiableList(sortedTargets);
    }

    /**
     * Returns an unmodifiable list of laser targets for this observation.
     * The laser targets are sorted by their types: base first, then guide stars, then user types.
     */
    public List<LaserTarget> getLaserTargetsSortedByType() {
        List<LaserTarget> laserTargets = new ArrayList<>();
        for (ObservationTarget t : getTargetsSortedByType()) {
            laserTargets.add(t.getLaserTarget());
        }
        return Collections.unmodifiableList(laserTargets);
    }

    // empty constructor needed for hibernate
    public Observation() {}

}
