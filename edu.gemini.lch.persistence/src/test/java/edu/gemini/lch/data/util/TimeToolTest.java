package edu.gemini.lch.data.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-data-test-context.xml"})
public class TimeToolTest {

    @Test
    public void canMapUTCTimes() throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssz");
        Date min = sdf.parse("20110624120000HST");
        Date max = sdf.parse("20110625120000HST");
        Date result = TimeTool.mapUTC(min, max, 4, 0, 0);

        assertTrue(result.after(min));
        assertTrue(result.before(max));

    }
}
