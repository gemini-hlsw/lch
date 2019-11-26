package edu.gemini.lch.web.app.windows.alarm;

import com.vaadin.server.StreamResource;
import com.vaadin.ui.*;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.AlarmService;
import edu.gemini.lch.services.LaserTargetsService;
import edu.gemini.lch.web.app.components.TimeZoneSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Configurable(preConstruction = true)
public final class Timeline extends HorizontalLayout implements TimeZoneSelector.Listener, Button.ClickListener{

    // define how many minutes before and after "now" should be shown by default (relative to zoom factor 1)
    // changing the relation also changes the relation of time shown before and after the now line (yellow marker)
    private static final Integer SECONDS_BEFORE = 15*60; // show 15 minutes before now on zoom level 1
    private static final Integer SECONDS_AFTER = 105*60; // show 105 minutes after now on zoom level 1
    // define how much we zoom in and out with every click on the +/- buttons
    private static final Double ZOOM_FACTOR = 1.5;

    @Autowired private LaserTargetsService targetsService;

    private ZonedDateTime lastUpdate;
    private ZoneId zoneId;
    private final Embedded embedded;
    private Button zoomIn;
    private Button zoomOut;
    private Button zoomReset;
    private Button zoomNight;
    private Double zoomLevel = 1.;

    /**
     * Constructs a timeline object.
     */
    Timeline() {
        lastUpdate = ZonedDateTime.ofInstant(Instant.MIN, ZoneId.systemDefault());
        zoneId = ZoneId.of("UTC");
        embedded = new Embedded();
        addComponent(embedded);
        addComponent(createButtons());
        setExpandRatio(embedded, 1.0f);
        setSizeFull();
    }

    /**
     * Creates the buttons of the timeline object.
     * @return
     */
    private ComponentContainer createButtons() {
        VerticalLayout layout = new VerticalLayout();
        zoomIn = new Button("Zoom In +", this);
        zoomOut = new Button("Zoom Out -", this);
        zoomReset = new Button("Zoom 2 Hrs", this);
        zoomNight = new Button("Zoom Night", this);
        zoomIn.setWidth("100px");
        zoomOut.setWidth("100px");
        zoomReset.setWidth("100px");
        zoomNight.setWidth("100px");
        layout.addComponent(zoomIn);
        layout.addComponent(zoomOut);
        layout.addComponent(zoomReset);
        layout.addComponent(zoomNight);
        layout.setSizeUndefined();
        return layout;
    }

    /**
     * Updates the image that is displayed in the timeline object.
     */
    void update(final Integer browserWidth, final AlarmService.Snapshot snapshot) {

        // don't update time line components more often than every two seconds
        Duration sinceLastUpdate = Duration.between(lastUpdate, ZonedDateTime.now());
        if (sinceLastUpdate.getSeconds() < 2) {
            return;
        }

        // calculate image width and height depending on current browser width (don't go below a given threshold)
        final int width = Math.max(300, browserWidth) - 170;
        final int height = 105;

        // create the image
        final byte[] image;
        if (snapshot.getNight() != null) {

            final LaserNight night = snapshot.getNight();
            final ZonedDateTime now = snapshot.getEpicsSnapshot().getTime();

            // limit zoom level to a reasonable value (max = display whole night, min = zoom in 7 times)
            zoomLevel = Math.min(zoomLevel, getMaxZoomLevel(night, now));
            zoomLevel = Math.max(zoomLevel, 1 / (Math.pow(ZOOM_FACTOR, 7.)));

            // calculate start and end times for current zoom level
            ZonedDateTime start = ZonedDateTime.now().minusSeconds((int) (SECONDS_BEFORE * zoomLevel));
            ZonedDateTime end   = ZonedDateTime.now().plusSeconds((int) (SECONDS_AFTER * zoomLevel));

            // don't show more than one hour before start and after end of night (earliest, latest)
            final ZonedDateTime earliest = getEarliest(night);
            final ZonedDateTime latest = getLatest(night);
            if (start.isBefore(earliest)) {
                start = earliest;
            }
            if (end.isAfter(latest)) {
                end = latest;
            }

            // there we go, all the numbers are calculated, draw the image
            if (snapshot.getTarget() == null) {
                image = targetsService.getImage(snapshot.getNight(), width, height, start, end, now, zoneId);
            } else {
                image = targetsService.getImage(snapshot.getNight(), snapshot.getTarget(), width, height, start, end, now, zoneId);
            }

        } else {
            image = createEmptyImage(width, height);
        }

        final StreamResource imgSource =
            new StreamResource(() -> new ByteArrayInputStream(image), "timeline-" + UUID.randomUUID() + ".png") {{
                setCacheTime(0); // don't cache!
            }};
        embedded.setSource(imgSource);
        embedded.setWidth(width, Unit.PIXELS);
        embedded.setHeight(height, Unit.PIXELS);

        // reset last update time
        lastUpdate = ZonedDateTime.now();

    }

    private byte[] createEmptyImage(int width, int height) {
        BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
        synchronized (ImageIO.class) {
            try {
                ImageIO.write(image, "png", imagebuffer);
            } catch (IOException e) {
                // intentionally blank
            }
        }
        return imagebuffer.toByteArray();
    }

    public void buttonClick(Button.ClickEvent event) {
        if      (event.getButton() == zoomIn)   zoomLevel /= ZOOM_FACTOR;
        else if (event.getButton() == zoomOut)  zoomLevel *= ZOOM_FACTOR;
        else if (event.getButton() == zoomReset) zoomLevel = 1.;
        else if (event.getButton() == zoomNight) {
            // setting to max value here, update() will set zoom level to maximum zoom level that makes sense
            zoomLevel = Double.MAX_VALUE;
        }
        lastUpdate = ZonedDateTime.ofInstant(Instant.MIN, ZoneId.systemDefault());
    }

    /**
     * Updates the time zone for displaying times.
     */
    @Override public void updateZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * Calculates the earliest time that should be displayed in the image.
     * Used to provide an upper limit for the zoom level.
     */
    private ZonedDateTime getEarliest(LaserNight night) {
        return night.getStart().minusHours(1);
    }

    /**
     * Calculates the latest time that should be displayed in the image.
     * Used to provide an upper limit for the zoom level.
     */
    private ZonedDateTime getLatest(LaserNight night) {
        return night.getEnd().plusHours(1);
    }

    /**
     * Calculates the maximal zoom level that displays the whole night from earliest til latest.
     */
    private Double getMaxZoomLevel(LaserNight night, ZonedDateTime now) {
        Double zoom = 1.;
        ZonedDateTime start = now;
        ZonedDateTime end   = now;
        while (start.isAfter(getEarliest(night)) || end.isBefore(getLatest(night))) {
            zoom *= ZOOM_FACTOR;
            start = ZonedDateTime.now().minusSeconds((int) (SECONDS_BEFORE * zoom));
            end   = ZonedDateTime.now().plusSeconds((int) (SECONDS_AFTER * zoom));
        }
        return zoom;
    }

}
