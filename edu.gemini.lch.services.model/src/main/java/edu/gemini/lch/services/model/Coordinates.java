package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"isRaDec", "c1", "c2"})
public class Coordinates {

    public static final Boolean RADEC = Boolean.TRUE;
    public static final Boolean AZEL = !RADEC;

    private Boolean isRaDec;
    private Double c1;
    private Double c2;

    public Boolean isRaDec() { return isRaDec; }
    public Boolean isAzEl() { return !isRaDec; }
    public Double getC1() { return c1; }
    public Double getC2() { return c2; }

    public Coordinates(Boolean isRaDec, Double c1, Double c2) {
        this.isRaDec = isRaDec;
        this.c1 = c1;
        this.c2 = c2;
    }

    public Coordinates() {}
}