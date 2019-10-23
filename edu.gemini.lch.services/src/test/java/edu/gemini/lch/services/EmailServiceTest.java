package edu.gemini.lch.services;

import edu.gemini.lch.data.fixture.DatabaseFixture;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Some tests for manually testing connections to email server.
 * These tests should be ignored by default.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-services-test-context.xml"})
public class EmailServiceTest extends DatabaseFixture {

    @Resource
    private EmailService emailService;


    @Ignore // this will process emails from the configured inbox; use for manual testing only
    @Test
    public void canReadEmails() throws Exception {
        emailService.checkForNewEmails();
    }

    @Ignore // this will send emails to fnussber; use for manual testing only
    @Test
    public void doesNotFloodMailboxes() throws Exception {
        String[] to = new String[] {"fnussber@gemini.edu"};
        String[] cc = new String[] {};
        String[] bcc = new String[] {};
        for (int i = 0; i < 10; i++) {
            emailService.sendEmail(to, cc, bcc, "subject", "text");
        }
    }

}
