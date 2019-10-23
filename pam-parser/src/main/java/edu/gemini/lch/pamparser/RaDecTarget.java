package edu.gemini.lch.pamparser;

/**
 */
public class RaDecTarget extends Target {

    public RaDecTarget(Double raDegrees, Double decDegrees) {
        super(raDegrees, decDegrees);
    }

    public Double getRaDegrees() {
        return degrees1;
    }

    public Double getDecDegrees() {
        return degrees2;
    }
}
