package edu.gemini.lch.model;

import javax.persistence.*;

@Entity
@DiscriminatorValue("Engineering")
public class EngineeringTarget extends ObservationTarget {

    public EngineeringTarget(String name, Double az, Double el) {
        super(name, "Base", az, el);
    }

    // empty constructor needed for hibernate
    public EngineeringTarget() {}

}
