package edu.gemini.lch.web.app.windows.night;

import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.EmailService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.util.PrmFile;
import edu.gemini.lch.web.app.components.DateDialogWindow;
import edu.gemini.lch.web.app.components.Header;
import edu.gemini.lch.web.app.components.SendPrmDialogWindow;
import edu.gemini.lch.web.app.components.StartEndDateDialogWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.vaadin.dialogs.ConfirmDialog;

import java.io.ByteArrayInputStream;

/**
 */
@Configurable(preConstruction = true)
public class NightWindowHeader extends Header {

    private final NightWindow nightWindow;

    @Autowired
    private LaserNightService laserNightService;

    @Autowired
    private EmailService emailService;

    public NightWindowHeader(final NightWindow window) {
        super(window);
        nightWindow = window;
    }

    @Override
    protected void addToMenu(final MenuBar menuBar) {
        final MenuBar.MenuItem nights = menuBar.addItem("Nights", null, null);
        nights.addItem("Go to Night", null, new GotoNightCommand());
        nights.addItem("Create Run", null, new CreateRunCommand());
        nights.addItem("Add Night", null, new CreateNightCommand());
        nights.addItem("Update Night", null, new UpdateNightCommand());
        nights.addItem("Cancel Night", null, new CancelNightCommand());

        final MenuBar.MenuItem prm = menuBar.addItem("PRM", null, null);
        prm.addItem("Show PRM file with all RaDec targets", null, new CreateRaDecPrm());
        prm.addItem("Show PRM file with all AzEl targets", null, new CreateAzElPrm());
        prm.addItem("Upload PRM file(s)", null, new SendPrm());
        prm.addItem("Download PAM file(s)", null, new DownloadPam());
    }

    public class GotoNightCommand implements MenuBar.Command, DateDialogWindow.DateDialogListener  {
        private DateDialogWindow dialog;
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            dialog = new DateDialogWindow("Go to a Laser Night", nightWindow.getClock().getSelectedTimeZone(), this);
        }
        public void okButtonClicked() {
            // go to the date or the closest nearby day
            nightWindow.initNight(dialog.getDate());
        }
    }

    public class CreateNightCommand implements MenuBar.Command, DateDialogWindow.DateDialogListener  {
        private DateDialogWindow dialog;
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            dialog = new DateDialogWindow("Add a Laser Night", nightWindow.getClock().getSelectedTimeZone(), this);
        }
        /** Handle Close button click and close the window. */
        public void okButtonClicked() {
            if (!laserNightService.laserNightExists(dialog.getDate())) {
                laserNightService.createAndPopulateLaserNight(dialog.getDate());
            }
            nightWindow.setNight(dialog.getDate());
        }
    }

    public class UpdateNightCommand implements MenuBar.Command  {
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            if (!nightWindow.getDisplayedNight().isTestNight()) {
                laserNightService.updateLaserNight(nightWindow.getDisplayedNight());
                nightWindow.setNight(nightWindow.getDisplayedNight().getStart());
            } else {
                Notification.show("Test night data can not be updated.", Notification.Type.WARNING_MESSAGE);
            }
        }
    }

    public class CreateRunCommand implements MenuBar.Command, DateDialogWindow.DateDialogListener  {
        private StartEndDateDialogWindow dialog;
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            dialog = new StartEndDateDialogWindow("Create a Laser Run", nightWindow.getClock().getSelectedTimeZone(), this);
        }
        /** Handle Close button click and close the window. */
        public void okButtonClicked() {
            laserNightService.createLaserRun(dialog.getStartDate(), dialog.getEndDate());
            nightWindow.setNight(dialog.getStartDate());
        }
    }

    public class CancelNightCommand implements MenuBar.Command {
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            final LaserNight night = nightWindow.getDisplayedNight();
            if (night != null) {
                if (!night.hasPrmSent()) {

                    ConfirmDialog.show(nightWindow.getUI(),
                            "Deleting a laser night:",
                            "This will delete the current laser night.\n\n" +
                            "Are you sure you want to do that?",
                            "Yes", "Cancel", dialog -> {
                                if (dialog.isConfirmed()) {
                                    laserNightService.deleteLaserNight(night.getId());
                                    nightWindow.initNight(night.getEnd());
                                }
                            });

                } else {

                    Notification.show(
                            "Can not delete a night for which PRM files have been sent to LCH.",
                            Notification.Type.WARNING_MESSAGE);

                }
            }
        }
    }

    public class CreateRaDecPrm implements MenuBar.Command  {
        public void menuSelected(final MenuBar.MenuItem selectedItem) {
            final PrmFile.File file = laserNightService.createSingleRaDecPrmFile(nightWindow.getDisplayedNight());
            final StreamResource.StreamSource source = () -> new ByteArrayInputStream(file.getFile().getBytes());
            final String filename = file.getName();
            final StreamResource resource = new StreamResource(source, filename);
            resource.setMIMEType("text/plain");
            resource.setCacheTime(0);

            // what happens now is depending on browser (save dialog, new window etc)
            Page.getCurrent().open(resource, filename, false);

        }
    }

     public class CreateAzElPrm implements MenuBar.Command  {
        public void menuSelected(MenuBar.MenuItem selectedItem) {
            final PrmFile.File file = laserNightService.createSingleAzElPrmFile(nightWindow.getDisplayedNight());
            final StreamResource.StreamSource source = () -> new ByteArrayInputStream(file.getFile().getBytes());
            final String filename = file.getName();
            final StreamResource resource = new StreamResource(source, filename);
            resource.setMIMEType("text/plain");
            resource.setCacheTime(0);

            // what happens now is depending on browser (save dialog, new window etc)
            Page.getCurrent().open(resource, filename, false);

        }
    }

    public class SendPrm implements MenuBar.Command  {
        public void menuSelected(MenuBar.MenuItem selectedItem) {
            new SendPrmDialogWindow(nightWindow);
        }
    }

    public class DownloadPam implements MenuBar.Command  {
        public void menuSelected(MenuBar.MenuItem selectedItem) {
            ConfirmDialog.show(nightWindow.getUI(),
                    "Downloading PAM files from space-track.org:",
                    "This should be done automatically but you can trigger a download manually in case " +
                    "you think there are unprocessed PAM files on space-track.org available.\n\n" +
                    "Are you sure you want to do this?",
                    "Download", "Cancel", dialog -> {
                        if (dialog.isConfirmed()) {
                            // trigger a download and a reload of the night
                            emailService.downloadPamsFromSpaceTrack();
                            nightWindow.setNight(nightWindow.getDisplayedNight().getId());
                        }
                    });

        }
    }

}
