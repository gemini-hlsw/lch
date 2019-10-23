package edu.gemini.lch.web.app.windows.admin;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.*;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.web.app.components.AudioElement;
import edu.gemini.lch.web.app.components.Footer;
import edu.gemini.lch.web.app.components.Header;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.List;

@Configurable(preConstruction = true)
@PreserveOnRefresh
public class AdministrationWindow extends VerticalLayout implements View {

    private static final Logger LOGGER = Logger.getLogger(AdministrationWindow.class);

    public static final String NAME = "administration";

    @Autowired
    private ConfigurationService configurationService;

    private final AudioElement audio;
    private final EngineeringTargetsTable engTargetsTable;

    public AdministrationWindow() {
        this.setCaption(AdministrationWindow.NAME);
        this.engTargetsTable = new EngineeringTargetsTable(this);
        //this.engTargetsTable = new EngineeringTargetsTable(this.getUI());
        //this.engTargetsTable = new EngineeringTargetsTable(UI.getCurrent());
        this.audio = new AudioElement();

        Header header = new AdminWindowHeader(this);
        Footer footer = new Footer();

        addComponent(header.getComponent());
        addComponent(createBody());
        addComponent(footer.getComponent());
        addComponent(audio);
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent e) {
        UI.getCurrent().getPage().setTitle("LTTS Administration");
    }

    public void playTestAudio() {
        audio.playTestAudio();
    }

    private Component createBody() {
        List<String> configGroups = configurationService.getConfigurationGroups();

        TabSheet tabsheet = new TabSheet();
        for (String group : configGroups) {
            ConfigurationEntriesTable table = new ConfigurationEntriesTable(this, group);
            tabsheet.addTab(table.getComponent(), group);
        }
        tabsheet.addTab(engTargetsTable.getComponent(), "Engineering Alt-Az Targets");

        HorizontalLayout layout = new HorizontalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.addComponent(tabsheet);

        return layout;
    }

}
