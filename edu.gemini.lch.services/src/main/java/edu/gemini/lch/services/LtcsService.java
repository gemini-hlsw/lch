package edu.gemini.lch.services;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * The LTCS service interface.
 */
public interface LtcsService {

    /**
     * Gets a snapshot of the most recent LTCS status.
     */
    Snapshot getSnapshot();

    /** Error codes */
    enum Error {
        NOT_CONNECTED,
        PROCESSES_DOWN,
        BAD_REQUEST,
        OTHER,
        NONE
    }

    interface Snapshot {
        Boolean isConnected();
        Error getError();
        String getMessage();

        /**
         * Gets a sorted list of upcoming laser collisions, sort order is start time ascending.
         */
        List<Collision> getCollisions();
    }

    interface Collision extends Comparable<Collision> {
        String getObservatory();
        String getPriority();
        ZonedDateTime getStart();
        ZonedDateTime getEnd();
        Boolean geminiHasPriority();
        Boolean contains(ZonedDateTime time);
    }
}
