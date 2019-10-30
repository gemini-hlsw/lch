package edu.gemini.lch.model;

import org.apache.commons.lang.Validate;

import javax.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Named queries for laser targets.
 */
@NamedQueries({
        @NamedQuery(name = LaserTarget.QUERY_FIND_BY_ID,
                query = "from LaserTarget t " +
                        "where t.id = :id "
        )
})

/**
 * Laser targets represent positions in the sky we are getting clearance for from LCH.
 * One laser target can cover several observation targets if they are close to each other.
 */
@Entity
@Table(name = "lch_laser_targets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class LaserTarget {

    public static final String QUERY_FIND_BY_ID = "laserTarget.findById";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "night_id")
    private LaserNight night;

    @Column(name="night_id", insertable = false, updatable = false)
    private Long nightId;

    @Column
    protected Boolean transmitted;

    @Column
    protected Double degrees1;

    @Column
    protected Double degrees2;

    @Column
    protected Instant risesAboveHorizon;

    @Column
    protected Instant risesAboveLimit;

    @Column
    protected Instant setsBelowLimit;

    @Column
    protected Instant setsBelowHorizon;

    @Column
    protected Instant windowsTimestamp;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "target_id")
    @OrderBy("start")
    private Set<PropagationWindow> windows;

    public LaserTarget(LaserNight night, Double degrees1, Double degrees2, Visibility visibility) {
        this.night = night;
        this.transmitted = false;
        this.windows = new TreeSet<>();
        this.degrees1 = degrees1;
        this.degrees2 = degrees2;
        this.risesAboveHorizon = visibility.getRises().orElse(null);
        this.setsBelowHorizon  = visibility.getSets().orElse(null);
        this.risesAboveLimit   = visibility.getRisesAboveLimit().orElse(null);
        this.setsBelowLimit    = visibility.getSetsBelowLimit().orElse(null);
    }

    public Long getId() {
        return id;
    }

    public Double getDegrees1() {
        return degrees1;
    }

    public Double getDegrees2() {
        return degrees2;
    }

    public ZonedDateTime getSetTime() {
        return ZonedDateTime.ofInstant(setsBelowHorizon, ZoneId.systemDefault());
    }

    public ZonedDateTime getRiseTime() {
        return ZonedDateTime.ofInstant(risesAboveHorizon, ZoneId.systemDefault());
    }

    public Visibility getVisibility() {
        final Optional<Visibility.RiseSet> aboveHorizon =
                (risesAboveHorizon != null && setsBelowHorizon != null) ? Optional.of(new Visibility.RiseSet(risesAboveHorizon, setsBelowHorizon)) : Optional.empty();
        final Optional<Visibility.RiseSet> aboveLimit   =
                (risesAboveLimit != null   && setsBelowLimit != null)   ? Optional.of(new Visibility.RiseSet(risesAboveLimit,   setsBelowLimit))   : Optional.empty();
        return new Visibility(aboveHorizon, aboveLimit);
    }

    public Set<PropagationWindow> getPropagationWindows() {
        return windows;
    }

    public List<ShutteringWindow> getShutteringWindows() {
        return PropagationWindow.getShutteringWindows(new ArrayList<>(windows));
    }

    public Boolean isTransmitted() {
        return transmitted;
    }

    public void setTransmitted(final Boolean transmitted) {
        this.transmitted = transmitted;
    }

    /**
     * Checks if the windows for this laser target have been updated.
     */
    public Boolean hasWindowsTimestamp() {
        return this.windowsTimestamp != null;
    }


    /**
     * Gets the timestamp of the most recent PRM report that caused the windows of this laser target to be updated.
     */
    public ZonedDateTime getWindowsTimestamp() {
        return ZonedDateTime.ofInstant(this.windowsTimestamp, ZoneId.systemDefault());
    }

    /**
     * Sets the timestamp to reflect the timestamp of the PRM report that caused the windows of this laser target
     * to be udpated.
     * @param timestamp
     */
    public void setWindowsTimestamp(final ZonedDateTime timestamp) {
        this.windowsTimestamp = timestamp.toInstant();
    }

    /**
     * Validates the propagation windows for this laser target by checking for overlapping windows.
     * @return
     */
    public Boolean windowsAreDisjoint() {
        Collections.sort(new ArrayList<>(windows));
        PropagationWindow w0 = null;
        for (PropagationWindow w1 : windows) {
            if (w0 != null) {
                if (!w0.getEnd().isBefore(w1.getStart())) {
                    return false;
                }
            }
            w0 = w1;
        }
        return true;
    }

    /**
     * Calculates the distance between this and another laser target.
     */
    public Double distanceTo(final LaserTarget other) {
        return distanceTo(other.degrees1, other.degrees2);
    }

    /**
     * Calculates the distance between the given coordinates and this laser target.
     */
    public Double distanceTo(final Double c1, final Double c2) {
        // cos(A) = sin(dec1)sin(dec2) + cos(dec1)cos(dec2)cos(ra1-ra2)

        // convert degrees to radians
        final double ra1  = this.degrees1*2.0*Math.PI/360.0;
        final double dec1 = this.degrees2*2.0*Math.PI/360.0;
        final double ra2  = c1*2.0*Math.PI/360.0;
        final double dec2 = c2*2.0*Math.PI/360.0;

        // do the math
        final double u = Math.sin(dec1)*Math.sin(dec2);
        final double v = Math.cos(dec1)*Math.cos(dec2);
        final double w = Math.cos(ra1-ra2);
        // limit u+v*w to the valid range for acos [-1..1], acos(1.0000002) will yield NaN for example!
        final double s = Math.max(-1.0, Math.min(1.0, u + (v * w)));
        final double r = Math.acos(s);
        // fast fail if we don't get the math right
        Validate.isTrue(!Double.isNaN(r));

        // convert result in radians back to degrees
        return (r*360.0)/(2.0*Math.PI);
    }

    /**
     * Finds the target that is closest to the given position.
     */
    public static <T extends LaserTarget> T getClosestTo(final Collection<T> targets, final Double c1, final Double c2) {

        T closest = null;
        for (T t : targets) {
            if (closest == null || closest.distanceTo(c1, c2) > t.distanceTo(c1, c2)) {
                closest = t;
            }
        }
        return closest;
    }

    // empty constructor needed for hibernate
    public LaserTarget() {}

}
