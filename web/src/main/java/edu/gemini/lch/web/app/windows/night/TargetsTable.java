package edu.gemini.lch.web.app.windows.night;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.LaserTarget;
import edu.gemini.lch.services.LaserTargetsService;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import edu.gemini.lch.web.app.util.CoordFormatter;
import edu.gemini.lch.web.app.util.TimeFormatter;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 */
@Configurable(preConstruction = true)
public abstract class TargetsTable extends Table implements TimeZoneSelector.Listener {

    @Autowired private LaserTargetsService targetService;

    protected BeanContainer<Long, LaserBean> container;

    private static final String[] defaultVisibleColumns = new String[]{"lra", "ldec", "lradeg", "ldecdeg", "above", "below", "transmitted", "timeline"};
    private static final String[] defaultColumnsNames = new String[]{"LT Ra", "LT Dec", "LT Ra\u00B0", "LT Dec\u00B0", "After", "Before", "Transmitted", ""};
    private static final String[] defaultCollapsedColumns = new String[]{"lradeg", "ldecdeg"};

    private final String[] visibleColumns;
    private final String[] columnsNames;
    private final String[] collapsedColumns;

    protected TimeFormatter timeFormatter;
    private ZoneId currentTimeZone;
    private Optional<LaserNight> night;

    public TargetsTable () {
        // use UTC as default
        this(ZoneId.of("UTC"), defaultVisibleColumns, defaultColumnsNames, defaultCollapsedColumns);
    }

    public TargetsTable(ZoneId zone, String[] visibleColumns, String[] columnNames, String[] collapsedColumns) {
        this.night = Optional.empty();
        this.currentTimeZone = zone;
        this.timeFormatter = new TimeFormatter(currentTimeZone);
        this.visibleColumns = visibleColumns;
        this.columnsNames = columnNames;
        this.collapsedColumns = collapsedColumns;
    }

    protected void init(Class clazz) {
        // show row number as first column
        setRowHeaderMode(RowHeaderMode.INDEX);

        // prepare timeline column
        setSizeFull();
        setColumnExpandRatio("timeline", 1.0f);
        setColumnHeader("timeline", "");

        // make rows clickable
        setSelectable(true);
        setImmediate(true);
        addValueChangeListener(e -> selectionChanged());

        // limit size
        setCacheRate(25);
        setPageLength(20);

        this.container = new BeanContainer<>(clazz);
        this.container.setBeanIdProperty("id");

        setContainerDataSource(this.container);

        // set default columns and their headers
        Validate.isTrue(visibleColumns.length == columnsNames.length);
        setVisibleColumns(visibleColumns);
        for (int i = 0; i < columnsNames.length; i++) {
            setColumnHeader(visibleColumns[i], columnsNames[i]);
        }
        setColumnWidth(null, 22);

        // set default collapsed
        setColumnCollapsingAllowed(true);
        for (String collapsedColumn : collapsedColumns) {
            setColumnCollapsed(collapsedColumn, true);
        }
    }

    protected void updateNight(LaserNight night, Class clazz) {
        this.night = Optional.of(night);
        updateTimeLineHeader(night, currentTimeZone);
    }

    private void updateTimeLineHeader(final LaserNight night, final ZoneId zone) {
        final byte[] headerImage = targetService.getImageHeader(night, 900, startOfNight(night), endOfNight(night), zone);
        final String name = "timeline-header"+UUID.randomUUID()+".png";
        final StreamResource imageresource = new StreamResource(() -> new ByteArrayInputStream(headerImage), name);
        setColumnIcon("timeline", imageresource);
    }

    private ZonedDateTime startOfNight(final LaserNight night) {
        return ZonedDateTime.ofInstant(night.getStart().toInstant(), ZoneId.systemDefault()).withMinute(0).withSecond(0).withNano(0);
    }

    private ZonedDateTime endOfNight(final LaserNight night) {
        return ZonedDateTime.ofInstant(night.getEnd().toInstant(), ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    @Override
    public void updateZoneId(final ZoneId zone) {
        night.ifPresent(night -> {
            updateTimeLineHeader(night, zone);
            currentTimeZone = zone;
            timeFormatter = new TimeFormatter(zone);
            for (final Long id : container.getItemIds()) {
                final BeanItem<LaserBean> item = container.getItem(id);
                item.getBean().updateTimes(item);
            }
        });
    }

    public void selectionChanged() {
        final LaserBean bean =  container.getItem(getValue()).getBean();
        final DetailsPopup details = new DetailsPopup(bean, timeFormatter);
        details.center();
        UI.getCurrent().addWindow(details);
    }

    private List<LaserTarget> createList(final LaserTarget laserTarget) {
        return new ArrayList<LaserTarget>() {{
                add(laserTarget);
            }};
    }

    public class LaserBean {
        private final List<LaserTarget> targets;
        private final boolean hasDifferentTargets;
        private final Long id;
        private final Label lra;
        private final Label ldec;
        private final Label lradeg;
        private final Label ldecdeg;
        private final Label transmitted;
        private final Component timeline;
        private Label above;
        private Label below;

        public LaserBean(Long id, LaserTarget laserTarget) {
            this(id, createList(laserTarget));
        }

        public LaserBean(Long id, List<LaserTarget> laserTargets) {
            this.id = id;
            targets = laserTargets;
            hasDifferentTargets = hasDifferentLaserTargets(targets);

            if (hasDifferentTargets) {

                // -- if there are several laser targets for this observation we show all of them on separate lines
                // (note that the order of the laser targets must be equal to the order they are displayed in
                // the observation columns!)
                final StringBuilder lraStr = new StringBuilder();
                final StringBuilder ldecStr = new StringBuilder();
                final StringBuilder lradegStr = new StringBuilder();
                final StringBuilder ldecdegStr = new StringBuilder();
                final StringBuilder aboveStr = new StringBuilder();
                final StringBuilder belowStr = new StringBuilder();
                final StringBuilder transmittedStr = new StringBuilder();
                VerticalLayout layout = new VerticalLayout();
                for (final LaserTarget laserTarget : targets) {
                    lraStr.append(CoordFormatter.asHMS(laserTarget.getDegrees1())).append("<br/>");
                    ldecStr.append(CoordFormatter.asDMS(laserTarget.getDegrees2())).append("<br/>");
                    lradegStr.append(CoordFormatter.asDegrees(laserTarget.getDegrees1())).append("<br/>");
                    ldecdegStr.append(CoordFormatter.asDegrees(laserTarget.getDegrees2())).append("<br/>");
                    aboveStr.append(timeFormatter.asTime(laserTarget.getRiseTime())).append("<br/>");
                    belowStr.append(timeFormatter.asTime(laserTarget.getSetTime())).append("<br/>");
                    transmittedStr.append(laserTarget.isTransmitted() ? "Yes" : "No");
                    layout.addComponent(createTimeline(night.get(), laserTarget, 1));
                }
                lra = new Label(lraStr.toString(), ContentMode.HTML);
                ldec = new Label(ldecStr.toString(), ContentMode.HTML);
                lradeg = new Label(lradegStr.toString(), ContentMode.HTML);
                ldecdeg = new Label(ldecdegStr.toString(), ContentMode.HTML);
                above = new Label(aboveStr.toString(), ContentMode.HTML);
                below = new Label(belowStr.toString(), ContentMode.HTML);
                transmitted = new Label(transmittedStr.toString(), ContentMode.HTML);
                timeline = layout;

            } else {

                // -- if all observation targets are represented by the same laser target we only show
                // one laser target
                LaserTarget laserTarget = targets.iterator().next();
                lra = new Label(CoordFormatter.asHMS(laserTarget.getDegrees1()));
                ldec = new Label(CoordFormatter.asDMS(laserTarget.getDegrees2()));
                lradeg = new Label(CoordFormatter.asDegrees(laserTarget.getDegrees1()));
                ldecdeg = new Label(CoordFormatter.asDegrees(laserTarget.getDegrees2()));
                above = new Label(timeFormatter.asTime(laserTarget.getRiseTime()));
                below = new Label(timeFormatter.asTime(laserTarget.getSetTime()));
                transmitted = new Label(laserTarget.isTransmitted() ? "Yes" : "No");
                timeline = createTimeline(night.get(), laserTarget, targets.size());
            }
        }

        public void updateTimes(BeanItem<LaserBean> item) {
            item.getItemProperty("above").setReadOnly(false);
            item.getItemProperty("below").setReadOnly(false);
            if (hasDifferentTargets) {
                final StringBuilder aboveStr = new StringBuilder();
                final StringBuilder belowStr = new StringBuilder();
                for (final LaserTarget laserTarget : targets) {
                    aboveStr.append(timeFormatter.asTime(laserTarget.getRiseTime())).append("<br/>");
                    belowStr.append(timeFormatter.asTime(laserTarget.getSetTime())).append("<br/>");
                }
                item.getItemProperty("above").setValue(new Label(aboveStr.toString(), ContentMode.HTML));
                item.getItemProperty("below").setValue(new Label(belowStr.toString(), ContentMode.HTML));
            } else {
                final LaserTarget laserTarget = targets.iterator().next();
                item.getItemProperty("above").setValue(new Label(timeFormatter.asTime(laserTarget.getRiseTime())));
                item.getItemProperty("below").setValue(new Label(timeFormatter.asTime(laserTarget.getSetTime())));
            }
        }

        public Long getId() { return id; }
        public Label getLra() { return lra; }
        public Label getLdec() { return ldec; }
        public Label getLradeg() { return lradeg; }
        public Label getLdecdeg() { return ldecdeg; }
        public Label getTransmitted() { return transmitted; }
        public Label getAbove() { return above; }
        public void setAbove(Label above) { this.above = above; }
        public Label getBelow() { return below; }
        public void setBelow(Label below) { this.below = below; }

        public Component getTimeline () { return timeline; }

        private Embedded createTimeline(final LaserNight night, final LaserTarget target, final int rows) {
            final String name = "timeline-"+ UUID.randomUUID() +".png";
            final byte[] bytes = targetService.getImage(night, target, 900, rows*11, startOfNight(night), endOfNight(night));
            final StreamResource imageresource = new StreamResource(() -> new ByteArrayInputStream(bytes), name);
            imageresource.setCacheTime(0);
            return new Embedded(null, imageresource);
       }

        private boolean hasDifferentLaserTargets(final List<LaserTarget> laserTargets) {
            final Set<Long> ids = new HashSet<>();
            for (LaserTarget t : laserTargets) { ids.add(t.getId()); }
            return ids.size() > 1;
        }

        List<LaserTarget> getLaserTargets() {
            return targets;
        }
    }

}
