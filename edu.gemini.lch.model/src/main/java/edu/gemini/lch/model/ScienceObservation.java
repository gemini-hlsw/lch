package edu.gemini.lch.model;

import javax.persistence.*;

@Entity
@DiscriminatorValue("Science")
public class ScienceObservation extends Observation {

    public ScienceObservation(String observationId) {
        super(observationId);
    }

    // empty constructor needed for hibernate
    public ScienceObservation() {}

}
