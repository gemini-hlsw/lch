package edu.gemini.lch.web.app.util;

/**
 * A pretty printer for coordinates.
 */
public final class CoordFormatter {

    public static String asDegrees(double degrees) {
        return String.format("%8.3f\u00B0", degrees);
    }

    public static String asHMS(double degrees) {
        Boolean isNegative = degrees < 0;
        if (isNegative) degrees = -degrees;
        Double hours = degrees / 15.0;
        Double mins  = (hours - Math.floor(hours)) * 60.0;
        Double secs  = (mins - Math.floor(mins)) * 60.0;
        String nice = String.format("%s%s", isNegative ? "-" : "", asPosition(hours, mins, secs));
        return nice;
    }

    public static String asDMS(double degrees) {
        Boolean isNegative = degrees < 0;
        if (isNegative) degrees = -degrees;
        Double arcMins = (degrees - Math.floor(degrees)) * 60.0;
        Double arcSecs = (arcMins - Math.floor(arcMins)) * 60.0;
        String nice = String.format("%s%s", isNegative ? "-" : "+", asPosition(degrees, arcMins, arcSecs));
        return nice;
    }

    /**
     * Pretty prints a HMS or DMS value represented as a "main" part (hours/degrees) and minutes and seconds.
     * Note that rounding up must carry over to minute and hour values to avoid results like "15:60:00.00" when
     * printing "15:59:59.999" or similar.
     * @param main
     * @param mins
     * @param secs
     * @return
     */
    private static String asPosition(double main, double mins, double secs) {
        if (secs < 0.0) {
            // ignore small negative values due to rounding errors
            secs = 0.0;
        }
        if (secs >= 59.995) {
            // if secs value is close enough to one minute round it up
            secs = 0.0;
            mins += 1.0;
        }
        if (mins >= 60.0) {
            // if mins value is close enough to one hour/degree round it up
            secs = 0.0;
            mins = 0.0;
            main += 1.0;
        }

        return String.format("%02d:%02d:%05.2f", (int) main, (int) mins, secs);
    }

}
