package edu.gemini.lch.services.impl;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Implementation of Gemini North specific EPICS stuff.
 */
@Service
public class EpicsServiceNorthImpl extends EpicsServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(EpicsServiceNorthImpl.class);

    private static final String tcs_m2GuideControl   = "tcs:m2GuideControl.A";
    private static final String tcs_wfsGuideMode     = "tcs:wfsGuideMode.A";
    private static final String tcs_mountGuideModeA  = "tcs:mountGuideMode.A";
    private static final String tcs_mountGuideModeB  = "tcs:mountGuideMode.B";
    private static final String tcs_m1GuideMode      = "tcs:m1GuideMode.A";
    private static final String tcs_rotGuideMode     = "tcs:rotGuideMode.A";
    private static final String tcs_aoCorrect        = "tcs:aoCorrect.A";
    private static final String tcs_aoFlatten        = "tcs:aoFlatten.A";
    private static final String tcs_apply            = "tcs:apply.DIR";

    private static final String bto_bdmMove          = "bto:BDMMove.A";
    private static final String bto_apply            = "bto:apply.DIR";

    private static final String[] writeChannelNames = {
            tcs_m2GuideControl,
            tcs_wfsGuideMode,
            tcs_mountGuideModeA,
            tcs_mountGuideModeB,
            tcs_m1GuideMode,
            tcs_rotGuideMode,
            tcs_aoCorrect,
            tcs_aoFlatten,
            tcs_apply,

            bto_bdmMove,
            bto_apply
    };

    public EpicsServiceNorthImpl() {
        // add all relevant write channel names
        super(defaultReadChannels, writeChannelNames);
    }

    /** {@inheritDoc} */
    @Override public void openLoops() {
        LOGGER.info("opening loops");

        // M2 guiding:
        // tcs:m2GuideControl.A On/Off
        writeChannel(tcs_m2GuideControl, "Off");
        // WFS guide mode:
        // tcs:wfsGuideMode.A  On/Off
        writeChannel(tcs_wfsGuideMode, "Off");
        // Mount guiding:
        // tcs:mountGuideMode.A On/Off
        // tcs:mountGuideMode.B SCS
        writeChannel(tcs_mountGuideModeA, "Off");
        writeChannel(tcs_mountGuideModeB, "SCS");
        // M1 guiding
        // tcs:m1GuideMode.A On/Off
        writeChannel(tcs_m1GuideMode, "Off");
        // Cass Rotator guiding
        // tcs:rotGuideMode.A On/Off  {always off}
        writeChannel(tcs_rotGuideMode, "Off");
        // Altair correction
        // tcs:aoCorrect.A On/Off
        // tcs:aoFlatten.A
        writeChannel(tcs_aoCorrect, "Off");
        writeChannel(tcs_aoFlatten, "MARK");

        // finally: apply
        // tcs:apply.DIR
        writeChannel(tcs_apply, "START");

        // -- LCH-276: give this command sequence a moment to do its thing
        // TODO: here we should wait for command to finish.. check EPICS channel.. as a shortcut we just wait for 2 secs
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // intentionally left blank
        }

    }

    /** {@inheritDoc} */
    @Override public void shutterLaser() {
        LOGGER.info("shuttering laser");
        // bto:BDMMove.A Out
        writeChannel(bto_bdmMove, "In");
        // bto:apply.DIR START
        writeChannel(bto_apply, "START");
    }

    /** {@inheritDoc} */
    @Override public void forceShutter() {
        // currently no support for forcing the laser to stay shuttered at GN
    }

    /** {@inheritDoc} */
    @Override public void updateHeartbeat() {
        // currently LIS at GN does not support the heartbeat signal
    }

}
