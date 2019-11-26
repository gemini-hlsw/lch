package edu.gemini.lch.services.impl;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.EpicsService;
import edu.gemini.lch.services.LtcsService;
import edu.gemini.lch.services.SiteService;
import org.apache.commons.lang.Validate;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for accessing the LTCS web application to get information about imminent collisions with the
 * laser beams of other observatories on MKO or CP.
 * In order to keep the load to a minimum the service updates the current collisions every few seconds and
 * stores the current state. Clients accessing the status will not cause calls to the LTCS server every time
 * but will get the most recent state.
 */
@Service
public class LtcsServiceImpl implements LtcsService {

    private static final Logger LOGGER = Logger.getLogger(LtcsServiceImpl.class);

    // LTCS service gives us a time stamp in local time for the next 24hrs
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(("HH:mm:ss");

    private static final String PROCESSES_DOWN = "PROCESSES DOWN";
    private static final String BAD_REQUEST = "WRONG INPUT";
    private static final String LASER_ON_SKY = "ON-SKY";
    private static final String LASER_OFF = "OFF";

    private static final String ERROR_LTCS_PROCESSES_DOWN = "LTCS processes are down.";
    private static final String ERROR_WRONG_INPUT = "Request is invalid.";

    @Resource
    private SiteService siteService;

    @Resource
    private EpicsService epicsService;

    @Resource
    private ConfigurationService configurationService;

    private DefaultHttpClient httpClient;
    private Snapshot currentStatus;

    @PostConstruct
    private void init() {
        // create a http client which will recycle open connections
        httpClient = new DefaultHttpClient();
        currentStatus = new Snapshot(Collections.emptyList());
    }

    @PreDestroy
    private void destroy() {
        // destroy the connection manager
        httpClient.getConnectionManager().shutdown();
    }

    @Override
    public Snapshot getSnapshot() {
        return currentStatus;
    }

    /**
     * Updates LTCS status every few seconds asynchronously.
     */
    @Scheduled(fixedDelay = 2000)
    public void update() {

        try {

            String url = configurationService.getString(Configuration.Value.LTCS_URL);
            Double ra = epicsService.getDemandRaDec().getRaDeg();
            Double dec = epicsService.getDemandRaDec().getDecDeg();
            String laser = epicsService.isOnSky() ? LASER_ON_SKY : LASER_OFF;
            URI uri = createURI(url, ra, dec, laser);

            LOGGER.trace("LTCS query       : " + uri);

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpGet get = new HttpGet(uri);
            String response = httpClient.execute(get, responseHandler);

            LOGGER.trace("LTCS replied with: " + response);

            // response was 2xx, let's assume it was 200 which is probably true for 99%
            // all is well, parse the result and return it
            parseResponse(response);

        } catch (ConnectException e) {
            // problem with connection, reset connection manager
            resetHttpClient();
            currentStatus = new Snapshot(Error.NOT_CONNECTED, e.getMessage());
            LOGGER.error("Could not connect to LTCS server", e);

        } catch (HttpResponseException e) {
            // status of HTTP response was >= 300
            currentStatus = new Snapshot(Error.OTHER, e.getMessage());
            LOGGER.error("LTCS server replied with status code " + e.getStatusCode(), e);

        } catch (Exception e) {
            // something else went wrong, reset connection manager just in case
            resetHttpClient();
            currentStatus = new Snapshot(Error.OTHER, e.getMessage());
            LOGGER.error("Could not update LTCS state", e);
        }

    }

    private void resetHttpClient() {
        httpClient.getConnectionManager().shutdown();
        httpClient = new DefaultHttpClient();
    }

    /**
     * Creates an URI with all the necessary parameters.
     */
    private URI createURI(String url, Double raDegrees, Double decDegrees, String laserState) throws URISyntaxException {
        return new URIBuilder(url+"/ltcs/screens/query.php")
                .setParameter("telescope",      "GEMINI")
                .setParameter("ra",             String.format("%.12f", raDegrees/15.0)) // ra in hours not degrees
                .setParameter("dec",            String.format("%.12f", decDegrees))     // dec in degrees
                .setParameter("equinox",        "2000")
                .setParameter("fov",            "0.17") // FOV value defined by Dolores Coulson, ask her for details
                .setParameter("laser_state",    laserState)
                .addParameter("no_dither",      "true") // use current values as opposed to pre-calculated ones(?)
                .build();
    }

    /**
     * Parses the LTCS server response.
     */
    protected void parseResponse(String response) {
        // LTCS service replies with error states in response without sending appropriate HTTP codes (!= 200),
        // therefore we need to handle these cases ("LTCS processes down" and "bad request" here separately..).
        if (response.contains(PROCESSES_DOWN)) {
            LOGGER.trace(ERROR_LTCS_PROCESSES_DOWN);
            currentStatus = new Snapshot(Error.PROCESSES_DOWN, ERROR_LTCS_PROCESSES_DOWN);

        } else if (response.contains(BAD_REQUEST)) {
            LOGGER.error("LTCS replied with bad request, check url format.");
            currentStatus = new Snapshot(Error.BAD_REQUEST, ERROR_WRONG_INPUT);

        // OK, it seems we indeed received a valid result, try to parse it
        } else {
            // go 2 minutes back so that if we get a collision window that just ended (few seconds ago) we don't
            // move it into the future but assume it was in the past and is not important anymore (if this is not
            // the case LTCS will keep sending it and we will get it 2 minutes later..)
            // NOTE: USE SITE LOCAL TIME NOT UTC! TIMES RETURNED BY LTCS ARE LOCAL TIMES!
            ZonedDateTime now = epicsService.getTime().minusMinutes(2).withZoneSameInstant(ZoneId.systemDefault());
            currentStatus = new Snapshot(parseCollisions(response, now));
            LOGGER.trace("LTCS status successfully updated, found " + currentStatus.collisions.size() + " collisions");
        }
    }

    /**
     * Parses and returns an unmodifiable list with the collisions from the string response from LTCS.
     */
    // NOTE: static so it can be used by the alternative implementation..
    static List<LtcsService.Collision> parseCollisions(String response, ZonedDateTime now) {

        // NOTE: responses look as follows
        // a) "GEMINI SUBARU 6:30:23 6:45:15 NO-LGS"
        //    where 0=GEMINI, 1 =<telescope>, 2=<start>, 3=<end>, 4=<priority>
        //    we are not interested in lines with "NO-LGS" as priority
        // b) "GEMINI SUBARU 6:30:23 6:45:15 SUBARU"
        //    yes, this is a collision we need to care about, SUBARU wins
        // c) "GEMINI KECK2 5:00:12 5:39:06 NO-LGS GEMINI SUBARU 4:46:08 5:06:14 NO-LGS"
        //    oh, two collisions on the same line, who said it is foul to use line breaks in cases like this??
        //    well, I guess a space does it, too


        if (response.trim().isEmpty() || response.contains("NONE")) {
            return Collections.emptyList();
        }

        List<LtcsService.Collision> collisions = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(response);
        while (tokenizer.hasMoreElements()) {

            // skip GEMINI
            tokenizer.nextToken();
            // get telescope and priority
            String telescope = tokenizer.nextToken();
            // get times from timestamps; use same "NOW" for both timestamps
            ZonedDateTime start   = toDateTime(tokenizer.nextToken(), now);
            ZonedDateTime end     = toDateTime(tokenizer.nextToken(), now);
            // get priority
            String priority  = tokenizer.nextToken();

            // e.g. (start=23:55:00,end=00:05:00) can not be on the same day, move start one day back
            if (start.isAfter(end)) {
                start = start.minusDays(1);
            }
            // assuming we only get future collisions if we get for example (start=00:01:30,end=00:05:30) and it
            // is 18:00:00 then these timestamps refer to the next day (tomorrow)
            if (end.isBefore(now)) {
                start = start.plusDays(1);
                end   = end.plusDays(1);
            }
            // obviously it must start before it ends...
            Validate.isTrue(start.isBefore(end));

            // only take collisions with priority != "NO-LGS"
            if (!"NO-LGS".equalsIgnoreCase(priority)) {
                // ok, seems we have our collision!
                Collision c = new Collision(telescope, priority, start, end);
                collisions.add(c);
            }

        }

        // sort relevant collisions by their start time and return unmodifiable list
        Collections.sort(collisions);
        return Collections.unmodifiableList(collisions);
    }

    private static ZonedDateTime toDateTime(String timestamp, ZonedDateTime today) {
        return timeFormatter.parse(timestamp, ZonedDateTime::from)
            .withYear(today.getYear())
            .withMonth(today.getMonthValue())
            .withDayOfMonth(today.getDayOfMonth());
    }

    /**
     * An immutable and consistent snapshot of all relevant data for the LTCS service.
     * A new instance of this snapshot object is created every few hundred milliseconds and can then be safely
     * passed around to clients interested in it without the danger of concurrent changes making parts of the data
     * inconsistent.
     */
    private static class Snapshot implements LtcsService.Snapshot {
        private final Error error;
        private final String message;
        private final List<LtcsService.Collision> collisions;
        Snapshot(Error error, String message) {
            this.error = error;
            this.message = message;
            this.collisions = Collections.emptyList();
        }
        Snapshot(List<LtcsService.Collision> collisions) {
            this.error = Error.NONE;
            this.message = "OK";
            this.collisions = collisions;
        }
        @Override
        public Boolean isConnected() { return error == Error.NONE; }
        @Override
        public Error getError() { return error; }
        @Override
        public String getMessage() { return message; }
        @Override
        public List<LtcsService.Collision> getCollisions() { return collisions; }
    }

    // simple implementation for an ltcs collision
    private static class Collision implements LtcsService.Collision {
        private final String observatory;
        private final String priority;
        private final ZonedDateTime start;
        private final ZonedDateTime end;
        Collision(String observatory, String priority, ZonedDateTime start, ZonedDateTime end) {
            this.observatory = observatory;
            this.priority = priority;
            this.start = start;
            this.end = end;
        }
        @Override
        public String getObservatory() { return observatory; }
        @Override
        public String getPriority() { return priority; }
        @Override
        public ZonedDateTime getStart() { return start; }
        @Override
        public ZonedDateTime getEnd() { return end; }
        @Override
        public int compareTo(LtcsService.Collision other) { return this.getStart().compareTo(other.getStart()); }
        @Override
        public Boolean geminiHasPriority() { return priority.equalsIgnoreCase("GEMINI"); }
        @Override
        public Boolean contains(ZonedDateTime time) {
            return (!getStart().isAfter(time) && getEnd().isAfter(time));
        }


    }
}
