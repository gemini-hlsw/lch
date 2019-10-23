package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * JAXB annotated value object for sending LTCS collisions to client.
 */
@XmlRootElement
@XmlType(propOrder = {"observatory", "priority", "start", "end"})
public class LtcsCollision {

    @XmlElement
    private String observatory;

    @XmlElement
    private String priority;

    @XmlElement
    private Date start;

    @XmlElement
    private Date end;

    public LtcsCollision(String observatory, String priority, Date start, Date end) {
        this.observatory = observatory;
        this.priority = priority;
        this.start = start;
        this.end = end;
    }

    public Date getStart() { return start; }
    public Date getEnd() { return end; }
    public String getObservatory() { return observatory; }
    public String getPriority() { return priority; }

    // silly wrapper object needed to send lists of collisions to client
    @XmlRootElement(name = "collisions")
    public static class List {
        @XmlElement(name = "collision")
        public Collection<LtcsCollision> collisions;
        public List(Collection<LtcsCollision> collisions) {
            this.collisions = collisions;
        }
        public List() {
            collisions = new ArrayList<>();
        }
    }

    // needed for JAXB
    public LtcsCollision() {}
}
