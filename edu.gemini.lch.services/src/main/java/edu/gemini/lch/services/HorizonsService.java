package edu.gemini.lch.services;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.spModel.core.HorizonsDesignation;
import jsky.coords.WorldCoords;

import java.util.List;

/**
 * A thin wrapper around the {@link edu.gemini.horizons.client.HorizonsClient} class which uses the JPL horizons
 * service http://ssd.jpl.nasa.gov/horizons.cgi to get ephemeris positions for non-sidereal objects like
 * planets, moons, comets etc.
 */
public interface HorizonsService {

    /**
     * Gets all positions for the target with the given object id over the course of a night.
     */
    List<WorldCoords> getCoordinates(LaserNight night, HorizonsDesignation horizonsDesignation);

}
