package edu.gemini.lch.data;

import edu.gemini.lch.data.fixture.DatabaseFixture;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * This test makes sure that all persistence objects can be successfully accessed. This will also test
 * that named queries are valid and in general make sure that all annotations etc. are sound and valid.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-data-test-context.xml"})
public class DataObjectTest extends DatabaseFixture {

    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Test
    public void checkLaserNight() {
        checkEntity("LaserNight");
    }

    @Test
    public void checkLaserTarget() {
        checkEntity("LaserTarget");
    }

    @Test
    public void checkEngineeringTarget() {
        checkEntity("EngineeringTarget");
    }

    @Test
    public void checkEngTargetTemplate() {
        checkEntity("EngTargetTemplate");
    }

    @Test
    public void checkDecLaserTarget() {
        checkEntity("RaDecLaserTarget");
    }

    @Test
    public void checkAzElLaserTarget() {
        checkEntity("AzElLaserTarget");
    }

    @Test
    public void checkSiderealTarget() {
        checkEntity("SiderealTarget");
    }

    @Test
    public void checkNonSiderealTarget() {
        checkEntity("NonSiderealTarget");
    }

    @Test
    public void checkPropagationWindow() {
        checkEntity("PropagationWindow");
    }

    @Test
    public void checkBlanketClosure() {
        checkEntity("BlanketClosure");
    }

    @Test
    public void checkConfiguration() {
        checkEntity("Configuration");
    }

    @Test
    public void checkStringConfiguration() {
        checkEntity("ConfigurationValue");
    }


    private void checkEntity(String entityName) {
        final Session session = sessionFactory.openSession();
        try {
            final Query query = session.createQuery("from " + entityName);
            List targets = query.list();
        } finally {
            session.close();
        }
    }


}
