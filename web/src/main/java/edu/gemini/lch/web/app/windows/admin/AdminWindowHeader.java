package edu.gemini.lch.web.app.windows.admin;

import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import edu.gemini.lch.services.AlarmService;
import edu.gemini.lch.services.EpicsService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.LoggingService;
import edu.gemini.lch.web.app.LchApplication;
import edu.gemini.lch.web.app.components.DateDialogWindow;
import edu.gemini.lch.web.app.components.Header;
import edu.gemini.lch.web.app.windows.night.NightWindow;
import org.apache.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.vaadin.dialogs.ConfirmDialog;

import java.time.ZoneId;

/**
 * The administration window.
 */
@Configurable(preConstruction = true)
public class AdminWindowHeader extends Header {

    private final AdministrationWindow adminWindow;

    @Autowired
    private LaserNightService laserNightService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private EpicsService epicsService;

    @Autowired
    private AlarmService alarmService;

    public AdminWindowHeader(AdministrationWindow adminWindow) {
        super(adminWindow);
        this.adminWindow = adminWindow;
    }

    @Override
    protected void addToMenu(MenuBar menuBar) {
        // log stuff
        MenuBar.MenuItem log = menuBar.addItem("Logging", null, null);
        log.addItem("Set to Warn",  null, menuItem -> loggingService.setLogLevel(Level.WARN));
        log.addItem("Set to Info",  null, menuItem -> loggingService.setLogLevel(Level.INFO));
        log.addItem("Set to Debug", null, menuItem -> loggingService.setLogLevel(Level.DEBUG));
        log.addItem("Set to Trace", null, menuItem -> loggingService.setLogLevel(Level.TRACE));

        // other testing capabilities
        MenuBar.MenuItem test = menuBar.addItem("Test", null, null);
        test.addItem("Toggle Auto Shutter (ON/OFF)",          menuItem -> toggleAutoShutter());
        test.addItem("Toggle TCS Simulator (ON/OFF)",         menuItem -> toggleTcsSimulator());
        test.addItem("Test Opening the Guide Loops...", null, menuItem -> testOpenLoops());
        test.addItem("Test Shuttering the Laser...",    null, menuItem -> testShutterLaser());
        test.addItem("Play Test Audio Message",         null, menuItem -> testAudio());
        test.addItem("Add Test Laser Day/Night...",     null, new CreateTestNightCommand());
    }

    public class CreateTestNightCommand implements MenuBar.Command, DateDialogWindow.DateDialogListener  {
        private DateDialogWindow dialog;
        public void menuSelected(MenuBar.MenuItem selectedItem) {
            dialog = new DateDialogWindow("Add a Test Laser Day/Night", ZoneId.of("UTC"), this);
        }
        public void okButtonClicked() {
            if (!laserNightService.laserNightExists(dialog.getDate())) {
                laserNightService.createTestLaserNight(dialog.getDate());
            } else {
                Notification.show("This date is already a laser night.", Notification.Type.WARNING_MESSAGE);
            }
            final NightWindow nightWindow = ((LchApplication) UI.getCurrent()).getNightWindow();
            nightWindow.setNight(dialog.getDate());
            UI.getCurrent().getNavigator().navigateTo(NightWindow.NAME);
        }
    }

    /**
     * Toggles the auto shutter and the LIS heartbeat.
     */
    private void toggleAutoShutter() {
        final Boolean enabled = alarmService.isAutoShutterEnabled();
        final String currentState = enabled ? "ON" : "OFF";
        final String toggledState = enabled ? "OFF" : "ON";

        final StringBuilder msg = new StringBuilder();
        msg.append("This will change the current auto shutter setting from ");
        msg.append(currentState);
        msg.append(" to ");
        msg.append(toggledState);
        msg.append(" and turn the LIS heart beat signals ");
        msg.append(toggledState);
        msg.append(".\n\n");
        msg.append("THIS CAN SERIOUSLY IMPACT OPERATIONS!\n");
        if ("OFF".equals(toggledState)) {
            msg.append("LIS WILL SHUTTER THE LASER WHEN LOOSING THE HEARTBEAT SIGNALS!\n");
        }
        msg.append("\n");
        msg.append("Are you sure you want to do this?");

        ConfirmDialog.show(adminWindow.getUI(),
                "Turn Auto Shutter " + toggledState + ":",
                 msg.toString(),
                "Yes", "Cancel", dialog -> {
                    if (dialog.isConfirmed()) {
                        alarmService.setAutoShutterEnabled(!enabled);
                    }
                });
    }

    /**
     * Toggles between the actual telescope control system (EPICS channels starting with "tcs:") and the
     * TCS simulator (EPICS channels starting with "tc1:").
     */
    private void toggleTcsSimulator() {
        final Boolean enabled = epicsService.usesTcsSimulator();
        final String currentState = enabled ? "ON" : "OFF";
        final String toggledState = enabled ? "OFF" : "ON";

        final StringBuilder msg = new StringBuilder();
        msg.append("This will change the current tcs simulator setting from ");
        msg.append(currentState);
        msg.append(" to ");
        msg.append(toggledState);
        msg.append(".\n\n");
        msg.append("THIS CAN SERIOUSLY IMPACT OPERATIONS!\n");
        if ("ON".equals(toggledState)) {
            msg.append("This will make the alarm page use SIMULATED telescope positions!\n");
        }
        if ("OFF".equals(toggledState)) {
            msg.append("This will make the alarm page use the REAL telescope positions!\n");
        }
        msg.append("\n");
        msg.append("Are you sure you want to do this?");

        ConfirmDialog.show(adminWindow.getUI(),
                "Turn tcs simulator " + toggledState + ":",
                msg.toString(),
                "Yes", "Cancel", dialog -> {
                    if (dialog.isConfirmed()) {
                        epicsService.connect(!enabled);
                    }
                });
    }

    /**
     * Tests the open loops command sequence.
     */
    private void testOpenLoops() {
        ConfirmDialog.show(adminWindow.getUI(),
                "Manually Execute the Open Loops Sequence:",
                "This will execute the sequence to open all loops for testing.\n\n" +
                "Are you sure you want to do this?",
                "Yes", "Cancel", dialog -> {
                    if (dialog.isConfirmed()) {
                        epicsService.openLoops();
                    }
                });
    }

    /**
     * Tests the shutter laser command sequence.
     */
    private void testShutterLaser() {
        ConfirmDialog.show(adminWindow.getUI(),
                "Manually Execute the Shutter Laser Sequence:",
                "This will execute the sequence to shutter the laser for testing.\n\n" +
                "Are you sure you want to do this?",
                "Yes", "Cancel", dialog -> {
                    if (dialog.isConfirmed()) {
                        epicsService.shutterLaser();
                    }
                });
    }

    /**
     * Plays a test message to check if audio works correctly and volume is up.
     */
    private void testAudio() {
        adminWindow.playTestAudio();
    }
}
