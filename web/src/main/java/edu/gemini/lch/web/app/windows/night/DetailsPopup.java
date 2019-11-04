package edu.gemini.lch.web.app.windows.night;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import edu.gemini.lch.model.LaserTarget;
import edu.gemini.lch.model.Observation;
import edu.gemini.lch.model.ObservationTarget;
import edu.gemini.lch.model.PropagationWindow;
import edu.gemini.lch.web.app.util.CoordFormatter;
import edu.gemini.lch.web.app.util.TimeFormatter;

import java.time.Duration;
import java.util.Collection;

/**
 * Simple popup to show details about an observation and/or a laser target when users click on a row in
 * one of the target tables.
 */
public final class DetailsPopup extends Window {

    private final TimeFormatter timeFormatter;

    public DetailsPopup(final TargetsTable.LaserBean target, final TimeFormatter timeFormatter) {
        this.timeFormatter = timeFormatter;
        showLaserTarget(target.getLaserTargets().iterator().next());
    }

    public DetailsPopup(final ObservationTargetsTable.ObservationBean target, final TimeFormatter timeFormatter) {
        this.timeFormatter = timeFormatter;
        showObservation(target.getObservation());
    }

    private void showObservation(final Observation o) {
        setCaption("Science Target");
        setWidth("" + o.getTargets().size() * 260 + "px");

        final GridLayout grid = new GridLayout(o.getTargets().size(), 2);
        grid.setMargin(true);
        grid.setSpacing(true);

        final Label label = new Label(o.getObservationId());
        label.setStyleName(Reindeer.LABEL_H2);
        grid.addComponent(label, 0, 0);

        int column = 0;
        for (final ObservationTarget t : o.getTargetsSortedByType()) {
            final VerticalLayout ll = new VerticalLayout();
            ll.addComponent(new Label(t.getName() + " (" + t.getType() + ")"));
            ll.addComponent(new ObservationTargetDetails(t));
            ll.addComponent(new Label("Laser Target"));
            ll.addComponent(targetComponent(t.getLaserTarget()));
            grid.addComponent(ll, column++, 1);
        }

        // putting the grid in a panel will add horizontal scroll bars (if needed)
        final Panel panel = new Panel();
        panel.setContent(grid);
        setContent(panel);
    }

    private void showLaserTarget(final LaserTarget t) {
        setCaption("Laser Target");
        setWidth("250px");

        final Label label = new Label("Laser Target");
        label.setStyleName(Reindeer.LABEL_H2);
        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(label);
        view.addComponent(targetComponent(t));
        setContent(view);
    }

    private Component targetComponent(final LaserTarget t) {
        final VerticalLayout l = new VerticalLayout();
        l.setSpacing(true);

        l.addComponent(new LaserTargetDetails(t));
        if (t.getPropagationWindows().size() > 0) {
            l.addComponent(new Label("Clearance Windows"));
            l.addComponent(new PropagationWindowsTable(t.getPropagationWindows()));
        } else {
            l.addComponent(new Label("<No Clearance Windows>"));
        }
        setContent(l);
        return l;
    }

    private class ObservationTargetDetails extends Table {
        ObservationTargetDetails(final ObservationTarget o) {
            int rows = 0;
            double ra = o.getDegrees1();
            double dec = o.getDegrees2();
            addContainerProperty("ra", String.class, "");
            addContainerProperty("dec", String.class, "");
            addItem(new Object[]{CoordFormatter.asHMS(ra), CoordFormatter.asDMS(dec)}, rows++);
            addItem(new Object[]{CoordFormatter.asDegrees(ra), CoordFormatter.asDegrees(dec)}, rows++);
            setPageLength(rows);
            setSizeFull();
        }
    }

    private class LaserTargetDetails extends Table {
        LaserTargetDetails(final LaserTarget l) {
            int rows = 0;
            double ra = l.getDegrees1();
            double dec = l.getDegrees2();
            addContainerProperty("ra", String.class, "");
            addContainerProperty("dec", String.class, "");
            addItem(new Object[]{CoordFormatter.asHMS(ra), CoordFormatter.asDMS(dec)}, rows++);
            addItem(new Object[]{CoordFormatter.asDegrees(ra), CoordFormatter.asDegrees(dec)}, rows++);
            setPageLength(rows);
        }
    }

    private class PropagationWindowsTable extends Table {
        PropagationWindowsTable(final Collection<PropagationWindow> windows) {
            final BeanItemContainer<PropagationWindow> container = new BeanItemContainer<>(PropagationWindow.class);
            container.addAll(windows);
            setContainerDataSource(container);
            addGeneratedColumn("start", (source, itemId, columnId)    -> timeFormatter.asTimeLong(container.getItem(itemId).getBean().getStart()));
            addGeneratedColumn("end", (source, itemId, columnId)      -> timeFormatter.asTimeLong(container.getItem(itemId).getBean().getEnd()));
            addGeneratedColumn("duration", (source, itemId, columnId) -> {
                Duration duration = container.getItem(itemId).getBean().getDuration();
                return TimeFormatter.asDuration(duration);
            });
            setVisibleColumns("start", "end", "duration"); // change order
            setPageLength(windows.size());                 // make page as long as needed
        }
    }


}