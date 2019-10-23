package edu.gemini.lch.model;

import javax.persistence.*;

/**
 * This class represents a science observation target from any of the science programs stored in the ODB.
 * In future versions this reference object could for example hold references to ODB objects like programs,
 * observations and the like, currently it just holds the program and target name imported from the LGS files.
 */
@Entity
@DiscriminatorValue("Sidereal")
public class SiderealTarget extends ObservationTarget {

    public SiderealTarget(String targetName, String targetType, Double degrees1, Double degrees2) {
        super(targetName, targetType, degrees1, degrees2);
    }

    // empty constructor needed for hibernate
    public SiderealTarget() {}

}
