package edu.gemini.lch.services;

import edu.gemini.lch.model.Visibility;
import jsky.coords.WorldCoords;

/**
 */
public interface VisibilityCalculator {

    Visibility calculateVisibility(WorldCoords obj);
    Double getHorizon();
    Double getLaserLimit();

}
