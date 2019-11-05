package edu.gemini.lch.web.app.components;

import com.vaadin.data.Property;
import com.vaadin.ui.OptionGroup;
import org.springframework.beans.factory.annotation.Configurable;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A simple time zone selector GUI thingy.
 */
@Configurable(preConstruction = true)
public class TimeZoneSelector extends OptionGroup implements Property.ValueChangeListener {

    private final Collection<Listener> listeners;

    public interface Listener {
        void updateZoneId(ZoneId zoneId);
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
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from this selector.
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Property change listener.
     */
    @Override public void valueChange(Property.ValueChangeEvent event) {
        ZoneId selectedZone = getSelectedZone();
        for (Listener listener : listeners) {
            listener.updateZoneId(selectedZone);
        }
    }

    /**
     * Gets either UTC or the current default time zone depending on user selection.
     * Note that the default time zone is either the site time zone (HST or CSLT) or the manually
     * configured UTC offset time zone. See {@link edu.gemini.lch.services.ConfigurationService} for details.
     */
    public ZoneId getSelectedZone() {
        if ("UTC".equals(getValue())) {
            return ZoneId.of("UTC");
        } else {
            return ZoneId.systemDefault();
        }
    }
}
