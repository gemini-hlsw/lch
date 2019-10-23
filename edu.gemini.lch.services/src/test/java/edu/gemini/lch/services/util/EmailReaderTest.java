package edu.gemini.lch.services.util;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.mail.BodyPart;
import javax.mail.Message;
import java.util.Set;

/**
 * Some tests for manually testing connections to email server.
 * These tests should be ignored by default.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class EmailReaderTest {

    @Ignore
    @Test
    public void testEmailReader() throws Exception {

        EmailReader e = new EmailReader();

        e.connect("gn-lch", "LaserGuide*");

        Message m;
        while ((m = e.getNextMessage()) != null) {
            Set<BodyPart> parts = e.getPamFiles(m);
            e.moveToProcessed(m);
        }

        e.disconnect();

    }
}
