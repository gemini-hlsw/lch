package edu.gemini.lch.services.impl;

import edu.gemini.lch.model.Site;
import edu.gemini.lch.services.SiteService;
import edu.gemini.shared.skycalc.Angle;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Service;


/**
 * Implementation of site service.
 * This service deals with all site related things and is configured for the particular
 * site and environment at creation time.
 */
@Service
public class SiteServiceImpl implements SiteService {

    private static final Logger LOGGER = Logger.getLogger(SiteServiceImpl.class.getName());

    private final Site site;
    private final String environment;
    private final String version;

    /**
     * Constructs a site service for the given site and environment.
     * @param site the site
     * @param environment the environment
     * @param version the version
     */
    public SiteServiceImpl(String site, String environment, String version) {
        this.site = site.toUpperCase().equals("NORTH") ? Site.NORTH : Site.SOUTH;
        this.environment = environment;
        this.version = version;
    }

    /** {@inheritDoc} */
    @Override public Site getSite() {
        return site;
    }

    /** {@inheritDoc} */
    @Override public String getEnvironment() {
        return environment;
    }

    /** {@inheritDoc} */
    @Override public Boolean isDevelopment() {
        return environment.toUpperCase().equals("DEVELOPMENT");
    }

    /** {@inheritDoc} */
    @Override public Boolean isQa() {
        return environment.toUpperCase().equals("QA");
    }

    /** {@inheritDoc} */
    @Override public Boolean isProduction() {
        return environment.toUpperCase().equals("PRODUCTION");
    }

    /** {@inheritDoc} */
    @Override public String getVersion() {
        return version;
    }

    /** {@inheritDoc} */
    @Override public DateTimeZone getSiteTimeZone() {
        return site.getTimeZone();
    }

    /** {@inheritDoc} */
    @Override public Angle getLatitude() {
        return new Angle(site.getLatitude(), Angle.Unit.DEGREES);
    }

}
