package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.EventFile;
import edu.gemini.lch.services.*;
import edu.gemini.lch.services.util.EmailReader;
import edu.gemini.lch.services.util.PrmFile;
import edu.gemini.shared.util.StringUtil;
import org.apache.log4j.*;
import org.hibernate.SessionFactory;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.*;

/**
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailServiceImpl.class);

    private static final int maxEmailsPerDay = 25;

    @Resource
    private ConfigurationService configurationService;

    @Resource
    private SessionFactory sessionFactory;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private JSpOCCommunicatorService jspocService;

    @Resource
    private LaserNightService laserNightService;

    @Resource
    private SiteService siteService;

    @Resource
    private AlarmService alarmService;

    @Resource
    private Factory factory;

    // --- limit number of emails that are sent per day!!
    // --- this is meant to keep lch from flooding people with mails in case something with the
    // --- processing of incoming emails goes wrong (e.g. mails are not moved to processed folder
    // --- and therefore processed over and over again or any other problem I haven't thought of...)
    private long sendCycleStart = 0;
    private int sendCycleCount = 0;
    private boolean restrictEmails(String subject) {
        if (sendCycleStart < (System.currentTimeMillis() - 24 * 60 * 60 * 1000)) {
            LOGGER.info("resetting email limit count to zero, starting new cycle; limit per day is " + maxEmailsPerDay);
            sendCycleStart = System.currentTimeMillis();
            sendCycleCount = 0;
        }
        if (sendCycleCount >= maxEmailsPerDay) {
            LOGGER.error("!!! max amount of outgoing emails reached, not sending email '" + subject + "' !!!");
            return true;
        }
        sendCycleCount++;
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void sendInternalEmail(String subject, String text) {
        String[] to = configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_TO_ADDRESSES);
        String[] cc = configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_CC_ADDRESSES);
        String[] bcc = configurationService.getStringArray(Configuration.Value.EMAILS_INTERNAL_BCC_ADDRESSES);
        sendEmail(to, cc, bcc, subject, text);
    }

    /** {@inheritDoc} */
    @Override
    public void sendEmail(String[] to, String[] cc, String[] bcc, String subject, String text) {
        sendEmailWithAttachment(to, cc, bcc, subject, text, Collections.EMPTY_LIST); // send without attachments
    }

    /** {@inheritDoc} */
    @Override
    public void sendEmailWithAttachment(String[] to, String[] cc, String[] bcc, String subject, String text, Collection<PrmFile.File> files) {
        if (restrictEmails(subject)) {
            return;
        }
        String toStr = StringUtil.mkString(Arrays.asList(to), ",");
        String ccStr = StringUtil.mkString(Arrays.asList(cc), ",");
        try {
            String from = configurationService.getString(Configuration.Value.EMAILS_FROM_ADDRESS);
            String replyTo = configurationService.getString(Configuration.Value.EMAILS_REPLY_TO_ADDRESS);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setFrom(from);
            message.setReplyTo(replyTo);
            message.setTo(to);
            message.setCc(cc);
            message.setBcc(bcc);
            message.setSubject(subject);
            message.setText(text);
            for (final PrmFile.File f : files) {
                InputStreamSource is = () -> new ByteArrayInputStream(f.getFile().getBytes());
                message.addAttachment(f.getName(), is, "text/plain");
            }
            mailSender.send(mimeMessage);

            LOGGER.info("sent email '" + subject + "' (to=" + toStr + " ; cc=" + ccStr + ")");

        } catch (MessagingException e) {

            LOGGER.error("could not send email '" + subject + "' (to=" + toStr + " ; cc=" + ccStr + ")", e);

        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkForNewEmails() {
        String mailUser = configurationService.getString(Configuration.Value.EMAILS_ACCOUNT_USER);
        String mailPassword = configurationService.getString(Configuration.Value.EMAILS_ACCOUNT_PASSWORD);
        checkForNewEmails(mailUser, mailPassword);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void downloadPamsFromSpaceTrack() {
        // this should really live on the JSPoC service but for historical reasons it is currently part of this service
        final Map<Long, List<EventFile>> successfulPams = new HashMap<>();
        final StringBuffer protocol = new StringBuffer();
        downloadPamsFromSpaceTrack(successfulPams, protocol);

        // bookkeeping: add events to the nights the PAM files belong to
        for (Long nightId : successfulPams.keySet()) {
            List<EventFile> files = successfulPams.get(nightId);
            String msg = String.format("Manually downloaded %d PAM files.", files.size());
            laserNightService.createEvent(nightId, msg, files);
        }

        // send email in case we've downloaded some PAMs
        if (successfulPams.size() > 0) {
            sendInternalEmail(
                "LTTS: Successfully digested manually downloaded PAM files",
                "Status of imported files:\n\n" + protocol.toString() + "\n");
        }

    }

    /**
     * Check for new emails for a single site and try to process all of them.
     * Synchronized to make sure that only one thread at a time is trying to access and alter the email inbox.
     * As things are set up this method should never be called more than once at a time, but it's probably not
     * bad to enforce this.
     */
    public synchronized void checkForNewEmails(String mailbox, String password) {
        LOGGER.trace("checking for new emails");

        Boolean hasImport = false;
        EmailReader emailReader = factory.createEmailReader();
        try {
            emailReader.connect(mailbox, password);
            Message message;
            while ((message = emailReader.getNextMessage()) != null) {

                // --- handle every single email separately, usually there will be only one email
                try {

                    hasImport |= handleNewEmail(emailReader, message);

                } catch (Exception e) {

                    // if something goes wrong log error
                    LOGGER.error("Could not process email '" + message.getSubject() + "'", e);
                    sendInternalEmail(
                            "LTTS: ERROR: Could not process email '" + message.getSubject() +"'",
                            "An error occurred while processing this email.\n\nContact your system administrator.\n\n" +
                            getErrorDetails(e)
                        );

                }
            }

        } catch (Exception e) {
            // IO or messaging exception accessing / reading messages
            LOGGER.error("Could not process emails. Note: This can happen from time to time and is only a problem if it happens repeatedly.", e);

        } finally {

            try {
                // make sure reader is closed  (this will delete / move messages)
                emailReader.disconnect();
            } catch (Exception e) {
                // wow, not much we can do here if this fails
                LOGGER.error("error while disconnecting from email", e);
            }

        }

        if (hasImport) {
            // last but not least: if we did import some files, then force an update of the night in the alarm service
            // to make sure that potential changes in the propagation windows are reflected in the alarm clients;
            // this will cause a reload of the current night and its targets and windows
            alarmService.updateNight();
        }
    }

    /**
     * Handles all incoming emails.
     * If there are PAM files attached those are processed, if it is a notification email from JSPoC the latest
     * PAM files are downloaded from space-track.org and processed. Usually we will receive notifications and
     * have to download the PAM files, processing attached PAM files is a useful "backdoor" in order to manually
     * force PAM files to be processed and for testing etc.
     * At the end emails are either moved to the 'processed' folder on success or to the 'failed' folder
     * if the email could not be processed properly.
     * @param emailReader
     * @param message
     * @throws javax.mail.MessagingException
     */
    private Boolean handleNewEmail(EmailReader emailReader, Message message) throws MessagingException, IOException {

        Set<BodyPart> attachedPams = emailReader.getPamFiles(message);
        boolean sentFromSpaceTrack = sentFromSpaceTrack(message);

        // do we have  any pam files or is this a notification from space track?
        if (attachedPams.size() == 0 && !sentFromSpaceTrack) {

            // if not, we're done!
            emailReader.moveToProcessed(message);
            LOGGER.info("Successfully digested email '" + message.getSubject() + "', no PAM files processed.");
            return false;

        } else {

            // if yes, process those PAMs!
            StringBuffer protocol = new StringBuffer();
            Map<Long, List<EventFile>> successfulPams = new HashMap<>();

            // -- process email attachments (if any)
            for (BodyPart part : attachedPams) {
                byte[] content = FileCopyUtils.copyToByteArray(part.getInputStream());
                PamFile pamFile = new PamFile(0, 0, part.getFileName(), content);
                processPam(successfulPams, protocol, pamFile);
            }

            // -- process new PAMs from space-track (if any)
            List<PamFile> downloadedPams;
            if (sentFromSpaceTrack) {
                downloadedPams = downloadPamsFromSpaceTrack(successfulPams, protocol);
            } else {
                downloadedPams = new ArrayList<>();
            }

            // -- book keeping..
            int totalPams = attachedPams.size() + downloadedPams.size();
            return processResult(emailReader, message, totalPams, successfulPams, protocol);
        }
    }

    private List<PamFile> downloadPamsFromSpaceTrack(Map<Long, List<EventFile>> successfulPams, StringBuffer protocol) {
        List<PamFile> downloadedPams;
        int curMaxId = configurationService.getInteger(Configuration.Value.SPACE_TRACK_MAX_FILE_ID);
        try {
            jspocService.connect(siteService.getSite());
            downloadedPams = jspocService.downloadNewPAMFilesAsJava(siteService.getSite(), curMaxId);
        } catch (Exception e) {
            LOGGER.error("An error occurred while trying to download files from space-track.org", e);
            downloadedPams = new ArrayList<>();
        }

        int maxId = 0;
        for (PamFile pamFile: downloadedPams) {
            processPam(successfulPams, protocol, pamFile);
            maxId = pamFile.fileId() > maxId ? pamFile.fileId() : maxId;
        }

        // set new maximal id if needed                                                 // TODO: is this a problem with concurrency ???
        if (maxId > curMaxId) {
            Configuration configuration = configurationService.getConfigurationEntry(Configuration.Value.SPACE_TRACK_MAX_FILE_ID);
            configurationService.update(configuration, Integer.toString(maxId));        // TODO: simplify this with additional method on configService
        }

        return downloadedPams;
    }

    /**
     * True if this email was sent from space-track.org, if so we assume it is a notification to let us
     * know there are new PAM files available.
     * @param message
     * @return
     * @throws MessagingException
     */
    private Boolean sentFromSpaceTrack(Message message) throws MessagingException {
        if (message.getFrom() == null) return false;
        if (message.getFrom().length == 0) return false;
        String trigger = configurationService.getString(Configuration.Value.SPACE_TRACK_EMAIL);
        return message.getFrom()[0].toString().contains(trigger);
    }

    /**
     * Evaluates the overall result for the given email and does some book keeping.
     * @param emailReader
     * @param message
     * @param totalPams
     * @param successfulPams
     * @param protocol
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private Boolean processResult(EmailReader emailReader, Message message, int totalPams, Map<Long, List<EventFile>> successfulPams, StringBuffer protocol)  throws MessagingException, IOException {
        // get the total count of PAMs that were successfully processed
        Integer successful = 0;
        for (List<EventFile> processedPams : successfulPams.values()) {
            successful += processedPams.size();
        }

        // add events to the nights the PAM files belong to
        for (Long nightId : successfulPams.keySet()) {
            List<EventFile> files = successfulPams.get(nightId);
            files.add(emailReader.getEmailAsEventFile(message));
            String msg = String.format("Email with %d PAM files digested.", totalPams);
            laserNightService.createEvent(nightId, msg, files);
        }

        // finally depending on outcome of everything log and email success notifications and warnings
        if (successful == totalPams) {
            // send success email if all files were successfully imported
            emailReader.moveToProcessed(message);
            String msg = "Successfully digested email '" + message.getSubject() + "'";
            LOGGER.info(msg);

            sendInternalEmail(
                    "LTTS: Successfully digested email '" + message.getSubject() +"'",
                    "Status of imported files:\n\n" + protocol.toString() + "\n" + msg);
        } else {
            // one or more files had a problem, send a warning message
            emailReader.moveToFailed(message);
            String msg = "Digested email '" + message.getSubject() + "' with errors";
            LOGGER.info(msg);

            sendInternalEmail(
                    "LTTS: WARNING: Digested email '" + message.getSubject() + "' with errors.",
                    "Status of imported files:\n\n" + protocol.toString() + "\n" + msg);

        }

        return successful > 0;
    }


    /**
     * Processes a PAM file and keeps track of the result in the successfulPams data structure.
     * Also adds some information to the protocol string which is sent to the users in the end.
     * @param successfulPams
     * @param protocol
     * @param pamFile
     */
    private void processPam(Map<Long, List<EventFile>> successfulPams, StringBuffer protocol, PamFile pamFile) {

        try {
            // handle response, this will finally actually do something on the database...
            // handlePamFile is also the transaction boundary, i.e. there is one transaction
            // per PAM file
            Long nightId = laserNightService.handlePamFile(pamFile);

            // if we get here, all is good!
            // keep track of successfully imported pams and the nights they belong to
            // (in theory an email can contain files for different nights...)
            String msg = "Successfully imported " + pamFile.name();
            LOGGER.info(msg);
            protocol.append(msg + "\n");
            if (!successfulPams.containsKey(nightId)) {
                successfulPams.put(nightId, new ArrayList<>());
            }
            List<EventFile> pamsForNight = successfulPams.get(nightId);
            pamsForNight.add(getPamAsEventFile(pamFile));
        } catch (Exception e) {
            // catch exception, log message and continue with next parts
            // since we send this information to the users we only send the first line of the stack trace instead
            // of the whole stack trace, this should in general give enough information about the problem
            String msg = "Failure when importing " + pamFile.name();
            LOGGER.warn(msg, e);
            protocol.
                    append("\n").append(msg).append("\n").
                    append(getErrorDetails(e)).append("\n");
        }

    }

    /**
     * Creates an event file that will represent a processed PAM file.
     * @param pamFile
     * @return
     * @throws IOException
     */
    private EventFile getPamAsEventFile(PamFile pamFile) throws IOException {
        // create the event file for this message part and add it to collection of processed event files
        StringWriter writer = new StringWriter();
        InputStream is = new ByteArrayInputStream(pamFile.content());
        FileCopyUtils.copy(new InputStreamReader(is), writer);
        return new EventFile(pamFile.folderId(), pamFile.fileId(), EventFile.Type.PAM, pamFile.name(), writer.toString());
    }

    /**
     * Gets a string that describes the exception.
     * @param e
     * @return
     */
    private String getErrorDetails(Exception e) {
        return new StringBuffer().
                append("Reason: ").append(e.getMessage()).append("\n").
                append("  [ ==> ").append(e.toString()).append(" ]\n").
                append("  [ ==> ").append(e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "<no stack trace> ").append(" ]\n").
                toString();
    }


}
