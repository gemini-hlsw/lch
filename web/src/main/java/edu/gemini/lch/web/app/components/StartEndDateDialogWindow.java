package edu.gemini.lch.web.app.components;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class StartEndDateDialogWindow extends DateDialogWindow {

    public StartEndDateDialogWindow(String caption, ZoneId zoneId, DateDialogListener listener) {
        super(caption, zoneId, listener, true);
    }

    public ZonedDateTime getStartDate() {
        return ZonedDateTime.ofInstant(startDate.getValue().toInstant(), ZoneId.systemDefault());
    }

    public ZonedDateTime getEndDate() {
        return ZonedDateTime.ofInstant(endDate.getValue().toInstant(), ZoneId.systemDefault());
    }

}
