package edu.gemini.lch.services.impl;

import edu.gemini.horizons.api.*;
import edu.gemini.horizons.server.backend.CgiQueryExecutor;
import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.Site;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.HorizonsService;
import edu.gemini.lch.services.SiteService;
import edu.gemini.spModel.core.HorizonsDesignation;
import jsky.coords.WorldCoords;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * A thin wrapper around the ODB Horizons Service to adapt it to the needs of LTTS and make it a Spring service.
 */
@Service
public class HorizonsServiceImpl implements HorizonsService {
    private edu.gemini.spModel.core.Site site = edu.gemini.spModel.core.Site.GN;
    private static final Logger LOGGER = Logger.getLogger(HorizonsServiceImpl.class.getName());

    @Resource
    private SiteService siteService;

    @Resource
    private ConfigurationService configurationService;

    /** {@inheritDoc} */
    @Override
    public List<WorldCoords> getCoordinates(
        LaserNight          night,
        HorizonsDesignation horizonsDesignation
    ) {

        // TODO: what are the units?
        int horizonsStepWidth = configurationService.getInteger(Configuration.Value.HORIZONS_STEP_WIDTH);

        // Convert the LCH site to a spmodel site for use with the query.
        edu.gemini.spModel.core.Site site = (siteService.getSite() == Site.NORTH)
                ? edu.gemini.spModel.core.Site.GN
                : edu.gemini.spModel.core.Site.GS;

        try {

            final List<WorldCoords> coordinates =
                HorizonsServiceBridge.unsafeLookupWorldCoordsForJava(
                    horizonsDesignation,
                    site,
                    night,
                    horizonsStepWidth
                );

            if (coordinates.isEmpty()) {
                // no positions, it seems something went wrong, log some information to be able to track this
                LOGGER.warn("lookup for object with id \"" + horizonsDesignation.show() +
                        "\" in Horizons database did not deliver any results");
            } else {
                // we received at least one position, so we assume we're good to go
                LOGGER.debug("lookup for object with id \"" + horizonsDesignation.show() +
                        "\" successful in Horizons database, " + coordinates.size() + " elements");
            }

            // return positions
            return coordinates;

        } catch (HorizonsException e) {
            LOGGER.warn("Exception executing horizons query for \"" + horizonsDesignation.show() + "\"", e);

            // wrap horizons exception as runtime exception and re-throw
            throw new RuntimeException(e);
        }

    }

}
