package edu.gemini.lch.model;

import java.time.ZoneId;
import java.util.TimeZone;

public enum Site {
    NORTH("Gemini North", "Mauna Kea", TimeZone.getTimeZone("Pacific/Honolulu"), ZoneId.of("Pacific/Honolulu"), 19.8238),
    SOUTH("Gemini South", "Cerro Pachon", TimeZone.getTimeZone("America/Santiago"), ZoneId.of("America/Santiago"), -30.2407);

    private final String displayName;
    private final String siteName;
    private final TimeZone timeZone;
    private final ZoneId zoneId;
    private final Double latitude;

    Site(String displayName, String siteName, TimeZone timeZone, ZoneId zoneId, Double latitude) {
        this.displayName = displayName;
        this.siteName = siteName;
        this.timeZone = timeZone;
        this.zoneId = zoneId;
        this.latitude = latitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSiteName() {
        return siteName;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public Double getLatitude() {
        return latitude;
    }

}
