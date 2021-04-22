package edu.gemini.lch.services.impl;

import edu.gemini.lch.services.LoggingService;
import edu.gemini.lch.services.SiteService;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.InputStream;
import java.lang.Override;

/**
 * A service that allows to change the logging dynamically (log4j).
 * This could be done using JMX, but it's probably simpler if people can do it directly through the application.
 */
@Service
public class LoggingServiceImpl implements LoggingService {

    private static final Logger LOGGER = Logger.getLogger(LoggingServiceImpl.class);

    @Resource
    private SiteService siteService;

    @PostConstruct
    private void init() {
        String environment = siteService.getEnvironment();
        String site = siteService.getSite().name().toLowerCase();
        String logConfigurationFile = "log4j." + environment + "." + site + ".properties";
        LOGGER.info("Configuring logger with settings from " + logConfigurationFile);
        InputStream in = getClass().getClassLoader().getResourceAsStream(logConfigurationFile);
        try {
            PropertyConfigurator.configure(in);
            in.close();
        } catch (Exception e) {
            LOGGER.error("Could not configure logger", e);
        }

        logApplicationInfo(logConfigurationFile);
    }

    /**
     * Logs general application information after logger is properly configured.
     */
    private void logApplicationInfo(String logConfigurationFile) {

        LOGGER.info("=================================================================================");
        LOGGER.info("Starting up LTTS");
        LOGGER.info("Site                   = " + siteService.getSite());
        LOGGER.info("Environment            = " + siteService.getEnvironment());
        LOGGER.info("Version                = " + siteService.getVersion());
        LOGGER.info("Log configuration file = " + logConfigurationFile);
        LOGGER.info("Start JspOC Service    = GN: " + JSpOCCredentials$.MODULE$.gnUserName() + " GS: " + JSpOCCredentials$.MODULE$.gsUserName());
        LOGGER.info("=================================================================================");

    }

    @Override
    public void setLogLevel(Level level) {
        LOGGER.info("Setting log level to " + level);
        LogManager.getRootLogger().setLevel(level);
    }
}
