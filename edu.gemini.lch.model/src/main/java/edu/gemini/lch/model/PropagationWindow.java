package edu.gemini.lch.model;

import javax.persistence.*;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Named queries for propagation windows.
 */
@NamedQueries({
        @NamedQuery(name = "propagationWindow.findForLaserTarget",
                query = "from PropagationWindow p " +
                        "where p.targetId = :targetId " +
                        "order by p.start"
        )
})

/**
 *
 */
@Entity
@Table(name = "lch_windows")
public class PropagationWindow extends Window {

    /* make target_id accessible in HSQL named queries, but do not allow to modify the value */
    @Column(name = "target_id", insertable = false, updatable = false)
    private Long targetId;


    public PropagationWindow(DateTime start, DateTime end) {
        super(start, end);
    }

    public static List<ShutteringWindow> getShutteringWindows(Collection<PropagationWindow> windows) {
        List<ShutteringWindow> gaps = new ArrayList<>();
        if (windows.size() >= 2) {
            PropagationWindow w1 = null;
            for (PropagationWindow w2 : windows) {
                if (w1 != null) {
                    gaps.add(new ShutteringWindow(w1.getEnd(), w2.getStart()));
                }
                w1 = w2;
            }
        }
        return Collections.unmodifiableList(gaps);
    }

    public static List<PropagationWindow> removeClosures(Collection<PropagationWindow> windows, List<? extends ShutteringWindow> closures) {
        List<PropagationWindow> result = removeClosures(windows, closures, PropagationWindow.class);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    // empty constructor for hibernate
    public PropagationWindow() {}

}
