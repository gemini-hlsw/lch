package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.SimpleLaserNight;
import edu.gemini.lch.services.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
@Service
public class SchedulerServiceImpl implements SchedulerService {

    private static final Logger LOGGER = Logger.getLogger(SchedulerServiceImpl.class.getName());

    @Resource private ThreadPoolTaskScheduler taskScheduler;
    @Resource private ThreadPoolTaskExecutor taskExecutor;

    @Resource private ConfigurationService configurationService;
    @Resource private LaserNightService laserNightService;
    @Resource private AlarmService alarmService;
    @Resource private EmailService emailService;

    private enum Task {
        EmailCheck,
        ProcessNights
    }

    private final Map<Task, DateTime> lastExecutionTimes;
    // there is no concurrent set, use concurrent map instead
    private final ConcurrentHashMap<ClockUpdateListener, ClockUpdateListener> clockUpdateListeners;
    private final ConcurrentHashMap<AlarmUpdateListener, AlarmUpdateListener> alarmUpdateListeners;


    SchedulerServiceImpl() {
        lastExecutionTimes = new HashMap<>();
        for (Task task : Task.values()) {
            lastExecutionTimes.put(task, DateTime.now());
        }
        clockUpdateListeners = new ConcurrentHashMap<> ();
        alarmUpdateListeners = new ConcurrentHashMap<> ();
    }

    /** {@inheritDoc} */
    @Override public void addListener(ClockUpdateListener listener) {
        clockUpdateListeners.put(listener, listener);
    }

    /** {@inheritDoc} */
    @Override public void removeListener(ClockUpdateListener listener) {
        clockUpdateListeners.remove(listener);
    }

    /** {@inheritDoc} */
    @Override public void addListener(AlarmUpdateListener listener) {
        alarmUpdateListeners.put(listener, listener);
    }

    /** {@inheritDoc} */
    @Override public void removeListener(AlarmUpdateListener listener) {
        alarmUpdateListeners.remove(listener);
    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 1000)
    @Override public void updateClocks() {
        for (final ClockUpdateListener listener : clockUpdateListeners.values()) {
            taskExecutor.execute(listener::update);
        }
    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 500)
    @Override public void updateAlarms() {
        for (final AlarmUpdateListener listener : alarmUpdateListeners.values()) {
            taskExecutor.execute(() -> listener.update(alarmService.getSnapshot()));
        }
    }

    /** {@inheritDoc} */
    @Scheduled(fixedDelay = 10000) // delay of 10 seconds after completion of last call, don't execute this twice in parallel!
    @Override public void checkForDueTasks() {
        LOGGER.trace("scheduler checking for tasks to execute");

        // -- check for new incoming emails (PAM files)
        try {
            Integer waitIntervall = configurationService.getInteger(Configuration.Value.NEW_MAIL_POLL_INTERVAL);
            if (isDue(Task.EmailCheck, waitIntervall)) {
                    emailService.checkForNewEmails();
            }
        } catch (Exception e) {
            LOGGER.error("could not check for new emails", e);
        }

        // -- process nights according to the schedule configured by users
        try {
            List<Period> schedule = configurationService.getPeriodList(Configuration.Value.SCHEDULER_PROCESS_NIGHTS_SCHEDULE);
            if (isDue(Task.ProcessNights, schedule)) {
                processNights();
            }
        } catch (Exception e) {
            LOGGER.error("could not process laser nights", e);
        }

        LOGGER.trace("scheduler done");
    }


    /**
     * Checks if a task is due according to a list of times (e.g. 11:00, 17:30).
     * @param task
     * @param timesOfDay
     * @return
     */
    private boolean isDue(Task task, List<Period> timesOfDay) {
        for (Period period : timesOfDay) {
            DateTime dueTime = DateTime.now().withTimeAtStartOfDay().plus(period);
            if (dueTime.isBeforeNow() && lastExecutionTimes.get(task).isBefore(dueTime)) {
                lastExecutionTimes.put(task, DateTime.now());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a task is due according to a waiting interval.
     * @param task
     * @param waitPeriodInSeconds
     * @return
     */
    private boolean isDue(Task task, Integer waitPeriodInSeconds) {
        DateTime dueTime = lastExecutionTimes.get(task).plusSeconds(waitPeriodInSeconds);
        if (dueTime.isBeforeNow()) {
            lastExecutionTimes.put(task, DateTime.now());
            return true;
        } else {
            return false;
        }
    }

    private void processNights() {
        // get all nights for next 10 days
        DateTime now = DateTime.now().withTimeAtStartOfDay();
        DateTime til = now.plusDays(10);
        List<SimpleLaserNight> nights = laserNightService.getShortLaserNights(now.toDateTime(), til.toDateTime());
        for (SimpleLaserNight night : nights) {
            try {
                laserNightService.processLaserNight(night.getId());
            } catch (Exception e) {
                LOGGER.error("could not process laser night " + night.getStart(), e);
            }
        }
    }

    @PreDestroy
    private void cleanup() {
        // Stopping the scheduler and executor here keeps the application from throwing a lot of exceptions
        // during shutdown because tasks are being executed while the infrastructure is already shut down.
        LOGGER.info("shutting down scheduler and executor ...");
        taskScheduler.shutdown();
        taskExecutor.shutdown();
        LOGGER.info("... scheduler and executor are shut down.");
    }


}
