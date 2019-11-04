package edu.gemini.lch.services.util;

import com.cosylab.epics.caj.CAJContext;
import edu.gemini.epics.EpicsObserver;
import edu.gemini.epics.api.EpicsClient;
import edu.gemini.epics.impl.EpicsObserverImpl;
import edu.gemini.lch.services.EpicsService;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.JCALibrary;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A very simple EPICS channel listener that just stores the latest updated value in a hash map.
 */
public class EpicsConnector implements EpicsClient, EpicsService.Connector {

    private static final Logger LOGGER = Logger.getLogger(EpicsConnector.class);

    private final Map<String, Object> readChannels;
    private final Map<String, Channel> writeChannels;
    private final Boolean useTcsSimulation;

    private edu.gemini.epics.EpicsService epicsService;
    private EpicsObserver observer;
    private ZonedDateTime lastUpdate;

    /**
     * Channel names starting with "tcs:" are changed to "tc1:" in case we are in simulation mode.
     */
    public EpicsConnector(Map<String, Object> readChannels, String[] writeChannels, Boolean useTcsSimulation) {

        // timestamp of last update and sim mode
        this.lastUpdate = ZonedDateTime.ofInstant(Instant.MIN, ZoneId.systemDefault());
        this.useTcsSimulation = useTcsSimulation;

        // configure read channels including their default values, translate tcs: to tc1: if needed (simulation)
        this.readChannels = new ConcurrentHashMap<>();
        for (Map.Entry<String, Object> entry : readChannels.entrySet()) {
            this.readChannels.put(getAliasChannelName(entry.getKey()), entry.getValue());
        }

        // create a map with channels we want to write to, translate tcs: to tc1: if needed (simulation)
        this.writeChannels = new HashMap<>();
        for (String name : writeChannels) {
            this.writeChannels.put(getAliasChannelName(name), null);
        }
    }

    public Boolean isConnected() {
        // we should get multiple updates every second (e.g. the time signal), so if we
        // don't get anything for more than 10 seconds we can assume something is wrong...
        return lastUpdate.isAfter(ZonedDateTime.now().minusSeconds(10));
    }

    public Boolean usesTcsSimulator() {
        return useTcsSimulation;
    }

    /**
     * Connects to the EPICS channels.
     * Starts the EPICS service and registers this client.
     */
    public void connect(String epicsAddressList) {

        // initialize epics service
        try {
            // work around for current limitation of EpicsService to allow only one IP address instead of a list:
            // create a JCA context with a list of addresses manually and pass it on to EpicsService
            System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", epicsAddressList);
            System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
            CAJContext epicsContext = (CAJContext) JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
            epicsContext.initialize();

            // use the manually created context for creating the EpicsService instead of an address list
            epicsService = new edu.gemini.epics.EpicsService(epicsContext);
            //epicsService.startService(); not needed because we create context manually
            observer = new EpicsObserverImpl(epicsService);
            observer.registerEpicsClient(this, new ArrayList(readChannels.keySet()));
            LOGGER.info("EPICS connection initialized");

            // initialize write connections
            for (String channelName : writeChannels.keySet()) {
                try {
                    Channel channel = epicsService.getJCAContext().createChannel(channelName);
                    writeChannels.put(channelName, channel);
                } catch (CAException e) {
                    LOGGER.error("could not create write channel " + channelName);
                }
            }

        } catch (Exception e) {
            LOGGER.error("could not connect to EPICS", e);
        }


    }

    /**
     * Disconnects from the EPICS channels.
     * Unregisters the EPICS client and stops the EPICS service.
     */
    public void disconnect() {

        // shut down write connections
        for (String channelName : writeChannels.keySet()) {
            Channel channel = writeChannels.get(channelName);
            if (channel != null) {
                try {
                    channel.destroy();
                } catch (CAException e) {
                    LOGGER.error("could not close write channel " + channelName);
                }
                writeChannels.put(channelName, null);
            }
        }

        // disconnect observer for reading and epics service
        try {
            observer.unregisterEpicsClient(this);   // this will call back to disconnected()!
            epicsService.stopService();
            observer = null;                        // make sure we don't use those anymore
            epicsService = null;
            LOGGER.info("EPICS connection closed");
        } catch (Exception e) {
            LOGGER.error("could not disconnect from EPICS", e);
        }
    }

    public ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    /** {@inheritDoc} */
    @Override public <T> void valueChanged(String channel, List<T> values) {
        if (values != null && !values.isEmpty()) {
            readChannels.put(channel, values.get(0));
            lastUpdate = ZonedDateTime.now();
        }
    }

    /**
     * Called by EPICS library after successfully connecting the first time.
     * NOTE: This method is not called after successfully reconnecting after the connection was lost.
     */
    @Override public void connected() {
        LOGGER.info("EPICS connected");
    }

    /**
     * Called in case the epics client is unregistered or in case the underlying library looses connection
     * to the EPICS channels and needs to be reset.
     */
    @Override public void disconnected() {
        LOGGER.error("EPICS disconnected");
    }

    /**
     * Reads the most recent value we've received for the given channel.
     * Channel names starting with "tcs:" are changed to "tc1:" in case we are in simulation mode.
     */
    @Override public Object readValue(String channelName) {
        // use getAliasChannelName to translate name to simulation name (tcs: -> tc1:) if needed
        return readChannels.get(getAliasChannelName(channelName));
    }

    /**
     * Write a value value to a channel.
     * Channel names starting with "tcs:" are changed to "tc1:" in case we are in simulation mode.
     */
    @Override public void writeValue(String channelName, Object value) {
        // use getAliasChannelName to translate name to simulation name (tcs: -> tc1:) if needed
        channelName = getAliasChannelName(channelName);

        // now write
        LOGGER.trace("writing " + value + " to channel " + channelName);
        try {
            Validate.isTrue(writeChannels.containsKey(channelName), "unknown channel name");
            Channel channel = writeChannels.get(channelName);
            Validate.notNull(channel, "channel not initialized");
            if (value instanceof Integer) {
                channel.put((Integer) value);
            } else if (value instanceof Double) {
                channel.put((Double) value);
            } else {
                // use string as fallback, add more else statements in case
                // other types are needed in the future
                channel.put(value.toString());
            }
        } catch (Exception e) {
            LOGGER.error("could not write to channel", e);
        }
    }

    /**
     * Gets the channel name for a given name in case we are using the simulation channels (tc1: instead of tcs:).
     * This is a bit of a hack, but it seemed to be the easiest way to do it so that only this low level is
     * impacted and the higher levels can always use the same EPICS channel names, regardless whether they
     * access the simulator or the actual telescope.
     */
    private String getAliasChannelName(String channelName) {
        if (useTcsSimulation && channelName.startsWith("tcs:")) {
            return channelName.replace("tcs:", "tc1:");
        } else {
            return channelName;
        }
    }
}

