package edu.gemini.lch.services.impl;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Implementation of Gemini South specific EPICS stuff.
 * Currently (May 2013) the laser related capabilities (e.g. available LIS commands) at GN and GS are different and
 * therefore we need GN and GS specific implementations.
 */
@Service
public class EpicsServiceSouthImpl extends EpicsServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(EpicsServiceSouthImpl.class);

    private static final String EPICS_LIS_PAUSE = "lis:pause.DIR";
    private static final String EPICS_LIS_HEARTBEAT = "lis:lttsHeartbeat.A";

    private static final String[] writeChannelNames = {
            EPICS_LIS_PAUSE,
            EPICS_LIS_HEARTBEAT
    };

    public EpicsServiceSouthImpl() {
        // add all relevant write channel names
        super(defaultReadChannels, writeChannelNames);
    }

    /** {@inheritDoc} */
    @Override public void openLoops() {
        LOGGER.info("opening loops (nothing to do for GS)");
        // currently nothing to do for GS
        //(in meeting on April 8, 2013 decision was made to NOT do anything but just put the BDM in)
    }

    /** {@inheritDoc} */
    @Override public void shutterLaser() {
        LOGGER.info("shuttering laser");
        writeChannel(EPICS_LIS_PAUSE, "MARK");
        writeChannel(EPICS_LIS_PAUSE, "START");
    }

    /** {@inheritDoc} */
    @Override public void forceShutter() {
        LOGGER.trace("forcing shutter");
        writeChannel(EPICS_LIS_PAUSE, "MARK");
        writeChannel(EPICS_LIS_PAUSE, "START");
    }

    /** {@inheritDoc} */
    @Override public void updateHeartbeat() {
        Integer value = getTime().getSecond() % 100;
        writeChannel(EPICS_LIS_HEARTBEAT, value);
        LOGGER.trace("updated LIS heartbeat: " + value);
    }
}
