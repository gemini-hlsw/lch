package edu.gemini.lch.web.app.util;

import sun.lwawt.macosx.CSystemTray;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;


/**
 * A pretty printer for times with the right time zone.
 */
public final class TimeFormatter {

    // TODO-JODA: There is no PeriodFormatter
    private static final PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendHours()
            .appendSeparator(":")
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendMinutes()
            .appendSeparator(":")
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendSeconds()
            .toFormatter();

    private final DateTimeFormatter timeFormatter;
    private final DateTimeFormatter timeLongFormatter;
    private final DateTimeFormatter dateAndTimeFormatter;
    private final DateTimeFormatter dateAndTimeLongFormatter;

    public TimeFormatter(ZoneId zoneId) {
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId);
        timeLongFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zoneId);
        dateAndTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId);
        dateAndTimeLongFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);
    }

    public String asTime(Instant instant) {
        return asTime(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    public String asTime(ZonedDateTime dateTime) {
        return timeFormatter.format(dateTime);
    }

    public String asTimeLong(ZonedDateTime dateTime) {
        return timeLongFormatter.format(dateTime);
    }

    public ZonedDateTime fromTimeLong(String string) {
        return ZonedDateTime.parse(string, timeLongFormatter);
    }

    public ZonedDateTime fromDateAndTime(String string) {
        return ZonedDateTime.parse(string, dateAndTimeFormatter);
    }

    public String asDateAndTime(ZonedDateTime dateTime) {
        return dateAndTimeFormatter.format(dateTime);
    }

    public String asDateAndTimeLong(ZonedDateTime dateTime) {
        return dateAndTimeLongFormatter.format(dateTime);
    }

    public static String asDuration(Instant start, Instant end) {
        return asDuration(ZonedDateTime.ofInstant(start, ZoneId.systemDefault()), ZonedDateTime.ofInstant(end, ZoneId.systemDefault()));
    }

    public static String asDuration(ZonedDateTime start, ZonedDateTime end) {
        return asDuration(Duration.between(start, end));
    }

    // TODO-JODA: There is no formatter for duration.
    public static String asDuration(Duration duration) {
        return periodFormatter.format(duration.toPeriod());
    }


}
