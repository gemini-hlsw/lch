package edu.gemini.lch.services;

import edu.gemini.lch.model.BaseLaserNight;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.Site;
import jsky.coords.SiteDesc;
import jsky.plot.ElevationPlotUtil;
import jsky.plot.SunRiseSet;
import jsky.plot.util.SkyCalc;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * A factory for creating model objects that are not straight forward to create.
 * These methods could be implemented as part of the model objects but that would introduce dependencies of the
 * model module to libraries like Joda Time and the jsky utilities. Since we want to be able to use the model
 * in as many places as possible I decided to keep these things separate.
 */
public class ModelFactory {

    /**
     * Creates a laser night for the given date (in local time of the site).
     */
    public static LaserNight createNight(Site site, ZonedDateTime day) {
        SunRiseSet sunCalc = createSunCalculator(site, day.toLocalDate().atStartOfDay(site.getZoneId()));
        ZonedDateTime sunsetUTC  = ZonedDateTime.ofInstant(sunCalc.getSunset().toInstant(), ZoneId.systemDefault());
        ZonedDateTime sunriseUTC = ZonedDateTime.ofInstant(sunCalc.getSunrise().toInstant(), ZoneId.systemDefault());
        return new LaserNight(site, sunsetUTC, sunriseUTC);
    }

    /**
     * Creates a sky calculator which can be used to calculate altitudes of objects in the sky
     * for a site and time.
     */
    public static SkyCalc createSkyCalculator(Site site) {
        return new SkyCalc(getSiteDescForSite(site));
    }

    /**
     * Creates a sun calculator which can be used to calculate sunrise and sunset and twilight times
     * for a night.
     */
    public static SunRiseSet createSunCalculator(BaseLaserNight night) {
        return createSunCalculator(night.getSite(), night.getStart());
    }

    /**
     * Creates a sun calculator which can be used to calculate sunrise and sunset and twilight times
     * for a given site and day.
     */
    public static SunRiseSet createSunCalculator(Site site, ZonedDateTime day)  {
        // TODO-JODA: Translate this? We need ZoneID instead of TimeZone?
        ZoneId localZoneId = site.getZoneId();
        ZonedDateTime date = day.toLocalDate().atStartOfDay(localZoneId);
        ZonedDateTime dayLOC = ZonedDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), 12, 0, 0, 0, localZoneId);
        ZonedDateTime dayUTC = ZonedDateTime.ofInstant(dayLOC.toInstant(), ZoneId.systemDefault());
        // IMPORTANT: Use local time noon for creation of SunRiseSet object or it will not yield expected results!
        // TODO-JODA: External library. Need to use Date.
        return new SunRiseSet(Date.from(dayUTC.toInstant()), getSiteDescForSite(site));
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
