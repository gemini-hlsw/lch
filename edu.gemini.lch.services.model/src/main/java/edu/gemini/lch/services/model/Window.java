package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

/**
 */
@XmlType(propOrder = {"start", "end"})
public abstract class Window {

    private Date start;
    private Date end;

    public Window(Date start, Date end) {
        this.start = start;
        this.end =end;
    }

    @XmlElement
    public Date getStart() {
        return this.start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    @XmlElement
    public Date getEnd() {
        return this.end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }
    // needed for JAXB
    public Window() {
        start = new Date();
        end = new Date();
    }
}
