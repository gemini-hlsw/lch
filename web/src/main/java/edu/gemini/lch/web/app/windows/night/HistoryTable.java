package edu.gemini.lch.web.app.windows.night;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.model.EventFile;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.LaserRunEvent;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import edu.gemini.lch.web.app.util.TimeFormatter;
import org.springframework.util.StringUtils;

import java.time.ZoneId;

/**
 */
public final class HistoryTable extends Panel implements TimeZoneSelector.Listener {

    private final BeanContainer<Long, LaserRunEvent> container;
    private final Table table;
    private final NightWindow parent;
    private TimeFormatter timeFormatter;

    public HistoryTable(NightWindow parent) {
        this.parent = parent;
        this.timeFormatter = new TimeFormatter(ZoneId.of("UTC"));

        this.container = new BeanContainer<>(LaserRunEvent.class);
        this.container.setBeanIdProperty("id");

        this.table = new Table();
        this.table.setSizeFull();
        this.table.setContainerDataSource(this.container);
        this.table.addGeneratedColumn("time", new TimeColumnGenerator());
        this.table.addGeneratedColumn("files", new FilesColumnGenerator());
        this.table.setVisibleColumns("time", "message", "files");
        this.table.setColumnWidth("time", 100);

        final Label header = new Label("History");
        header.setStyleName(Reindeer.LABEL_H2);

        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(header);
        view.addComponent(this.table);
        setContent(view);
    }

    public void updateNight(LaserNight night) {
        container.removeAllItems();
        container.addAll(night.getEvents());
        table.setPageLength(Math.min(7, container.size()));
    }

    @Override
    public void updateZoneId(ZoneId zoneId) {
        // update the time formatter and then refresh all rows to update the displayed values
        timeFormatter = new TimeFormatter(zoneId);
        table.refreshRowCache();
    }

    private class TimeColumnGenerator implements Table.ColumnGenerator {
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            LaserRunEvent event = container.getItem(itemId).getBean();
            return timeFormatter.asDateAndTime(event.getTime());
        }
    }

    private class FilesColumnGenerator implements Table.ColumnGenerator {
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            LaserRunEvent event = container.getItem(itemId).getBean();
            HorizontalLayout l = new HorizontalLayout();
            for (EventFile file : event.getFiles()) {
                l.addComponent(new FileViewButton(parent.getUI(), file));
            }
            return l;
        }

        public class FileViewButton extends CustomComponent implements Button.ClickListener {
            private final UI parent;
            private final Button button;
            private final EventFile file;

            public FileViewButton(UI parent, EventFile file) {
                this.parent = parent;
                this.file = file;
                button = new Button();
                button.setStyleName(Reindeer.BUTTON_LINK);
                button.setIcon(getIconForFile(file));
                button.setDescription(file.getName());
                button.addClickListener(this);
                setCompositionRoot(button);
            }

            public void buttonClick (Button.ClickEvent event) {
                new FileShowWindow(parent, file);
            }
        }

        private Resource getIconForFile(EventFile file) {
            switch(file.getType()) {
                case EMAIL_HTML: return new ThemeResource("img/email-small-icon.png");
                case EMAIL: return new ThemeResource("img/email-small-icon.png");
                case PAM: return new ThemeResource("img/pam-file-small-icon.png");
                case PRM: return new ThemeResource("img/prm-file-small-icon.png");
                default: throw new IllegalArgumentException("unknown file type");
            }
        }


    }

    private class FileShowWindow extends Window {
        public FileShowWindow(UI parent, EventFile file) {
            setCaption(file.getName());
            parent.addWindow(this);
            final Layout layout = new FormLayout();
            final Label label = new Label(getText(file), getContentMode(file));
            layout.addComponent(label);
            setContent(layout);
            setWidth("1000px");
            setHeight("600px");
        }
    }

    private String getText(EventFile file) {
        // replace weird line feed combinations (CR+LF and CR only) with LF only
        String better = StringUtils.replace(file.getContent(), "\r\n", "\n");    // replace CR+LF (windows)
        return StringUtils.replace(better, "\r", "\n");                          // replace CR only (old MACs)
    }

    private ContentMode getContentMode(EventFile file) {
        if (file.getType() == EventFile.Type.EMAIL_HTML) {
            return ContentMode.HTML;
        } else {
            return ContentMode.PREFORMATTED;
        }
    }

}
