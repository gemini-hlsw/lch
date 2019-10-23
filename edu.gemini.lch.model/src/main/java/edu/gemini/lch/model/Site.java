package edu.gemini.lch.model;

import org.joda.time.DateTimeZone;

/**
 */
public enum Site {
    NORTH("Gemini North", "Mauna Kea", DateTimeZone.forID("Pacific/Honolulu"), 19.8238),
    SOUTH("Gemini South", "Cerro Pachon", DateTimeZone.forID("America/Santiago"), -30.2407);

    private final String displayName;
    private final String siteName;
    private final DateTimeZone timeZone;
    private final Double latitude;

    Site(String displayName, String siteName, DateTimeZone timeZone, Double latitude) {
        this.displayName = displayName;
        this.siteName = siteName;
        this.timeZone = timeZone;
        this.latitude = latitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSiteName() {
        return siteName;
    }

    public DateTimeZone getTimeZone() {
        return timeZone;
    }

    public Double getLatitude() {
        return latitude;
    }

}
