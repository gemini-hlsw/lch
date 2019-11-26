package edu.gemini.lch.model;

import javax.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.TreeSet;

/**
 */
@Entity
@Table(name = "lch_events")
public class LaserRunEvent implements Comparable<LaserRunEvent> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "event_id")
    @OrderBy("type, name")
    private Set<EventFile> files;


    @Column(name = "event_time")
    private Instant time;

    @Column(name = "message")
    private String message;

    public LaserRunEvent(String message) {
        this.time = Instant.now();
        this.files = new TreeSet<>();
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public ZonedDateTime getTime() {
        return ZonedDateTime.ofInstant(time, ZoneId.systemDefault());
    }

    public String getMessage() {
        return message;
    }

    public Set<EventFile> getFiles() {
        return files;
    }

    /** {@inheritDoc} */
    // NOTE: compareTo should be consistent with order defined in @OrderBy in LaserRun for LaserRunEvent objects.
    @Override
    public int compareTo(LaserRunEvent event) {
        return -time.compareTo(event.time); // negate result of comparison to get descending ordering
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return time.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof LaserRunEvent)) return false;
        LaserRunEvent other = (LaserRunEvent) o;
        if (!time.equals(other.time)) return false;
        if (!message.equals(other.message)) return false;
        if (!files.equals(other.files)) return false;
        return true;
    }

    // empty constructor needed for hibernate
    public LaserRunEvent() {}

}
