package edu.gemini.lch.services.util;

import org.apache.log4j.*;

import java.io.StringWriter;
import java.io.Writer;

/**
 * A logger that can be used to collect log messages.
 * This utility logger appends to the logger registered for the given clazz and logs all log messages written
 * to it with level Info or higher. This utility can be used to get a copy of the log messages that are
 * created during the processing of emails etc. This seemed to be the simpler approach than writing to log files
 * in parallel (the "normal" log file and a special log that can be sent to users as part of the emails).
 * IMPORTANT: The assumption is that the class we attach to is executed only once at a time, otherwise we will end
 * up with a mixture of log messages from different instances of the class running! This is currently the case, but
 * if this ever changes, something a bit more elaborate is needed.
 */
public class StringLogger {

    private final Writer writer;
    private final Logger logger;
    private final WriterAppender appender;

    /**
     * Create a string logger and attach it to the logger registered for the given class.
     * @param clazz
     */
    public StringLogger(Class clazz) {
        logger = Logger.getLogger(clazz);
        writer = new StringWriter();
        appender = new WriterAppender(new PatternLayout("%m%n"), writer);
        appender.setThreshold(Level.INFO); // Make sure we get all the messages we are interested in.
        logger.addAppender(appender);
    }

    /**
     * Get the log messages as a string.
     * @return
     */
    public String toString() {
        return writer.toString();
    }

    /**
     * Make sure that on destruction of the logger the appender is unregistered.
     * @throws Throwable
     */
    protected void finalize () throws Throwable {
        logger.removeAppender(appender);
        super.finalize();
    }

}
