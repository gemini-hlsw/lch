package edu.gemini.lch.model;

import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.Date;


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
    private Date actual;

    @Column(name = "observed")
    private Date observed;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DateTime getActualDate() {
        return new DateTime(actual);
    }

    public DateTime getObservedDate() {
        return new DateTime(observed);
    }

    // empty constructor for Hibernate
    public Holiday() {}

    // simple constructor for testing
    public Holiday(DateTime date) {
        actual = date.toDate();
        observed = date.toDate();
    }

}
