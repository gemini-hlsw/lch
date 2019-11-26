package edu.gemini.lch.model;

import javax.persistence.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

@NamedQueries({
        /*
            Named query to load a full laser night by its id.
            This query is also optimised to prefetch all children of all relations in order to speed
            up loading full nights for the GUI.
         */
    @NamedQuery(name = LaserNight.QUERY_LOAD_BY_ID,
        query = "from LaserNight night " +
                "left join fetch night.observations o " +
                "left join fetch o.targets ot " +
                "left join fetch ot.laserTarget lt " +
//                "left join fetch lt.windows w " +       // windows are eagerly fetched by the target
                "left join fetch night.events e " +
                "left join fetch e.files " +
                "left join fetch night.closures " +
                "where " +
                "night.id = :id "
    )
})

@Entity
@Table(name = "lch_laser_nights")
public class LaserNight extends BaseLaserNight {

    public static final String QUERY_LOAD_BY_ID = "laserNight.findById";

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "night_id")
    private Set<Observation> observations;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "night_id")
    @OrderBy("time DESC")
    private Set<LaserRunEvent> events;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "night_id")
    @OrderBy("starts")
    private Set<BlanketClosure> closures;

    private transient TimeZone timeZone;

    public LaserNight(Site site, ZonedDateTime start, ZonedDateTime end) {
        super(site, start, end);
        this.closures = new TreeSet<>();
        this.events = new TreeSet<>();
        this.observations = new HashSet<>();
        this.timeZone = null;
    }

    public Set<Observation> getObservations() {
        return observations;
    }

    public Set<BlanketClosure> getClosures() {
        return closures;
    }

    public List<ScienceObservation> getScienceObservations() {
        List<ScienceObservation> observations = new ArrayList<>();
        for (Observation o : this.observations) {
            if (o instanceof ScienceObservation) {
                observations.add((ScienceObservation) o);
            }
        }
        return Collections.unmodifiableList(observations);
    }

    public List<ScienceObservation> getScienceObservations(String string) {
        List<ScienceObservation> observations = new ArrayList<>();
        for (Observation o : this.observations) {
            if (o instanceof ScienceObservation && o.getObservationId().contains(string)) {
                observations.add((ScienceObservation) o);
            }
        }
        return Collections.unmodifiableList(observations);
    }

    public List<EngineeringObservation> getEngineeringObservations() {
        List<EngineeringObservation> observations = new ArrayList<>();
        for (Observation o : this.observations) {
            if (o instanceof EngineeringObservation) {
                observations.add((EngineeringObservation) o);
            }
        }
        return Collections.unmodifiableList(observations);
    }

    public Set<LaserRunEvent> getEvents() {
        return events;
    }

    public Set<LaserTarget> getLaserTargets() {
        Set<LaserTarget> laserTargets = new HashSet<>();
        for (Observation o : observations) {
            for (ObservationTarget t : o.getTargets()) {
                laserTargets.add(t.getLaserTarget());
            }
        }
        return Collections.unmodifiableSet(laserTargets);
    }

    public Set<RaDecLaserTarget> getRaDecLaserTargets() {
        Set<RaDecLaserTarget> s = new HashSet<>();
        for (LaserTarget t : getLaserTargets()) {
            if (t instanceof RaDecLaserTarget) {
                s.add((RaDecLaserTarget) t);
            }
        }
        return Collections.unmodifiableSet(s);
    }
    public Set<AzElLaserTarget> getAzElLaserTargets() {
        Set<AzElLaserTarget> s = new HashSet<>();
        for (LaserTarget t : getLaserTargets()) {
            if (t instanceof AzElLaserTarget) {
                s.add((AzElLaserTarget) t);
            }
        }
        return Collections.unmodifiableSet(s);
    }

    public Set<Observation> getUncoveredObservations() {
        Set<Observation> uncovered = new HashSet<>();
        for (Observation o : observations) {
            for (ObservationTarget t : o.getTargets()) {
                if (!t.isCovered()) {
                    uncovered.add(o);
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(uncovered);
    }

    public Set<Observation> getObservationsWithoutWindows() {
        Set<Observation> without = new HashSet<>();
        for (Observation o : observations) {
            for (ObservationTarget t : o.getTargets()) {
                if (t.getLaserTarget().getPropagationWindows().size() == 0) {
                    without.add(o);
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(without);
    }

    public Set<LaserTarget> getLaserTargetsWithoutWindows() {
        Set<LaserTarget> without = new HashSet<>();
        for (LaserTarget t : getLaserTargets()) {
                if (t.getPropagationWindows().size() == 0) {
                    without.add(t);
                }
        }
        return Collections.unmodifiableSet(without);
    }

    public Set<LaserTarget> getUntransmittedLaserTargets() {
        Set<LaserTarget> untransmitted = new HashSet<>();
        for (LaserTarget t : getLaserTargets()) {
            if (!t.isTransmitted()) {
                untransmitted.add(t);
            }
        }
        return Collections.unmodifiableSet(untransmitted);
    }

    public static Site getSite(String site) {
        if (site.toUpperCase().equals(Site.SOUTH.name())) {
            return Site.SOUTH;
        } else if (site.toUpperCase().equals(Site.NORTH.name())) {
            return Site.NORTH;
        } else {
            throw new RuntimeException("unknown site " + site);
        }
    }

    /**
     * Gets the semester this night belongs to, e.g. 2012A.
     */
    public String getSemester() {
        return getSemester(getStart());
    }
    public String getSemester(int offset) {
        return getSemester(getStart(), offset);
    }

    /**
     * Gets the semester with an offset to the current night.
     */
    public static String getSemester(ZonedDateTime day, int offset) {
        return getSemester(day.plusMonths(offset * 6));
    }

    public static String getSemester(ZonedDateTime day) {
        if (day.getMonthValue() < 2) {
            return (day.getYear() - 1) + "B";
        } else if (day.getMonthValue() < 8) {
            return (day.getYear()) + "A";
        } else {
            return (day.getYear()) + "B";
        }
    }

    public LaserTarget findClosestRaDecTarget(Double c1, Double c2, Double maxDistance) {
        return findByPosition(getRaDecLaserTargets(), c1, c2, maxDistance);
    }

    public LaserTarget findClosestAzElTarget(Double c1, Double c2, Double maxDistance) {
        return findByPosition(getAzElLaserTargets(), c1, c2, maxDistance);
    }

    private LaserTarget findByPosition(Collection<? extends LaserTarget> targets, Double c1, Double c2, Double maxDistance) {
        LaserTarget closest = LaserTarget.getClosestTo(targets, c1, c2);
        // check minimal distance and return result, can be null!
        if (closest != null && closest.distanceTo(c1, c2) > maxDistance) {
            return null;
        } else {
            return closest;
        }
    }

    public List<Observation> findObservationsForTarget(LaserTarget target) {
        Set<Observation> observations = new HashSet<>();
        for (Observation o : getObservations()) {
           for (ObservationTarget ot : o.getTargets()) {
               if (ot.getLaserTarget().getId().equals(target.getId())) {
                   observations.add(o);
               }
           }
        }
        return Collections.unmodifiableList(new ArrayList<>(observations));
    }

    /**
     * Checks if a night contains only test data.
     * Test nights are created for a full 24hrs interval.
     */
    public boolean isTestNight() {
        return Duration.between(getStart(), getEnd()).toHours() == 24;
    }

    // empty constructor needed by hibernate
    public LaserNight() {}
}
