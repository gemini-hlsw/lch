package edu.gemini.lch.model;

import javax.persistence.*;

/**
 */
@Entity
@DiscriminatorValue("NonSidereal")
public class NonSiderealTarget extends ObservationTarget {

    @Column(name = "horizons_id")
    protected String horizonsid;

    public NonSiderealTarget(String targetName, String targetType, String horizonsId, Double ra, Double dec) {
        super(targetName, targetType, ra, dec);
        this.horizonsid = horizonsId;
    }

    public String getHorizonsId() {
        return horizonsid;
    }

    public RaDecLaserTarget getNonSiderealLaserTarget() {
        return (RaDecLaserTarget) getLaserTarget();
    }

    // empty constructor needed for hibernate
    public NonSiderealTarget() {}

}
