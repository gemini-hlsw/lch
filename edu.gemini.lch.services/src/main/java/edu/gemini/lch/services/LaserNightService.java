package edu.gemini.lch.services;

import edu.gemini.lch.model.*;
import edu.gemini.lch.pamparser.Response;
import edu.gemini.lch.services.util.PrmFile;
import edu.gemini.odb.browser.QueryResult;
import org.joda.time.DateTime;

import javax.mail.MessagingException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 */
public interface LaserNightService {
    boolean laserNightExists(DateTime day);
    void createLaserRun(DateTime firstDay, DateTime lastDay);

    /**
     * Creates a new empy laser night.
     * @param day
     * @return
     */
    LaserNight createLaserNight(DateTime day);

    /**
     * Creates a new night and populates it with data if the day is not too far in the future.
     * @param day
     * @return
     */
    LaserNight createAndPopulateLaserNight(DateTime day);

    /**
     * Creates a new night and populates it with the given data.
     * @param day
     * @param scienceTargets
     * @param engineeringTargets
     * @return
     */
    LaserNight createAndPopulateLaserNight(DateTime day, QueryResult scienceTargets, QueryResult engineeringTargets);

    /**
     * Updates a night.
     * @param night
     * @return
     */
    LaserNight updateLaserNight(LaserNight night);

    /**
     * Updates a night with the given data.
     * @param night
     * @param scienceTargets
     * @param engineeringTargets
     * @return
     */
    LaserNight updateLaserNight(LaserNight night, QueryResult scienceTargets, QueryResult engineeringTargets);

    LaserNight loadLaserNight(Long id);
    LaserNight loadLaserNight(DateTime day);

    /**
     * Deletes a laser night.
     * @param id
     */
    void deleteLaserNight(Long id);

//    void uploadPropagationWindows(InputStream propagationWindowsFile);

    /**
     * Gets all engineering targets that are currenly configured for a site.
     * @param site
     * @return
     */
    List<EngTargetTemplate> getEngineeringTargetsForSite(Site site);

    SimpleLaserNight getShortLaserNight(Long id);
    SimpleLaserNight getShortLaserNight(DateTime date);
    SimpleLaserNight getLaserNight(DateTime day);
    SimpleLaserNight getNextLaserNight(DateTime after);
    SimpleLaserNight getPreviousLaserNight(DateTime before);
    List<SimpleLaserNight> getNextLaserNights(DateTime after, Integer maxCount);
    List<SimpleLaserNight> getPreviousLaserNights(DateTime before, Integer maxCount);

    /**
     * Gets the laser night which is covering the given time.
     * @param time
     * @return
     */
    SimpleLaserNight getShortLaserNightCovering(DateTime time);

    List<SimpleLaserNight> getShortLaserNights(DateTime startsAfter, DateTime startsBefore);

    List<BlanketClosure> getBlanketClosures(Long nightId);

    PrmFile.File createSingleRaDecPrmFile(LaserNight night);
    PrmFile.File  createSingleAzElPrmFile(LaserNight night);
    Set<PrmFile.File> createRaDecPrmFiles(LaserNight night);
    Set<PrmFile.File> createAzElPrmFiles(LaserNight night);
    void sendPrm(LaserNight night);

    /**
     * Adds new windows to laser targets; already existing windows are replaced by the newer ones.
     * @param night
     * @param response
     * @throws IOException
     * @throws ParseException
     * @throws MessagingException
     */
    void addAndReplacePropagationWindows(LaserNight night, Response response);

    void addClosure(LaserNight night, DateTime start, DateTime end);
    void updateClosure(LaserNight night, BlanketClosure closure);
    void deleteClosure(LaserNight night, BlanketClosure closure);
    void processLaserNight(Long id);
    void saveOrUpdate(Object object);
    void delete(Object object);

    /**
     * Creates a plain event for the given night.
     * @param night
     * @param message
     */
    void createEvent(LaserNight night, String message);

    /**
     * Creates an event including some files.
     * @param nightId
     * @param message
     * @param files
     */
    void createEvent(Long nightId, String message, Collection<EventFile> files);


    LaserNight createTestLaserNight(DateTime day);
    LaserNight createAndPopulateTestLaserNight(DateTime day, QueryResult testScienceData, QueryResult testEngineeringData);

    /**
     * Gets the earliest time we are allowed to propagate the laser for a given night.
     * @param night
     * @return
     */
    DateTime getEarliestPropagation(LaserNight night);

    /**
     * Gets the latest time we are allowed to propagate the laser for a given night.
     * @param night
     * @return
     */
    DateTime getLatestPropagation(LaserNight night);

    /**
     * Handles a PAM file, throws unchecked exception in case of a problem which will rollback transaction.
     * Database transaction boundaries are per PAM file.
     * @param pamFile
     * @return the id of the night this part belongs to
     */
    Long handlePamFile(PamFile pamFile);

}