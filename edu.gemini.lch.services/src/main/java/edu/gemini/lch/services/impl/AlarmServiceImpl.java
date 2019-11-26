package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.*;
import edu.gemini.lch.services.*;
import edu.gemini.shared.skycalc.Angle;
import jsky.coords.WorldCoords;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * This service provides a snapshot of all relevant information for alarm clients which is updated every second.
 * The idea is that in order to reduce the amount of load a large number of alarm clients potentially puts on
 * the server the current alarm state is centrally updated asynchronously and then all clients just display the
 * most recent state.
 */
@Service
public class AlarmServiceImpl implements AlarmService {

    private static final Logger LOGGER = Logger.getLogger(AlarmServiceImpl.class);

    @Resource
    private ConfigurationService configService;

    @Resource
    private EpicsService epicsService;

    @Resource
    private LaserNightService nightService;

    @Resource
    private LaserTargetsService targetsService;

    @Resource
    private LtcsService ltcsService;

    @Resource
    private SiteService siteService;

    private Integer bufferBefore;
    private Integer bufferAfter;
    private ZonedDateTime earliestPropagation;
    private ZonedDateTime latestPropagation;
    private Angle errorCone;
    private LaserNight currentNight;
    private Snapshot currentStatus;
    private AutoShutter currentAutoShutter;

    /**
     * Initializes the alarm service.
     * Sets all status information to sensible defaults until they are updated for the first time with
     * real data.
     */
    @PostConstruct
    private void init() {
        this.bufferBefore = 10;
        this.bufferAfter = 10;
        this.errorCone = new Angle(360.0, Angle.Unit.ARCSECS);
        this.earliestPropagation = ZonedDateTime.now();
        this.latestPropagation = earliestPropagation;
        this.currentAutoShutter = getStartupAutoShutter();
        this.currentStatus =
                new Snapshot(
                        null,                                   // laser night (none)
                        null,                                   // laser target (none)
                        null,                                   // earliest propagation (none)
                        null,                                   // latest propagation (none)
                        Collections.emptyList(),                 // observations (none)
                        errorCone,                              // error cone
                        new Angle(0, Angle.Unit.ARCSECS),       // distance
                        ltcsService.getSnapshot(),              // ltcs status
                        epicsService.getSnapshot(),             // epics status
                        currentAutoShutter,                     // current auto shutter
                        bufferBefore,                           // safety buffer before
                        bufferAfter);                           // safety buffer after
    }

    /**
     * Gets the latest alarm status that has been calculated.
     */
    @Override
    public AlarmService.Snapshot getSnapshot() {
        return currentStatus;
    }

    @Override
    public Boolean isAutoShutterEnabled() {
        // any setting other than OFF means the auto shutter is currently turned on
        return currentAutoShutter != AutoShutter.OFF;
    }

    @Override
    public void setAutoShutterEnabled(boolean enabled) {
        if (enabled) {
            LOGGER.warn("AUTO SHUTTERING HAS BEEN TURNED ON!");
            currentAutoShutter = AutoShutter.SHUTTERING;
        } else {
            LOGGER.warn("AUTO SHUTTERING HAS BEEN TURNED OFF!");
            currentAutoShutter = AutoShutter.OFF;
        }
    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 60000) // update from database every minute
    @Transactional(readOnly = true)
    @Override
    public void updateFromDatabase() {
        // update all configuration values that are relevant for alarm service
        bufferBefore = configService.getInteger(Configuration.Value.LIS_BUFFER_BEFORE_SHUTTER_WINDOW);
        bufferAfter  = configService.getInteger(Configuration.Value.LIS_BUFFER_AFTER_SHUTTER_WINDOW);
        errorCone = new Angle(configService.getDouble(Configuration.Value.ERROR_CONE_ANGLE), Angle.Unit.ARCSECS);
        // update the current night (important in case we are entering or leaving a night..)
        updateNight(Boolean.FALSE);
    }

    @Override
    public void updateNight() {
        updateNight(Boolean.TRUE);
    }

    /**
     * Updates the night.
     * Has to be called whenever a change is done to the night that needs to be reflected in the alarm clients.
     */
    @Transactional(readOnly = true)
    synchronized void updateNight(Boolean forceUpdate) {
        // update the night in case we are forced to do so or we currently don't have a night or are leaving a night
        if (forceUpdate || currentNight == null || !currentNight.covers(epicsService.getTime())) {
            SimpleLaserNight night = nightService.getShortLaserNightCovering(epicsService.getTime());
            if (night != null) {
                this.currentNight = nightService.loadLaserNight(night.getId());
                this.earliestPropagation = nightService.getEarliestPropagation(currentNight);
                this.latestPropagation = nightService.getLatestPropagation(currentNight);
            } else {
                this.currentNight = null;
                this.earliestPropagation = ZonedDateTime.now();
                this.latestPropagation = earliestPropagation;
            }
        }
    }


    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 500)
    @Transactional(readOnly = true)
    @Override
    public void update() {

        // make sure we use the same night everywhere, NOTE: currentNight might be updated anytime
        LaserNight night = currentNight;

        // get snapshots of current ltcs and epics values
        // this is to make sure that all calculations use the same values and we end up displaying the
        // values to the users which have actually been used for the calculations...
        LtcsService.Snapshot ltcsSnapshot = ltcsService.getSnapshot();
        EpicsService.Snapshot epicsSnapshot = epicsService.getSnapshot();

        // do the work..
        LaserTarget target = null;
        Angle distance = new Angle(0, Angle.Unit.ARCSECS);

        if (night != null) {

            Double currentRa = epicsSnapshot.getCurrentRaDec().getRaDeg();
            Double currentDec = epicsSnapshot.getCurrentRaDec().getDecDeg();
            Double currentAz = epicsSnapshot.getCurrentAz().toDegrees().getMagnitude();
            Double currentEl = epicsSnapshot.getCurrentEl().toDegrees().getMagnitude();
            Double maxDistanceDeg = errorCone.toDegrees().getMagnitude()/2;

            LaserTarget raDec = night.findClosestRaDecTarget(currentRa, currentDec, maxDistanceDeg);
            LaserTarget azEl  = night.findClosestAzElTarget(currentAz, currentEl, maxDistanceDeg);

            if (raDec == null && azEl != null) {
                target = azEl;

            } else if (raDec != null && azEl == null) {
                target = raDec;

            } else if (raDec != null && azEl != null) {
                Double raDecDistance = raDec.distanceTo(currentRa, currentDec);
                Double azElDistance = azEl.distanceTo(currentAz, currentEl);

                if (raDecDistance < azElDistance) {
                    target = raDec;
                } else {
                    target = azEl;
                }
            }

            if (target != null) {
                distance = getDistance(
                        target,
                        epicsSnapshot.getCurrentAz(),
                        epicsSnapshot.getCurrentEl(),
                        epicsSnapshot.getCurrentRaDec());
            }
        }

        // load observations for target (if we have a target)
        List<Observation> observations = Collections.emptyList();
        if (target != null) {
            observations = night.findObservationsForTarget(target);
        }

        // update current status with most recent values
        currentStatus = new Snapshot(
                night,
                target,
                earliestPropagation,
                latestPropagation,
                observations,
                errorCone,
                distance,
                ltcsSnapshot,
                epicsSnapshot,
                currentAutoShutter,
                bufferBefore,
                bufferAfter
            );

    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 500)
    @Override
    public void forceShutter() {
        if (currentAutoShutter == AutoShutter.SHUTTERED) {
            epicsService.forceShutter();
        }
    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 200)
    @Override
    public void updateAutoShutter() {

        // don't do anything if auto shutter is turned off
        if (currentAutoShutter == AutoShutter.OFF) {
            return;
        }
        // set inactive if we're not inside of the start and end time of a night
        LaserNight night = getSnapshot().getNight();
        if (night == null || epicsService.getTime().isBefore(night.getStart()) || epicsService.getTime().isAfter(night.getEnd())) {
            currentAutoShutter = AutoShutter.INACTIVE;
            return;
        }

        // shutter laser if it is on sky and we are outside of all propagation windows!
        currentAutoShutter = getCurrentAutoShutter();
        // current auto shutter status is now visible, we can wait for the shutter laser op to finish..
        if (currentAutoShutter == AutoShutter.SHUTTERING) {
            LOGGER.warn(">>> LASER IS ON SKY AND WE ARE NOT CLEAR TO PROPAGATE -> SHUTTERING LASER <<<<");
            LOGGER.warn(getDetailedShutteringLog());
            // TODO: here we should check if not another command is already running? check EPICS channel..
            // Potentially the waiting etc should be part of the openLoops and shutterLaser methods since
            // they can be different between GN and GS
            epicsService.openLoops();
            epicsService.shutterLaser();
            // TODO: here we should wait for command to finish.. check EPICS channel..
            // As a shortcut we just wait for 2 seconds..
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // Intentionally left blank
            }
        }
    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 1000)
    @Override
    public void updateLISHeartbeat() {
        if (isAutoShutterEnabled()) {
            epicsService.updateHeartbeat();
        }
    }


    /**
     * Gets the current shutter status.
     */
    private AutoShutter getCurrentAutoShutter() {
        if (clearToPropagate(epicsService.getTime())) {
            // all good, we are clear to propagate
            return AutoShutter.CLEAR;
        } else {
            if (epicsService.isOnSky()) {
                // not clear and laser is on sky -> shutter immediately!
                return AutoShutter.SHUTTERING;
            } else {
                // not clear but laser is not on sky, so currently no problem
                return AutoShutter.SHUTTERED;
            }
        }
    }

    /**
     * Checks if all conditions are met to be clear to propagate the laser.
     * The conditions are:
     * <ul>
     *     <li>We are pointing at a known laser target.</li>
     *     <li>We are inside the propagation limits (civil, nautical or astronomical twilight).</li>
     *     <li>We are pointing at a position inside the error cone around the target.</li>
     *     <li>We are inside a propagation window (taking safety buffers and blanket closures into account).</li>
     * </ul>
     */
    private Boolean clearToPropagate(ZonedDateTime currentTime) {

        AlarmService.Snapshot s = getSnapshot();

        // -- not ok to propagate if we have no target in vicinity
        if (s.getTarget() == null) {
            return Boolean.FALSE;

        // -- not ok to propagate if we are before or after allowed propagation times
        } else if (currentTime.isBefore(earliestPropagation)) {
            return Boolean.FALSE;

        } else if (currentTime.isAfter(latestPropagation)) {
            return Boolean.FALSE;

        // check if the current position is inside the error cone of the target
        } else if (!positionIsInsideErrorCone(s.getTarget())) {
            return Boolean.FALSE;

        // -- finally check if we are inside of a propagation window (excluding safety buffers)
        } else {

            // compare time with list of propagation windows, the snapshot provides propagation windows
            // that take blanket closures and safety buffers into account, so we don't need to deal
            // with those separately
            for (PropagationWindow w : getSnapshot().getPropagationWindows()) {
                if (w.contains(currentTime)) {
                    // ok, we are inside a propagation window
                    return Boolean.TRUE;
                }
            }

            // -- nope, not inside a propagation window, don't lase!
            return Boolean.FALSE;
        }

    }

    /**
     * Checks if the current position is inside the error cone around the closest approved laser target.
     */
    private Boolean positionIsInsideErrorCone(LaserTarget target) {
        // calculate distance from target to most recent EPICS values (don't use snapshot to make it more accurate)
        Angle curDistance = getDistance(
                target,
                epicsService.getCurrentAz(),
                epicsService.getCurrentEl(),
                epicsService.getCurrentRaDec()
                );

        if (curDistance.convertTo(errorCone.getUnit()).getMagnitude() > errorCone.getMagnitude()/2) {
            return Boolean.FALSE;    // oops, we're outside! PANIC
        } else {
            return Boolean.TRUE;     // ok, all green, still inside the error cone
        }
    }

    /**
     * Calculate distance between the closest approved laser target and the current position.
     */
    private Angle getDistance(LaserTarget target, Angle currentAz, Angle currentEl, WorldCoords currentRaDec) {
        Validate.notNull(target);

        if (target instanceof RaDecLaserTarget) {
            Double ra  = currentRaDec.getRaDeg();
            Double dec = currentRaDec.getDecDeg();
            return new Angle(target.distanceTo(ra, dec), Angle.Unit.DEGREES);
        } else {
            Double az = currentAz.toDegrees().getMagnitude();
            Double el = currentEl.toDegrees().getMagnitude();
            return new Angle(target.distanceTo(az, el), Angle.Unit.DEGREES);
        }
    }


    /**
     * Gets the auto shutter setting to be used on startup.
     * <p>
     * By default the auto shutter should be turned on on production environments and turned off on
     * all other environments. Having it turned on by default on production helps to recover after a
     * potential restart of the application during operations by not forcing the operator to remember to
     * turn the auto shutter on before they can continue to work.
     * <p>
     * On all other environments by default we start with auto shutter turned off.
     * @return the auto shutter default
     */
    private AutoShutter getStartupAutoShutter() {
        if (siteService.isProduction()) {
            return AutoShutter.INACTIVE;
        } else {
            return AutoShutter.OFF;
        }
    }

    /**
     * Gets a string with detailed information to be logged when laser is shuttered.
     * This should help to reconstruct what happened in cases where the laser is shuttered unexpectedly.
     */
    private String getDetailedShutteringLog() {
        final StringBuilder sb = new StringBuilder();
        sb.append(">>> demand RA/Dec: ");
        sb.append(epicsService.getDemandRaDec());
        sb.append("; current RA/Dec: ");
        sb.append(epicsService.getCurrentRaDec());
        sb.append("; current Az/El: ");
        sb.append(epicsService.getCurrentAz());
        sb.append(" / ");
        sb.append(epicsService.getCurrentEl());
        sb.append("; laser: ");
        sb.append(epicsService.getLaserStatus());
        sb.append("; system time: ");
        sb.append(System.currentTimeMillis());
        sb.append("; epics time: ");
        sb.append(epicsService.getTime().toEpochSecond());
        return sb.toString();
    }


    /**
     * An immutable and consistent snapshot of all relevant data for the alarm service.
     * A new instance of this snapshot object is created every few hundred milliseconds and can then be safely
     * passed around to clients interested in it without the danger of concurrent changes in EPICS channels
     * making parts of the data inconsistent. Of course this also means there is always a slight delay between
     * the real, most current data and the snapshot, but as long as this difference is not too big, it does
     * not matter for the purposes of this application (after all, this is a web server and NOT a real time
     * system).
     */
    static class Snapshot implements AlarmService.Snapshot {
        private final LaserNight night;
        private final LaserTarget target;
        private final List<PropagationWindow> propagationWindows;
        private final List<ShutteringWindow> shutteringWindows;
        private final ZonedDateTime earliestPropagation;
        private final ZonedDateTime latestPropagation;
        private final List<Observation> observations;
        private final Angle errorCone;
        private final Angle distance;
        private final LtcsService.Snapshot ltcsSnapshot;
        private final EpicsService.Snapshot epicsSnapshot;
        private final AutoShutter autoShutter;
        protected Snapshot(
                LaserNight night,
                LaserTarget target,
                ZonedDateTime earliestPropagation,
                ZonedDateTime latestPropagation,
                List<Observation> observations,
                Angle errorCone,
                Angle distance,
                LtcsService.Snapshot ltcsSnapshot,
                EpicsService.Snapshot epicsSnapshot,
                AutoShutter autoShutter,
                Integer safetyBufferBefore,
                Integer safetyBufferAfter) {
            this.night = night;
            this.target = target;
            this.earliestPropagation = earliestPropagation;
            this.latestPropagation = latestPropagation;
            this.observations = observations;
            this.errorCone = errorCone;
            this.distance = distance;
            this.ltcsSnapshot = ltcsSnapshot;
            this.epicsSnapshot = epicsSnapshot;
            this.autoShutter = autoShutter;
            this.propagationWindows = createPropagationWindows(safetyBufferBefore, safetyBufferAfter);
            this.shutteringWindows = PropagationWindow.getShutteringWindows(this.propagationWindows);
        }

        /**
         * Creates a list of propagation windows taking safety buffers and user defined blanket closures into account.
         * Only the propagation windows and the blanket closures are actually stored, all the other windows must be
         * created on the fly. We do this here once for the current snapshot.
         */
        private List<PropagationWindow> createPropagationWindows(Integer safetyBufferBefore, Integer safetyBufferAfter) {

            if (this.night == null || this.target == null) {
                return Collections.emptyList();
            }

            // get propagation windows, blanket closures and shuttering windows (i.e. intervals between propagation windows)
            Set<PropagationWindow> p0 = this.target.getPropagationWindows();
            Set<BlanketClosure>    closures = this.night.getClosures();
            List<ShutteringWindow> shutWins = PropagationWindow.getShutteringWindows(p0);

            // add safety buffers to closures and shuttering windows
            List<ShutteringWindow> bufferedClosures = bufferedClosures(closures, safetyBufferBefore, safetyBufferAfter);
            List<ShutteringWindow> bufferedShutWins = bufferedClosures(shutWins, safetyBufferBefore, safetyBufferAfter);

            // now calculate a set of propagation windows that includes the buffered blanket closures and shuttering windows
            List<PropagationWindow> p1 = PropagationWindow.removeClosures(p0, bufferedClosures);
            List<PropagationWindow> p2 = PropagationWindow.removeClosures(p1, bufferedShutWins);

            // use this for graphical output etc in the alarm client
            return p2;
        }

        private List<ShutteringWindow> bufferedClosures(Collection<? extends ShutteringWindow> shutteringWindows, Integer safetyBufferBefore, Integer safetyBufferAfter) {
            List<ShutteringWindow> buffered = new ArrayList<>();
            for (ShutteringWindow w : shutteringWindows) {
                buffered.add(new ShutteringWindow(
                        w.getStart().minusSeconds(safetyBufferBefore),
                        w.getEnd().plusSeconds(safetyBufferAfter)
                ));
            }
            return buffered;
        }

        /**
         * Checks if the night or the target has changed between an earlier and the current snapshot.
         * @param earlier
         * @return
         */
        @Override
        public Boolean nightOrTargetHasChanged(AlarmService.Snapshot earlier) {
            return targetHasChanged(earlier) || nightHasChanged(earlier);
        }

        /**
         * Checks if the night has changed between an earlier and the current snapshot.
         * @param earlier
         * @return
         */
        @Override
        public Boolean nightHasChanged(AlarmService.Snapshot earlier) {
            return earlier == null ? Boolean.TRUE : this.night != earlier.getNight();
        }

        /**
         * Check if the target has changed between an earlier and the current snapshot.
         * @param earlier
         * @return
         */
        @Override
        public Boolean targetHasChanged(AlarmService.Snapshot earlier) {
            // Note: we only need to check if the target object changed, this will also include changes in the list
            // of propagation windows due to LCH resending PAM files with updated information because this
            // will cause a reload of the night, the targets and all propagation windows and therefore result
            // in a new target object
            return earlier == null ? Boolean.TRUE : this.target != earlier.getTarget();
        }

        /**
         * Gets the time between the two snapshots in milliseconds.
         * @return
         */
        @Override
        public Duration getTimeSince(AlarmService.Snapshot earlier) {
            if (earlier == null) {
                return new Duration(new DateTime(0), epicsSnapshot.getTime());
            } else {
                return new Duration(earlier.getEpicsSnapshot().getTime(), epicsSnapshot.getTime());
            }
        }

        // -- some self explaining getters
        @Override
        public LaserNight getNight() { return night; }
        @Override
        public LaserTarget getTarget() { return target; }
        @Override
        public List<PropagationWindow> getPropagationWindows() { return propagationWindows; }
        @Override
        public List<ShutteringWindow> getShutteringWindows() { return shutteringWindows; }
        @Override
        public ZonedDateTime getEarliestPropagation() { return earliestPropagation; }
        @Override
        public ZonedDateTime getLatestPropagation() { return latestPropagation; }
        @Override
        public List<Observation> getObservations() { return observations; }
        @Override
        public Angle getErrorCone() { return errorCone; }
        @Override
        public Angle getDistance() { return distance; }
        @Override
        public LtcsService.Snapshot getLtcsSnapshot() { return ltcsSnapshot; }
        @Override
        public EpicsService.Snapshot getEpicsSnapshot() { return epicsSnapshot; }
        @Override
        public AutoShutter getAutoShutter() { return autoShutter; }
    }

}
