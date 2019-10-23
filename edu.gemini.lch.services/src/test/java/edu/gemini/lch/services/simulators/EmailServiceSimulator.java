package edu.gemini.lch.services.simulators;

import edu.gemini.lch.services.EmailService;
import edu.gemini.lch.services.util.PrmFile;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * A simulator to replace the email service during tests.
 */
public class EmailServiceSimulator implements EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailServiceSimulator.class);

    @Override
    public void checkForNewEmails() {
        LOGGER.debug("simulating checking for new emails");
    }

    @Override
    public void downloadPamsFromSpaceTrack() {
        LOGGER.debug("simulating downloading PAMs");
    }

    @Override
    public void sendEmail(String[] to, String[] cc, String[] bcc, String subject, String text) {
        LOGGER.debug("simulating sending emails");
    }

    @Override
    public void sendEmailWithAttachment(String[] to, String[] cc, String[] bcc, String subject, String text, Collection<PrmFile.File> files) {
        LOGGER.debug("simulating sending emails with attachments");
    }

    @Override
    public void sendInternalEmail(String subject, String text) {
        LOGGER.debug("simulating sending an internal email");
    }

}
