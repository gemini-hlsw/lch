package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.time.Instant;

/**
 */
@XmlType(propOrder = {"start", "end"})
public abstract class Window {

    private Instant start;
    private Instant end;

    public Window(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    @XmlElement
    public Instant getStart() {
        return this.start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    @XmlElement
    public Instant getEnd() {
        return this.end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }
    // needed for JAXB
    public Window() {
        start = Instant.now();
        end = Instant.now();
    }
}
