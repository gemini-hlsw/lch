package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Transfer object data structure for visibility information.
 */
@XmlRootElement
@XmlType(propOrder = {"aboveHorizon", "aboveLaserLimit"})
@XmlAccessorType(XmlAccessType.FIELD)
public class Visibility {

    @XmlElementWrapper(name = "aboveHorizon")
    @XmlElement(name = "interval")
    private List<Interval> aboveHorizon;

    @XmlElementWrapper(name = "aboveLaserLimit")
    @XmlElement(name = "interval")
    private List<Interval> aboveLaserLimit;

    public List<Interval> getAboveHorizon() { return Collections.unmodifiableList(aboveHorizon); }
    public List<Interval> getAboveLaserLimit() { return Collections.unmodifiableList(aboveLaserLimit); }

    public Visibility(List<Interval> aboveHorizon, List<Interval> aboveLaserLimit) {
        this.aboveHorizon = aboveHorizon;
        this.aboveLaserLimit = aboveLaserLimit;
    }

    public Visibility() {}

    @XmlRootElement
    @XmlType(propOrder = {"start", "end"})
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Interval {

        private Date start;
        private Date end;

        public Date getStart() { return start; }
        public Date getEnd() { return end; }

        public Interval(Date start, Date end) {
            this.start = start;
            this.end = end;
        }

        public Interval() {}
    }

}
