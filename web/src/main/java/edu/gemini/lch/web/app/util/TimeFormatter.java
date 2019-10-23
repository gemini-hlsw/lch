package edu.gemini.lch.web.app.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;


/**
 * A pretty printer for times with the right time zone.
 */
public final class TimeFormatter {

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

    public TimeFormatter(DateTimeZone timeZone) {
        timeFormatter = DateTimeFormat.forPattern("HH:mm").withZone(timeZone);
        timeLongFormatter = DateTimeFormat.forPattern("HH:mm:ss").withZone(timeZone);
        dateAndTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withZone(timeZone);
        dateAndTimeLongFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(timeZone);
    }

    public String asTime(Date date) {
        return asTime(new DateTime(date));
    }

    public String asTime(DateTime dateTime) {
        return timeFormatter.print(dateTime);
    }

    public String asTimeLong(DateTime dateTime) {
        return timeLongFormatter.print(dateTime);
    }

    public DateTime fromTimeLong(String string) {
        return timeLongFormatter.parseDateTime(string);
    }

    public DateTime fromDateAndTime(String string) {
        return dateAndTimeFormatter.parseDateTime(string);
    }

    public String asDateAndTime(DateTime dateTime) {
        return dateAndTimeFormatter.print(dateTime);
    }

    public String asDateAndTimeLong(DateTime dateTime) {
        return dateAndTimeLongFormatter.print(dateTime);
    }

    public static String asDuration(Date start, Date end) {
        return asDuration(new DateTime(start), new DateTime(end));
    }

    public static String asDuration(DateTime start, DateTime end) {
        return asDuration(new Duration(start, end));
    }

    public static String asDuration(Duration duration) {
        return periodFormatter.print(duration.toPeriod());
    }


}
