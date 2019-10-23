package edu.gemini.lch.services;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.configuration.Selection;
import org.joda.time.Period;

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
     * @return
     */
    List<String> getConfigurationGroups();

    /**
     * Gets all configuration entries of the given group for a site.
     * @param group the group
     * @return
     */
    List<Configuration> getConfigurationEntries(String group);

    /**
     * Gets the unique configuration entry for the given site and name.
     * @param name
     * @return
     */
    Configuration getConfigurationEntry(Configuration.Value name);

    /**
     * Checks if a value is set for this parameter.
     * @param name
     * @return <code>false</code> if value is <code>null</code> or an empty string, <code>true</code> otherwise.
     */
    Boolean isEmpty(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a String.
     * @param name
     * @return
     */
    String getString(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a list of Strings.
     * @param name
     * @return
     */
    List<String> getStringList(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a list of Strings.
     * @param name
     * @return
     */
    String[] getStringArray(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as an Integer.
     * @param name
     * @return
     */
    Integer getInteger(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a list of Integers.
     * @param name
     * @return
     */
    List<Integer> getIntegerList(Configuration.Value name);


    /**
     * Gets the configuration value for the given site and name as a Double
     * @param name
     * @return
     */
    Period getPeriod(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     * @param name
     * @return
     */
    List<Period> getPeriodList(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     * @param name
     * @return
     */
    Double getDouble(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     * @param name
     * @return
     */
    Boolean getBoolean(Configuration.Value name);

    /**
     * Gets the configuration value for the given site and name as a Double
     * @param name
     * @return
     */
    Selection getSelection(Configuration.Value name);

    /**
     * Updates a configuration entry.
     * @param entry
     * @param newValue
     */
    void update(Configuration entry, String newValue);
}
