package edu.gemini.lch.web.app.windows.alarm;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;
import edu.gemini.lch.model.PropagationWindow;
import edu.gemini.lch.model.ShutteringWindow;
import edu.gemini.lch.services.AlarmService;
import edu.gemini.lch.services.EpicsService;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import edu.gemini.lch.web.app.util.TimeFormatter;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Base class for displaying tables of windows (propagation and shuttering windows) in the alarm window.
 * @param <T>
 */
abstract class WindowsTable<T extends edu.gemini.lch.model.Window> extends Table implements TimeZoneSelector.Listener {

    private static final Logger LOGGER = Logger.getLogger(WindowsTable.class);

    protected static final Integer MIN_PAGE_LENGTH = 8;
    protected final BeanItemContainer<T> container;
    protected TimeFormatter timeFormatter;

    /**
     * Constructs a windows table.
     */
    WindowsTable(ZoneId zoneId, Class clazz) {
        timeFormatter = new TimeFormatter(zoneId);
        container = new BeanItemContainer<>(clazz);
        setContainerDataSource(container);
        addGeneratedColumn("start", (source, itemId, columnId)    -> timeFormatter.asTimeLong(container.getItem(itemId).getBean().getStart()));
        addGeneratedColumn("end", (source, itemId, columnId)      -> timeFormatter.asTimeLong(container.getItem(itemId).getBean().getEnd()));
        addGeneratedColumn("duration", (source, itemId, columnId) -> {
            Duration duration = container.getItem(itemId).getBean().getDuration();
            return TimeFormatter.asDuration(duration);
        });

        setVisibleColumns("start", "end", "duration");
        setPageLength(MIN_PAGE_LENGTH);
        setSizeFull();
    }

    /**
     * Updates the table with the latest data.
     */
    abstract void update(AlarmService.Snapshot snapshot);

    /**
     * Updates the time zone used for displaying times.
     */
    @Override public void updateZoneId(ZoneId zoneId) {
        timeFormatter = new TimeFormatter(zoneId);
        refreshRowCache(); // refresh the table to make new formatter have an effect
    }

    /** Propagation windows table. */
    static final class Propagation extends WindowsTable<PropagationWindow> {

        private Optional<PropagationWindow> current;

        Propagation(final ZoneId zone) {
            super(zone, PropagationWindow.class);
        }
        Propagation(final ZoneId zoneId, final EpicsService epicsService, final AlarmService.Snapshot snapshot) {
            super(zoneId, PropagationWindow.class);
            container.addAll(snapshot.getPropagationWindows());
            setCellStyleGenerator(new CellStyleGenerator(epicsService));
            setPageLength(Math.max(MIN_PAGE_LENGTH, container.size()));
        }

        /** {@inheritDoc} */
        @Override void update(final AlarmService.Snapshot snapshot) {
            final ZonedDateTime currentTime = snapshot.getEpicsSnapshot().getTime();
            final Optional<PropagationWindow> cur = container.getItemIds().stream().filter(w -> w.contains(currentTime)).findFirst();
            if (!cur.equals(current)) {
                // updating client-side tables is very CPU intensive for the browser and should only be done if necessary
                LOGGER.trace("Updating propagation windows table.");
                refreshRowCache();
                current = cur;
            }
        }

        // do some layout tricks...
        private class CellStyleGenerator implements  Table.CellStyleGenerator {
            private final EpicsService epicsService;
            CellStyleGenerator(final EpicsService epicsService) {
                this.epicsService = epicsService;
            }
            /** {@inheritDoc} */
            @Override public String getStyle(final Table table, final Object itemId, final Object propertyId) {
                ZonedDateTime currentTime = epicsService.getTime();
                PropagationWindow w = container.getItem(itemId).getBean();
                if (w.contains(currentTime)) {
                    return "ok";
                } else {
                    return null;
                }
            }
        }
    }

    /** Shuttering windows table */
    static final class Shuttering extends WindowsTable<ShutteringWindow> {

        private Optional<ShutteringWindow> current;

        Shuttering(final ZoneId zoneId) {
            super(zoneId, ShutteringWindow.class);
        }

        Shuttering(final ZoneId zoneId, final EpicsService epicsService, final AlarmService.Snapshot snapshot) {
            super(zoneId, ShutteringWindow.class);
            current = Optional.empty();
            container.addAll(snapshot.getShutteringWindows());
            setCellStyleGenerator(new CellStyleGenerator(snapshot.getShutteringWindows(), epicsService));
            setPageLength(Math.max(8, container.size()));
        }

        /** {@inheritDoc} */
        @Override void update(final AlarmService.Snapshot snapshot) {
            final ZonedDateTime currentTime = snapshot.getEpicsSnapshot().getTime();
            final Optional<ShutteringWindow> cur = container.getItemIds().stream().filter(w -> w.contains(currentTime)).findFirst();
            if (!cur.equals(current)) {
                // updating client-side tables is very CPU intensive for the browser and should only be done if necessary
                LOGGER.trace("Updating shuttering windows table.");
                refreshRowCache();
                current = cur;
            }
        }

        // do some layout tricks...
        private final class CellStyleGenerator implements  Table.CellStyleGenerator {
            private final List<ShutteringWindow> windows;
            private final EpicsService epicsService;

            CellStyleGenerator(final List<ShutteringWindow> windows, final EpicsService epicsService) {
                this.windows = windows;
                this.epicsService = epicsService;
            }

            public String getStyle(final Table table, final Object itemId, final Object propertyId) {
                final ShutteringWindow w = container.getItem(itemId).getBean();
                final ZonedDateTime currentTime = epicsService.getTime();
                if (w.contains(currentTime)) {
                    return "alarm";
                } else if (isNextWindow(w, currentTime)) {
                    return "warn";
                } else {
                    return null;
                }
            }

            private Boolean isNextWindow(final ShutteringWindow candidate, final ZonedDateTime currentTime) {
                for (final ShutteringWindow w : windows) {
                    // if there is a window that covers the current time we don't have an upcoming window
                    if (w.contains(currentTime)) {
                        return false;
                    }
                    // check if first window that starts after current time is our candidate or not
                    if (w.getStart().isAfter(currentTime)) {
                        return w == candidate;
                    }
                }
                // none of the windows starts after current time
                return false;
            }
        }
    }

}
