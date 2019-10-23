package edu.gemini.lch.services;

import edu.gemini.lch.model.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 */
public interface LaserTargetsService {

    LaserTarget findById(Long targetId);
    LaserTarget findRaDecByPosition(Long nightId, Double ra, Double dec, Double maxDistance);
    LaserTarget findAzElByPosition(Long nightId, Double az, Double el, Double maxDistance);
    List<Observation> getObservations(Long targetId);
    List<PropagationWindow> getPropagationWindows(Long nightId, Long targetId, boolean removeBlanketClosures);
    List<ShutteringWindow> getShutteringWindows(Long nightId, Long targetId, boolean includeBlanketClosures);
    byte[] getImageHeader(LaserNight night, Integer width, DateTime start, DateTime end, DateTimeZone zone);
    byte[] getImage(LaserNight night, LaserTarget target, Integer width, Integer height, DateTime start, DateTime end);
    byte[] getImage(LaserNight night, Integer width, Integer height, DateTime start, DateTime end, DateTime now, DateTimeZone zone);
    byte[] getImage(LaserNight night, LaserTarget target, Integer width, Integer height, DateTime start, DateTime end, DateTime now, DateTimeZone zone);

}
