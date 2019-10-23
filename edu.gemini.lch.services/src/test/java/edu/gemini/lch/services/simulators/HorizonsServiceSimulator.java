package edu.gemini.lch.services.simulators;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.HorizonsService;
import edu.gemini.spModel.core.HorizonsDesignation;
import jsky.coords.WorldCoords;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A Horizons simulator which does not actually connect an external service but just returns a fixed position.
 * To be used in test cases.
 */
@Service
public class HorizonsServiceSimulator implements HorizonsService {
    @Override
    public List<WorldCoords> getCoordinates(LaserNight night, HorizonsDesignation horizonsId) {
        if ("HORIZONS_DOES_NOT_KNOW_ME".equals(horizonsId.queryString())) {
            return new ArrayList<>();
        } else {
            List<WorldCoords> coordinates = new ArrayList<>();
            coordinates.add(new WorldCoords("04:48:47.03", "21:38:03.1"));
            return coordinates;
        }
    }
}
