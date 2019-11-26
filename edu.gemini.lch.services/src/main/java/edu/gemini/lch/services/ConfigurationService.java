package edu.gemini.lch.services;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.configuration.Selection;

import java.time.Period;
import java.util.List;

/**
 * Service for accessing and updating LLTS configuration values.
 * The set of configuration entries is assumed not to change during the up-time of the application, only the values
 * can be changed, therefore only an update method is needed. Group and parameter names for all known entries
 * are defined in ConfigurationEntry and have to be present in the database when the application starts.
 */
public interface ConfigurationService {

    /**
     * Gets a list of all known configuration groups.
     */
    List<String> getConfigurationGroups();

    /**
     * Gets all configuration entries of the given group for a site.
     */
    List<Configuration> getConfigurationEntries(String group);

    /**
     * Gets the unique configuration entry for the given site and name.
     */
    Configuration getConfigurationEntry(Configuration.Value name);

    /**
     * Checks if a value is set for this parameter.
     * @return <code>false</code> if value is <code>null</code> or an empty string, <code>true</code> otherwise.
     */
    Boolean isEmpty(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a String.
     */
    String getString(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a list of Strings.
     */
    List<String> getStringList(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a list of Strings.
     */
    String[] getStringArray(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as an Integer.
     */
    Integer getInteger(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a list of Integers.
     */
    List<Integer> getIntegerList(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     */
    Period getPeriod(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     */
    List<Period> getPeriodList(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     */
    Double getDouble(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     */
    Boolean getBoolean(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     */
    Selection getSelection(Configuration.Value name);

    /**
     * Updates a configuration entry.
     */
    void update(Configuration entry, String newValue);
}
