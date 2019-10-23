package edu.gemini.lch.services;


import edu.gemini.lch.model.*;
import edu.gemini.shared.skycalc.Angle;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;

/**
 * This service provides all information that is needed for the alarm display.
 * It does so using some timers which update all relevant information in given intervals. It is also
 * responsible for writing a heartbeat signal to a dedicated EPICS channel for the LIS, if the heartbeat
 * stops for too long, the LIS will shutter the laser.
 */
public interface AlarmService {

    Snapshot getSnapshot();

    /**
     * Updates the alarm status.
     * This method is called by the Spring scheduler and runs asynchronously.
     */
    void update();

    /**
     * In order to avoid propagation to sky while we are not allowed to propagate send
     * shutter command repeatedly. NOTE: this is not the best way to do this, in the future we
     * will have to find a better way (read: a better integration with the LIS).
     */
    void forceShutter();

    /**
     * Updates the auto shutter, this needs to be done more frequently than the updates of the general alarm state.
     * This method is called by the Spring scheduler and runs asynchronously.
     */
    void updateAutoShutter();

    /**
     * Write LIS heartbeats once a second.
     * Update heartbeat only if auto shutter is enabled, this is important in order to avoid a situation where
     * the production LTTS is down but LIS keeps receiving heartbeats from the test installation.
     */
    void updateLISHeartbeat();

    /**
     * Updates configuration values from the database and checks for the current night if none is loaded yet.
     * This method is called by the Spring scheduler and runs asynchronously.
     */
    void updateFromDatabase();

    void updateNight();
    Boolean isAutoShutterEnabled();
    void setAutoShutterEnabled(boolean enabled);

    interface Snapshot {
        Boolean nightOrTargetHasChanged(Snapshot earlier);
        Boolean nightHasChanged(Snapshot earlier);
        Boolean targetHasChanged(Snapshot earlier);
        Duration getTimeSince(AlarmService.Snapshot earlier);
        LaserNight getNight();
        LaserTarget getTarget();
        List<PropagationWindow> getPropagationWindows();
        List<ShutteringWindow> getShutteringWindows();
        DateTime getEarliestPropagation();
        DateTime getLatestPropagation();
        List<Observation> getObservations();
        Angle getErrorCone();
        Angle getDistance();
        LtcsService.Snapshot getLtcsSnapshot();
        EpicsService.Snapshot getEpicsSnapshot();
        AutoShutter getAutoShutter();
    }

    /**
     * The auto shutter states.
     */
    enum AutoShutter {
        /** Auto shutter is turned off, no auto shutter activity. */
        OFF,
        /** Auto shutter is on, but not active (i.e. we are not between sunset and sunrise of a laser night). */
        INACTIVE,
        /** We are clear to propagate. */
        CLEAR,
        /** The auto shutter detected a situation where it needs to shutter and is trying to do so. */
        SHUTTERING,
        /** The auto shutter detected a situation where it needs to shutter and the laser is already shuttered. */
        SHUTTERED
    }

    /**
     * Listener interface which needs to be implemented by all classes that want to be kept up to date
     * about changes in the alarm state.
     */
    interface Listener {
        void update(Snapshot snapshot);
    }

}
