package edu.gemini.lch.data.util;

import org.apache.commons.lang.Validate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 */
public class TimeTool {

    public static Date changeTimeZone(String datePattern, String dateString, TimeZone newTimeZone) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
        Date date = sdf.parse(dateString);
        return changeTimeZone(date, newTimeZone);
    }

    public static Date changeTimeZone(Date date, TimeZone newTimeZone) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.setTimeZone(newTimeZone);
        return calendar.getTime();
    }

    public static Date mapUTC(Date min, Date max, int hrs, int mins, int secs) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(min);  // set date and timezone
        calendar.set(Calendar.HOUR_OF_DAY, hrs);
        calendar.set(Calendar.MINUTE, mins);
        calendar.set(Calendar.SECOND, secs);
        calendar.add(Calendar.MILLISECOND, calendar.getTimeZone().getOffset(min.getTime())); // change from UTC to local time

        if (calendar.getTime().before(min)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (calendar.getTime().after(max)) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        Validate.isTrue(calendar.getTime().after(min));
        Validate.isTrue(calendar.getTime().before(max));

        return calendar.getTime();
    }
}
