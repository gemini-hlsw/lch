package edu.gemini.lch.services;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.data.fixture.DatabaseFixture;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * Tests for the configuration service.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class ConfigurationServiceTest extends DatabaseFixture {

    @Resource
    private ConfigurationService configurationService;

    private static final String GROUP = "Visibility"; // arbitrary group name

    @Test
    public void canGetGroupNames() {
        List<String> groups = configurationService.getConfigurationGroups();
        Assert.assertTrue(groups.size() > 0);
        Assert.assertTrue(groups.contains(GROUP));
    }

    @Test
    public void canGetAllConfigsForGroup() {
        List<? extends Configuration> entries = configurationService.getConfigurationEntries(GROUP);
        Assert.assertTrue(entries.size() > 0);
    }

    @Test
    public void canGetSingleConfigAsDouble() {
        Double value = configurationService.getDouble(Configuration.Value.VISIBILITY_MIN_ALTITUDE);
        //Assert.assertEquals(35.0, value, 0.000001);
    }

    @Test
    public void canReadAllValues() {
        // make sure everything is sane, no typos in database values etc
        for (Configuration.Value v : Configuration.Value.values()) {
            // accessing this will throw an exception if not sane
            String s = configurationService.getString(v);
        }
    }

}
