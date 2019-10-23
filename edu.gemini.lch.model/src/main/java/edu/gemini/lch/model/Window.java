package edu.gemini.lch.model;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Abstract base class for windows, i.e. time intervals with a start and and end.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Window implements Comparable<Window> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "starts")
    protected Date start;

    @Column(name = "ends")
    protected Date end;

    public Window(DateTime start, DateTime end) {
        Validate.isTrue(start.isBefore(end));
        this.start = start.toDate();
        this.end = end.toDate();
    }

    public Long getId() {
        return id;
    }

    public DateTime getStart() {
        return new DateTime(start);
    }

    public DateTime getEnd() {
        return new DateTime(end);
    }

    public Duration getDuration() {
        return new Duration(getStart(), getEnd());
    }

    public Interval getInterval() {
        return new Interval(getStart(), getEnd());
    }

    /**
     * Returns true if the time passed as parameter is inside the window, i.e. time is in [start, end)
     * @param time
     * @return
     */
    public Boolean contains(DateTime time) {
        return (!getStart().isAfter(time) && getEnd().isAfter(time));
    }

    @Override
    public int compareTo(Window window) {
        return start.compareTo(window.start);
    }

    @Override
    public int hashCode() {
        return start.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Window)) return false;
        Window other = (Window) o;
        if (!start.equals(other.start)) return false;
        if (!end.equals(other.end)) return false;
        return true;
    }

    protected static <T extends Window> List<T> removeClosures(Collection<T> windows, Collection<? extends ShutteringWindow> closures, Class clazz) {
        List<T> result = new ArrayList<>(windows);
        for (ShutteringWindow c : closures) {
            result = removeClosure(result, c, clazz);
        }
        return result; // NOTE: result is not sorted!
    }

    protected static <T extends Window> List<T> removeClosure(Collection<T> windows, ShutteringWindow closure, Class clazz) {
        List<T> result = new ArrayList<>();
        for (T w : windows) {
            if (w.getInterval().overlaps(closure.getInterval())) {
                if (w.getStart().isBefore(closure.getStart())) {
                    result.add((T)createWindow(w.getStart(), closure.getStart(), clazz));
                }
                if (w.getEnd().isAfter(closure.getEnd())) {
                    result.add((T)createWindow(closure.getEnd(), w.getEnd(), clazz));
                }
            } else {
                result.add(w);
            }
        }
        return result;
    }

    protected static <T extends Window> T createWindow(DateTime start, DateTime end, Class clazz) {
        try {
            T newT = (T) clazz.newInstance();
            newT.start = start.toDate();
            newT.end = end.toDate();
            return newT;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // empty constructor for hibernate
    public Window() {}

}
