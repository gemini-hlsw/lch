package edu.gemini.lch.services;

import org.apache.log4j.Level;

/**
 * Simple service for dynamically changing the log configuration.
 */
public interface LoggingService {

    void setLogLevel(Level level);

}
