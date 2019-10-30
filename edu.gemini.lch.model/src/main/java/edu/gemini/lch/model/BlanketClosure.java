package edu.gemini.lch.model;

import javax.persistence.*;
import java.time.ZonedDateTime;

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
    public void setStart(ZonedDateTime start) {
        this.start = start.toInstant();
    }
    public void setEnd(ZonedDateTime end) {
        this.end = end.toInstant();
    }

    public BlanketClosure(ZonedDateTime start, ZonedDateTime end) {
        super(start, end);
    }

    @Override
    public boolean isBlanketClosure() {
        return true;
    }

    // empty constructor for hibernate
    public BlanketClosure() {}
}
