package edu.gemini.lch.web.app.components;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 */
public class StartEndDateDialogWindow extends DateDialogWindow {

    public StartEndDateDialogWindow(String caption, DateTimeZone timeZone, DateDialogListener listener) {
        super(caption, timeZone, listener, true);
    }

    public DateTime getStartDate() {
        return new DateTime(startDate.getValue());
    }

    public DateTime getEndDate() {
        return new DateTime(endDate.getValue());
    }

}
