package edu.gemini.lch.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Named queries for AzEl laser targets.
 */
@NamedQueries({
        @NamedQuery(name = RaDecLaserTarget.QUERY_FIND_BY_NIGHT,
                query = "from RaDecLaserTarget t " +
                        "where " +
                        "t.nightId = :nightId"
        )
})

/**
 */
@Entity
@DiscriminatorValue("RaDec")
public class RaDecLaserTarget extends LaserTarget {

    public static final String QUERY_FIND_BY_NIGHT = "raDecLaserTarget.findByNight";

    public RaDecLaserTarget(LaserNight night, Double raDegrees, Double decDegrees, Visibility visibility) {
        super(night, raDegrees, decDegrees, visibility);
    }

    public Double getRaDegrees() {
        return degrees1;
    }

    public Double getDecDegrees() {
        return degrees2;
    }

    public void setRaDegrees(Double raDegrees) {
        degrees1 = raDegrees;
    }

    public void setDecDegrees(Double decDegrees) {
        degrees2 = decDegrees;
    }

    // empty constructor needed for hibernate
    public RaDecLaserTarget() {}

}
