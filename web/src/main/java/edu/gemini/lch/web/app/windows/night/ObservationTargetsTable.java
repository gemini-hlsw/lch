package edu.gemini.lch.web.app.windows.night;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import edu.gemini.lch.model.*;
import edu.gemini.lch.web.app.util.CoordFormatter;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public abstract class ObservationTargetsTable extends TargetsTable {

    public static class Science extends ObservationTargetsTable {

        public Science(DateTimeZone selectedZone) {
            super(
                    selectedZone,
                    new String[]{"state", "observationId", "target", "targetType", "c1hms", "c2dms", "c1deg", "c2deg", "lra", "ldec", "lradeg", "ldecdeg", "above", "below", "timeline"},
                    new String[]{"State", "Observation Id", "Target", "Type", "Ra", "Dec", "Ra\u00B0", "Dec\u00B0", "LT Ra", "LT Dec", "LT Ra\u00B0", "LT Dec\u00B0", "After", "Before", ""},
                    new String[]{"c1deg", "c2deg", "lradeg", "ldecdeg"}
            );
        }

        public boolean isEmpty() {
            return container.size() == 0;
        }

        public void updateNight(LaserNight night, List<ScienceObservation> observations) {
            super.updateNight(night, ObservationBean.class);
            container.removeAllItems();
            container.addAll(createBeans(observations));
            sort();
        }
        private Collection<ObservationBean> createBeans(Collection<ScienceObservation> observations) {
            List<ObservationBean> beans = new ArrayList<>();
            for (Observation o : observations) {
                beans.add(new ObservationBean(o));
            }
            return beans;
        }
    }

    public static final class AzEl extends ObservationTargetsTable {

        public AzEl() {
            super(
                    DateTimeZone.UTC,
                    new String[]{"state", "observationId", "target", "targetType", "c1deg", "c2deg", "lradeg", "ldecdeg", "timeline"},
                    new String[]{"State", "Observation Id", "Target", "Type", "Az\u00B0", "El\u00B0", "LT Az\u00B0", "LT El\u00B0", ""},
                    new String[]{}
            );
        }
        public void updateNight(LaserNight night) {
            updateNight(night, AzElLaserTarget.class);
        }
    }

    public static final class RaDec extends ObservationTargetsTable {
        public RaDec() {
            super(
                    DateTimeZone.UTC,
                    new String[]{"state", "observationId", "target", "targetType", "c1hms", "c2dms", "c1deg", "c2deg", "lra", "ldec", "lradeg", "ldecdeg", "above", "below", "timeline"},
                    new String[]{"State", "Observation Id", "Target", "Type", "Ra", "Dec", "Ra\u00B0", "Dec\u00B0", "LT Ra", "LT Dec", "LT Ra\u00B0", "LT Dec\u00B0", "After", "Before", ""},
                    new String[]{"c1deg", "c2deg", "lradeg", "ldecdeg"}
            );
        }
        public void updateNight(LaserNight night) {
            updateNight(night, RaDecLaserTarget.class);
        }
    }

    protected ObservationTargetsTable(DateTimeZone zone, String[] visibleColumns, String[] columnNames, String[] collapsedColumns) {
        super(zone, visibleColumns, columnNames, collapsedColumns);
        init(ObservationBean.class);
    }

    @Override protected void init(Class clazz) {
        super.init(clazz);

        // set column widths to make sure everything is visible
        setColumnWidth("state", 80);
        setColumnWidth("c1hms", 80);
        setColumnWidth("c2dms", 80);
        setColumnWidth("observationId", 150);
        setColumnWidth("target", 280);
        setColumnWidth("targetType", 100);
        setColumnWidth("lra", 80);
        setColumnWidth("ldec", 80);
        setColumnWidth("timeline", 900);  // important: this will force horizontal scrollbar to show up

        setSortContainerPropertyId("observationId");
    }

    @Override public void selectionChanged() {
        final ObservationBean bean = (ObservationBean) container.getItem(getValue()).getBean();
        final DetailsPopup details = new DetailsPopup(bean, timeFormatter);
        details.center();
        UI.getCurrent().addWindow(details);
    }

    @Override protected void updateNight(LaserNight night, Class targetClass) {
        super.updateNight(night, ObservationBean.class);
        container.removeAllItems();
        container.addAll(createBeans(night.getEngineeringObservations(), targetClass));
    }

    private Collection<ObservationBean> createBeans(Collection<? extends Observation> observations, Class targetClass) {
        List<ObservationBean> beans = new ArrayList<>();
        for (Observation o : observations) {
            if (o.getTargets().iterator().next().getLaserTarget().getClass().equals(targetClass)) {
                beans.add(new ObservationBean(o));
            }
        }
        return beans;
    }

    public class ObservationBean extends LaserBean {
        private final Observation observation;
        private final ObservationId observationId;
        private final Label target;
        private final Label targetType;
        private final Label c1hms;
        private final Label c2dms;
        private final Label c1deg;
        private final Label c2deg;
        private final Label state;

        protected ObservationBean(Observation o) {
            super(o.getId(), o.getLaserTargetsSortedByType());
            final StringBuilder targetStr = new StringBuilder();
            final StringBuilder targetTypeStr = new StringBuilder();
            final StringBuilder c1hmsStr = new StringBuilder();
            final StringBuilder c2dmsStr = new StringBuilder();
            final StringBuilder c1degStr = new StringBuilder();
            final StringBuilder c2degStr = new StringBuilder();
            final StringBuilder stateStr = new StringBuilder();
            for (ObservationTarget t : o.getTargetsSortedByType()) {
                targetStr.append(t.getName()).append("<br/>");
                targetTypeStr.append(t.getType()).append("<br/>");
                c1hmsStr.append(CoordFormatter.asHMS(t.getDegrees1())).append("<br/>");
                c2dmsStr.append(CoordFormatter.asDMS(t.getDegrees2())).append("<br/>");
                c1degStr.append(CoordFormatter.asDegrees(t.getDegrees1())).append("<br/>");
                c2degStr.append(CoordFormatter.asDegrees(t.getDegrees2())).append("<br/>");
                stateStr.append(t.getState()).append("<br/>");
            }

            this.observation = o;
            this.observationId = new ObservationId(o.getObservationId());
            target = new Label(targetStr.toString(), ContentMode.HTML);
            targetType = new Label(targetTypeStr.toString(), ContentMode.HTML);
            c1hms = new Label(c1hmsStr.toString(), ContentMode.HTML);
            c2dms = new Label(c2dmsStr.toString(), ContentMode.HTML);
            c1deg = new Label(c1degStr.toString(), ContentMode.HTML);
            c2deg = new Label(c2degStr.toString(), ContentMode.HTML);
            state = new Label(stateStr.toString(), ContentMode.HTML);
        }

        public ObservationId getObservationId() { return observationId; }
        public Label getTarget() { return target; }
        public Label getTargetType() { return targetType; }
        public Label getC1hms() { return c1hms; }
        public Label getC2dms() { return c2dms; }
        public Label getC1deg() { return c1deg; }
        public Label getC2deg() { return c2deg; }
        public Label getState() { return state; }

        Observation getObservation() {
            return observation;
        }
    }

}
