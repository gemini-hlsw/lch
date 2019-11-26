package edu.gemini.lch.model;

import javax.persistence.*;
import java.time.ZonedDateTime;


/**
 */
@NamedQueries({
    @NamedQuery(name = SimpleLaserNight.FIND_BY_ID_QUERY,
            query = "from SimpleLaserNight n " +
                    "where n.id = :nightId"
    ),
    @NamedQuery(name = SimpleLaserNight.FIND_BY_SITE_AND_START_QUERY,
        query = "from SimpleLaserNight n " +
                "where n.site = :site " +
                "and n.start >= :startsAfter and n.start < :startsBefore " +
                "order by n.start"
    ),
        @NamedQuery(name = SimpleLaserNight.FIND_BY_SITE_AND_TIME_QUERY,
        query = "from SimpleLaserNight n " +
                "where n.site = :site " +
                "and :time >= n.start and :time < n.end " +
                "order by n.start"
    ),
    @NamedQuery(name = SimpleLaserNight.QUERY_FIND_BY_SITE_AND_DATE,
            query = "from SimpleLaserNight night " +
                    "where " +
                    "night.site = :site and night.start >= :dayStart and night.start < :dayEnd"
    ),
    @NamedQuery(name = SimpleLaserNight.QUERY_FIND_BY_SITE_STARTING_AFTER,
            query = "from SimpleLaserNight night " +
                    "where " +
                    "night.site = :site and night.start >= :after " +
                    "order by night.start asc"
    ),
    @NamedQuery(name = SimpleLaserNight.QUERY_FIND_BY_SITE_ENDING_BEFORE,
            query = "from SimpleLaserNight night " +
                    "where " +
                    "night.site = :site and night.end < :before " +
                    "order by night.end desc"
    )

})

@Entity
@Table(name = "lch_laser_nights")
public class SimpleLaserNight extends BaseLaserNight {

    public static final String FIND_BY_ID_QUERY = "SimpleLaserNight.findById";
    public static final String FIND_BY_SITE_AND_START_QUERY = "SimpleLaserNight.findBySiteAndStart";
    public static final String FIND_BY_SITE_AND_TIME_QUERY = "SimpleLaserNight.findBySiteAndTime";
    public static final String QUERY_FIND_BY_SITE_AND_DATE = "SimpleLaserNight.findBySiteAndDate";
    public static final String QUERY_FIND_BY_SITE_STARTING_AFTER = "SimpleLaserNight.findBySiteStartingAfter";
    public static final String QUERY_FIND_BY_SITE_ENDING_BEFORE = "SimpleLaserNight.findBySiteEndingBefore";


    public SimpleLaserNight(Site site, ZonedDateTime start, ZonedDateTime end) {
        super(site, start, end);
    }

    // empty constructor needed by hibernate
    public SimpleLaserNight() {}
}
