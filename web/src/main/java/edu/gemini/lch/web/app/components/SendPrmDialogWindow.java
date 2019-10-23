package edu.gemini.lch.web.app.components;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.impl.Factory;
import edu.gemini.lch.web.app.windows.night.NightWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 */
@Configurable(preConstruction = true)
public class SendPrmDialogWindow extends Window implements Button.ClickListener {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LaserNightService nightService;

    @Autowired
    private Factory factory;

    private final NightWindow parent;
    private final LaserNight night;
    private final Button cancelButton;
    private final Button sendButton;

    public SendPrmDialogWindow(NightWindow parent) {
        this.parent = parent;
        this.night = parent.getDisplayedNight();
        this.cancelButton = new Button("Cancel", this);
        this.sendButton = new Button("Upload", this);

        setCaption(getMyCaption());
        setWidth("800px");

        parent.getUI().addWindow(this);
        HorizontalLayout hl = new HorizontalLayout();
        Label filler = new Label("");
        filler.setWidth("100%");
        hl.addComponent(filler);
        hl.addComponent(cancelButton);
        hl.addComponent(sendButton);
        hl.setExpandRatio(filler, 1.0f);
        hl.setSizeFull();
        hl.setSpacing(true);

        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(new Label(getMyDescription(), ContentMode.HTML));
        view.addComponent(hl);
        setContent(view);

        setModal(true);
    }

    private String getMyCaption() {
        final int newLaserTargets = night.getUntransmittedLaserTargets().size();
        if (!night.hasPrmSent())
            return "Send PRMs";
        else if (newLaserTargets == 0)
            return "Resend PRMs";
        else
            return "Send Addendum with Unsent Laser Targets";
    }

    private String getMyDescription() {
        final int newLaserTargets = night.getUntransmittedLaserTargets().size();
        if (!night.hasPrmSent()) {
            return
                    "This will upload the PRM files for this night to space-track.org.<br/>" +
                            "LTTS will normally send the PRM files automatically.<br/><br/>" +
                            "<b>Are you sure you want to do this manually?</b><br/><br/>";
        } else if (newLaserTargets == 0) {
            return
                    "There are currently NO new laser targets that need to be uploaded to space-track.org.<br/><br/>" +
                            "<b>Are you sure you want to upload the original PRM files?</b><br/><br/>";
        } else {
            return
                    "This will upload an updated version of the PRM files for this night to space-track.org.<br/><br/>" +
                            "There " + (newLaserTargets > 1 ? "are" : "is") + " currently " + newLaserTargets + " new laser " +
                            "target" + (newLaserTargets > 1 ? "s" : "") + " which will be sent to LCH<br/>" +
                            "along with all other laser targets from the most recent PRM files.<br/><br/>" +
                            "<b>Are you sure you want to upload the updated target list?</b><br/><br/>";
        }
    }

    public void buttonClick(Button.ClickEvent event) {
        if (event.getButton() == sendButton) {
            // upload PRMs
            nightService.sendPrm(night);
            // reload and redisplay night to reflect any changes
            parent.setNight(night.getId());
            parent.getUI().removeWindow(this);

        } else if (event.getButton() == cancelButton) {
            parent.getUI().removeWindow(this);
        }

    }

}
