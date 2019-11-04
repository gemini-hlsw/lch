package edu.gemini.lch.services;

import edu.gemini.lch.model.Site;
import edu.gemini.shared.skycalc.Angle;

import java.util.TimeZone;

/**
 * A service that deals with site related issues.
 * The main problem this service is meant to deal with is the issue that Chile has no regular schedule to change
 * between standard time and day light saving time which can result in the local time calculated by the JVM and
 * the actual current local time in Chile being off by an hour. To solve this this service allows to set an
 * explicit time zone offset relative to GMT/UTC and provides a time zone which can be used for dealing with local
 * times.
 */
public interface SiteService {

    /**
     * Gets the site this server is representing.
     * @return
     */
    Site getSite();

    /**
     * Gets the name of the environment (e.g. "Development").
     * @return
     */
    String getEnvironment();

    /**
     * Checks if we are running in a development environment.
     * @return
     */
    Boolean isDevelopment();

    /**
     * Checks if we are running in a test (quality assurance) environment.
     * @return
     */
    Boolean isQa();

    /**
     * Checks if we are running in production.
     * @return
     */
    Boolean isProduction();

    /**
     * Gets the version number.
     * @return
     */
    String getVersion();

    /**
     * Gets the time zone of this site.
     */
    TimeZone getSiteTimeZone();

    /**
     * Gets the latitude of the site.
     */
    Angle getLatitude();

}
