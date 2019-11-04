package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.configuration.Selection;
import edu.gemini.lch.model.*;
import edu.gemini.lch.pamparser.AzAltTarget;
import edu.gemini.lch.pamparser.RaDecTarget;
import edu.gemini.lch.pamparser.Response;
import edu.gemini.lch.pamparser.Target;
import edu.gemini.lch.services.*;
import edu.gemini.lch.services.util.PrmFile;
import edu.gemini.lch.services.util.TemplateEngine;
import edu.gemini.lch.services.util.WorkDayCalendar;
import edu.gemini.odb.browser.OdbBrowser;
import edu.gemini.odb.browser.QueryResult;
import jsky.plot.SunRiseSet;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.util.Try;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Implementation of the laser night service using Hibernate to store the data in a database.
 */
@Service
public class LaserNightServiceImpl implements LaserNightService {

    private static final Logger LOGGER = Logger.getLogger(LaserNightServiceImpl.class);

    @Resource
    private SessionFactory sessionFactory;

    @Resource
    private ConfigurationService configurationService;

    @Resource
    private SiteService siteService;

    @Resource
    private EmailService emailService;

    @Resource
    private OdbBrowser odbBrowser;

    @Resource
    private AlarmService alarmService;

    @Resource
    private JSpOCCommunicatorService jspocService;

    @Resource
    private Factory factory;

    @Override
    @Transactional
    public PrmFile.File createSingleRaDecPrmFile(LaserNight night) {
        PrmFile.RaDec prms = factory.createPrmFileRaDec(night, Integer.MAX_VALUE);
        return prms.getPrmFiles().iterator().next();
    }

    @Override
    @Transactional
    public PrmFile.File createSingleAzElPrmFile(LaserNight night) {
        PrmFile.AzEl prms = factory.createPrmFileAzEl(night, Integer.MAX_VALUE);
        return prms.getPrmFiles().iterator().next();
    }

    @Override
    @Transactional
    public Set<PrmFile.File> createRaDecPrmFiles(LaserNight night) {
        Integer maxTargets = configurationService.getInteger(Configuration.Value.PRM_MAX_NUMBER_OF_TARGETS);
        PrmFile.RaDec prms = factory.createPrmFileRaDec(night, maxTargets);
        return prms.getPrmFiles();
    }

    @Override
    @Transactional
    public Set<PrmFile.File> createAzElPrmFiles(LaserNight night) {
        Integer maxTargets = configurationService.getInteger(Configuration.Value.PRM_MAX_NUMBER_OF_TARGETS);
        PrmFile.AzEl prms = factory.createPrmFileAzEl(night, maxTargets);
        return prms.getPrmFiles();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight updateLaserNight(LaserNight night) {
        DateTime date = new DateTime(night.getStart());
        QueryResult scienceTargets = getScienceQueryResult(date);
        QueryResult engineeringTargets = getEngineeringQueryResult(date);
        return updateLaserNight(night, scienceTargets, engineeringTargets);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight updateLaserNight(LaserNight night, QueryResult scienceTargets, QueryResult engineeringTargets) {
        LaserNightUpdater updater = factory.createUpdater();
        if (night.hasPrmSent()) {
            updater.updateObservations(night, scienceTargets, engineeringTargets);
        } else {
            updater.replaceObservations(night, scienceTargets, engineeringTargets);
        }
        sessionFactory.getCurrentSession().saveOrUpdate(night);
        createEvent(night, "Laser night updated.");
        return night;
    }

    @Override
    @Transactional
    public void sendPrm(final LaserNight night) {
        // create attachments
        final List<PrmFile.File> attachments = createAllPrmFiles(night);
        final boolean hasPrmSent = night.hasPrmSent();

        // update night as needed (mark laser targets as sent and reset update information)
        night.setLatestPrmSent(DateTime.now());
        LaserNightUpdater.markAllLaserTargetsAsSent(night);             // mark all laser targets as sent (do this first)
        LaserNightUpdater.incorporateAddedObservationTargets(night);    // change ADDED to OK (only allowed for sent targets)
        sessionFactory.getCurrentSession().saveOrUpdate(night);

        // upload files and create an event in history with all files.
        final Collection<EventFile> files = new HashSet<>();
        final Collection<EventFile> failedFiles = new HashSet<>();

        jspocService.connect(siteService.getSite());
        for (final PrmFile.File attachment : attachments) {
            final EventFile eventFile = new EventFile(EventFile.Type.PRM, attachment.getName(), attachment.getFile());
            final Try result = jspocService.uploadPRMFile(siteService.getSite(), attachment.getName(), attachment.getFile().getBytes());
            if (result.isSuccess()) {
                files.add(eventFile);
            } else {
                final Throwable t = (Throwable) result.failed().get();
                LOGGER.error("Could not upload file " + attachment.getName(), t);
                failedFiles.add(eventFile);
            }
        }

        if (files.size() > 0) {
            final String eventMessage = hasPrmSent ?
                    "Uploaded " + files.size() + " PRM files with additional targets." :
                    "Uploaded " + files.size() + " PRM files.";
            createEvent(night, eventMessage, files);
        }
        if (failedFiles.size() > 0) {
            final String eventMessage = "Failed to upload " + files.size() + " PRM files.";
            createEvent(night, eventMessage, failedFiles);

            final StringBuilder names = new StringBuilder();
            failedFiles.forEach(e -> names.append(e.getName()).append("\n"));
            emailService.sendInternalEmail(
                    "LTTS: WARNING: Problems during creation/update of night " + night.getStart().withZone(DateTimeZone.UTC).toString("yyyy-MM-dd (DDD) z"),
                    "Could not upload PRM file(s):\n\n" + names.toString()
            );
        }
    }

    private void sendUnsentTargetsWarning(LaserNight night) {
        TemplateEngine engine = factory.createTemplateEngine(night);
        emailService.sendEmail(
                configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_TO_ADDRESSES),
                configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_CC_ADDRESSES),
                configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_BCC_ADDRESSES),
                engine.fillTemplate(Configuration.Value.EMAILS_NEW_TARGETS_EMAIL_SUBJECT_TEMPLATE),
                engine.fillTemplate(Configuration.Value.EMAILS_NEW_TARGETS_EMAIL_BODY_TEMPLATE));
    }

    private void sendNoPamsReceivedWarning(LaserNight night) {
        TemplateEngine engine = factory.createTemplateEngine(night);
        emailService.sendEmail(
                configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_TO_ADDRESSES),
                configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_CC_ADDRESSES),
                configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_BCC_ADDRESSES),
                engine.fillTemplate(Configuration.Value.EMAILS_PAM_MISSING_EMAIL_SUBJECT_TEMPLATE),
                engine.fillTemplate(Configuration.Value.EMAILS_PAM_MISSING_EMAIL_BODY_TEMPLATE));
    }

    @Override
    @Transactional
    public void processLaserNight(Long nightId) {

        LaserNight night = loadLaserNight(nightId);
        // check if night has been deleted in the mean time or if
        // night contains nothing but test data (do NOT process test nights!
        // we don't want to send test data to LCH..)
        if (night == null || night.isTestNight()) {
            return;
        }

        // get ready to do some work
        WorkDayCalendar calendar = factory.createCalendar();
        int sendPrmDaysBefore = configurationService.getInteger(Configuration.Value.EMAILS_PRM_SEND_WORK_DAYS_AHEAD);
        int updateDaysBefore = sendPrmDaysBefore + 3;

        // ==== PROCESSING (updating and sending PRM files to LCH)

        // no PRMs sent and 4 or less working days left: update and send PRMs; DONE
        if (calendar.workDaysBefore(night.getStart()) <= sendPrmDaysBefore && !night.hasPrmSent()) {
            LaserNight updated = updateLaserNight(night);
            sendPrm(updated);
            return;
        }

        // no PRMs sent yet and we're getting closer than a week to the date: update the data; DONE
        if (calendar.daysBefore(night.getStart()) <= updateDaysBefore && !night.hasPrmSent()) {
            updateLaserNight(night);
            return;
        }

        // ==== WARNINGS (in case of unsent targets (added after PRMs have been sent) and missing PAMs)

        // PRMs have been sent and we have new unsent targets: warn
        if (night.hasPrmSent()) {
            LaserNight updated = updateLaserNight(night);
            if (updated.getUntransmittedLaserTargets().size() > 0) {
                sendUnsentTargetsWarning(updated);
            }
        }

        // laser night starts soon and we don't have PAMs received: warn
        if ((new Period(DateTime.now(), night.getStart()).toStandardHours().getHours() < 24) && !night.hasPamReceived()) {
            sendNoPamsReceivedWarning(night);
        }
    }

    private List<PrmFile.File> createAllPrmFiles(LaserNight night) {
        List<PrmFile.File> files = new ArrayList<>();
        files.addAll(createRaDecPrmFiles(night));
        files.addAll(createAzElPrmFiles(night));
        return files;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean laserNightExists(DateTime day) {
        return (getLaserNight(day) != null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void createLaserRun(DateTime firstDay, DateTime lastDay) {
        DateTime day = firstDay;
        while (!day.isAfter(lastDay)) {
            if (!laserNightExists(day)) {
                createAndPopulateLaserNight(day);
            }
            day = day.plusDays(1);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight createLaserNight(DateTime day) {
        LaserNight laserNight = ModelFactory.createNight(siteService.getSite(), day);
        sessionFactory.getCurrentSession().save(laserNight);
        createEvent(laserNight, "Laser night created.");
        return laserNight;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight createAndPopulateLaserNight(DateTime day) {
        LaserNight night = createLaserNight(day);
        // keep time down it takes to create laser runs: don't add observations for nights in the past or in the far future
        Duration untilStart = new Duration(DateTime.now(),  night.getStart());
        if (untilStart.getStandardDays() >= 0 && untilStart.getStandardDays() < 5) {
            return updateLaserNight(night);
        } else {
            return night;
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight createAndPopulateLaserNight(DateTime day, QueryResult scienceTargets, QueryResult engineeringTargets) {
        LaserNight night = createLaserNight(day);
        return updateLaserNight(night, scienceTargets, engineeringTargets);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public LaserNight loadLaserNight(Long id) {
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery(LaserNight.QUERY_LOAD_BY_ID).
                setLong("id", id);
        LaserNight laserRun = (LaserNight) q.uniqueResult();
        return laserRun;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public LaserNight loadLaserNight(DateTime dateTime) {
        SimpleLaserNight night = getLaserNight(dateTime);
        if (night != null) {
            return loadLaserNight(night.getId());
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteLaserNight(Long id) {
        LaserNight laserNight = loadLaserNight(id);
        sessionFactory.getCurrentSession().delete(laserNight);
        sessionFactory.getCurrentSession().flush();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<BlanketClosure> getBlanketClosures(Long id) {
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery(BlanketClosure.FIND_FOR_NIGHT).
                setLong("nightId", id);
        return q.list();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<EngTargetTemplate> getEngineeringTargetsForSite(Site site) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(EngTargetTemplate.FIND_BY_SITE_QUERY).
                setString("site", site.name());
        return (List<EngTargetTemplate>) q.list();
    }

    @Override
    @Transactional(readOnly = true)
    public SimpleLaserNight getShortLaserNight(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(SimpleLaserNight.FIND_BY_ID_QUERY).
                setLong("nightId", id);
        return (SimpleLaserNight)q.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public SimpleLaserNight getShortLaserNight(DateTime date) {
        DateTime startOfDay = date.withTimeAtStartOfDay();
        List<SimpleLaserNight> nights = getShortLaserNights(startOfDay, startOfDay.plusDays(1));
        Validate.isTrue(nights.size() <= 1);
        if (nights.size() == 0) {
            return null;
        } else {
            return nights.get(0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SimpleLaserNight getShortLaserNightCovering(DateTime time) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(SimpleLaserNight.FIND_BY_SITE_AND_TIME_QUERY).
                setString("site", siteService.getSite().name()).
                setTimestamp("time", time.toDate());
        return (SimpleLaserNight) q.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimpleLaserNight> getShortLaserNights(DateTime startsAfter, DateTime startsBefore) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(SimpleLaserNight.FIND_BY_SITE_AND_START_QUERY).
                setString("site", siteService.getSite().name()).
                setTimestamp("startsAfter", startsAfter.toDate()).
                setTimestamp("startsBefore", startsBefore.toDate());
        return (List<SimpleLaserNight>) q.list();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public SimpleLaserNight getNextLaserNight(DateTime after) {
        List<SimpleLaserNight> nights = getNextLaserNights(after, 1);
        if (nights.size() > 0) {
            return nights.get(0);
        } else {
            return null;
        }

    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<SimpleLaserNight> getNextLaserNights(DateTime after, Integer maxResult) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(SimpleLaserNight.QUERY_FIND_BY_SITE_STARTING_AFTER).
                setString("site", siteService.getSite().name()).
                setTimestamp("after", after.toDate()).
                setMaxResults(maxResult);
        return q.list();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public SimpleLaserNight getPreviousLaserNight(DateTime before) {
        List<SimpleLaserNight> nights = getPreviousLaserNights(before, 1);
        if (nights.size() > 0) {
            return nights.get(0);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<SimpleLaserNight> getPreviousLaserNights(DateTime before, Integer maxResult) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(SimpleLaserNight.QUERY_FIND_BY_SITE_ENDING_BEFORE).
                setString("site", siteService.getSite().name()).
                setTimestamp("before", before.toDate()).
                setMaxResults(maxResult);
        return q.list();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public SimpleLaserNight getLaserNight(DateTime day) {
        Session session = sessionFactory.getCurrentSession();
        DateTime dayStart = day.withTimeAtStartOfDay();
        DateTime dayEnd = dayStart.plusDays(1);
        Query q = session.
                getNamedQuery(SimpleLaserNight.QUERY_FIND_BY_SITE_AND_DATE).
                setString("site", siteService.getSite().name()).
                setTimestamp("dayStart", dayStart.toDate()).
                setTimestamp("dayEnd", dayEnd.toDate());
        return (SimpleLaserNight) q.uniqueResult();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addClosure(LaserNight night, DateTime start, DateTime end) {
        BlanketClosure closure = new BlanketClosure(new DateTime(start), new DateTime(end));
        createEvent(night,
                "Blanket closure " +
                start.toDateTime(DateTimeZone.UTC).toString("HH:mm:ss z") + " - " +
                end.toDateTime(DateTimeZone.UTC).toString("HH:mm:ss z") +
                " created.");

        Session session = sessionFactory.getCurrentSession();
        night.getClosures().add(closure);
        session.saveOrUpdate(night);

        // adding closures might potentially impact alarm service
        alarmService.updateNight();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void updateClosure(LaserNight night, BlanketClosure o) {
        createEvent(night,
                "Blanket closure " +
                        o.getStart().toDateTime(DateTimeZone.UTC).toString("HH:mm:ss z") + " - " +
                        o.getEnd().toDateTime(DateTimeZone.UTC).toString("HH:mm:ss z") +
                        " updated.");

        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(o);

        // updating closures might potentially impact alarm service
        alarmService.updateNight();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteClosure(LaserNight night, BlanketClosure o) {
        createEvent(night,
                "Blanket closure " +
                        o.getStart().toDateTime(DateTimeZone.UTC).toString("HH:mm:ss z") + " - " +
                        o.getEnd().toDateTime(DateTimeZone.UTC).toString("HH:mm:ss z") +
                        " deleted.");

        Session session = sessionFactory.getCurrentSession();
        night.getClosures().remove(o);
        session.saveOrUpdate(night);

        // deleting closures might potentially impact alarm service
        alarmService.updateNight();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void saveOrUpdate(Object o) {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(o);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Object o) {
        Session session = sessionFactory.getCurrentSession();
        session.delete(o);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addAndReplacePropagationWindows(LaserNight night, Response response) {
        // sanity checks
        DateTime missionStart = response.getMissionStart();
        DateTime missionEnd = response.getMissionEnd();
        Validate.notNull(response.getSite());
        Validate.notNull(missionStart);
        Validate.notNull(missionEnd);
        Validate.isTrue(missionEnd.isAfter(missionStart));

        // get the candidate targets for this night
        Collection<AzElLaserTarget> azElCandidates = night.getAzElLaserTargets();
        Collection<RaDecLaserTarget> raDecCandidates = night.getRaDecLaserTargets();

        // do the actual work
        for (Target target : response.getTargets()) {

            // find the laser target for the target in the PAM file
            LaserTarget laserTarget = null;
            if (response.isAzEl()) {
                AzAltTarget t = (AzAltTarget) target;
                laserTarget = findLaserTargetForPamTarget(t, azElCandidates);
            }
            if (response.isRaDec()) {
                RaDecTarget t = (RaDecTarget) target;
                laserTarget = findLaserTargetForPamTarget(t, raDecCandidates);
            }

            // check if we did find a corresponding laser target
            if (laserTarget == null) {
                // sanity check, we don't expect this to ever happen, check data if this does happen
                LOGGER.warn(
                    String.format("there is a problem with the data: could  not find laser target (%.3f %.3f) in laser run", target.getDegrees1(), target.getDegrees2())
                );
                continue;
            }
            // check if data in report is older than what we already have
            if (laserTarget.hasWindowsTimestamp() && laserTarget.getWindowsTimestamp().isAfter(response.getReportTime())) {
                // don't update with older data than what we have, ignore older data
                LOGGER.warn(
                    String.format("timestamp of report is older than propagation windows already imported for (%.3f %.3f), windows are ignored", laserTarget.getDegrees1(), laserTarget.getDegrees2())
                );
                continue;
            }

            // all seems to be ok, update data by replacing old with new windows
            List<PropagationWindow> windows = response.getWindowsForTarget(target);
            laserTarget.setWindowsTimestamp(response.getReportTime());
            laserTarget.getPropagationWindows().clear();
            laserTarget.getPropagationWindows().addAll(windows);
            Validate.isTrue(laserTarget.windowsAreDisjoint()); // we trust LCH to do this right, but just to be sure
            sessionFactory.getCurrentSession().saveOrUpdate(laserTarget);
        }
    }

    /**
     * Finds the laser target that corresponds to the given target from the pam file.
     * @param pamTarget
     * @param candidates
     * @return
     */
    private LaserTarget findLaserTargetForPamTarget(Target pamTarget, Collection<? extends LaserTarget> candidates) {
        for (LaserTarget laserTarget : candidates) {
            if (hasSameCoordinates(pamTarget, laserTarget)) {
                return laserTarget;
            }
        }
        return null;
    }

    /**
     * Checks if the coordinates of a target from the PAM files matches the ones of a laser target.
     * We get 3 decimal digits in the PAM files, so we have to allow for quite a bit of error when comparing.
     * @param pamTarget
     * @param laserTarget
     * @return
     */
    private boolean hasSameCoordinates(Target pamTarget, LaserTarget laserTarget) {
        if (Math.abs(pamTarget.getDegrees1() - laserTarget.getDegrees1()) > 0.001) {
            return false;
        }
        if (Math.abs(pamTarget.getDegrees2() - laserTarget.getDegrees2()) > 0.001) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void createEvent(LaserNight night, String message) {
        Collection<EventFile> files = Collections.emptySet();
        createEvent(night, message, files);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void createEvent(Long nightId, String message, Collection<EventFile> files) {
        LaserNight night = loadLaserNight(nightId);
        createEvent(night, message, files);
    }

    private void createEvent(LaserNight night, String message, Collection<EventFile> files) {
        LaserRunEvent event = new LaserRunEvent(message);
        event.getFiles().addAll(files);
        night.getEvents().add(event);
        sessionFactory.getCurrentSession().saveOrUpdate(night);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight createTestLaserNight(DateTime day) {
        QueryResult testData = getTestScienceQueryResult(day);
        QueryResult engData = new QueryResult();
        return createAndPopulateTestLaserNight(day, testData, engData);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LaserNight createAndPopulateTestLaserNight(DateTime day, QueryResult testData, QueryResult engData) {

        // if there is already a laser night for this date, just return the existing one
        SimpleLaserNight n = getLaserNight(day);
        if (n != null) {
            return loadLaserNight(n.getId());
        }

        // otherwise create a new test laser night
        LOGGER.info("==== Creating test laser night for " + day + " ... ====");

        DateTime start = day.withTimeAtStartOfDay();
        // create a 24hrs day / night so that test data is available for 24hrs
        LaserNight night = new LaserNight(siteService.getSite(), start, start.plusDays(1));
        night = updateLaserNight(night, testData, engData);

        // populate test night
        populateTestNight(night.getRaDecLaserTargets(), night);
        populateTestNight(night.getAzElLaserTargets(), night);

        sessionFactory.getCurrentSession().save(night);

        LOGGER.info("==== Test laser night created ====");

        return night;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DateTime getEarliestPropagation(LaserNight night) {
        // unfortunately we need a special handling here for test days/nights: propagation is supposed to be ok 24hrs
        if (night.isTestNight()) {
            return night.getStart();
        }
        // for "real" nights we only allow propagation between the configurable twilight limits
        Selection.Twilight twilight = (Selection.Twilight) configurationService.getSelection(Configuration.Value.VISIBILITY_TWILIGHT);
        SunRiseSet sunCalculator = ModelFactory.createSunCalculator(night);
        switch (twilight) {
            case SUNRISE:       return new DateTime(sunCalculator.getSunset());
            case CIVIL:         return new DateTime(sunCalculator.getCivilTwilightStart());
            case NAUTICAL:      return new DateTime(sunCalculator.getNauticalTwilightStart());
            case ASTRONOMICAL:  return new DateTime(sunCalculator.getAstronomicalTwilightStart());
            default:            throw new RuntimeException("invalid twilight value");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DateTime getLatestPropagation(LaserNight night) {
        // unfortunately we need a special handling here for test days/nights: propagation is supposed to be ok 24hrs
        if (night.isTestNight()) {
            return night.getEnd();
        }
        // for "real" nights we only allow propagation between the configurable twilight limits
        Selection.Twilight twilight = (Selection.Twilight) configurationService.getSelection(Configuration.Value.VISIBILITY_TWILIGHT);
        SunRiseSet sunCalculator = ModelFactory.createSunCalculator(night);
        switch (twilight) {
            case SUNRISE:       return new DateTime(sunCalculator.getSunrise());
            case CIVIL:         return new DateTime(sunCalculator.getCivilTwilightEnd());
            case NAUTICAL:      return new DateTime(sunCalculator.getNauticalTwilightEnd());
            case ASTRONOMICAL:  return new DateTime(sunCalculator.getAstronomicalTwilightEnd());
            default:            throw new RuntimeException("invalid twilight value");
        }
    }


    /** {@inheritDoc} */
    @Override
    @Transactional
    public Long handlePamFile(PamFile pamFile) {

        try {

            InputStream is = new ByteArrayInputStream(pamFile.content());
            Response response = Response.parseResponse(is);
            validateResponse(response);

            LaserNight night = loadLaserNight(response.getMissionStart());
            if (night == null) {
                // if there is no laser night: error
                throw new RuntimeException("No laser night defined in LTTS for " + response.getMissionStart().toDateTime(DateTimeZone.UTC).toString("EEEE, MMM dd, yyyy (D) z") + ".");
            }

            // if there is a laser night continue processing the response
            addAndReplacePropagationWindows(night, response);
            night.setLatestPamReceived(ZonedDateTime.now());
            saveOrUpdate(night);

            // return the id of the night this part belongs to
            return night.getId();

        // catch checked exceptions and rethrow as runtime exc. in order to rollback transaction
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateResponse(Response response) {
        // NOTE: the mission id in the PAM file looks like this: Gemini North_589nm_14W_.65urad_100kHz_11148104428_P

        String id = response.getMissionId().toLowerCase();
        // mission id must mention Gemini (otherwise it's for another observatory/laser operator)
        if (!id.contains("gemini")) {
            throw new RuntimeException("The PAM file is not for Gemini.");
        }
        // mission id must contain site (north or south)
        if (!id.contains("north") && !id.contains("south")) {
            throw new RuntimeException("The PAM file's id does not mention the site (north or south).");
        }
        // the PAM file must be sent to the proper LTTS instance
        if (!response.getSite().equals(siteService.getSite())) {
            throw new RuntimeException("The PAM file is not for this site.");
        }
    }


    private void populateTestNight(Collection<? extends LaserTarget> targets, LaserNight night) {
        List<Target> fakeTargets = new ArrayList<>();
        Map<Target, List<PropagationWindow>> fakeWindows = new HashMap<>();

        int offsetMinutes = 5;
        int durationSeconds = 1;
        Duration interval = Duration.ofMinutes(15);
        for (LaserTarget t : targets) {
            Duration offset = Duration.ofMinutes(offsetMinutes);
            Duration duration = Duration.ofSeconds(durationSeconds);
            List<PropagationWindow> trgWin = createTestWindows(night.getStart(), offset, duration, interval, night.getEnd());

            // create fake target for response
            Target fakeTarget;
            if (targets.iterator().next() instanceof RaDecLaserTarget) {
                fakeTarget = new RaDecTarget(t.getDegrees1(), t.getDegrees2());
            } else {
                fakeTarget = new AzAltTarget(t.getDegrees1(), t.getDegrees2());
            }
            // create series of fake windows for this target
            fakeTargets.add(fakeTarget);
            fakeWindows.put(fakeTarget, trgWin);

            // slightly change offsets and duration of shuttering windows
            offsetMinutes = ((offsetMinutes) + 5) % 60;
            durationSeconds = ((durationSeconds + 2) % 60);
        }

        Response fakeResponse = new Response(fakeTargets, fakeWindows, night.getSite(), ZonedDateTime.now(), night.getStart(), night.getEnd());
        addAndReplacePropagationWindows(night, fakeResponse);
    }

    private List<PropagationWindow> createTestWindows(ZonedDateTime start, Duration offset, Duration duration, Duration interval, ZonedDateTime end) {
        List<PropagationWindow> windows = new ArrayList<>();
        ZonedDateTime time = start;
        while (time.isBefore(end)) {
            ZonedDateTime windowStart = time.plus(offset);
            ZonedDateTime windowEnd = windowStart.plus(interval).minus(duration);
            PropagationWindow window = new PropagationWindow(windowStart, windowEnd);
            windows.add(window);
            time = time.plus(interval);
        }
        return windows;
    }

    /**
     * Gets all science targets according to the configured query.
     * @param day
     * @return
     */
    private QueryResult getScienceQueryResult(DateTime day) {
        return getQueryResult(day, Configuration.Value.SCIENCE_QUERY);
    }

    /**
     * Gets all test science targets according to the configured query.
     * @param day
     * @return
     */
    private QueryResult getTestScienceQueryResult(ZonedDateTime day) {
        return getQueryResult(day, Configuration.Value.TEST_SCIENCE_QUERY);
    }

    /**
     * Gets all engineering targets according to the configured query.
     * @param day
     * @return
     */
    private QueryResult getEngineeringQueryResult(ZonedDateTime day) {
        return getQueryResult(day, Configuration.Value.ENGINEERING_QUERY);
    }

    private QueryResult getQueryResult(ZonedDateTime day, Configuration.Value type) {
        TemplateEngine engine = factory.createTemplateEngine(day);
        // TODO: change interface so that we can pass URL and list of query paramters down to ODB
        String url = configurationService.getString(Configuration.Value.ODB_URL);
        String queryTemplate = configurationService.getString(type);
        String query = engine.fillTemplate(queryTemplate);
        // TODO: change this interface, pass separate parameters down instead of one string
        return odbBrowser.query(url + "?" + query);
    }

}
