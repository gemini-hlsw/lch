package edu.gemini.lch.web.app.windows.night;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.model.LaserTarget;
import edu.gemini.lch.model.Observation;
import edu.gemini.lch.services.ModelFactory;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import edu.gemini.lch.web.app.util.TimeFormatter;
import jsky.plot.SunRiseSet;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/**
 * A panel that shows sunrise, sunset and twilight times for the night.
 */
public class InfoPanel extends Panel implements TimeZoneSelector.Listener {

    private final Table infoTable;
    private Optional<LaserNight> night;
    private TimeFormatter timeFormatter;

    public InfoPanel() {

        infoTable = new Table();
        infoTable.setSizeFull();
        infoTable.setSortContainerPropertyId("priority");

        night = Optional.empty();
        timeFormatter = new TimeFormatter(ZoneId.of("UTC"));

        final Label header = new Label("Info");
        header.setStyleName(Reindeer.LABEL_H2);

        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(header);
        view.addComponent(infoTable);
        setContent(view);
    }

    public void update(final LaserNight night) {
        this.night = Optional.of(night);

        final SunRiseSet sunCalc = ModelFactory.createSunCalculator(night);

        final BeanContainer<String, NameValueBean> infoContainer = new BeanContainer<>(NameValueBean.class);
        infoContainer.setBeanIdProperty("name");
        infoContainer.addAll(getTimeBeans(sunCalc));
        infoContainer.addAll(getInfoBeans(night));

        infoTable.setContainerDataSource(infoContainer);
        infoTable.setPageLength(Math.min(7, infoContainer.size()));
        infoTable.setVisibleColumns("type", "name", "value");
        infoTable.setColumnWidth("type", 25);
        infoTable.setColumnWidth("name", 150);
        infoTable.sort();
    }

    @Override
    public void updateZoneId(final ZoneId zone) {
        // update the time formatter and recalculate all table values..
        timeFormatter = new TimeFormatter(zone);
        night.ifPresent(this::update);
    }

    private List<NameValueBean> getTimeBeans(final SunRiseSet sunCalc) {
        final List<NameValueBean> beans = new ArrayList<>();
        // JSKY: Must use Date
        beans.add(new NameValueBean("Sunset/Sunrise", getTimeInfo(sunCalc.getSunset().toInstant(), sunCalc.getSunrise().toInstant())));
        beans.add(new NameValueBean("Civil Twilight", getTimeInfo(sunCalc.getCivilTwilightStart().toInstant(), sunCalc.getCivilTwilightEnd().toInstant())));
        beans.add(new NameValueBean("Nautical Twilight", getTimeInfo(sunCalc.getNauticalTwilightStart().toInstant(), sunCalc.getNauticalTwilightEnd().toInstant())));
        beans.add(new NameValueBean("Astronomical Twilight", getTimeInfo(sunCalc.getAstronomicalTwilightStart().toInstant(), sunCalc.getAstronomicalTwilightEnd().toInstant())));
        return beans;
    }

    private List<NameValueBean> getInfoBeans(final LaserNight night) {
        final List<NameValueBean> beans = new ArrayList<>();

        // general information
        beans.add(new NameValueBean("Observations", Integer.toString(night.getObservations().size())));
        beans.add(new NameValueBean("Laser Targets", Integer.toString(night.getLaserTargets().size())));

        // warn if we have uncovered observations (i.e. untransmitted laser targets) after PRMs have been sent to LCH
        if (night.hasPrmSent()) {
            final Set<Observation> uncoveredObservations = night.getUncoveredObservations();
            if (uncoveredObservations.size() > 0) {
                Set<LaserTarget> untransmittedLaserTargets = night.getUntransmittedLaserTargets();
                beans.add(new NameValueBean(NameValueBean.Type.WARNING, "Uncovered Observations", getObsNamesString(uncoveredObservations)));
                beans.add(new NameValueBean(NameValueBean.Type.WARNING, "Unsent Laser Targets", Integer.toString(untransmittedLaserTargets.size())));
            }
        }

        // warn for observations that have no windows after PAMs have been received from LCH
        if (night.hasPamReceived()) {
            final Set<Observation> observationsWithoutWindows = night.getObservationsWithoutWindows();
            if (observationsWithoutWindows.size() > 0) {
                Set<LaserTarget> laserTargetsWithoutWindows = night.getLaserTargetsWithoutWindows();
                beans.add(new NameValueBean(NameValueBean.Type.WARNING, "Observations w/o Windows", getObsNamesString(observationsWithoutWindows)));
                beans.add(new NameValueBean(NameValueBean.Type.WARNING, "Laser Targets w/o Windows", Integer.toString(laserTargetsWithoutWindows.size())));
            }
        }

        return beans;
    }

    private String getObsNamesString(final Set<Observation> observations) {
        final int maxNames = 3;

        final StringBuilder names = new StringBuilder();
        Iterator<Observation> iter = observations.iterator();
        for (int i = 0; i < Math.min(maxNames, observations.size()); i++) {
            if (i > 0) { names.append(", "); }
            names.append(iter.next().getObservationId());
        }
        if (observations.size() > maxNames) {
            names.append(", ... (and ");
            names.append(observations.size() - maxNames);
            names.append(" more)");
        }

        return names.toString();
    }

    private String getTimeInfo(final Instant start, final Instant end) {
        return new StringBuilder().
                append(timeFormatter.asTime(start)).
                append(" - ").
                append(timeFormatter.asTime(end)).
                append(" / ").
                append(TimeFormatter.asDuration(start, end)).
                toString();
    }

    public static class NameValueBean {

        public enum Type {
            INFO,
            WARNING,
            ERROR
        }

        private final Type type;
        private final String name;
        private final String value;

        public NameValueBean(final String name, final String value) {
            this(Type.INFO, name, value);
        }
        public NameValueBean(final Type type, final String name, final String value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }
        public Embedded getType() {
            final Embedded icon;
            switch (type) {
                case INFO:      icon = new Embedded("", new ThemeResource("img/info-icon.png")); break;
                case WARNING:   icon = new Embedded("", new ThemeResource("img/warning-icon.png")); break;
                case ERROR:     icon = new Embedded("", new ThemeResource("img/error-icon.png")); break;
                default:        throw new IllegalArgumentException("unknown type");
            }
            icon.setHeight(12, Unit.PIXELS);
            return icon;
        }
        public Integer getPriority() {
            switch (type) {
                case INFO:      return 2;
                case WARNING:   return 1;
                case ERROR:     return 0;
                default:        throw new IllegalArgumentException("unknown type");
            }
        }
        public String getName() {
            return name;
        }
        public String getValue() {
            return value;
        }
    }

}
