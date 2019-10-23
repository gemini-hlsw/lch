package edu.gemini.lch.model;

import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Named queries for blanket closures.
 */
@NamedQueries({
        @NamedQuery(name = BlanketClosure.FIND_FOR_NIGHT,
                query = "from BlanketClosure b " +
                        "where b.nightId = :nightId " +
                        "order by b.start"
        )
})

/**
 * Manual closures are valid for a night for all targets.
 */
@Entity
@Table(name = "lch_closures")
public class BlanketClosure extends ShutteringWindow {

    public static final String FIND_FOR_NIGHT = "blanketClosure.findForNight";

    @Column(name="night_id", insertable = false, updatable = false)
    private Long nightId;

    // -- blanket closures need a setter to allow them being edited through the GUI
    // (keeping them immutable would mean to have to delete them and insert a new one, works, but editing is simpler)
    public void setStart(DateTime start) {
        this.start = start.toDate();
    }
    public void setEnd(DateTime end) {
        this.end = end.toDate();
    }

    public BlanketClosure(DateTime start, DateTime end) {
        super(start, end);
    }

    @Override
    public boolean isBlanketClosure() {
        return true;
    }

    // empty constructor for hibernate
    public BlanketClosure() {}
}
