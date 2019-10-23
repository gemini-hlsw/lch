package edu.gemini.lch.web.app.windows.alarm;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import edu.gemini.lch.model.*;
import edu.gemini.lch.services.AlarmService;
import edu.gemini.lch.services.EpicsService;
import edu.gemini.lch.services.LtcsService;
import edu.gemini.lch.web.app.util.TimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.List;

abstract class StatusPanel extends Panel {

    protected final Label label;

    public StatusPanel() {
        label = new Label();
        label.setStyleName("bigfont");
        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(label);
        layout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setHeight(80, Unit.PIXELS);
        setContent(layout);
        setStyleName("countdown");
    }

    /**
     * Sets color and text of status panel according to current system state.
     * @param snapshot
     */
    public abstract void update(AlarmService.Snapshot snapshot);

    protected void setInactive() {
        setInactive("--:--:--");
    }

    protected void setInactive(String text) {
        setMyStyle("inactive");
        label.setValue(text);
    }

    protected void setOk(Duration duration) {
        setMyStyle("ok");
        label.setValue(TimeFormatter.asDuration(duration));
    }

    protected void setOk(String text) {
        setMyStyle("ok");
        label.setValue(text);
    }

    protected void setWarn(Duration duration) {
        setMyStyle("warn");
        label.setValue(TimeFormatter.asDuration(duration));
    }

    protected void setWarn(String text) {
        setMyStyle("warn");
        label.setValue(text);
    }

    protected void setAlarm(Duration duration) {
        setMyStyle("alarm");
        label.setValue(TimeFormatter.asDuration(duration));
    }

    protected void setAlarm(String text) {
        setMyStyle("alarm");
        label.setValue(text);
    }

    protected void setMyStyle(String style) {
        removeStyleName("inactive");
        removeStyleName("ok");
        removeStyleName("warn");
        removeStyleName("alarm");
        addStyleName(style);
    }

    /**
     * Status panel for current laser status.
     */
    public static class Laser extends StatusPanel {

        /** {@inheritDoc} */
        @Override public void update(AlarmService.Snapshot snapshot) {
            String laserStatus = snapshot.getEpicsSnapshot().getLaserStatus();
            if ("SKY".equals(laserStatus)) {
                setOk("ON SKY");
            } else if ("LASER SHUTTER".equals(laserStatus)) {
                setAlarm("SHUTTERED");
            } else if ("BDM".equals(laserStatus)) {
                setAlarm("BDM");
            } else {
                setInactive(laserStatus);
            }
        }
    }

    /**
     * Status panel for auto shutter.
     */
    public static class AutoShutter extends StatusPanel {

        /** {@inheritDoc} */
        @Override public void update(AlarmService.Snapshot snapshot) {
            switch(snapshot.getAutoShutter()) {
                case OFF:
                    setInactive("OFF");
                    break;
                case INACTIVE:
                    setInactive("INACTIVE");
                    break;
                case CLEAR:
                    setOk("CLEAR");
                    break;
                case SHUTTERING:
                    setWarn("SHUTTERING");
                    break;
                case SHUTTERED:
                    setAlarm("SHUTTERED");
                    break;
                default:
                    throw new IllegalArgumentException("unknown state");
            }
        }
    }

    /**
     * Status panel for propagation countdown.
     */
    public static class Propagation extends StatusPanel {

        /** {@inheritDoc} */
        @Override public void update(AlarmService.Snapshot snapshot) {

            if (snapshot.getTarget() == null) {
                setInactive();
                return;
            }

            if (!snapshot.getEpicsSnapshot().isConnected()) {
                setAlarm("EPICS DISCONNECTED");
                return;
            }

            DateTime epicsTime = snapshot.getEpicsSnapshot().getTime();
            Boolean isOnSky = snapshot.getEpicsSnapshot().isOnSky();

            // inside a propagation window: show remaining time in current propagation window
            for (PropagationWindow w : snapshot.getPropagationWindows()) {
                if (w.contains(epicsTime)) {
                    Duration duration = new Duration(epicsTime, w.getEnd());
                    if (isOnSky && duration.getStandardSeconds() <= 30 && duration.getStandardSeconds() % 2 == 0) {
                        setAlarm(duration);
                    } else if (isOnSky && duration.getStandardMinutes() < 5) {
                        setWarn(duration);
                    } else {
                        setOk(duration);
                    }
                    return;
                }
            }

            // inside a shuttering window: show remaining time in current shuttering window
            for (ShutteringWindow w : snapshot.getShutteringWindows()) {
                if (w.contains(epicsTime)) {
                    Duration d = new Duration(epicsTime, w.getEnd());
                    setAlarm(d);
                    return;
                }
            }

            // show time to first propagation window
            if (snapshot.getPropagationWindows().size() > 0) {
                PropagationWindow w = snapshot.getPropagationWindows().get(0);
                Duration d = new Duration(epicsTime, w.getStart());
                if (d.getMillis() >= 0) {
                    setAlarm(d);
                    return;
                }
            }

            // hmm, ok we are after the last propagation window or there were no propagation windows at all
            setInactive();
        }
    }

    /**
     * Status panel for LTCS beam collisions.
     */
    public static class BeamCollision extends StatusPanel {

        public BeamCollision() {
            label.setContentMode(ContentMode.HTML);
        }

        /** {@inheritDoc} */
        @Override public void update(AlarmService.Snapshot snapshot) {

            if (snapshot.getNight() == null) {
                setInactive();
                return;
            }

            if (!snapshot.getLtcsSnapshot().isConnected()) {
                switch (snapshot.getLtcsSnapshot().getError()) {
                    case PROCESSES_DOWN:
                        setAlarm("LTCS DOWN");
                        return;
                    case NOT_CONNECTED:
                        setAlarm("LTCS DISCONNECTED");
                        return;
                    default:
                        setAlarm("LTCS ERROR");
                        return;
                }

            } else {

                List<LtcsService.Collision> collisions = snapshot.getLtcsSnapshot().getCollisions();
                EpicsService.Snapshot epicsSnapshot = snapshot.getEpicsSnapshot();
                DateTime epicsTime = epicsSnapshot.getTime();

                if (collisions.isEmpty()) {

                    // there's no collision right now
                    setOk("OK");

                } else {

                    // get first collision (sorted by time) and check if we need to react
                    // we need to react if a) laser is on sky and b) other telescope has priority
                    LtcsService.Collision c = collisions.get(0);
                    Boolean needToReact = snapshot.getEpicsSnapshot().isOnSky() && !c.geminiHasPriority();

                    if (!c.getStart().isAfter(epicsTime) && c.getEnd().isAfter(epicsTime)) {
                        // inside collision! calculate remaining time as time from now until end of collision
                        // LTCS updates the start time of the collision window continuously
                        Duration d = new Duration(epicsTime, c.getEnd());
                        setAlarm(c.getPriority() + ": " + TimeFormatter.asDuration(d));
                    } else {
                        // before collision
                        Duration d = new Duration(epicsTime, c.getStart());
                        String time = TimeFormatter.asDuration(d);
                        if (needToReact && d.getStandardSeconds() <= 30 && d.getStandardSeconds() % 2 == 0) {
                            setAlarm(c.getPriority() + ": " + time);
                        } else if (needToReact && d.getStandardMinutes() < 5) {
                            setWarn(c.getPriority() + ": " + time);
                        } else {
                            setOk(c.getPriority() + ": " + time);
                        }
                    }

                }
            }
        }

    }


    /**
     * Status panel for elevation limit.
     */
    public static class ElevationLimit extends StatusPanel {

        /** {@inheritDoc} */
        @Override public void update(AlarmService.Snapshot snapshot) {

            if (snapshot.getNight() == null ||
                snapshot.getTarget() == null ||
                snapshot.getTarget() instanceof AzElLaserTarget) {

                setInactive();
                return;
            }

            BaseLaserNight night = snapshot.getNight();
            LaserTarget target = snapshot.getTarget();
            Visibility visibility = target.getVisibility();

            DateTime epicsTime = snapshot.getEpicsSnapshot().getTime();
            List<Interval> visible = visibility.getVisibleIntervalsAboveLimitDuring(night);
            // we should always have at least one visible interval, but check for it just in case
            if (visible.size() == 0) {
                setInactive();
                return;
            }

            // are we inside one of the visible intervals?
            Interval inside = inside(epicsTime, visible);
            if (inside != null) {
                Duration d = new Duration(epicsTime, inside.getEnd());
                setOk(d);
                return;
            }

            // are we before one of the visible intervals?
            Interval before = before(epicsTime, visible);
            if (before != null) {
                Duration d = new Duration(epicsTime, before.getStart());
                setWarn(d);
                return;
            }

            // if not inside or before we must be after the last one
            Duration d = new Duration(visible.get(visible.size()-1).getEnd(), epicsTime);
            setWarn(d);
        }

        /**
         * Finds the earliest interval that starts after the given time.
         * @param time
         * @param intervals
         * @return
         */
        private Interval before(DateTime time, List<Interval> intervals) {
            for (Interval i : intervals) {
                if (i.isAfter(time)) {
                    return i;
                }
            }
            return null;
        }

        /**
         * Finds the first interval that contains the given time.
         * @param time
         * @param intervals
         * @return
         */
        private Interval inside(DateTime time, List<Interval> intervals) {
            for (Interval i : intervals) {
                if (i.contains(time)) {
                    return i;
                }
            }
            return null;
        }
    }

    /**
     * Status panel for position inside error cone.
     */
    public static class ErrorConePosition extends StatusPanel {

        /** {@inheritDoc} */
        @Override public void update(AlarmService.Snapshot snapshot) {

            if (snapshot.getTarget() == null) {
                setInactive("--");
                return;
            }

            Double errorConeRadius = snapshot.getErrorCone().toArcsecs().getMagnitude() / 2.0;
            Double distance = snapshot.getDistance().toArcsecs().getMagnitude();
            Double percent = distance/errorConeRadius*100.0;
            String percentText = String.format("%.0f%%", percent);

            if (percent < 66.0) {
                setOk(percentText);
            } else if (percent < 95.0) {
                setWarn(percentText);
            } else {
                setAlarm(percentText);
            }
        }
    }



}

