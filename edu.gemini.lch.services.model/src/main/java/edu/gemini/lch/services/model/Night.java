package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import java.time.Instant;

public class Night {

    private Long id;
    private String site;
    private Instant start;
    private Instant end;
    private Instant latestPrmSent;
    private Instant latestPamReceived;

    public Night(Long id, String site, Instant start, Instant end, Instant latestPrmSent, Instant latestPamReceived) {
        this.id = id;
        this.site = site;
        this.start = start;
        this.end = end;
        this.latestPrmSent = latestPrmSent;
        this.latestPamReceived = latestPamReceived;
    }

    @XmlElement
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @XmlElement
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    @XmlElement
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }

    @XmlElement
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }

    @XmlElement
    public Instant getLatestPrmSent() { return latestPrmSent; }
    public void setLatestPrmSent(Instant latestPrmSent) { this.latestPrmSent = latestPrmSent; }

    @XmlElement
    public Instant getLatestPamReceived() { return latestPamReceived; }
    public void setLatestPamReceived(Instant latestPamReceived) { this.latestPamReceived = latestPamReceived; }

    // empty constructor needed for JAXB
    public Night() {
    }
}
