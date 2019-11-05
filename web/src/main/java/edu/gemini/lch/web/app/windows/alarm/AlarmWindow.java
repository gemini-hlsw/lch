package edu.gemini.lch.web.app.windows.alarm;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import edu.gemini.lch.model.*;
import edu.gemini.lch.services.*;
import edu.gemini.lch.web.app.components.*;
import edu.gemini.lch.web.app.util.CoordFormatter;
import edu.gemini.lch.web.app.util.TimeFormatter;
import edu.gemini.shared.skycalc.Angle;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Configurable(preConstruction = true)
@PreserveOnRefresh
public final class AlarmWindow extends VerticalLayout implements View, SchedulerService.AlarmUpdateListener, TimeZoneSelector.Listener {

    private static final Logger LOGGER = Logger.getLogger(AlarmWindow.class);

    public static final String NAME = "alarms";

    private AlarmService.Snapshot lastSnapshot;

    private final TimeZoneSelector timeZoneSelector;

    private TextField timeUtc;
    private TextField timeLocal;
    private TextField currentAz;
    private TextField currentEl;
    private TextField currentRa;
    private TextField currentDec;
    private TextField demandRa;
    private TextField demandDec;
    private TextField nearestApprovedC1;
    private TextField nearestApprovedC2;
    private TextField distance;
    private TextField maxDistance;

    private StatusPanel autoShutterPanel;
    private StatusPanel laserStatusPanel;
    private StatusPanel propagationStatusPanel;
    private StatusPanel beamCollisionStatusPanel;
    private StatusPanel elevationLimitStatusPanel;
    private StatusPanel errorConePositionPanel;

    private Timeline timeline;
    private WindowsTable.Propagation propagationWindowsTable;
    private WindowsTable.Shuttering shutteringWindowTable;
    private InformationTable informationTable;
    private ObservationTable observationTable;
    private LabeledComponent propWinLabelled;
    private LabeledComponent shutWinLabelled;
    private LabeledComponent infoTabLabelled;
    private LabeledComponent obseTabLabelled;

    private final AudioAlarm audio;
    private final TimeFormatter utcTimeFormatter;
    private TimeFormatter timeFormatter;

    @Autowired
    private SiteService siteService;

    @Autowired
    private EpicsService epicsService;

    @Autowired
    private AlarmService alarmService;

    public AlarmWindow() {

        // audio for audible alarms
        audio = new AudioAlarm();

        // time zone selector
        timeZoneSelector = new TimeZoneSelector();
        timeZoneSelector.addListener(this);
        // time formatters for UTC and whatever is currently selected (UTC/local)
        utcTimeFormatter = new TimeFormatter(ZoneId.of("UTC"));
        timeFormatter = new TimeFormatter(timeZoneSelector.getSelectedZone());

        Header header = new AlarmWindowHeader(this);
        Footer footer = new Footer();

        addComponent(header.getComponent());
        addComponent(createPanels());
        addComponent(createPositions());
        addComponent(createTimeline());
        addComponent(createWindowTables());
        addComponent(createInformationTable());
        addComponent(createObservationTable());
        addComponent(footer.getComponent());
        addComponent(audio);

    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent e) {
        UI.getCurrent().getPage().setTitle("LTTS Alarms");
    }

    private Component createTimeline() {
        timeline = new Timeline();
        timeZoneSelector.addListener(timeline);
        return new LabeledComponent("Timeline", timeline);
    }

    private ComponentContainer createPanels() {
        laserStatusPanel = new StatusPanel.Laser();
        autoShutterPanel = new StatusPanel.AutoShutter();
        propagationStatusPanel = new StatusPanel.Propagation();
        beamCollisionStatusPanel = new StatusPanel.BeamCollision();
        elevationLimitStatusPanel = new StatusPanel.ElevationLimit();
        errorConePositionPanel = new StatusPanel.ErrorConePosition();

        final HorizontalLayout hl = new HorizontalLayout();
        hl.setSizeFull();
        hl.addComponent(new LabeledComponent("Laser Status", laserStatusPanel));
        hl.addComponent(new LabeledComponent("Auto Shutter", autoShutterPanel));
        hl.addComponent(new LabeledComponent("Propagation Window", propagationStatusPanel));
        hl.addComponent(new LabeledComponent("Beam Collision (LTCS)", beamCollisionStatusPanel));
        hl.addComponent(new LabeledComponent("LGS Elevation Limit", elevationLimitStatusPanel));
        hl.addComponent(new LabeledComponent("Cone Limit", errorConePositionPanel));
        return hl;
    }

    private ComponentContainer createPositions() {

        final VerticalLayout time = new VerticalLayout();
        timeUtc = new TextField("UTC");
        timeLocal = new TextField("Local");
        time.addComponent(timeUtc);
        time.addComponent(timeLocal);
        time.addComponent(timeZoneSelector);
        time.setSpacing(true);
        time.setSizeFull();

        final VerticalLayout telescopeDemand = new VerticalLayout();
        demandRa = new TextField("RA");
        demandDec = new TextField("Dec");
        telescopeDemand.addComponent(demandRa);
        telescopeDemand.addComponent(demandDec);
        telescopeDemand.setSpacing(true);
        telescopeDemand.setSizeFull();

        final VerticalLayout telescopeCurrentAzEl = new VerticalLayout();
        currentAz = new TextField("Azimuth");
        currentEl = new TextField("Elevation");
        telescopeCurrentAzEl.addComponent(currentAz);
        telescopeCurrentAzEl.addComponent(currentEl);
        telescopeCurrentAzEl.setSpacing(true);
        telescopeCurrentAzEl.setSizeFull();

        final VerticalLayout telescopeCurrentRaDec = new VerticalLayout();
        currentRa = new TextField("RA");
        currentDec = new TextField("Dec");
        telescopeCurrentRaDec.addComponent(currentRa);
        telescopeCurrentRaDec.addComponent(currentDec);
        telescopeCurrentRaDec.setSpacing(true);
        telescopeCurrentRaDec.setSizeFull();

        final VerticalLayout nearest = new VerticalLayout();
        nearestApprovedC1 = new TextField("RA");
        nearestApprovedC2 = new TextField("Dec");
        nearest.addComponent(nearestApprovedC1);
        nearest.addComponent(nearestApprovedC2);
        nearest.setSpacing(true);
        nearest.setSizeFull();

        final VerticalLayout distances = new VerticalLayout();
        distance = new TextField("From Approved Position");
        maxDistance = new TextField("Max. Distance Allowed");
        distances.addComponent(distance);
        distances.addComponent(maxDistance);
        distances.setSpacing(true);
        distances.setSizeFull();

        final HorizontalLayout layout = new HorizontalLayout();
        layout.addComponent(new LabeledComponent("Time", time));
        layout.addComponent(new LabeledComponent("Current Az/El", telescopeCurrentAzEl));
        layout.addComponent(new LabeledComponent("Current Ra/Dec", telescopeCurrentRaDec));
        layout.addComponent(new LabeledComponent("Demand Ra/Dec", telescopeDemand));
        layout.addComponent(new LabeledComponent("Nearest Approved Position", nearest));
        layout.addComponent(new LabeledComponent("Distances", distances));
        layout.setSizeFull();

        return layout;
    }

    private ComponentContainer createWindowTables() {
        propagationWindowsTable = new WindowsTable.Propagation(timeZoneSelector.getSelectedZone());
        shutteringWindowTable = new WindowsTable.Shuttering(timeZoneSelector.getSelectedZone());

        timeZoneSelector.addListener((TimeZoneSelector.Listener)propagationWindowsTable);
        timeZoneSelector.addListener((TimeZoneSelector.Listener)shutteringWindowTable);

        propWinLabelled = new LabeledComponent("Propagation Windows", propagationWindowsTable);
        shutWinLabelled = new LabeledComponent("Shuttering Windows", shutteringWindowTable);
        propWinLabelled.setSizeFull();
        shutWinLabelled.setSizeFull();

        final HorizontalLayout layout = new HorizontalLayout();
        layout.setSizeFull();
        layout.addComponent(propWinLabelled);
        layout.addComponent(shutWinLabelled);
        return layout;
    }

    private ComponentContainer createInformationTable() {
        informationTable = new InformationTable();
        infoTabLabelled = new LabeledComponent("Information", informationTable);
        return infoTabLabelled;
    }

    private ComponentContainer createObservationTable() {
        observationTable = new ObservationTable();
        obseTabLabelled = new LabeledComponent("Observations in Range", observationTable);
        return obseTabLabelled;
    }

    @Override
    public void updateZoneId(final ZoneId zoneId) {
        timeFormatter = new TimeFormatter(zoneId);
    }

    @Override
    public void update(final AlarmService.Snapshot snapshot) {

        if (getUI() == null) return;

        final ZonedDateTime start = ZonedDateTime.now();

        // avoid ConcurrentModificationExceptions: changes to components that are initiated by the server
        // need to be synchronized with application object (user session)
        getUI().access(() -> {

            // update status panels
            laserStatusPanel.update(snapshot);
            autoShutterPanel.update(snapshot);
            propagationStatusPanel.update(snapshot);
            beamCollisionStatusPanel.update(snapshot);
            elevationLimitStatusPanel.update(snapshot);
            errorConePositionPanel.update(snapshot);

            // time formatter for currently active local (default) time zone
            final TimeFormatter localTimeFormatter = new TimeFormatter(DateTimeZone.getDefault());

            // update epics channel data
            final EpicsService.Snapshot epicsSnapshot = snapshot.getEpicsSnapshot();
            timeUtc.setValue(utcTimeFormatter.asDateAndTimeLong(epicsSnapshot.getTime()));
            timeLocal.setValue(localTimeFormatter.asDateAndTimeLong(epicsSnapshot.getTime()));
            currentAz.setValue(CoordFormatter.asDMS(epicsSnapshot.getCurrentAz().toDegrees().getMagnitude()));
            currentEl.setValue(CoordFormatter.asDMS(epicsSnapshot.getCurrentEl().toDegrees().getMagnitude()));
            currentRa.setValue(CoordFormatter.asHMS(epicsSnapshot.getCurrentRaDec().getRaDeg()));
            currentDec.setValue(CoordFormatter.asDMS(epicsSnapshot.getCurrentRaDec().getDecDeg()));
            demandRa.setValue(CoordFormatter.asHMS(epicsSnapshot.getDemandRaDec().getRaDeg()));
            demandDec.setValue(CoordFormatter.asDMS(epicsSnapshot.getDemandRaDec().getDecDeg()));
            maxDistance.setValue(CoordFormatter.asDMS(snapshot.getErrorCone().toDegrees().getMagnitude() / 2));

            // the following data has only to be updated if the target changed
            // (or if the propagation windows were updated)
            if (snapshot.targetHasChanged(lastSnapshot)) {

                // -- recreate & replace the whole data tables (reusing same table and only updating data leaves memory leaks)
                // Note: after the Vaadin upgrade from 6.8.10 to 6.8.13 this might no longer be needed due to a fix
                // with table memory leaks but I don't want to test this right now, feel free to clean this up if possible
                // - first remove listeners
                timeZoneSelector.removeListener((TimeZoneSelector.Listener) propagationWindowsTable);
                timeZoneSelector.removeListener((TimeZoneSelector.Listener) shutteringWindowTable);
                // - second create new tables and replace old ones
                propagationWindowsTable = new WindowsTable.Propagation(timeZoneSelector.getSelectedZone(), epicsService, snapshot);
                shutteringWindowTable = new WindowsTable.Shuttering(timeZoneSelector.getSelectedZone(), epicsService, snapshot);
                propWinLabelled.replaceComponent(propagationWindowsTable);
                shutWinLabelled.replaceComponent(shutteringWindowTable);
                // - then add new tables to listeners
                timeZoneSelector.addListener((TimeZoneSelector.Listener) propagationWindowsTable);
                timeZoneSelector.addListener((TimeZoneSelector.Listener) shutteringWindowTable);

                // -- recreate & replace the whole data tables (reusing same table and only updating data leaves memory leaks)
                observationTable = new ObservationTable(snapshot);
                obseTabLabelled.replaceComponent(observationTable);
            }

            // refresh the window tables in order to change coloring of cells:
            // current propagation window (green) and/or next/current shuttering window (orange/red)
            propagationWindowsTable.update(snapshot);
            shutteringWindowTable.update(snapshot);
            // update information table (error messages etc)
            informationTable.update(snapshot);

            // finally update current target info
            if (snapshot.getTarget() != null) {
                LaserTarget t = snapshot.getTarget();
                if (t instanceof RaDecLaserTarget) {
                    nearestApprovedC1.setCaption("RA");
                    nearestApprovedC1.setValue(CoordFormatter.asHMS(t.getDegrees1()));
                    nearestApprovedC2.setCaption("Dec");
                    nearestApprovedC2.setValue(CoordFormatter.asDMS(t.getDegrees2()));
                } else {
                    nearestApprovedC1.setCaption("Azimuth");
                    nearestApprovedC1.setValue(CoordFormatter.asDMS(t.getDegrees1()));
                    nearestApprovedC2.setCaption("Elevation");
                    nearestApprovedC2.setValue(CoordFormatter.asDMS(t.getDegrees2()));
                }
                distance.setValue(CoordFormatter.asDMS(snapshot.getDistance().toDegrees().getMagnitude()));

            } else {

                nearestApprovedC1.setValue("None");
                nearestApprovedC2.setValue("None");
                distance.setValue("N/A");

            }

            // update timeline and audio
            audio.update(snapshot);
            timeline.update(getUI().getPage().getBrowserWindowWidth(), snapshot);

            // set last snapshot to this one
            lastSnapshot = snapshot;
        });

        LOGGER.trace("Updated alarm window in " + new Duration(start, DateTime.now()).getMillis() + "ms");
    }

    public void playTestAudio() {
        audio.playTestAudio();
    }

    private class InformationTable extends Table {

        private final Integer MIN_PAGE_LENGTH = 5;
        private final BeanItemContainer<Message> container;

        InformationTable() {
            container = new BeanItemContainer<>(Message.class);
            setContainerDataSource(container);
            setCellStyleGenerator(new CellStyleGenerator());
            setPageLength(MIN_PAGE_LENGTH);
            setSizeFull();
        }

        InformationTable(final AlarmService.Snapshot snapshot) {
            this();
            update(snapshot);
        }

        private void update(final AlarmService.Snapshot snapshot) {
            final List<Message> messages = new ArrayList<>();

            // create messages
            if (snapshot.getNight() == null) {
                messages.add(new Message(Message.Level.INFORMATION, "Not between sunset and sunrise of a laser night."));
            } else {

                if (snapshot.getTarget() == null) {
                    messages.add(new Message(
                            Message.Level.WARNING,
                            String.format(
                                    "Currently no laser target inside error cone (%.3f\").",
                                    snapshot.getErrorCone().toArcsecs().getMagnitude())
                    ));
                }

                if (snapshot.getTarget() != null) {
                    final Angle errorCone = snapshot.getErrorCone();
                    final Double distance = snapshot.getDistance().convertTo(errorCone.getUnit()).getMagnitude();
                    if (distance > errorCone.getMagnitude()/2) {
                        messages.add(new Message(
                            Message.Level.WARNING,
                            String.format(
                                    "Distance from approved laser target (%.2f\") is bigger than radius of error cone (%.2f\").",
                                    snapshot.getDistance().toArcsecs().getMagnitude(),
                                    snapshot.getErrorCone().toArcsecs().getMagnitude() / 2)
                        ));
                    }
                    if (snapshot.getPropagationWindows().isEmpty()) {
                        messages.add(new Message(
                            Message.Level.WARNING,
                            "There are no propagation windows for this target."
                        ));
                    }
                }

                final DateTime earliestPropagation = snapshot.getEarliestPropagation();
                final DateTime latestPropagation = snapshot.getLatestPropagation();
                Validate.notNull(earliestPropagation);
                Validate.notNull(latestPropagation);
                if (snapshot.getEpicsSnapshot().getTime().isBefore(earliestPropagation)) {
                    messages.add(new Message(Message.Level.WARNING, "Propagation of laser is not allowed before " + timeFormatter.asTimeLong(earliestPropagation) + "."));
                }
                if (snapshot.getEpicsSnapshot().getTime().isAfter(latestPropagation)) {
                    messages.add(new Message(Message.Level.WARNING, "Propagation of laser is not allowed after " + timeFormatter.asTimeLong(latestPropagation) + "."));
                }
                if (snapshot.getNight().isTestNight()) {
                    messages.add(new Message(Message.Level.WARNING, "USING LASER NIGHT TEST DATA INSTEAD OF REAL DATA!"));
                }
            }

            // create error message etc. for LTCS status
            if (!snapshot.getLtcsSnapshot().isConnected()) {
                messages.add(new Message(Message.Level.ERROR, snapshot.getLtcsSnapshot().getMessage()));
            }

            // create informational messages for upcoming collisions
            for (final LtcsService.Collision c : snapshot.getLtcsSnapshot().getCollisions()) {
                messages.add(new Message(Message.Level.INFORMATION, getCollisionMessage(c)));
            }

            // create warning in case we are using TCS simulator
            if (epicsService.usesTcsSimulator()) {
                messages.add(new Message(Message.Level.WARNING, "USING TCS SIMULATOR INSTEAD OF ACTUAL TELESCOPE POSITIONS!"));
            }

            // show all messages sorted by their priority
            Collections.sort(messages);
            if (!messages.equals(container.getItemIds())) {
                // only update if there is actually a change!
                // updating client-side tables is very CPU intensive for the browser and should only be done if necessary
                container.removeAllItems();
                container.addAll(messages);
                setPageLength(Math.max(MIN_PAGE_LENGTH, container.size()));
            }
        }

        private String getCollisionMessage(LtcsService.Collision collision) {
            final StringBuilder message = new StringBuilder();
            message.
                    append("Collision with ").
                    append(collision.getObservatory()).
                    append(" from ").
                    append(timeFormatter.asTime(collision.getStart())).
                    append(" until ").
                    append(timeFormatter.asTime(collision.getEnd())).
                    append(", ").
                    append(collision.getPriority()).
                    append(" has priority.");
            return message.toString();
        }

        // do some layout tricks...
        private class CellStyleGenerator implements  Table.CellStyleGenerator {
            /** {@inheritDoc} */
            @Override public String getStyle(Table table, Object itemId, Object propertyId) {
                if ("level".equals(propertyId)) {
                    Message m = container.getItem(itemId).getBean();
                    switch (m.getLevel()) {
                        case WARNING: return "warn";
                        case ERROR: return "alarm";
                        default: return null;
                    }
                } else {
                    return null;
                }
            }
        }

    }

    public final class ObservationTable extends Table {

        public final class Bean {
            private final Observation observation;
            private final ObservationTarget target;
            Bean(Observation observation, ObservationTarget target) {
                this.observation = observation;
                this.target = target;
            }
            public Long getId() { return target.getId(); }
            public String getObservation() { return observation.getObservationId(); }
            public String getTarget() { return target.getName(); }
            public String getType() { return target.getType(); }
            public Double getC1() { return target.getDegrees1(); }
            public Double getC2() { return target.getDegrees2(); }
        }

        private final BeanItemContainer<Bean> container;

        ObservationTable() {
            container = new BeanItemContainer<>(Bean.class);
            setContainerDataSource(container);
            setVisibleColumns("observation", "target", "type", "c1", "c2");
            setSortContainerPropertyId("id");
            setSizeFull();
        }

        ObservationTable(final AlarmService.Snapshot snapshot) {
            this();
            update(snapshot);
        }

        private void update(final AlarmService.Snapshot snapshot) {
            for (Observation o : snapshot.getObservations()) {
                for (ObservationTarget t : o.getTargets()) {
                    container.addBean(new Bean(o, t));
                }
            }
            setPageLength(Math.max(5, container.size()));
            sort();
        }
    }

    /**
     * Audio alarm component which plays a wav file on the client.
     * This component also contains all the logic that deals with finding pending alarms and making sure
     * that no two alarms are played at the same time (which causes the first alarm to be cut off by the
     * second one).
     */
    public final class AudioAlarm extends AudioElement {
        // max duration of an audio alarm in seconds
        // Longest one is "Collision in x minutes. Gemini has Priority." with roughly 6 seconds.
        private static final long MAX_ALARM_DURATION = 6;

        private final Set<String> alarmsPending;
        private final Map<String, DateTime> alarmsGiven;
        public AudioAlarm() {
            setShowControls(false);
            alarmsPending = new HashSet<>();
            alarmsGiven = new HashMap<>();
        }

        /**
         * Updates the alarms.
         * Collects all different alarm types, prioritizes them by their time (next one has highest priority)
         * and - if there are pending alarms - plays the one with the highest priority.
         * @param snapshot
         */
        public void update(final AlarmService.Snapshot snapshot) {
            // -- no night, no target, no alarms
            if (snapshot.getNight() == null || snapshot.getTarget() == null) {
                return;
            }

            // -- collect all currently pending alarms
            collectClearToPropagateAlarms(snapshot);
            collectNoPropagationWindowsAlarms(snapshot);
            collectPropagationAlarms(snapshot);
            collectCollisionAlarms(snapshot);
            collectElevationAlarms(snapshot);
            // -- ok, now go ahead and play alarms
            playNextAlarm();
        }

        /**
         * Collects pending clear to propagate alarms.
         * @param snapshot
         */
        private void collectClearToPropagateAlarms(final AlarmService.Snapshot snapshot) {
            if (lastSnapshot == null) return;
            // if auto shutter changed from "not clear" to "clear" play audible alarm
            if (!(lastSnapshot.getAutoShutter() == AlarmService.AutoShutter.CLEAR) &&
                 (snapshot.getAutoShutter() == AlarmService.AutoShutter.CLEAR)) {
                addPendingAlarm("ClearToPropagate.wav", 0, 0);
            }
        }

        /**
         * Collects no propagation windows alarms.
         * @param snapshot
         */
        private void collectNoPropagationWindowsAlarms(final AlarmService.Snapshot snapshot) {
            // check if we are on a new target without propagation windows
            if (snapshot.targetHasChanged(lastSnapshot) && snapshot.getPropagationWindows().isEmpty()) {
                addPendingAlarm("NoPropagationWindows.wav" , 0, 0);
            }
        }

        /**
         * Collects pending propagation alarms.
         * @param snapshot
         */
        private void collectPropagationAlarms(final AlarmService.Snapshot snapshot) {
            // if laser is not on sky we don't want to play propagation alarms
            if (!snapshot.getEpicsSnapshot().isOnSky()) {
                return;
            }

            // check for duration to next warning limit and play audible alarm if appropriate
            final DateTime now = snapshot.getEpicsSnapshot().getTime();
            for (PropagationWindow w : snapshot.getPropagationWindows()) {
                if (w.contains(now)) {
                    long secondsToGo = new Duration(now, w.getEnd()).getStandardSeconds();
                    addPendingAlarm("ShutteringChirp00.wav", 0, secondsToGo);
                    addPendingAlarm("ShutteringChirp05.wav", 5, secondsToGo);
                    addPendingAlarm("ShutteringChirp10.wav", 10, secondsToGo);
                    addPendingAlarm("ShutteringChirp15.wav", 15, secondsToGo);
                    addPendingAlarm("ShutteringChirp20.wav", 20, secondsToGo);
                    addPendingAlarm("ShutteringChirp25.wav", 25, secondsToGo);
                    addPendingAlarm("ShutteringChirp30.wav", 30, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn01.wav", 60, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn02.wav", 120, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn03.wav", 180, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn05.wav", 300, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn10.wav", 600, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn15.wav", 900, secondsToGo);
                    addPendingAlarm("ShutteringWindowIn30.wav", 1800, secondsToGo);
                    break;
                }
            }
        }

        /**
         * Collects all pending LTCS collision alarms.
         * The start and end time of the predicted collisions can fluctuate by a few second between subsequent calls.
         * Since the {@link #addPendingAlarm(String, long, long)} allows for a few seconds of deviation and we
         * make sure that the same alarm is not added to the pending list more than once every 15 seconds that
         * does not matter.
         * @param snapshot
         */
        private void collectCollisionAlarms(final AlarmService.Snapshot snapshot) {
            // if laser is not on sky we don't want to play collision alarms
            if (!snapshot.getEpicsSnapshot().isOnSky()) {
                return;
            }

            // check for next collision alarm
            final List<LtcsService.Collision> collisions = snapshot.getLtcsSnapshot().getCollisions();
            if (!collisions.isEmpty()) {
                final LtcsService.Collision nextCollision = collisions.get(0); // list is sorted by time
                final DateTime now = snapshot.getEpicsSnapshot().getTime();
                final long secondsToGo = new Duration(now, nextCollision.getStart()).getStandardSeconds();
                if (nextCollision.geminiHasPriority()) {
                    addPendingAlarm("CollisionIn01Gemini.wav", 60, secondsToGo);
                    addPendingAlarm("CollisionIn02Gemini.wav", 120, secondsToGo);
                    addPendingAlarm("CollisionIn03Gemini.wav", 180, secondsToGo);
                    addPendingAlarm("CollisionIn05Gemini.wav", 300, secondsToGo);
                    addPendingAlarm("CollisionIn10Gemini.wav", 600, secondsToGo);
                    addPendingAlarm("CollisionIn15Gemini.wav", 900, secondsToGo);
                    addPendingAlarm("CollisionIn30Gemini.wav", 1800, secondsToGo);
                } else {
                    addPendingAlarm("CollisionIn01.wav", 60, secondsToGo);
                    addPendingAlarm("CollisionIn02.wav", 120, secondsToGo);
                    addPendingAlarm("CollisionIn03.wav", 180, secondsToGo);
                    addPendingAlarm("CollisionIn05.wav", 300, secondsToGo);
                    addPendingAlarm("CollisionIn10.wav", 600, secondsToGo);
                    addPendingAlarm("CollisionIn15.wav", 900, secondsToGo);
                    addPendingAlarm("CollisionIn30.wav", 1800, secondsToGo);
                }
            }
        }

        /**
         * Collects pending elevation alarms.
         * @param snapshot
         */
        private void collectElevationAlarms(final AlarmService.Snapshot snapshot) {
            final LaserNight night = snapshot.getNight();
            final LaserTarget target = snapshot.getTarget();
            final DateTime now = snapshot.getEpicsSnapshot().getTime();
            final List<Interval> intervals = target.getVisibility().getVisibleIntervalsAboveLimitDuring(night);

            // check if we are inside an interval
            for (final Interval i : intervals) {
                if (i.contains(now)) {
                    final long secondsToGo = new Duration(now, i.getEnd()).getStandardSeconds();
                    addPendingAlarm("ElevationLimitReached.wav", 0, secondsToGo);
                    //collectPendingAlarms("ElevationLimitIn01.wav", 60, secondsToGo);
                    //collectPendingAlarms("ElevationLimitIn02.wav", 120, secondsToGo);
                    //collectPendingAlarms("ElevationLimitIn03.wav", 180, secondsToGo);
                    addPendingAlarm("ElevationLimitIn05.wav", 300, secondsToGo);
                    addPendingAlarm("ElevationLimitIn10.wav", 600, secondsToGo);
                    addPendingAlarm("ElevationLimitIn15.wav", 900, secondsToGo);
                    addPendingAlarm("ElevationLimitIn30.wav", 1800, secondsToGo);
                    break;
                }
            }
        }

        /**
         * Adds an alarm that is due to the set of pending alarms.
         * Since this method can be called several times a second we have to make sure we don't give the same
         * alarm several times. To avoid this, the same alarm is only given once every 15 seconds.
         * @param name
         * @param alarmSeconds
         * @param secondsToGo
         */
        private void addPendingAlarm(final String name, final long alarmSeconds, final long secondsToGo) {
            // check if alarm is due, leave a few seconds room for delays etc
            if (secondsToGo >= (alarmSeconds -  3) && secondsToGo <= alarmSeconds) {
                // make sure alarms are not played twice in short sequence
                if (!hasRecentlyPlayed(name)) {
                    alarmsPending.add(name);
                }
            }
        }

        /**
         * Plays the next alarm by setting the source of the embedded object to the appropriate WAV file name.
         * Note that theoretically it is possible that we are playing an alarm (or start playing an alarm) for a
         * target *after* we already changed to a new target. This all depends on timing and is difficult to keep
         * from happening entirely (especially the case where we change the target while an alarm is playing).
         * Because this won't happen too often though I don't bother implementing a more sophisticated
         * mechanism to deal with these cases and keep it as simple as possible knowing the solution here is not
         * perfect. Remember also that having more than one alarm at the same time is an exception, not the rule.
         */
        private void playNextAlarm() {
            // if no alarms are pending we are done
            if (alarmsPending.isEmpty()) {
                return;
            }

            // get first of the pending alarms (arbitrary order)
            // since this should not happen very often, we don't put effort into prioritizing alarms here
            final String alarm = alarmsPending.iterator().next();

            // Avoid playing two alarms at the same time; wait for one alarm to end before starting another one.
            // Exception are chirps (30 seconds til shuttering window) which are assumed to be always highest priority,
            // they are played anyway even if cutting off another alarm by doing so.
            if (!alarm.startsWith("ShutteringChirp") && isPlaying()) {
                return;
            }

            // remove alarm from pending alarms, store time we are starting to play it, set source and repaint
            // to make client reflect the change in the embedded component (new wav file)
            alarmsPending.remove(alarm);
            alarmsGiven.put(alarm, DateTime.now());
            LOGGER.trace("play alarm " + alarm);
            setSource(new ThemeResource("alarms/" + alarm));
            play();
        }

        /**
         * Checks if an alarm audio file is still playing, assuming that no alarm audio file takes longer
         * than a given number of seconds to play.
         * @return
         */
        private boolean isPlaying() {
            for (DateTime alarmStarted : alarmsGiven.values()) {
                final Duration duration = new Duration(alarmStarted, DateTime.now());
                if (duration.getStandardSeconds() <= MAX_ALARM_DURATION) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks if an alarm has been played recently.
         * @return
         */
        private boolean hasRecentlyPlayed(final String alarm) {
            if (alarmsGiven.containsKey(alarm)) {
                final Duration duration = new Duration(alarmsGiven.get(alarm), DateTime.now());
                if (duration.getStandardSeconds() <= 15) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * Helper class for messages in the info table.
     */
    public static final class Message implements Comparable<Message> {
        public enum Level {
            INFORMATION,
            WARNING,
            ERROR
        }
        private final Level level;
        private final String message;
        protected Message(final Level level, final String message) {
            this.level = level;
            this.message = message;
        }

        /** Gets the level. */
        public Level getLevel() { return level; }

        /** Gets the message. */
        public String getMessage() { return message; }

        /**
         * Order messages according to their priority (level) and alphabetically in case of equal priority.
         * @param other
         * @return
         */
        @Override public int compareTo(final Message other) {
            if (level != other.level) {
                return other.level.ordinal() - this.level.ordinal();
            } else {
                return message.compareTo(other.message);
            }
        }

        @Override public boolean equals(final Object object) {
            if (object == null) return false;
            if (!(object instanceof Message)) return false;
            final Message other = (Message) object;
            return (level == other.level) && message.equals(other.message);
        }

    }

}
