package edu.gemini.lch.web.app.components;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Audio;
import edu.gemini.lch.web.app.windows.admin.AdministrationWindow;
import org.apache.log4j.Logger;

/**
 * A simple audio element that allows to play a test message.
 */
public class AudioElement extends Audio {

    private static final Logger LOGGER = Logger.getLogger(AdministrationWindow.class);

    public AudioElement() {
        setShowControls(false);
    }

    public void playTestAudio() {
        LOGGER.trace("play test audio message");
        final double r = Math.random();
        if (r > 0.96) setSource(new ThemeResource("alarms/TestMessage2.wav"));
        else if (r > 0.92) setSource(new ThemeResource("alarms/TestMessage3.wav"));
        else if (r > 0.88) setSource(new ThemeResource("alarms/TestMessage4.wav"));
        else if (r > 0.84) setSource(new ThemeResource("alarms/TestMessage5.wav"));
        else if (r > 0.80) setSource(new ThemeResource("alarms/TestMessage6.wav"));
        else setSource(new ThemeResource("alarms/TestMessage.wav"));
        play();
    }

}
