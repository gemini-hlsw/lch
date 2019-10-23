package edu.gemini.lch.web.app.components;

import com.vaadin.navigator.View;
import com.vaadin.server.*;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.services.*;
import edu.gemini.lch.web.app.windows.admin.AdministrationWindow;
import edu.gemini.lch.web.app.windows.alarm.AlarmWindow;
import edu.gemini.lch.web.app.windows.night.NightWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 */
@Configurable(preConstruction = true)
public abstract class Header {

    private final View window;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteService siteService;

    protected Header(final View window) {
        this.window = window;
    }

    protected abstract void addToMenu(MenuBar menuBar);

    public Component getComponent() {
        final Component image = new Embedded("", new ThemeResource("img/geminiLogo.gif"));

        final StringBuilder headerLabel = new StringBuilder("LTTS - ");
        headerLabel.append(siteService.getSite().getDisplayName());
        if (!siteService.isProduction()) {
            headerLabel.append(" (");
            headerLabel.append(siteService.getEnvironment().toUpperCase());
            headerLabel.append(")");
        }

        final Label label = new Label(headerLabel.toString());
        label.setStyleName(Reindeer.LABEL_H1);
        final Label subLabel = new Label("Laser Target Tracking System");
        subLabel.setStyleName(Reindeer.LABEL_H2);

        final VerticalLayout vl = new VerticalLayout();
        vl.setWidth("100%");
        vl.setMargin(true);
        vl.addComponent(label);
        vl.addComponent(subLabel);

        final HorizontalLayout layout = new HorizontalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.addComponent(image);
        layout.addComponent(vl);
        layout.setComponentAlignment(image, Alignment.MIDDLE_LEFT);
        layout.setComponentAlignment(vl, Alignment.MIDDLE_LEFT);
        layout.setExpandRatio(vl, 1.0f);

        final VerticalLayout vl2 = new VerticalLayout();
        vl2.setSizeFull();
        vl2.addComponent(createMenu());
        vl2.addComponent(layout);

        return vl2;
    }

    private Component createMenu() {
        final MenuBar menuBar = new MenuBar();
        menuBar.setSizeFull();

        // windows override this for their individual menu entries
        addToMenu(menuBar);

        // menu with window selection (exclude the current window)
        final MenuBar.MenuItem windowMenu = menuBar.addItem("Window", null, null);
        if (!(window instanceof NightWindow)) {
            windowMenu.addItem("Nights", null, new OpenWindowCommand(NightWindow.NAME));
        }
        if (!(window instanceof AlarmWindow)) {
            windowMenu.addItem("Alarms", null, new OpenWindowCommand(AlarmWindow.NAME));
        }
        if (!(window instanceof AdministrationWindow)) {
            windowMenu.addItem("Administration", null, new OpenWindowCommand(AdministrationWindow.NAME));
        }

        // help menu
        final MenuBar.MenuItem helpMenu = menuBar.addItem("Help", null, null);
        helpMenu.addItem("Help Page", null, new GotoHelpCommand());

        // add version information at the right side
        final MenuBar.MenuItem version = menuBar.addItem(siteService.getVersion(), null, null);
        version.setStyleName("menuRight");

        return menuBar;
    }


    // -- Menu commands

    private class OpenWindowCommand implements MenuBar.Command  {
        private final String windowName;
        public OpenWindowCommand(final String windowName) {
            this.windowName = windowName;
        }
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            UI.getCurrent().getNavigator().navigateTo(windowName);
        }
    }

    private class GotoHelpCommand implements MenuBar.Command  {
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            final String url = configurationService.getString(Configuration.Value.HELP_URL);
            Page.getCurrent().open(new ExternalResource(url).getURL(), "LTTS Help");
        }
    }

}
