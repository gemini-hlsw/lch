package edu.gemini.lch.services;

import edu.gemini.lch.services.util.PrmFile;

import java.util.Collection;

/**
 * Service for sending emails and attachments.
 */
public interface EmailService {

    /**
     * Checks if new incoming emails are available that need to be processed.
     */
    void checkForNewEmails();

    /**
     * Triggers a download of PAM files from the space-track.org web page.
     */
    void downloadPamsFromSpaceTrack();

    /**
     * Sends a text email.
     * @param to
     * @param cc
     * @param bcc
     * @param subject
     * @param text
     */
    void sendEmail(String[] to, String[] cc, String[] bcc, String subject, String text);

    /**
     * Sends an email with attachments.
     * @param to
     * @param cc
     * @param bcc
     * @param subject
     * @param text
     * @param files
     */
    void sendEmailWithAttachment(String[] to, String[] cc, String[] bcc, String subject, String text, Collection<PrmFile.File> files);

    /**
     * Shortcut for sending an email to the internal email addresses.
     * Used for sending warning and error messages.
     * @param subject
     * @param text
     */
    void sendInternalEmail(String subject, String text);

}
