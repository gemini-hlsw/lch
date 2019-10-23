package edu.gemini.lch.web.app.windows.night;

import org.apache.commons.lang.StringUtils;

/**
 * Helper class for custom ordering of observation id column in the target tables.
 */
public class ObservationId implements Comparable<ObservationId> {
    private final String id;

    public ObservationId(String id) {
        this.id = id;
    }

    @Override
    public int compareTo(ObservationId observationId) {
        String[] myParts = this.id.split("-");
        String[] otherParts = observationId.id.split("-");
        for (Integer i = 0; i < myParts.length && i < otherParts.length; i++) {
            if (!myParts[i].equals(otherParts[i])) {
                if (StringUtils.isNumeric(myParts[i]) && StringUtils.isNumeric(otherParts[i])) {
                    // if both parts are numeric compare them as integers
                    Integer myInt = Integer.parseInt(myParts[i]);
                    Integer otherInt = Integer.parseInt(otherParts[i]);
                    return myInt.compareTo(otherInt);
                } else {
                    // if they are not both numeric compare them as strings
                    return myParts[i].compareTo(otherParts[i]);
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof ObservationId)) return false;
        if (!((ObservationId) o).id.equals(id)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
