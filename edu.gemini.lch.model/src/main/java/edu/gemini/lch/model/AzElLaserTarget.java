package edu.gemini.lch.model;

import javax.persistence.*;

/**
 * Named queries for AzEl laser targets.
 */
@NamedQueries({
        @NamedQuery(name = AzElLaserTarget.QUERY_FIND_BY_NIGHT,
                query = "from AzElLaserTarget t " +
                        "where " +
                        "t.nightId = :nightId"
        )
})

@Entity
@DiscriminatorValue("AzEl")
public class AzElLaserTarget extends LaserTarget {

    public static final String QUERY_FIND_BY_NIGHT = "azElLaserTarget.findByNight";

    public AzElLaserTarget(LaserNight night, double azimuthDegrees, double elevationDegrees) {
        super(night, azimuthDegrees, elevationDegrees, Visibility.ALWAYS);
    }

    public Double getAzDegrees() {
        return degrees1;
    }

    public Double getElDegrees() {
        return degrees2;
    }

    public void setAzDegrees(double azimuthDegrees) {
        degrees1 = azimuthDegrees;
    }

    public void setElDegrees(double elevationDegrees) {
        degrees2 = elevationDegrees;
    }

    // empty constructor needed for hibernate
    public AzElLaserTarget() {}

}
