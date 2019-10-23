package edu.gemini.lch.services;

/**
 * A scheduler that executes tasks on a regular basis.
 */
public interface SchedulerService {

    /**
     * Updates the clock in night windows asynchronously once a second to reflect current time.
     * NOTE: Unfortunately we can't use @Async on NightWindow because it is created using AOP therefore we
     * schedule an asynchronous task manually.
     */
    void updateClocks();

    /**
     * Updates all alarm windows asynchronously.
     * NOTE: Unfortunately we can't use @Async on AlarmWindow because it is created using AOP therefore we
     * schedule an asynchronous task manually.
     */
    void updateAlarms();

    /**
     * Checks for due tasks and executes them.
     * In order to be able to respond to configuration changes we can not schedule the tasks using annotations
     * and Spring scheduled tasks but call this method every 10 seconds and let it decide based on the current
     * configuration which tasks need to be executed.
     */
    void checkForDueTasks();

    /** Adds a listener that needs to be called once a second. */
    void addListener(ClockUpdateListener listener);

    /** Removes a listener that needs to be called once a second. */
    void removeListener(ClockUpdateListener listener);

    /** Adds a listener that needs to be called several times a second. */
    void addListener(AlarmUpdateListener listener);

    /** Removes a listener that needs to be called several times a second. */
    void removeListener(AlarmUpdateListener listener);

    interface ClockUpdateListener {
        void update();
    }

    interface AlarmUpdateListener {
        void update(AlarmService.Snapshot snapshot);
    }
}
