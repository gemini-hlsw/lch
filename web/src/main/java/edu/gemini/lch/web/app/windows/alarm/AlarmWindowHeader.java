package edu.gemini.lch.web.app.windows.alarm;

import com.vaadin.ui.MenuBar;
import edu.gemini.lch.web.app.components.Header;
import org.springframework.beans.factory.annotation.Configurable;

/**
 */
@Configurable(preConstruction = true)
public class AlarmWindowHeader extends Header {

    final AlarmWindow window;

    public AlarmWindowHeader(final AlarmWindow window) {
        super(window);
        this.window = window;
    }

    protected void addToMenu(final MenuBar menuBar) {
        final MenuBar.MenuItem test = menuBar.addItem("Audio", null, null);
        test.addItem("Play Test Audio Message",  null, menuItem -> window.playTestAudio());
    }

}
