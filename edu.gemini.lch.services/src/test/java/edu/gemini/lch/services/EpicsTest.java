package edu.gemini.lch.services;

import edu.gemini.epics.EpicsObserver;
import edu.gemini.epics.EpicsService;
import edu.gemini.epics.api.EpicsClient;
import edu.gemini.epics.impl.EpicsObserverImpl;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A test case that allows to play around with EPICS channels.
 * (Not meant to be run as part of automated tests.)
 */
public class EpicsTest {

    @Ignore
    @Test
    public void goEpics() throws Exception {
        EpicsService epicsService = new EpicsService("10.2.2.255");
        epicsService.startService();
        assert epicsService.isContextAvailable();

        EpicsObserver observer = new EpicsObserverImpl(epicsService);
        EpicsClient client = new DaEpicsClient();
        observer.registerEpicsClient(client, new ArrayList() {{add("tcs:currentEl");}});

        // Give it 15 seconds
        TimeUnit.MILLISECONDS.sleep(15000);

        observer.unregisterEpicsClient(client);

        epicsService.stopService();
    }

    public class DaEpicsClient implements EpicsClient {

        @Override
        public <T> void valueChanged(String channel, List<T> values) {
            if (values != null && !values.isEmpty() && values.get(0) instanceof Double) {
                List<Double> doubleValues = (List<Double>) values;
                System.out.println(channel + ": " + doubleValues);
            }
        }

        @Override
        public void connected() { }
        @Override
        public void disconnected() { }
    }


}
