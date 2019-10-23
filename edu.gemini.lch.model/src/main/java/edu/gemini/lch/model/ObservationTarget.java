package edu.gemini.lch.model;

import javax.persistence.*;

/**
 * This class represents a science observation target from any of the science programs stored in the ODB.
 * In future versions this reference object could for example hold references to ODB objects like programs,
 * observations and the like, currently it just holds the program and target name imported from the LGS files.
 */
@Entity
@Table(name = "lch_observation_targets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class ObservationTarget {
//    private static final Logger LOGGER = Logger.getLogger(ObservationTarget.class);

    /**
     * The state of an observation target tells us how it changed since the PRM files have been sent to LCH.
     */
    public enum State {
        OK,                 // no changes detected
        ADDED,              // this target has been changed or added (i.e. it is new or has new coordinates)
        REMOVED             // this target has been changed or deleted (i.e. it has been removed or has new coordinates)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column
    private State state;

    @Column
    private String name;

    @Column
    private String targetType;

    @Column
    protected Double degrees1;

    @Column
    protected Double degrees2;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "target_id")
    private LaserTarget laserTarget;

    @Column(name="target_id", insertable = false, updatable = false)
    private Long targetId;

    @Column(name="observation_id", insertable = false, updatable = false)
    private Long observationId;

    public ObservationTarget(String targetName, String targetType, Double degrees1, Double degrees2) {
        this.state = State.OK;
        this.name = targetName;
        this.targetType = targetType;
        this.degrees1 = degrees1;
        this.degrees2 = degrees2;
    }

    public boolean isScience() {
        return targetType.equals("Base");  // TODO: NOTE THAT NON-SIDEREAL OBSERVATIONS WILL HAVE MORE THAN ONE SCIENCE TARGET (?)
    }

    public Long getId() {
        return id;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public Boolean isCovered() {
        return laserTarget.isTransmitted();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return targetType;
    }

    public Double getDegrees1() {
        return degrees1;
    }

    public Double getDegrees2() {
        return degrees2;
    }

    public void setLaserTarget(LaserTarget laserTarget) {
        this.laserTarget = laserTarget;
    }

    public LaserTarget getLaserTarget() {
        return laserTarget;
    }

    // empty constructor needed for hibernate
    public ObservationTarget() {}

}
