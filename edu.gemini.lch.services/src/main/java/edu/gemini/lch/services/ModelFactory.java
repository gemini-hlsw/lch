package edu.gemini.lch.services;

import edu.gemini.lch.model.BaseLaserNight;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.Site;
import jsky.coords.SiteDesc;
import jsky.plot.ElevationPlotUtil;
import jsky.plot.SunRiseSet;
import jsky.plot.util.SkyCalc;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * A factory for creating model objects that are not straight forward to create.
 * These methods could be implemented as part of the model objects but that would introduce dependencies of the
 * model module to libraries like Joda Time and the jsky utilities. Since we want to be able to use the model
 * in as many places as possible I decided to keep these things separate.
 */
public class ModelFactory {

    /**
     * Creates a laser night for the given date (in local time of the site).
     * @param site
     * @param day
     * @return
     */
    public static LaserNight createNight(Site site, DateTime day) {
        SunRiseSet sunCalc = createSunCalculator(site, day.withTimeAtStartOfDay());
        DateTime sunsetUTC  = new DateTime(sunCalc.getSunset(),  DateTimeZone.UTC);
        DateTime sunriseUTC = new DateTime(sunCalc.getSunrise(), DateTimeZone.UTC);
        return new LaserNight(site, sunsetUTC, sunriseUTC);
    }

    /**
     * Creates a sky calculator which can be used to calculate altitudes of objects in the sky
     * for a site and time.
     * @param site
     * @return
     */
    public static SkyCalc createSkyCalculator(Site site) {
        return new SkyCalc(getSiteDescForSite(site));
    }

    /**
     * Creates a sun calculator which can be used to calculate sunrise and sunset and twilight times
     * for a night.
     * @param night
     * @return
     */
    public static SunRiseSet createSunCalculator(BaseLaserNight night) {
        return createSunCalculator(night.getSite(), night.getStart());
    }

    /**
     * Creates a sun calculator which can be used to calculate sunrise and sunset and twilight times
     * for a given site and day.
     * @param site
     * @param day
     * @return
     */
    public static SunRiseSet createSunCalculator(Site site, DateTime day)  {
        DateTime date = day.withTimeAtStartOfDay();
        DateTimeZone localTimeZone = site.getTimeZone();
        DateTime dayLOC = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 12, 0, 0, localTimeZone);
        DateTime dayUTC = new DateTime(dayLOC, DateTimeZone.UTC);
        // IMPORTANT: Use local time noon for creation of SunRiseSet object or it will not yield expected results!
        return new SunRiseSet(dayUTC.toDate(), getSiteDescForSite(site));
    }

    // ============= private helpers

    private static SiteDesc getSiteDescForSite(Site site) {
        switch(site) {
            case NORTH: return ElevationPlotUtil.MAUNA_KEA;
            case SOUTH: return ElevationPlotUtil.CERRO_PANCHON;
            default: throw new IllegalArgumentException("unknown site for laser night");
        }
    }


}
