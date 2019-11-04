package edu.gemini.lch.services;

import edu.gemini.lch.model.*;
import edu.gemini.lch.pamparser.Response;
import edu.gemini.lch.services.util.PrmFile;
import edu.gemini.odb.browser.QueryResult;

import javax.mail.MessagingException;
import java.io.IOException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface LaserNightService {
    boolean laserNightExists(ZonedDateTime day);
    void createLaserRun(ZonedDateTime firstDay, ZonedDateTime lastDay);

    /**
     * Creates a new empy laser night.
     */
    LaserNight createLaserNight(ZonedDateTime day);

    /**
     * Creates a new night and populates it with data if the day is not too far in the future.
     */
    LaserNight createAndPopulateLaserNight(ZonedDateTime day);

    /**
     * Creates a new night and populates it with the given data.
     */
    LaserNight createAndPopulateLaserNight(ZonedDateTime day, QueryResult scienceTargets, QueryResult engineeringTargets);

    /**
     * Updates a night.
     */
    LaserNight updateLaserNight(LaserNight night);

    /**
     * Updates a night with the given data.
     */
    LaserNight updateLaserNight(LaserNight night, QueryResult scienceTargets, QueryResult engineeringTargets);

    LaserNight loadLaserNight(Long id);
    LaserNight loadLaserNight(ZonedDateTime day);

    /**
     * Deletes a laser night.
     */
    void deleteLaserNight(Long id);

    /**
     * Gets all engineering targets that are currenly configured for a site.
     */
    List<EngTargetTemplate> getEngineeringTargetsForSite(Site site);

    SimpleLaserNight getShortLaserNight(Long id);
    SimpleLaserNight getShortLaserNight(ZonedDateTime date);
    SimpleLaserNight getLaserNight(ZonedDateTime day);
    SimpleLaserNight getNextLaserNight(ZonedDateTime after);
    SimpleLaserNight getPreviousLaserNight(ZonedDateTime before);
    List<SimpleLaserNight> getNextLaserNights(ZonedDateTime after, Integer maxCount);
    List<SimpleLaserNight> getPreviousLaserNights(ZonedDateTime before, Integer maxCount);

    /**
     * Gets the laser night which is covering the given time.
     */
    SimpleLaserNight getShortLaserNightCovering(ZonedDateTime time);

    List<SimpleLaserNight> getShortLaserNights(ZonedDateTime startsAfter, ZonedDateTime startsBefore);

    List<BlanketClosure> getBlanketClosures(Long nightId);

    PrmFile.File createSingleRaDecPrmFile(LaserNight night);
    PrmFile.File  createSingleAzElPrmFile(LaserNight night);
    Set<PrmFile.File> createRaDecPrmFiles(LaserNight night);
    Set<PrmFile.File> createAzElPrmFiles(LaserNight night);
    void sendPrm(LaserNight night);

    /**
     * Adds new windows to laser targets; already existing windows are replaced by the newer ones.
     */
    void addAndReplacePropagationWindows(LaserNight night, Response response);

    void addClosure(LaserNight night, ZonedDateTime start, ZonedDateTime end);
    void updateClosure(LaserNight night, BlanketClosure closure);
    void deleteClosure(LaserNight night, BlanketClosure closure);
    void processLaserNight(Long id);
    void saveOrUpdate(Object object);
    void delete(Object object);

    /**
     * Creates a plain event for the given night.
     */
    void createEvent(LaserNight night, String message);

    /**
     * Creates an event including some files.
     */
    void createEvent(Long nightId, String message, Collection<EventFile> files);


    LaserNight createTestLaserNight(ZonedDateTime day);
    LaserNight createAndPopulateTestLaserNight(ZonedDateTime day, QueryResult testScienceData, QueryResult testEngineeringData);

    /**
     * Gets the earliest time we are allowed to propagate the laser for a given night.
     */
    ZonedDateTime getEarliestPropagation(LaserNight night);

    /**
     * Gets the latest time we are allowed to propagate the laser for a given night.
     */
    ZonedDateTime getLatestPropagation(LaserNight night);

    /**
     * Handles a PAM file, throws unchecked exception in case of a problem which will rollback transaction.
     * Database transaction boundaries are per PAM file.
     */
    Long handlePamFile(PamFile pamFile);

}