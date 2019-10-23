package edu.gemini.lch.web.app.windows.night;

import com.vaadin.data.Property;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.model.SimpleLaserNight;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.vaadin.risto.stylecalendar.DateOptionsGenerator;
import org.vaadin.risto.stylecalendar.StyleCalendar;

import java.util.*;


/**
 */
@Configurable(preConstruction = true)
public final class Calendar extends Panel implements TimeZoneSelector.Listener {

    @Autowired
    private LaserNightService laserNightService;

    private final Set<DayChangeListener> listeners;
    private final StyleCalendar calendar;
    private final Component header;

    private List<SimpleLaserNight> laserNights;
    private DateTimeZone currentTimeZone;
    private DateTime currentDate;

    public Calendar() {
        final Button prevYear  = new Button("<<");
        final Button prevMonth = new Button("<");
        final Button nextMonth = new Button(">");
        final Button nextYear  = new Button(">>");
        final Component empty = new Button();
        header = new Label();
        header.setStyleName(Reindeer.LABEL_H2);

        currentTimeZone = DateTimeZone.UTC;
        listeners = new HashSet<>();
        laserNights = getLaserNightsForCurrentMonth();

        calendar = new MyStyleCalendar();
        calendar.setRenderWeekNumbers(false);
        calendar.setRenderHeader(false);
        calendar.setWidthUndefined();
        calendar.setHeight(120, Unit.PIXELS);
        calendar.setDateOptionsGenerator(new MyDateOptionsGenerator());
        calendar.addValueChangeListener((Property.ValueChangeEvent event) -> {
            Date selected = (Date) event.getProperty().getValue();
            SimpleLaserNight night = findNight(selected);
            if (night != null) {
                for (DayChangeListener listener : listeners) {
                    listener.setNight(night.getId());
                }
            }
        });

        prevYear.addClickListener((Button.ClickEvent clickEvent) -> {
            calendar.showPreviousYear();
        });
        prevMonth.addClickListener((Button.ClickEvent clickEvent) -> {
            calendar.showPreviousMonth();
        });
        nextMonth.addClickListener((Button.ClickEvent clickEvent) -> {
            calendar.showNextMonth();
        });
        nextYear.addClickListener((Button.ClickEvent clickEvent)  -> { calendar.showNextYear(); });

        final HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent(prevYear);
        buttons.addComponent(prevMonth);
        buttons.addComponent(empty);
        buttons.addComponent(nextMonth);
        buttons.addComponent(nextYear);
        buttons.setExpandRatio(empty, 1.0f);

        final VerticalLayout vl = new VerticalLayout();
        vl.setMargin(true);
        vl.addComponent(header);
        vl.addComponent(calendar);
        vl.addComponent(buttons);
        setContent(vl);

        // set tooltips
        prevYear.setDescription("Go one year back.");
        prevMonth.setDescription("Go one month back.");
        nextMonth.setDescription("Go one month forward.");
        nextYear.setDescription("Go one year forward.");

    }

    public void setShowingDate(DateTime date) {
        calendar.setShowingDate(date.toDate());
    }

    public void addListener(DayChangeListener listener) {
        listeners.add(listener);
    }

    private SimpleLaserNight findNight(Date day) {
        // unfortunately we can not set the time zone of the calendar component
        // therefore we have to translate the day into a day of the current time zone first
        DateTime date = new DateTime(day);
        DateTime localDate = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 12, 0, currentTimeZone).withTimeAtStartOfDay();
        for (SimpleLaserNight night : laserNights) {
            DateTime dateNight = night.getStart().withZone(currentTimeZone).withTimeAtStartOfDay();
            if (localDate.equals(dateNight)) {
                return night;
            }
        }
        return null;
    }

    @Override
    public void updateTimeZone(DateTimeZone zone) {
        laserNights = getLaserNightsForCurrentMonth();
        currentTimeZone = zone;
        setShowingDate(currentDate);
        markAsDirtyRecursive();
    }

    private class MyStyleCalendar extends StyleCalendar {
        @Override
        public void setShowingDate(Date date) {
            currentDate = new DateTime(date, currentTimeZone);
            laserNights = getLaserNightsForCurrentMonth();
            header.setCaption(currentDate.toString("MMMM yyyy"));

            // TODO: check if this can be done a bit more elegantly
            // since we can not set the time zone of the component we need to fake the date we want to show by "mapping" the current date to the default time zone of the JVM
            DateTimeZone defaultZone = DateTimeZone.forTimeZone(TimeZone.getDefault());
            DateTime showDate = new DateTime(currentDate.getYear(), currentDate.getMonthOfYear(), currentDate.getDayOfMonth(), 12, 0, defaultZone);
            super.setShowingDate(showDate.toDate());
        }
    }

    private List<SimpleLaserNight> getLaserNightsForCurrentMonth() {
        DateTime d = (currentDate == null) ? DateTime.now() : currentDate;
        return laserNightService.getShortLaserNights(d.minusMonths(1), d.plusMonths(1));
    }

    private class MyDateOptionsGenerator implements DateOptionsGenerator {

        @Override
        public String getStyleName(Date date, StyleCalendar styleCalendar) {
            // days that don't belong to the shown month are displayed but not clickable
            // in order not to confuse everybody don't show any information to keep people from clicking on them
            if ((date.getMonth()+1) != currentDate.getMonthOfYear()) {
                return "inactive";
            }

            // now do the real stuff
            SimpleLaserNight night = findNight(date);
            if (night != null) {
                if (night.hasPamReceived()) {
                    return "pamreceived";
                } else if (night.hasPrmSent()) {
                    return "prmsent";
                } else {
                    return "preparation";
                }
            }
            return "inactive";
        }

        @Override
        public String getTooltip(Date date, StyleCalendar styleCalendar) {
            // days that don't belong to the shown month are displayed but not clickable
            // in order not to confuse everybody don't show any information to keep people from clicking on them
            if ((date.getMonth()+1) != currentDate.getMonthOfYear()) {
                return "";
            }

            // now do the real stuff
            SimpleLaserNight night = findNight(date);
            if (night != null) {
                String jDay = night.getStart().toDateTime(DateTimeZone.UTC).toString("D");
                if (night.hasPamReceived()) {
                    return "UTC JDay " + jDay + ": PAMs received from LCH.";
                } else if (night.hasPrmSent()) {
                    return "UTC JDay " + jDay + ": PRMs sent to LCH.";
                } else {
                    return "UTC JDay " + jDay + ": In preparation.";
                }
            }
            return "Not a laser night.";
        }

        @Override
        public boolean isDateDisabled(Date date, StyleCalendar styleCalendar) {
            return false;
        }

    }

    public interface DayChangeListener {
        void setNight(Long id);
    }
}
