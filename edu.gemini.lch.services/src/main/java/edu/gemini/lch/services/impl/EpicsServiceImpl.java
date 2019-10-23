package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.SiteService;
import edu.gemini.lch.services.util.EpicsConnector;
import edu.gemini.shared.skycalc.Angle;
import jsky.coords.DMS;
import jsky.coords.HMS;
import jsky.coords.WorldCoords;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;

/**
 * A very simple EPICS channel listener that just stores the latest updated value in a hash map.
 */
public abstract class EpicsServiceImpl implements edu.gemini.lch.services.EpicsService {

    private static final Logger LOGGER = Logger.getLogger(EpicsServiceImpl.class);

    @Resource private ConfigurationService configurationService;
    @Resource private SiteService siteService;

    protected static final String EPICS_CURRENT_AZ    = "tcs:currentAz";
    protected static final String EPICS_CURRENT_EL    = "tcs:currentEl";
    protected static final String EPICS_CURRENT_RA    = "tcs:telescopeRA";
    protected static final String EPICS_CURRENT_DEC   = "tcs:telescopeDec";
    protected static final String EPICS_DEMAND_RA     = "tcs:demandRA";
    protected static final String EPICS_DEMAND_DEC    = "tcs:demandDec";
    protected static final String EPICS_DATE          = "tcs:date";
    protected static final String EPICS_UTC           = "tcs:UTC";
    protected static final String EPICS_LASER_STATUS  = "lis:lasPropProc.VALA";

    protected static final Map<String, Object> defaultReadChannels = new HashMap<String, Object>() {{
        put(EPICS_CURRENT_AZ, (double) 0);
        put(EPICS_CURRENT_EL, (double) 0);
        put(EPICS_CURRENT_RA, (double) 0);
        put(EPICS_CURRENT_DEC, (double) 0);
        put(EPICS_DEMAND_RA, "00:00:00");
        put(EPICS_DEMAND_DEC, "00:00:00");
        put(EPICS_DATE, "1970-01-01");
        put(EPICS_UTC, "00:00:00.0");
        put(EPICS_LASER_STATUS, "");
    }};

    private final DateTimeFormatter formatUTC;
    private final Map<String, Object> readChannels;
    private final String[] writeChannels;
    private Connector connector;
    private Snapshot snapshot;

    protected EpicsServiceImpl(Map<String, Object> readChannels, String[] writeChannels) {
        this.readChannels = readChannels;
        this.writeChannels = writeChannels;
        this.formatUTC = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S").withZone(DateTimeZone.UTC);
    }

    /** {@inheritDoc} */
    @Override public edu.gemini.lch.services.EpicsService.Snapshot getSnapshot() {
        return snapshot;
    }

    /** {@inheritDoc} */
    @Override public DateTime getTime() {
        String timeString = connector.readValue(EPICS_DATE) + " " + connector.readValue(EPICS_UTC);
        return formatUTC.parseDateTime(timeString);
    }

    /** {@inheritDoc} */
    @Override public Angle getCurrentAz() {
        return new Angle((Double) connector.readValue(EPICS_CURRENT_AZ), Angle.Unit.DEGREES);
    }

    /** {@inheritDoc} */
    @Override public Angle getCurrentEl() {
        return new Angle((Double) connector.readValue(EPICS_CURRENT_EL), Angle.Unit.DEGREES);
    }

    /** {@inheritDoc} */
    @Override public WorldCoords getCurrentRaDec() {
        Double ra  = (Double) connector.readValue(EPICS_CURRENT_RA);
        Double dec = (Double) connector.readValue(EPICS_CURRENT_DEC);
        return new WorldCoords(ra, dec);
    }

    /** {@inheritDoc} */
    @Override public WorldCoords getDemandRaDec() {
        HMS ra  = new HMS((String) connector.readValue(EPICS_DEMAND_RA));
        DMS dec = new DMS((String) connector.readValue(EPICS_DEMAND_DEC));
        return new WorldCoords(ra, dec);
    }

    /** {@inheritDoc} */
    @Override public String getLaserStatus() {
        return (String) connector.readValue(EPICS_LASER_STATUS);
    }

    /** {@inheritDoc} */
    @Override public Boolean isOnSky() {
        return "SKY".equals(getLaserStatus());
    }

    public void writeChannel(String channelName, Object value) {
        connector.writeValue(channelName, value);
    }

    protected Object readChannel(String channelName) {
        return connector.readValue(channelName);
    }

    /**
     * Initialize with some sensible start data and connect to EPICS channels.
     */
    @PostConstruct public void init() {
        connect(false);
        snapshot = new Snapshot(
                    false,
                    getTime(),
                    getCurrentAz(),
                    getCurrentEl(),
                    getCurrentRaDec(),
                    getDemandRaDec(),
                    getLaserStatus());
    }

    /** {@inheritDoc} */
    @Override public synchronized void connect(Boolean useTcsSimulation) {
        if (connector != null) {
            connector.disconnect();
        }
        connector = createConnector(readChannels, writeChannels, useTcsSimulation);
        String epicsAddressList = configurationService.getString(Configuration.Value.EPICS_ADDRESS_LIST);
        connector.connect(epicsAddressList);
    }

    /**
     * Cleans up and disconnects from EPICS channels.
     */
    @PreDestroy public void destroy() {
        connector.disconnect();
    }

    /**
     * Updates the snapshot with the most current EPICS data.
     * Scheduled to be called as asynchronous task by Spring scheduler.
     */
    @Scheduled(fixedDelay = 100)
    public void update() {
        snapshot = new Snapshot(
                        connector.isConnected(),
                        getTime(),
                        getCurrentAz(),
                        getCurrentEl(),
                        getCurrentRaDec(),
                        getDemandRaDec(),
                        getLaserStatus());
    }

    /**
     * Tries to reset the connection to the EPICS channels in case the connection has been lost for too long
     * by fully disconnecting and reconnecting.
     * Scheduled to be called as asynchronous task by Spring scheduler.
     */
    @Scheduled(fixedDelay = 60000)
    public synchronized void reconnect() {
        // The underlying channel access library seems to reconnect properly in case the network connection
        // gets lost. Not sure if this is really necessary, but in case something hangs for too long it might
        // help to fully disconnect and reconnect the underlying channel access.
        if (!connector.isConnected()) {
            // if we lost connection for more than a few minutes try a full reconnect...
            if (connector.getLastUpdate().isBefore(DateTime.now().minusMinutes(3))) {
                LOGGER.error("No updates from Epics received for more than 3 minutes, trying to reconnect...");
                connect(connector.usesTcsSimulator());
            }
        }
    }

    /** {@inheritDoc} */
    @Override public Boolean usesTcsSimulator() {
        return connector.usesTcsSimulator();
    }

    /**
     * Creates the connector used to read from and write to EPICS.
     * This is separated out in a separate method so that derived classes can override it and replace
     * the EPICS connector with something else for simulation, see EpicsServicesSimulator.
     * @param useTcsSimulation
     * @return
     */
    protected Connector createConnector(Map<String, Object> readChannels, String[] writeChannels, Boolean useTcsSimulation) {
        return new EpicsConnector(readChannels, writeChannels, useTcsSimulation);
    }



    /**
     * An immutable and consistent snapshot of all relevant data for the EPICS service.
     * A new instance of this snapshot object is created every few hundred milliseconds and can then be safely
     * passed around to clients interested in it without the danger of concurrent changes making parts of the data
     * inconsistent.
     */
    // make this class public for testing etc.
    public static class Snapshot implements edu.gemini.lch.services.EpicsService.Snapshot {
        private final Boolean isConnected;
        private final DateTime time;
        private final Angle currentAz;
        private final Angle currentEl;
        private final WorldCoords currentRaDec;
        private final WorldCoords demandRaDec;
        private final String laserStatus;
        public Snapshot(Boolean isConnected, DateTime time, Angle currentAz, Angle currentEl, WorldCoords currentRaDec, WorldCoords demandRaDec, String laserStatus) {
            this.isConnected = isConnected;
            this.time = time;
            this.currentAz = currentAz;
            this.currentEl = currentEl;
            this.currentRaDec = currentRaDec;
            this.demandRaDec = demandRaDec;
            this.laserStatus = laserStatus;
        }
        @Override public Boolean isConnected() { return isConnected; }
        @Override public DateTime getTime() { return time; }
        @Override public Angle getCurrentAz() { return currentAz; }
        @Override public Angle getCurrentEl() { return currentEl; }
        @Override public WorldCoords getCurrentRaDec() { return currentRaDec; }
        @Override public WorldCoords getDemandRaDec() { return demandRaDec; }
        @Override public String getLaserStatus() { return laserStatus; }
        @Override public Boolean isOnSky() { return "SKY".equals(laserStatus); }

    }

}
