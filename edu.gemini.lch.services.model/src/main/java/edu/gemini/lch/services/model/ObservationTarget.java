package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class ObservationTarget {

    private LaserTarget laserTarget;
    private String name;
    private String type;


    public ObservationTarget(String name, String type, LaserTarget laserTarget) {
        this.name = name;
        this.type = type;
        this.laserTarget = laserTarget;
    }

    @XmlTransient
    public boolean isScience() {
        return type.equals("Base");
    }

    @XmlElement
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    @XmlIDREF
    public LaserTarget getLaserTarget() {
        return laserTarget;
    }
    public void setLaserTarget(LaserTarget laserTarget) {
        this.laserTarget = laserTarget;
    }

    public ObservationTarget() {}
}
