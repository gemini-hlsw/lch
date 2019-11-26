package edu.gemini.lch.web.app.components;

import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

/**
 */
public class DateDialogWindow extends Window implements Button.ClickListener {

    // VAADIN
    protected final DateField startDate;
    protected final DateField endDate;
    private final DateDialogListener listener;
    private final Button okButton;

    public DateDialogWindow(String caption, ZoneId zoneId, DateDialogListener listener) {
        this(caption, zoneId, listener, false);
    }

    protected DateDialogWindow(String caption, ZoneId zoneId, DateDialogListener listener, boolean includeEndField) {
        this.listener = listener;
        startDate = new InlineDateField() {{
            setTimeZone(TimeZone.getTimeZone(zoneId));
            setValue(new Date());
            setResolution(Resolution.DAY);
        }};
        endDate = new InlineDateField() {{
            setTimeZone(TimeZone.getTimeZone(zoneId));
            setValue(new Date());
            setResolution(Resolution.DAY);
        }};

        okButton = new Button("OK", this);

        final Form form = new Form() {{
            setCaption("Select a date.");
            setDescription("The time zone is " + TimeZone.getTimeZone(zoneId).getID());
            addField("startDate", startDate);
            if (includeEndField) {
                addField("endDate", endDate);
                setCaption("Select a start and end date.");
            }
        }};

        final HorizontalLayout buttons = new HorizontalLayout() {{
            addComponent(okButton);
            addComponent(new Button("Cancel", DateDialogWindow.this));
        }};

        final VerticalLayout view = new VerticalLayout() {{
            setMargin(true);
            addComponent(form);
            addComponent(buttons);
        }};

        setCaption(caption);
        setWidth("300px");
        setContent(view);
        setModal(true);
        setResizable(false);

        UI.getCurrent().addWindow(this);
    }

    public ZonedDateTime getDate() {
        return ZonedDateTime.ofInstant(startDate.getValue().toInstant(), ZoneId.systemDefault());
    }

    @Override
    public void buttonClick(Button.ClickEvent event) {
        UI.getCurrent().removeWindow(this);
        if (event.getButton() == okButton) {
            listener.okButtonClicked();
        }
    }

    public interface DateDialogListener {
        void okButtonClicked();
    }

}
