package edu.gemini.lch.web.app.windows.night;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.ScienceObservation;
import edu.gemini.lch.model.SimpleLaserNight;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.SchedulerService;
import edu.gemini.lch.web.app.components.Footer;
import edu.gemini.lch.web.app.components.Header;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.List;

/**
 */
@Configurable(preConstruction = true)
public class NightWindow extends VerticalLayout implements View, TimeZoneSelector.Listener, Calendar.DayChangeListener, SchedulerService.ClockUpdateListener {
    private static final Logger LOGGER = Logger.getLogger(NightWindow.class);

    public static final String NAME = "night";

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private LaserNightService laserNightService;

    private Label mainLabel;
    private Label minorLabel;
    private PrevNightButton prevNight;
    private NextNightButton nextNight;
    private InfoPanel infoPanel;
    private HistoryTable historyTable;


    private LaserNight night;


    // the view components
    private final ObservationTargetsTable.AzEl azElEngineeringTargetsTable;
    private final ObservationTargetsTable.RaDec raDecEngineeringTargetsTable;
    private final RaDecLaserTargetsTable raDecLaserTargetsTable;
    private final BlanketClosuresTable blanketClosuresTable;
    private final Clock clock;
    private final Calendar calendar;
    private final TabSheet tabsheet;

    public NightWindow() {
        azElEngineeringTargetsTable = new ObservationTargetsTable.AzEl();
        raDecEngineeringTargetsTable = new ObservationTargetsTable.RaDec();
        raDecLaserTargetsTable = new RaDecLaserTargetsTable();
        blanketClosuresTable = new BlanketClosuresTable(this);
        clock = new Clock();
        calendar = new Calendar();
        tabsheet = createTabSheet();

        final Header header = new NightWindowHeader(this);
        final Footer footer = new Footer();

        addComponent(header.getComponent());
        addComponent(createBody());
        addComponent(footer.getComponent());

        // register components that need to know about time zone changes
        clock.addListener(this);
        clock.addListener(historyTable);
        clock.addListener(blanketClosuresTable);
        clock.addListener(infoPanel);
        clock.addListener(raDecEngineeringTargetsTable);
        clock.addListener(azElEngineeringTargetsTable);
        clock.addListener(raDecLaserTargetsTable);
        clock.addListener(calendar);

        // register components interested in day selections from calendar
        calendar.addListener(this);
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent e) {
        UI.getCurrent().getPage().setTitle("LTTS Night");
    }

    public LaserNight getDisplayedNight() {
        return night;
    }

    public Clock getClock() {
        return clock;
    }

    private Component createBody() {
        VerticalLayout l = new VerticalLayout();
        l.setSizeFull();
        l.addComponent(createNavigator());
        l.addComponent(createMainInfo());
        return l;
    }

    public void setNight(DateTime dateTime) {
        // fully load laser night
        LaserNight night = laserNightService.loadLaserNight(dateTime);
        setNight(night);
    }

    public void setNight(Long id) {
        // fully load laser night
        LaserNight night = laserNightService.loadLaserNight(id);
        setNight(night);
    }

    private void setNight(LaserNight night) {
        if (night != null) {
            this.night = night;
            nextNight.setEnabled(true);
            prevNight.setEnabled(true);
            infoPanel.update(night);
            historyTable.updateNight(night);
            blanketClosuresTable.updateNight(night);
            azElEngineeringTargetsTable.updateNight(night);
            raDecEngineeringTargetsTable.updateNight(night);
            raDecLaserTargetsTable.updateNight(night);
            updateTabSheet(night);

            calendar.setShowingDate(night.getStart());

            // set header
            updateTimeZone(clock.getSelectedTimeZone());
        }
    }

    private Component createNavigator() {
        mainLabel = new Label();
        mainLabel.setContentMode(ContentMode.HTML);
        mainLabel.setSizeUndefined();
        mainLabel.setStyleName(Reindeer.LABEL_H1);
        mainLabel.setDescription("The date this night starts on.");
        minorLabel = new Label();
        minorLabel.setContentMode(ContentMode.HTML);
        minorLabel.setSizeUndefined();
        minorLabel.setStyleName(Reindeer.LABEL_H2);
        minorLabel.setDescription("The UTC JDay this night starts on.");

        VerticalLayout labels = new VerticalLayout();
        labels.addComponent(mainLabel);
        labels.addComponent(minorLabel);
        labels.setComponentAlignment(mainLabel, Alignment.MIDDLE_CENTER);
        labels.setComponentAlignment(minorLabel, Alignment.MIDDLE_CENTER);

        HorizontalLayout l = new HorizontalLayout();
        l.setMargin(true);
        l.setSizeFull();

        prevNight = new PrevNightButton();
        prevNight.setSizeUndefined();
        nextNight = new NextNightButton();
        nextNight.setSizeUndefined();

        l.addComponent(prevNight);
        l.addComponent(labels);
        l.addComponent(nextNight);

        l.setComponentAlignment(prevNight, Alignment.MIDDLE_LEFT);
        l.setComponentAlignment(labels, Alignment.MIDDLE_CENTER);
        l.setComponentAlignment(nextNight, Alignment.MIDDLE_RIGHT);

        return l;
    }

    private Component createMainInfo() {
        infoPanel = new InfoPanel();
        historyTable = new HistoryTable(this);

        HorizontalLayout horizontal = new HorizontalLayout();
        horizontal.setSizeFull();

        clock.setHeight("230px");
        clock.setWidth("200px");
        calendar.setHeight("230px");
        calendar.setWidth("230px");
        blanketClosuresTable.setHeight("230px");
        blanketClosuresTable.setWidth("250px");
        infoPanel.setHeight("230px");
        historyTable.setHeight("230px");

        horizontal.addComponent(clock);
        horizontal.addComponent(calendar);
        horizontal.addComponent(infoPanel);
        horizontal.addComponent(historyTable);
        horizontal.addComponent(blanketClosuresTable);

        horizontal.setExpandRatio(infoPanel, 0.25f);
        horizontal.setExpandRatio(historyTable, 0.25f);

        VerticalLayout l = new VerticalLayout();
        l.setMargin(true);
        l.addComponent(horizontal);
        l.addComponent(tabsheet);
        return l;
    }

    private TabSheet createTabSheet() {
        TabSheet tabsheet = new TabSheet();
        tabsheet.
                addTab(raDecEngineeringTargetsTable, "Ra/Dec Engineering Targets",  new ThemeResource("img/hammer-screwdriver-icon.png")).
                setDescription("All Ra/Dec engineering targets and their laser targets.");
        tabsheet.
                addTab(azElEngineeringTargetsTable, "Az/El Engineering Targets",  new ThemeResource("img/hammer-screwdriver-icon.png")).
                setDescription("All Az/El engineering targets and their laser targets.");
        tabsheet.
                addTab(raDecLaserTargetsTable, "Unique Ra/Dec Laser Targets",  new ThemeResource("img/Laser-icon.png")).
                setDescription("Overview of the laser targets covering all science and engineering targets.");

        return tabsheet;
    }

    private final List<TabSheet.Tab> tabs = new ArrayList<>();

    private void updateTabSheet(LaserNight night) {
        // remove all existing tabs
        for (TabSheet.Tab tab : tabs) {
            clock.removeListener((TimeZoneSelector.Listener) tab.getComponent());
            tabsheet.removeTab(tab);
        }
        tabs.clear();

        // add the new ones
        int position = 0;
        for (int i = 0; i < 8; i++) {                           // TODO: remove magic number 8
            String semester = night.getSemester(i - 5);         // TODO: remove magic number 5
            position = addObservationTab(semester, position);
        }
        addObservationTab("LTTS-Test", position);
    }

    private int addObservationTab(String key, int position) {
        List<ScienceObservation> observations = night.getScienceObservations(key);
        if (observations.size() > 0) {
            ObservationTargetsTable.Science table = new ObservationTargetsTable.Science(clock.getSelectedTimeZone());
            TabSheet.Tab tab = tabsheet.addTab(table, position++);
            table.updateNight(night, observations);
            clock.addListener(table);
            tab.setCaption("Science Targets " + key);
            tab.setDescription("All science targets for Semester " + key + " and their laser targets.");
            tab.setIcon(new ThemeResource("img/star-2-icon.png"));
            tabsheet.setSelectedTab(tab.getComponent());
            tabs.add(tab);
        }
        return position;
    }

    public void initNight() {
        initNight(DateTime.now());
    }

    public void initNight(DateTime day) {

        SimpleLaserNight night = laserNightService.getLaserNight(day);
        if (night == null) {
            night = laserNightService.getNextLaserNight(day);
        }
        if (night == null) {
            night = laserNightService.getPreviousLaserNight(day);
        }

        // set the data / update all labels, containers etc.
        if (night != null) {
            setNight(night.getId());
        } else {
            // no nights, database is empty
            mainLabel.setValue("EMPTY DATABASE!");
            minorLabel.setValue("");
        }
    }

    @Override
    public void update() {
        // avoid ConcurrentModificationExceptions: changes to components that are initiated by the server
        // need to be synchronized with application object (user session)
//        synchronized (getApplication()) {
            clock.update();
//        }
    }

    @Override
    public void updateTimeZone(DateTimeZone zone) {
        if (night != null) {
            final String startMonth = night.getStart().toString("MMMM");
            final String startYear  = night.getStart().toString("yyyy");
            final String endMonth   = night.getEnd().toString("MMMM");
            final String endYear    = night.getEnd().toString("yyyy");

            final StringBuilder startDate = new StringBuilder();
            startDate.append(startMonth);
            startDate.append(" ");
            startDate.append(night.getStart().toString("dd"));
            if (!startYear.equals(endYear)) {
                startDate.append(", ");
                startDate.append(startYear);
            }

            final StringBuilder endDate = new StringBuilder();
            if (!startMonth.equals(endMonth)) {
                endDate.append(endMonth);
                endDate.append(" ");
            }
            endDate.append(night.getEnd().toString("dd"));
            endDate.append(", ");
            endDate.append(endYear);

            final StringBuilder mainLabel = new StringBuilder();
            mainLabel.append(night.getSite().getSiteName()).
                append(", ").
                append(startDate).
                append(" &#8594; ").                // unicode right arrow
                append(endDate);

            final DateTime startUtc = night.getStart().toDateTime(DateTimeZone.UTC);
            final DateTime endUtc   = night.getEnd().  toDateTime(DateTimeZone.UTC);
            final String jDayStart  = startUtc.toString("D");
            final String jDayEnd    = endUtc.toString("D");
            final StringBuilder minorLabel = new StringBuilder();
            minorLabel.append("UTC: ").
                append(startUtc.toString("MMMM dd")).
                append(" (").
                append(jDayStart).
                append(")");
            if (!jDayStart.equals(jDayEnd)) {
                minorLabel.append(" &#8594; ").     // unicode right arrow
                    append(endUtc.toString("MMMM dd")).
                    append(" (").
                    append(jDayEnd).
                    append(")");
            }

            this.mainLabel.setValue(mainLabel.toString());
            this.minorLabel.setValue(minorLabel.toString());
        }
    }

    private class NextNightButton extends CustomComponent implements Button.ClickListener {
        final Button button;
        public NextNightButton() {
            button = new Button("Next Night");
            button.addClickListener(this);
            setCompositionRoot(button);
        }
        public void buttonClick (Button.ClickEvent event) {
            if (night == null) {
                return;
            }
            SimpleLaserNight nextNight = laserNightService.getNextLaserNight(night.getEnd());
            if (nextNight == null) {
                button.setEnabled(false);
            } else {
                setNight(nextNight.getId());
            }
        }
        public void setEnabled(boolean enabled) {
            button.setEnabled(enabled);
        }
    }

    private class PrevNightButton extends CustomComponent implements Button.ClickListener {
        final Button button;
        public PrevNightButton() {
            button = new Button("Previous Night");
            button.addClickListener(this);
            setCompositionRoot(button);
        }
        public void buttonClick (Button.ClickEvent event) {
            if (night == null) {
                return;
            }
            SimpleLaserNight prevNight = laserNightService.getPreviousLaserNight(night.getStart());
            if (prevNight == null) {
                button.setEnabled(false);
            } else {
                setNight(prevNight.getId());
            }
        }
        public void setEnabled(boolean enabled) {
            button.setEnabled(enabled);
        }
    }

}
