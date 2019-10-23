package edu.gemini.lch.services.util;

import edu.gemini.lch.model.EventFile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.*;

/**
 * Helper class to access an imap server, get emails from it and collect attachments of interest (PAM files)
 * that have been sent in the email.
 * Emails that can not be read for whatever reason should be marked as seen, they will stay in the inbox but
 * will not be returned by getNextMessage anymore. This allows to process as many emails as possible while
 * emails causing problems can stay in the inbox for manual inspection.
 */
@Component
@Scope("prototype")
public class EmailReader {

    private static final Logger LOGGER = Logger.getLogger(EmailReader.class);

    private static final String IN_FOLDER = "INBOX";
    private static final String FAILED_FOLDER = "LTTS/failed";
    private static final String PROCESSED_FOLDER = "LTTS/processed";

    @Value("${lch.email.host}")
    private String server;

    private Store store;
    private Folder folder;
    private Folder failedFolder;
    private Folder processedFolder;
    private Set<Message> messages;

    /**
     * Connects to a mail server using imap as the protocol.
     * @param user
     * @param password
     * @throws MessagingException
     */
    public void connect(final String user, final String password) throws MessagingException {
        // IMPORTANT: Default timeouts are infinite! This can cause the email thread to get blocked for ever which
        // will keep LTTS from digesting incoming emails until the next server restart! Set a timeout to avoid this.
        final Properties props = new Properties();
        props.setProperty("mail.imap.connectiontimeout", "30000");
        props.setProperty("mail.imap.timeout", "30000");
        props.setProperty("mail.debug", "true");
        // -- ok, now we should be good to go..

        // get an imap session
        final Session mailSession = Session.getInstance(props, null);
        mailSession.setDebug(true);
        store = mailSession.getStore("imaps");
        store.connect(server, user, password);
        folder = store.getFolder(IN_FOLDER);
        folder.open(Folder.READ_WRITE);
        failedFolder = store.getFolder(FAILED_FOLDER);
        failedFolder.open(Folder.READ_WRITE);
        processedFolder = store.getFolder(PROCESSED_FOLDER);
        processedFolder.open(Folder.READ_WRITE);
        // this are the currently pending messages
        messages = new HashSet<>(Arrays.asList(folder.getMessages()));
    }

    /**
     * Gets the next message (according to sent timestamp) from the current pending batch of messages.
     * @return
     * @throws MessagingException
     */
    public Message getNextMessage() throws MessagingException {
        Message nextMessage = null;
        for (final Message message : messages) {
            if (nextMessage == null || message.getSentDate().before(nextMessage.getSentDate())) {
                nextMessage = message;
            }
        }
        if (nextMessage != null) {
            messages.remove(nextMessage);
        }
        return nextMessage;
    }

    /**
     * Gets all attachments of interest from the given message (LCH PRM files).
     * @param message
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    public Set<BodyPart> getPamFiles(final Message message) throws MessagingException, IOException {
        final Set<BodyPart> attachments = new HashSet<>();
        // need to copy message to mark it as read in the inbox, don't ask me why
        final Message messageCopy = new MimeMessage((MimeMessage)message);
        if(messageCopy.getContent() instanceof Multipart) {
            collectPamFiles(attachments, (Multipart) messageCopy.getContent());
        }
        return attachments;
    }

    public void moveToProcessed(final Message message) throws MessagingException {
        moveTo(message, processedFolder);
    }

    public void moveToFailed(final Message message) throws MessagingException {
        moveTo(message, failedFolder);
    }

    private void moveTo(final Message message, final Folder targetFolder) throws MessagingException {
        final Message[] messages = new Message[] {message};
        folder.copyMessages(messages, targetFolder);
        folder.setFlags(messages, new Flags(Flags.Flag.DELETED), true);
    }

    /**
     * Disconnects from the mail server and expunges deleted emails.
     * @throws MessagingException
     */
    public void disconnect() throws MessagingException {
        try {
            if (processedFolder != null) processedFolder.close(true);
            if (failedFolder != null) failedFolder.close(true);
            if (folder != null) folder.close(true);
        } catch (MessagingException e) {
            // ignore exceptions when closing folders
            LOGGER.error("could not close imap folders", e);
        }
        // try to make sure that store is closed so that a reconnect will work
        // there was at least one occasion where we were not able to reconnect to the mail server again
        // hopefully this helps to make reconnects more reliable
        if (store != null) {
            store.close();
        }
        // make sure these objects are not accidentally used after close()
        processedFolder = null;
        failedFolder = null;
        folder = null;
        store = null;
    }

    public EventFile getEmailAsEventFile(final Message message) throws MessagingException, IOException {
        if(message.getContent() instanceof Multipart) {
            return getBodyPart((Multipart) message.getContent(), message.getSubject());
        } else {
            return new EventFile(EventFile.Type.EMAIL, message.getSubject(), message.getContent().toString());
        }
    }

    private EventFile getBodyPart(final Multipart multipart, final String subject) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            final BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.getContent() instanceof Multipart) {
                return getBodyPart((Multipart) bodyPart.getContent(), subject);
            } else {
                // not interested in attachments
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    continue;
                }
                final String contentType = bodyPart.getContentType();
                if (contentType.toLowerCase().contains("text/plain")) {
                    return new EventFile(EventFile.Type.EMAIL, subject, bodyPart.getContent().toString());
                }
                if (contentType.toLowerCase().contains("text/html")) {
                    return new EventFile(EventFile.Type.EMAIL_HTML, subject, bodyPart.getContent().toString());
                }
            }
        }

        return new EventFile(EventFile.Type.EMAIL, subject, "<<<Email did not have a text/plain or text/html part.>>>");
    }

    private void collectPamFiles(final Set<BodyPart> attachments, final Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            final BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.getContentType().toLowerCase().startsWith("multipart")) {
                collectPamFiles(attachments, (Multipart) bodyPart.getContent());
            } else {
                // we are only interested in attachments
                final String disposition = bodyPart.getDisposition();
                if (disposition == null || !bodyPart.getDisposition().equalsIgnoreCase(Part.ATTACHMENT)) {
                    continue;
                }
                // we are only interested in plain text attachments
                if (!bodyPart.getContentType().toLowerCase().contains("text/plain")) {
                    continue;
                }
                // and we only want to look at files with a name starting with "PAM" (sometimes
                // the input "PRM" files generated by Gemini are attached to the replies, too)
                // NOTE: this check is risky in case someone changes the naming schema of the files
                // if that happens we maybe want to replace this with a check for some string that
                // we expect to be in the actual file or we just parse the file and see if it matches...
                if (!bodyPart.getFileName().startsWith("PAM")) {
                    continue;
                }

                // ok, this seems to be a prm file attached to the email, now parse it
                attachments.add(bodyPart);
            }
        }
    }

}
