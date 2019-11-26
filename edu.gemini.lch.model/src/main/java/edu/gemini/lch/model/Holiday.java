package edu.gemini.lch.model;

import javax.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;


/**
 */
@NamedQueries({
        /*
            Find all laser holidays between two given dates in ascending order.
         */
        @NamedQuery(name = Holiday.QUERY_FIND_BETWEEN_DATES,
                query = "from Holiday holiday " +
                        "where " +
                        "holiday.observed >= :first and holiday.observed < :last " +
                        "order by holiday.observed asc"
        )
})

/**
 * Federal US Holidays. For LTTS only the actual observation date is relevant.
 */
@Entity
@Table(name = "lch_holidays")
public class Holiday {

    public static final String QUERY_FIND_BETWEEN_DATES = "holidays.betweenDates";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String name;

    @Column(name = "actual")
    private Instant actual;

    @Column(name = "observed")
    private Instant observed;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ZonedDateTime getActualDate() {
        return ZonedDateTime.ofInstant(actual, ZoneId.systemDefault());
    }

    public ZonedDateTime getObservedDate() {
        return ZonedDateTime.ofInstant(observed, ZoneId.systemDefault());
    }

    // empty constructor for Hibernate
    public Holiday() {}

    // simple constructor for testing
    public Holiday(ZonedDateTime date) {
        actual = date.toInstant();
        observed = date.toInstant();
    }

}
