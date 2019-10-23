package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 */
@XmlRootElement
public class Target {

    private LaserTarget laserTarget;
    private List<Observation> observations;

    public Target(LaserTarget laserTarget, List<Observation> observations) {
        this.laserTarget = laserTarget;
        this.observations = observations;
    }

    @XmlElement(name = "laserTarget")
    public LaserTarget getLaserTarget() {
        return laserTarget;
    }
    @XmlElementWrapper(name = "observations")
    @XmlElement(name = "observation")
    public List<Observation> getObservations() {
        if (observations == null) observations = new ArrayList<>();
        return observations;
    }

    // empty constructor needed for JAXB
    public Target() {
        this.observations = new ArrayList<>();
    }
}
