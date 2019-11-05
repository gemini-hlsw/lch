package edu.gemini.lch.services.simulators;

import edu.gemini.lch.services.EpicsService;
import edu.gemini.lch.services.impl.EpicsServiceImpl;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test simulator for EPICS channels reading its inputs in regular intervals from a file.
 * Check the service configuration file and replace the actual EPICS service implementation with this
 * one in order for easy testing. Not to be confused with the EPICS based telescope-wide TCS simulator
 * which allows for testing EPICS based interaction with the TCS without having to do actual dome movements.
 * Switching between the TCS simulator and the actual telescope control system is done inside the
 * EpicsConnector, for details see there. This simulator only deals with the simple case of feeding
 * arbitrary values into the system without any EPICS interaction at all.
 */
@Service
public class EpicsServiceSimulator extends EpicsServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(EpicsServiceSimulator.class);

    @Value("${lch.simulators.input}")
    private String inputFilePath;

    public EpicsServiceSimulator() {
        super(defaultReadChannels, new String[]{});
        LOGGER.info("Input file for simulated EPICS channel values: " + inputFilePath);
    }


    @Override
    protected Connector createConnector(Map<String, Object> readChannels, String[] writeChannels, Boolean useTcsSimulation) {
        // instead of epics connector use the one for simulation
        return new Connector(readChannels, writeChannels);
    }

    @Scheduled(fixedDelay = 500)
    public void updateValues() {

        try {
            ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"));

            Properties props = new Properties();
            FileInputStream in = new FileInputStream(inputFilePath + File.separator + "epics.txt");
            props.load(in);

            for (Object key : props.keySet()) {
                String value = props.getProperty((String) key);

                // use current date and time if no date and/or time is defined in the text file
                if (EPICS_UTC.equals(key) && "now".equalsIgnoreCase(value)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.S");
                    writeChannel(EPICS_UTC, formatter.format(now));

                } else if (EPICS_DATE.equals(key) && "today".equalsIgnoreCase(value)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    writeChannel(EPICS_DATE, formatter.format(now));

                // for all other values (or if UTC and DATE are set) just set the values in the hash map
                } else if (readChannel((String) key) instanceof Double) {
                    writeChannel((String) key, new Double(value));

                } else {
                    writeChannel((String) key, value);
                }
            }


            in.close();

        } catch (Exception e) {
            LOGGER.error("could not read input file", e);
        }

    }

    /** {@inheritDoc} */
    @Override public void openLoops() {
        LOGGER.info("opening loops");
    }

    /** {@inheritDoc} */
    @Override public void shutterLaser() {
        LOGGER.info("shuttering laser");
    }

    /** {@inheritDoc} */
    @Override public void forceShutter() {
        LOGGER.trace("forcing shutter");
    }

    /** {@inheritDoc} */
    @Override public void updateHeartbeat() {
        LOGGER.trace("updating heartbeat");
    }

    private static class Connector implements EpicsService.Connector {

        private final Map<String, Object> readChannels;

        public Connector(Map<String, Object> readChannels, String[] writeChannels) {
            this.readChannels = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : readChannels.entrySet()) {
                this.readChannels.put(entry.getKey(), entry.getValue());
            }

        }

        @Override
        public void connect(String epicsAddressList) {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public Boolean isConnected() {
            return true;
        }

        @Override
        public Boolean usesTcsSimulator() {
            // strictly speaking not true, we use a file based simulation, but it is a simulation nevertheless
            return true;
        }

        @Override
        public ZonedDateTime getLastUpdate() {
            return ZonedDateTime.now();
        }

        @Override
        public Object readValue(String name) {
            return readChannels.get(name);
        }

        @Override
        public void writeValue(String name, Object value) {
            if (readChannels.containsKey(name)) {
                readChannels.put(name, value);
            } else {
                LOGGER.debug("writing channel: " + name + ", " + value);
            }
        }
    }
}
