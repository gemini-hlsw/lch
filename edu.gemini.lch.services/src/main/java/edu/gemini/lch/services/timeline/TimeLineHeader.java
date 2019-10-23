package edu.gemini.lch.services.timeline;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 */
public class TimeLineHeader {

    private static final Logger LOGGER = Logger.getLogger(TimeLineHeader.class.getName());

    private final int SPACE = 2;

    private final byte[] imageBytes;

    public TimeLineHeader(DateTime start, DateTime end, Integer width, Integer height, DateTimeZone zone) {
        this.imageBytes = createImage(start, end, width, height, zone);
    }

    public byte[] getAsBytes () {
        return imageBytes;
    }

    public InputStream getAsStream () {
        return new ByteArrayInputStream(imageBytes);
    }

    private byte[] createImage(DateTime start, DateTime end, Integer width, Integer height, DateTimeZone zone) {

        BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
        Graphics drawable = image.getGraphics();

        drawable.setColor(new Color(218, 220, 222));
        drawable.fillRect(0, 0, width, height);

        try {

            int hours = (int) new Duration(start, end).getStandardHours();
            double curX = SPACE;
            double hoursX = (width - (hours*SPACE)) / hours;
            DateTime time = start;
            while (time.isBefore(end)) {
                String imageName = "timeline-" + time.toDateTime(zone).toString("HHmm") + ".png";
                InputStream stream = getClass().getResourceAsStream(imageName);
                BufferedImage headerImage = ImageIO.read(stream);
                drawable.drawImage(headerImage, (int) (curX + ((hoursX-headerImage.getWidth())/2)), 5, null);
                drawable.setColor(Color.WHITE);
                drawable.fillRect((int)(curX + hoursX), 0, 2, height);
                curX += hoursX + SPACE;
                time = time.plusHours(1);
            }

            // NOTE: ImageIO is *not* thread safe!
            synchronized (ImageIO.class) {
                /* Write the image to a buffer. */
                ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imagebuffer);
                image.flush();
                imagebuffer.close();
                return imagebuffer.toByteArray();
            }


        } catch (IOException e) {
            throw new RuntimeException("could not create image", e);
        }
    }
}
