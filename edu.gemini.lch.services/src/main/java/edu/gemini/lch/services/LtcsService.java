package edu.gemini.lch.services;

import org.joda.time.DateTime;

import java.util.List;

/**
 * The LTCS service interface.
 */
public interface LtcsService {

    /**
     * Gets a snapshot of the most recent LTCS status.
     * @return
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
         * @return
         */
        List<Collision> getCollisions();
    }

    interface Collision extends Comparable<Collision> {
        String getObservatory();
        String getPriority();
        DateTime getStart();
        DateTime getEnd();
        Boolean geminiHasPriority();
        Boolean contains(DateTime time);
    }
}
