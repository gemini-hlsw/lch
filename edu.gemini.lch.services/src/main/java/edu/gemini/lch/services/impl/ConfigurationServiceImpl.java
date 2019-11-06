package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.*;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.SiteService;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the configuration service using a database.
 */
@Service
public class ConfigurationServiceImpl implements ConfigurationService, ApplicationListener<ContextRefreshedEvent> {

    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(ConfigurationServiceImpl.class.getName());

    /** The cache. */
    private Cache cache;

    /** The session factory */
    @Resource private SessionFactory sessionFactory;
    @Resource private SiteService siteService;

    @PostConstruct
    private void init() {
        cache = new Cache();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public List<String> getConfigurationGroups() {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.createSQLQuery("select distinct groupname from lch_configuration_entries");
        return q.list();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public List<Configuration> getConfigurationEntries(String group) {
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(Configuration.QUERY_FIND_BY_SITE_AND_GROUP).
                setString("site", siteService.getSite().name()).
                setString("group", group);
        return q.list();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Configuration getConfigurationEntry(Configuration.Value name) {
        return cache.getValue(name);
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Boolean isEmpty(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().isEmpty();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public String getString(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsString();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public List<String> getStringList(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsStringList();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public String[] getStringArray(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsStringArray();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Integer getInteger(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsInteger();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public List<Integer> getIntegerList(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsIntegerList();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Double getDouble(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsDouble();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Period getPeriod(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsPeriod();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public List<Period> getPeriodList(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsPeriodList();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Boolean getBoolean(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        return c.getValue().getAsBoolean();
    }

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override public Selection getSelection(Configuration.Value name) {
        Configuration c = getConfigurationEntry(name);
        Validate.isTrue(c.getType().equals(Configuration.Type.SELECTION), "operation only allowed on selection values");
        // TODO: make this available for other selections, too; maybe pass down class?
        return Selection.Twilight.valueOf(c.getValue().getAsString());
    }

    /** {@inheritDoc} */
    @Transactional
    @Override public void update(Configuration entry, String newValue) {
        // remove stored value from cache to make sure new value is read on next update
        Configuration.Value value = Configuration.Value.valueOf(entry.getParamName());
        cache.removeValue(value);

        // log configuration changes..
        if (entry.getType().equals(Configuration.Type.TEXT)) {
            LOGGER.log(Level.INFO, "configuration change for " + entry.getParamName() + ":\n\n" + entry.getAsString() + "\n\n==>\n\n" + newValue +"\n");
        } else {
            LOGGER.log(Level.INFO, "configuration change for " + entry.getParamName() + ": " + entry.getAsString()+ " ==> " + newValue);
        }

        // change configuration in database
        entry.setFromString(newValue);
        sessionFactory.getCurrentSession().update(entry);

        // last step: do any actions that are needed to reflect configuration change in app
        // most changes don't need to be processed specially, they will be picked up after a while automatically
        // but sometimes something needs to be done, e.g. changing the default time zone
        processChange(value);
    }

    /**
     * Called on application startup, will push any actions that need to be done to reflect current configuration.
     */
    @Transactional(readOnly = true)
    @Override public void onApplicationEvent(ContextRefreshedEvent event) {
        // application is fully wired and properly set up now, so we can do anything we want
        LOGGER.info("configuring application...");

        // make sure that on startup default time zone is set properly according to current configuration
        updateDefaultTimeZone();

        /** add any other configuration magic that needs to be done at startup here **/

        LOGGER.info("... done!");
    }


    @Transactional(readOnly = true)
    Configuration readConfigurationEntry(Configuration.Value name) {
        LOGGER.log(Level.TRACE, "reading configuration entry from database: " + name);
        Session session = sessionFactory.getCurrentSession();
        Query q = session.
                getNamedQuery(Configuration.QUERY_FIND_BY_SITE_AND_NAME).
                setString("site", siteService.getSite().name()).
                setString("name", name.name());
        Configuration entry = (Configuration) q.uniqueResult();
        if (entry == null) {
            LOGGER.log(Level.ERROR, "invalid configuration: no value with name " + name + " in database");
            throw new IllegalArgumentException("internal error: invalid configuration " + name);
        }
        return entry;
    }

    /**
     * Processes any configuration changes that need a special action to be reflected in the application (as opposed
     * to automatically being picked up after a while).
     * Note: Be aware that throwing a runtime exception here will cause the config change transaction to be
     * rolled back, which may or may not be the right thing to do. Deal with exceptions accordingly.
     */
    private void processChange(Configuration.Value value) {
        if (value == Configuration.Value.TIME_ZONE_OFFSET) {
            updateDefaultTimeZone();
        }
    }

    /**
     * Updates the default time zone, i.e. the time zone which is used as local time.
     */
    private void updateDefaultTimeZone() {
        TimeZone.setDefault(siteService.getSiteTimeZone());
    }

    /**
     * A very simple cache for configuration values.
     * Values are stored for no more than 30 seconds and then reread from the database. Rereading them allows to change
     * the configuration through other channels than just the application administration window (e.g. directly in the
     * database using SQL).
     */
    private class Cache {d
        private static final int CACHE_REFRESH_SECONDS = 30;

        private final Map<Configuration.Value, Entry> map;

        /** A cache entry (value and timestamp). */
        private class Entry {
            final Configuration configuration;
            final ZonedDateTime timestamp;
            Entry(Configuration configuration) {
                this.configuration = configuration;
                this.timestamp = ZonedDateTime.now();
            }
        }

        /** Constructs an empty cache. */
        Cache() {
            // use a concurrent hash map, access to this service is not synchronized
            // and may happen from several threads in parallel
            map = new ConcurrentHashMap<>();
        }

        /**
         * Gets a value from the cache or reads it from the database if it was not yet loaded or the value is too old.
         */
        Configuration getValue(Configuration.Value value) {

            // check if we have the value in the cache
            if (map.containsKey(value)) {
                // we have a configuration stored for this name
                Entry e = map.get(value);
                if (e.timestamp.isAfter(ZonedDateTime.now().minusSeconds(CACHE_REFRESH_SECONDS))) {
                    // if it is not too old return it
                    return e.configuration;
                }
            }

            // value is unknown or too old: read it from database
            Configuration configuration = readConfigurationEntry(value);
            Entry e = new Entry(configuration);
            map.put(value, e);
            return configuration;
        }

        /**
         * Removes a value from the cache and forces a reload on next access.
         */
        void removeValue(Configuration.Value value) {
            map.remove(value);
        }
    }

}
