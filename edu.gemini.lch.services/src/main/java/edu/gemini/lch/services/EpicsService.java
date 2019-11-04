package edu.gemini.lch.services;

import edu.gemini.shared.skycalc.Angle;
import jsky.coords.WorldCoords;

import java.time.ZonedDateTime;

/**
 * This service listens to some EPICS channels which are relevant for LTTS and allows to either get the most recent
 * values or get an immutable snapshot of those values for further processing.
 */
public interface EpicsService {

    /**
     * Get latest snapshot of EPICS values.
     */
    Snapshot getSnapshot();

    /**
     * Current time (UTC).
     */
    ZonedDateTime getTime();

    /**
     * Gets the current azimuth angle.
     */
    Angle getCurrentAz();

    /**
     * Gets the current elevation angle.
     */
    Angle getCurrentEl();

    /**
     * Current RA/DEC position.
     */
    WorldCoords getCurrentRaDec();

    /**
     * Demanded RA/DEC position.
     */
    WorldCoords getDemandRaDec();

    /**
     * Current laser status.
     */
    String getLaserStatus();

    /**
     * Checks if laser is on sky.
     */
    Boolean isOnSky();

    /**
     * Opens all loops to make the shuttering a bit less messy.
     */
    void openLoops();

    /**
     * Shutter the laser by moving the BDM in.
     */
    void shutterLaser();

    /**
     * Forces the laser to stay shuttered during periods we are not clear to propagate.
     * Called twice a second.
     */
    void forceShutter();

    /**
     * Writes a new heartbeat value.
     * Called once a second in order to update the heartbeat value which has to be in the range [0,100).
     * If the LIS does not see this value change for several seconds it will assume the LTTS server
     * is dead and shutter the laser as a precaution.
     */
    void updateHeartbeat();

    /**
     * Connects to the EPICS layer using either the tcs simulator or the real thing.
     */
    void connect(Boolean useTcsSimulation);

    /**
     * Returns true if tcs simulation is used ("tc1:" channels instead of "tcs:").
     */
    Boolean usesTcsSimulator();

    /**
     * Snapshot of latest EPICS values.
     */
    interface Snapshot {
        Boolean isConnected();
        ZonedDateTime getTime();
        Angle getCurrentAz();
        Angle getCurrentEl();
        WorldCoords getCurrentRaDec();
        WorldCoords getDemandRaDec();
        String getLaserStatus();
        Boolean isOnSky();
    }

    /**
     * A connector that reads and writes to and from EPICS
     */
    interface Connector {
        void connect(String epicsAddressList);
        void disconnect();
        Boolean isConnected();
        Boolean usesTcsSimulator();
        ZonedDateTime getLastUpdate();
        Object readValue(String name);
        void writeValue(String name, Object value);
    }

}
