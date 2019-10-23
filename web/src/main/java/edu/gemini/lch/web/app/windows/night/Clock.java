package edu.gemini.lch.web.app.windows.night;

import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.services.SiteService;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * A component for displaying a clock and a time zone selector.
 */
@Configurable(preConstruction = true)
public class Clock extends Panel {

    private final Label utcDate;
    private final Label utcTime;
    private final Label localDate;
    private final Label localTime;
    private final TimeZoneSelector timeZoneSelector;

    @Autowired
    private SiteService siteService;

    public Clock() {

        utcDate = new Label();
        utcDate.setContentMode(ContentMode.HTML);
        utcTime = new Label();
        utcTime.setStyleName(Reindeer.LABEL_H2);
        localDate = new Label();
        localDate.setContentMode(ContentMode.HTML);
        localTime = new Label();
        localTime.setStyleName(Reindeer.LABEL_H2);
        timeZoneSelector = new TimeZoneSelector();

        final Embedded utcFlag = new Embedded(null, new ThemeResource("img/Globe-icon.png"));
        final Embedded localFlag = getLocalFlag();

        final Label header = new Label("Time");
        header.setStyleName(Reindeer.LABEL_H2);

        final GridLayout grid = new GridLayout(2, 4);
        grid.setSpacing(true);
        grid.addComponent(utcFlag, 0, 0, 0, 1);
        grid.addComponent(localFlag, 0, 2, 0, 3);
        grid.addComponent(utcDate, 1, 0);
        grid.addComponent(utcTime, 1, 1);
        grid.addComponent(localDate, 1, 2);
        grid.addComponent(localTime, 1, 3);
        grid.setComponentAlignment(utcTime, Alignment.MIDDLE_CENTER);
        grid.setComponentAlignment(localTime, Alignment.MIDDLE_CENTER);

        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(header);
        view.addComponent(grid);
        view.addComponent(timeZoneSelector);
        setContent(view);

    }

    public void addListener(TimeZoneSelector.Listener listener) {
        timeZoneSelector.addListener(listener);
    }
    public void removeListener(TimeZoneSelector.Listener listener) {
        timeZoneSelector.removeListener(listener);
    }

    public DateTimeZone getSelectedTimeZone() {
        return timeZoneSelector.getSelectedZone();
    }

    public DateTimeZone getDeselectedTimeZone() {
        return timeZoneSelector.getSelectedZone() == DateTimeZone.UTC ? DateTimeZone.getDefault() : DateTimeZone.UTC;
    }

    public void update() {
        DateTime nowLocal = new DateTime();
        DateTime nowUTC = new DateTime(DateTimeZone.UTC);
        utcDate.setValue("<b>"+nowUTC.toString("yyyy-MM-dd (D)")+"</b>");
        utcTime.setValue(nowUTC.toString("HH:mm:ss"));
        localDate.setValue("<b>"+nowLocal.toString("yyyy-MM-dd (D)")+"</b>");
        localTime.setValue(nowLocal.toString("HH:mm:ss"));
    }

    private Embedded getLocalFlag() {
        switch(siteService.getSite()) {
            case NORTH: return new Embedded(null, new ThemeResource("img/Hawaii-Flag-icon.png"));
            case SOUTH: return new Embedded(null, new ThemeResource("img/Chile-Flag-icon.png"));
            default: throw new IllegalArgumentException("unknonw site");
        }
    }

}
