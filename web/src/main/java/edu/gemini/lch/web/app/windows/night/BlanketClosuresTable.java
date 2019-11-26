package edu.gemini.lch.web.app.windows.night;

import com.vaadin.data.Validator;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.model.BlanketClosure;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.web.app.components.ActionColumn;
import edu.gemini.lch.web.app.components.EditDialogWindow;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import edu.gemini.lch.web.app.util.TimeFormatter;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A table for displaying and editing engineering targets.
 */
@Configurable(preConstruction = true)
public final class BlanketClosuresTable extends Panel implements EditDialogWindow.Listener, TimeZoneSelector.Listener, ActionColumn.Listener {

    @Autowired
    private LaserNightService laserNightService;

    private final ActionColumn.Action editAction;
    private final ActionColumn.Action deleteAction;
    private final ActionColumn.Action endNowAction;

    private final BeanContainer<Long, BlanketClosure> container;
    private final NightWindow parent;
    private final Table table;
    private final NewButton newButton;
    private LaserNight night;
    private TimeFormatter timeFormatter;

    public BlanketClosuresTable(final NightWindow parent) {
        this.parent = parent;
        this.timeFormatter = new TimeFormatter(ZoneId.of("UTC"));

        this.container = new BeanContainer<>(BlanketClosure.class);
        this.container.setBeanIdProperty("id");

        this.editAction = ActionColumn.createEditAction(this);
        this.endNowAction = new ActionColumn.Action(new ThemeResource("img/stop-icon.png"), this, "End this closure now.");
        this.deleteAction = ActionColumn.createDeleteAction(this);

        final ActionColumn actions = new ActionColumn(parent.getUI(), this.container);
        actions.addAction(editAction);
        actions.addAction(endNowAction);
        actions.addAction(deleteAction);

        this.table = new Table();
        this.table.setContainerDataSource(this.container);
        this.table.addGeneratedColumn("start", new StartColumnGenerator());
        this.table.addGeneratedColumn("end", new EndColumnGenerator());
        this.table.addGeneratedColumn("actions", actions);
        this.table.setVisibleColumns("start", "end", "actions");
        this.table.setPageLength(4);

        // enable once first night is loaded
        this.newButton = new NewButton() {{setEnabled(false);}};

        final Label header = new Label("Blanket Closures");
        header.setStyleName(Reindeer.LABEL_H2);

        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(header);
        view.addComponent(this.table);
        view.addComponent(this.newButton);
        setContent(view);
    }

    public void updateNight(final LaserNight night) {
        this.night = night;
        this.newButton.setEnabled(true);
        container.removeAllItems();
        container.addAll(night.getClosures());
    }

    @Override
    public void updateZoneId(final ZoneId zoneId) {
        // update the time formatter and then refresh all rows to update the displayed values
        timeFormatter = new TimeFormatter(zoneId);
        table.refreshRowCache();
    }

    private class StartColumnGenerator implements Table.ColumnGenerator {
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            BlanketClosure event = container.getItem(itemId).getBean();
            return timeFormatter.asTime(event.getStart());
        }
    }

    private class EndColumnGenerator implements Table.ColumnGenerator {
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            BlanketClosure event = container.getItem(itemId).getBean();
            return timeFormatter.asTime(event.getEnd());
        }
    }


    private class NewButton extends CustomComponent implements Button.ClickListener {
        final com.vaadin.ui.Button button;

        public NewButton() {
            button = new Button("New");
            button.addClickListener(this);
            setCompositionRoot(button);
        }

        public void buttonClick (final Button.ClickEvent event) {
            new EditDialogWindow(parent.getUI(), null, BlanketClosuresTable.this);
        }
    }

    @Override
    public void actionClick(final ActionColumn.Action action, final Object itemId) {
        if (action.equals(endNowAction)) {
            endNow(itemId);

        } else if (action.equals(editAction)) {
            new EditDialogWindow(parent.getUI(), itemId, this);

        } else if (action.equals(deleteAction)) {
            delete(itemId);

        } else {
            throw new RuntimeException("unknown action");
        }
    }

    private void endNow(final Object itemId) {
        final BlanketClosure closure = container.getItem(itemId).getBean();
        ZonedDateTime now = ZonedDateTime.now();
        if (closure.getStart().isBefore(now) && closure.getEnd().isAfter(now) {
            closure.setEnd(now);
            laserNightService.updateClosure(night, closure);
            parent.setNight(night.getId());
        }
    }

    private void delete(final Object itemId) {
        final BlanketClosure closure = container.getItem(itemId).getBean();
        laserNightService.deleteClosure(night, closure);
        parent.setNight(night.getId());
    }

    // -- EDIT FORM

    @Override
    public Form getForm(final Object itemId) {
        final BlanketClosureForm form = new BlanketClosureForm(night);
        final BlanketClosure closure;
        if (itemId != null) {
            closure = container.getItem(itemId).getBean();
            form.setStartTime(closure.getStart());
            form.setEndTime(closure.getEnd());
        }
        return form;
    }

    @Override
    public void okButtonClicked(final Form xForm, final Object itemId) {
        final BlanketClosureForm form = (BlanketClosureForm) xForm;
        if (itemId != null) {
            BlanketClosure bean = container.getItem(itemId).getBean();
            bean.setStart(form.getStartTime());
            bean.setEnd(form.getEndTime());
            laserNightService.updateClosure(night, bean);
        } else {
            laserNightService.addClosure(night, form.getStartTime(), form.getEndTime());
        }
        // NOTE: since we're reloading/updating everything we don't even have to add the new bean to the container!
        parent.setNight(night.getId());
    }

    private class BlanketClosureForm extends Form {
        private final LaserNight night;
        private final TextField startTime;
        private final TextField endTime;

        BlanketClosureForm(final LaserNight night) {
            setCaption("Edit Blanket Closure");
            setDescription("Edit start and end time of blanket closure.");

            setSizeFull();

            this.night = night;
            final ZonedDateTime start = mapToNight(ZonedDateTime.now().withSecond(0).withNano(0));
            final ZonedDateTime end   = mapToNight(start.plusMinutes(1));

            startTime = new TextField();
            startTime.setValue(timeFormatter.asTimeLong(start));
            startTime.addValidator(new ValidFormatValidator());
            startTime.setRequired(true);
            addField("start", startTime);

            endTime = new TextField();
            endTime.setValue(timeFormatter.asTimeLong(end));
            endTime.addValidator(new ValidFormatValidator());
            endTime.addValidator(new EndIsAfterStartValidator());
            endTime.setRequired(true);
            addField("end", endTime);

            setValidationVisible(true);
            setImmediate(true);
        }

        void setStartTime(final ZonedDateTime startTime) {
            this.startTime.setValue(timeFormatter.asTimeLong(startTime));
        }
        void setEndTime(final ZonedDateTime endTime) {
            this.endTime.setValue(timeFormatter.asTimeLong(endTime));
        }

        ZonedDateTime getStartTime() {
            try {
                return mapToNight(timeFormatter.fromTimeLong(startTime.getValue()));
            } catch (IllegalArgumentException e) {
                return night.getStart();
            }
        }
        ZonedDateTime getEndTime() {
            try {
                return mapToNight(timeFormatter.fromTimeLong(endTime.getValue()));
            } catch (IllegalArgumentException e) {
                return night.getStart().plusMinutes(1);
            }
        }

        private ZonedDateTime mapToNight(final ZonedDateTime time) {
            final ZonedDateTime time2 = ZonedDateTime.ofInstant(night.getStart().toInstant(), time.getZone());
            final ZonedDateTime mapped2 = time2.withHour(time.getHour()).withMinute(time.getMinute()).withSecond(time.getSecond()).withNano(0);
            final ZonedDateTime result;
            if (mapped2.isBefore(night.getStart())) {
                result = mapped2.plusDays(1);
            } else {
                result = mapped2;
            }
            Validate.isTrue(result.isAfter(night.getStart().truncatedTo(ChronoUnit.DAYS)));
            Validate.isTrue(result.isBefore(night.getEnd().truncatedTo(ChronoUnit.DAYS).plusDays(1)));
            return result;
        }

        private class ValidFormatValidator implements Validator {
            public boolean isValid(Object value) {
                try {
                    timeFormatter.fromTimeLong(startTime.getValue());
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            public void validate(Object value) {
                if (!isValid(value)) {
                    throw new InvalidValueException("Time must be in format hh:mm:ss.");
                }
            }
        }

        private class EndIsAfterStartValidator implements Validator {
            public boolean isValid(Object value) {
                return getEndTime().isAfter(getStartTime());
            }

            public void validate(Object value) {
                if (!isValid(value)) {
                    throw new InvalidValueException("End time must be after start time.");
                }
            }
        }
    }

}
