package edu.gemini.lch.pamparser;

/**
 */
public class AzAltTarget extends Target {

    public AzAltTarget(double azimuthDegrees, double altitudeDegrees) {
        super(azimuthDegrees, altitudeDegrees);
    }
    public Double getAzDegrees() {
        return degrees1;
    }

    public Double getAltDegrees() {
        return degrees2;
    }
}
