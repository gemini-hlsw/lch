package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 */
@XmlRootElement(name = "night")
public class NightFull extends Night {

    private List<Observation> observations;
    private List<LaserTarget> laserTargets;

    public NightFull(Long id, String site, Instant start, Instant end, Instant latestPrmSent, Instant latestPamReceived) {
        super(id, site, start, end, latestPrmSent, latestPamReceived);
    }

    @XmlElementWrapper(name = "laserTargets")
    @XmlElement(name = "laserTarget")
    public List<LaserTarget> getLaserTargets() {
        if (laserTargets == null) laserTargets = new ArrayList<>();
        return laserTargets;
    }
    @XmlElementWrapper(name = "observations")
    @XmlElement(name = "observation")
    public List<Observation> getObservations() {
        if (observations == null) observations = new ArrayList<>();
        return observations;
    }

    // empty constructor needed for JAXB
    public NightFull() {
        this.observations = new ArrayList<>();
        this.laserTargets = new ArrayList<>();
    }
}
