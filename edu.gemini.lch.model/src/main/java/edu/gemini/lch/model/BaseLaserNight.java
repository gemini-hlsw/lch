package edu.gemini.lch.model;

import javax.persistence.*;
import java.time.*;

@MappedSuperclass
public abstract class BaseLaserNight {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column
    private Site site;

    @Column(name = "starts")
    private Instant start;

    @Column(name = "ends")
    private Instant end;

    @Column
    private Instant latestPrmSent;

    @Column
    private Instant latestPamReceived;

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public ZonedDateTime getStart() {
        return ZonedDateTime.ofInstant(start, ZoneId.systemDefault());
    }

    public ZonedDateTime getEnd() {
        return ZonedDateTime.ofInstant(end, ZoneId.systemDefault());
    }

    public boolean hasPrmSent() {
        return latestPrmSent != null;
    }

    public ZonedDateTime getLatestPrmSent() {
        return ZonedDateTime.ofInstant(latestPrmSent, ZoneId.systemDefault());
    }

    public void setLatestPrmSent(Instant latestPrmSent) {
        this.latestPrmSent = latestPrmSent;
    }

    public boolean hasPamReceived() {
        return latestPamReceived != null;
    }

    public ZonedDateTime getLatestPamReceived() {
        return ZonedDateTime.ofInstant(latestPamReceived, ZoneId.systemDefault());
    }

    public void setLatestPamReceived(Instant latestPamReceived) {
        this.latestPamReceived = latestPamReceived;
    }

    public Duration getDuration() {
        return Duration.between(getStart(), getEnd());
    }

    public Boolean covers(ZonedDateTime time) {
        return (!getStart().isAfter(time) && getEnd().isAfter(time));
    }

    BaseLaserNight(Site site, ZonedDateTime start, ZonedDateTime end) {
        this.site = site;
        this.start = start.toInstant();
        this.end = end.toInstant();
    }

    // empty constructor needed by hibernate
    BaseLaserNight() {}
}
