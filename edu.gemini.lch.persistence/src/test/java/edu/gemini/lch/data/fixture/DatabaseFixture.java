package edu.gemini.lch.data.fixture;

import org.dbunit.database.DatabaseConnection;
import org.junit.After;
import org.junit.Before;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * The mother of all tests.
 */
public class DatabaseFixture {

    @Resource(name = "lchDataSource")
    private DataSource dataSource;

    @Before
    public void beforeTest() throws Exception {
        // cleanup database before every test (deleting laser runs will cascade down to all other tables)
        // note: do NOT delete the configuration tables!
        DatabaseConnection c = new DatabaseConnection(dataSource.getConnection());
        c.getConnection().createStatement().execute("delete from lch_laser_nights;");
        c.close();
    }


    @After
    public void afterTest() {
        // nothing to be done right now
    }

}
