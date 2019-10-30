package edu.gemini.lch.model;

import javax.persistence.*;

@Entity
@DiscriminatorValue("Engineering")
public class EngineeringObservation extends Observation {

    public EngineeringObservation(String observationId) {
        super(observationId);
    }

    // empty constructor needed for hibernate
    public EngineeringObservation() {}

}
