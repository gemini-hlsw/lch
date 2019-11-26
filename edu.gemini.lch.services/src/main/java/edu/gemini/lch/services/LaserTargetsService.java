package edu.gemini.lch.services;

import edu.gemini.lch.model.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public interface LaserTargetsService {

    LaserTarget findById(Long targetId);
    LaserTarget findRaDecByPosition(Long nightId, Double ra, Double dec, Double maxDistance);
    LaserTarget findAzElByPosition(Long nightId, Double az, Double el, Double maxDistance);
    List<Observation> getObservations(Long targetId);
    List<PropagationWindow> getPropagationWindows(Long nightId, Long targetId, boolean removeBlanketClosures);
    List<ShutteringWindow> getShutteringWindows(Long nightId, Long targetId, boolean includeBlanketClosures);
    byte[] getImageHeader(LaserNight night, Integer width, ZonedDateTime start, ZonedDateTime end, ZoneId zone);
    byte[] getImage(LaserNight night, LaserTarget target, Integer width, Integer height, ZonedDateTime start, ZoneId end);
    byte[] getImage(LaserNight night, Integer width, Integer height, ZonedDateTime start, ZonedDateTime end, ZonedDateTime now, ZoneId zone);
    byte[] getImage(LaserNight night, LaserTarget target, Integer width, Integer height, ZonedDateTime start, ZonedDateTime end, ZonedDateTime now, ZoneId zone);

}
