package edu.gemini.lch.pamparser;

/**
 */
public abstract class Target {
    protected final Double degrees1;
    protected final Double degrees2;
    public Target(Double degrees1, Double degrees2) {
        this.degrees1 = degrees1;
        this.degrees2 = degrees2;
    }

    public Double getDegrees1() {
        return degrees1;
    }

    public  Double getDegrees2() {
        return degrees2;
    }
}
