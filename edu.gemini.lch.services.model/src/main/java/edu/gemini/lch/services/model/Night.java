package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

/**
 */
public class Night {

    private Long id;
    private String site;
    private Date start;
    private Date end;
    private Date latestPrmSent;
    private Date latestPamReceived;

    public Night(Long id, String site, Date start, Date end, Date latestPrmSent, Date latestPamReceived) {
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
    public Date getStart() { return start; }
    public void setStart(Date start) { this.start = start; }

    @XmlElement
    public Date getEnd() { return end; }
    public void setEnd(Date end) { this.end = end; }

    @XmlElement
    public Date getLatestPrmSent() { return latestPrmSent; }
    public void setLatestPrmSent(Date latestPrmSent) { this.latestPrmSent = latestPrmSent; }

    @XmlElement
    public Date getLatestPamReceived() { return latestPamReceived; }
    public void setLatestPamReceived(Date latestPamReceived) { this.latestPamReceived = latestPamReceived; }

    // empty constructor needed for JAXB
    public Night() {
    }
}
