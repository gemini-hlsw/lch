package edu.gemini.lch.web.app.components;

import com.vaadin.data.Property;
import com.vaadin.ui.OptionGroup;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A simple time zone selector GUI thingy.
 */
@Configurable(preConstruction = true)
public class TimeZoneSelector extends OptionGroup implements Property.ValueChangeListener {

    private final Collection<Listener> listeners;

    public interface Listener {
        void updateTimeZone(DateTimeZone timeZone);
    }

    /**
     * Creates a time selector.
     */
    public TimeZoneSelector() {
        listeners = new ArrayList<>();
        setMultiSelect(false);
        setImmediate(true);
        addItem("UTC");
        addItem("Local");
        addValueChangeListener(this);
        select("UTC");
        addStyleName("horizontal");
    }

    /**
     * Adds a listener to this selector.
     * @param listener
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from this selector.
     * @param listener
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Property change listener.
     * @param event
     */
    @Override public void valueChange(Property.ValueChangeEvent event) {
        DateTimeZone selectedZone = getSelectedZone();
        for (Listener listener : listeners) {
            listener.updateTimeZone(selectedZone);
        }
    }

    /**
     * Gets either UTC or the current default time zone depending on user selection.
     * Note that the default time zone is either the site time zone (HST or CSLT) or the manually
     * configured UTC offset time zone. See {@link edu.gemini.lch.services.ConfigurationService} for details.
     * @return
     */
    public DateTimeZone getSelectedZone() {
        if ("UTC".equals(getValue())) {
            return DateTimeZone.UTC;
        } else {
            return DateTimeZone.getDefault();
        }
    }
}
