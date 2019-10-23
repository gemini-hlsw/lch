package edu.gemini.lch.model;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.persistence.*;
import java.util.*;

@MappedSuperclass
public abstract class BaseLaserNight {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column
    private Site site;

    @Column(name = "starts")
    private Date start;

    @Column(name = "ends")
    private Date end;

    @Column
    private Date latestPrmSent;

    @Column
    private Date latestPamReceived;

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public DateTime getStart() {
        return new DateTime(start);
    }

    public DateTime getEnd() {
        return new DateTime(end);
    }

    public boolean hasPrmSent() {
        return latestPrmSent != null;
    }

    public DateTime getLatestPrmSent() {
        return new DateTime(latestPrmSent);
    }

    public void setLatestPrmSent(DateTime latestPrmSent) {
        this.latestPrmSent = latestPrmSent.toDate();
    }

    public boolean hasPamReceived() {
        return latestPamReceived != null;
    }

    public DateTime getLatestPamReceived() {
        return new DateTime(latestPamReceived);
    }

    public void setLatestPamReceived(DateTime latestPamReceived) {
        this.latestPamReceived = latestPamReceived.toDate();
    }

    public Duration getDuration() {
        return new Duration(getStart(), getEnd());
    }

    /**
     * Returns true if the time passed as parameter is inside the night, i.e. time is in [start, end)
     * @param time
     * @return
     */
    public Boolean covers(DateTime time) {
        return (!getStart().isAfter(time) && getEnd().isAfter(time));
    }

    public BaseLaserNight(Site site, DateTime start, DateTime end) {
        this.site = site;
        this.start = start.toDate();
        this.end = end.toDate();
    }

    // empty constructor needed by hibernate
    public BaseLaserNight() {}
}
