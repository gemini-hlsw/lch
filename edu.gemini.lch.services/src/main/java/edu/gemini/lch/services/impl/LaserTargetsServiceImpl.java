package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.*;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.LaserTargetsService;
import edu.gemini.lch.services.timeline.TimeLineImage;
import edu.gemini.lch.services.timeline.TimeLineHeader;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 */
@Service
public class LaserTargetsServiceImpl implements LaserTargetsService {

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource
    private LaserNightService laserNightService;

    @Resource
    private ConfigurationService configurationService;

    @Override
    @Transactional(readOnly = true)
    public LaserTarget findById(Long targetId) {
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery(LaserTarget.QUERY_FIND_BY_ID).
                setLong("id", targetId);
        LaserTarget target = (LaserTarget) q.uniqueResult();
        return target;
    }

// TODO: next methods might become obsolete when controller for web servicse uses the cached values instead...
    @Override
    @Transactional(readOnly = true)
    public LaserTarget findRaDecByPosition(Long nightId, Double ra, Double dec, Double maxDistance) {
        return findByPosition(nightId, RaDecLaserTarget.QUERY_FIND_BY_NIGHT, ra, dec, maxDistance);
    }

    @Override
    @Transactional(readOnly = true)
    public LaserTarget findAzElByPosition(Long nightId, Double az, Double el, Double maxDistance) {
        return findByPosition(nightId, AzElLaserTarget.QUERY_FIND_BY_NIGHT, az, el, maxDistance);
    }

    private LaserTarget findByPosition(Long nightId, String queryName, Double c1, Double c2, Double maxDistance) {
        // get a set of candidates in the given range from the database
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery(queryName).
                setLong("nightId", nightId);
        // get the closest of all laser targets for this night
        LaserTarget closest = LaserTarget.getClosestTo(q.list(), c1, c2);
        // check minimal distance and return result, can be null!
        if (closest != null && closest.distanceTo(c1, c2) > maxDistance) {
            return null;
        } else {
            return closest;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Observation> getObservations(Long targetId) {
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery("observation.findForLaserTarget").
                setLong("targetId", targetId);
        return q.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropagationWindow> getPropagationWindows(Long nightId, Long targetId, boolean removeBlanketClosures) {
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery("propagationWindow.findForLaserTarget").
                setLong("targetId", targetId);
        if (removeBlanketClosures) {
            List<BlanketClosure> closures = laserNightService.getBlanketClosures(nightId);
            return PropagationWindow.removeClosures(q.list(), closures);
        } else {
            return q.list();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShutteringWindow> getShutteringWindows(Long nightId, Long targetId, boolean includeBlanketClosures) {
        Query q = sessionFactory.getCurrentSession().
                getNamedQuery("propagationWindow.findForLaserTarget").
                setLong("targetId", targetId);
        List<ShutteringWindow> shutteringWindows = PropagationWindow.getShutteringWindows(q.list());
        if (includeBlanketClosures) {
            List<BlanketClosure> closures = laserNightService.getBlanketClosures(nightId);
            return ShutteringWindow.includeBlanketClosures(shutteringWindows, closures);
        } else {
            return shutteringWindows;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImageHeader(LaserNight night, Integer width, DateTime start, DateTime end, DateTimeZone zone) {
        TimeLineHeader header = new TimeLineHeader(start, end, width, 16, zone);
        return header.getAsBytes();
    }


    @Override
    @Transactional(readOnly = true)
    public byte[] getImage(LaserNight night, LaserTarget target, Integer width, Integer height, DateTime start, DateTime end) {
        Period before = Period.seconds(configurationService.getInteger(Configuration.Value.LIS_BUFFER_BEFORE_SHUTTER_WINDOW));
        Period after = Period.seconds(configurationService.getInteger(Configuration.Value.LIS_BUFFER_AFTER_SHUTTER_WINDOW));
        TimeLineImage image =
                new TimeLineImage(night).
                        withTarget(target).
                        withTimes(start, end).
                        withDimensions(width, height).
                        withBuffers(before, after);
        return image.bytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImage(LaserNight night, LaserTarget target,  Integer width, Integer height, DateTime start, DateTime end, DateTime now, DateTimeZone zone) {
        Period before = Period.seconds(configurationService.getInteger(Configuration.Value.LIS_BUFFER_BEFORE_SHUTTER_WINDOW));
        Period after = Period.seconds(configurationService.getInteger(Configuration.Value.LIS_BUFFER_AFTER_SHUTTER_WINDOW));
        TimeLineImage image =
                new TimeLineImage(night).
                        withTarget(target).
                        withTimes(start, end).
                        withDimensions(width, height).
                        withNowMarker(now).
                        withText(zone, 13).
                        withBuffers(before, after).
                        withElevationLine();
        return image.bytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImage(LaserNight night, Integer width, Integer height, DateTime start, DateTime end, DateTime now, DateTimeZone zone) {
        Period before = Period.seconds(configurationService.getInteger(Configuration.Value.LIS_BUFFER_BEFORE_SHUTTER_WINDOW));
        Period after = Period.seconds(configurationService.getInteger(Configuration.Value.LIS_BUFFER_AFTER_SHUTTER_WINDOW));
        TimeLineImage image =
                new TimeLineImage(night).
                        withTimes(start, end).
                        withDimensions(width, height).
                        withNowMarker(now).
                        withText(zone, 13).
                        withBuffers(before, after).
                        withElevationLine();
        return image.bytes();
    }


}
