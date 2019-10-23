package edu.gemini.lch.configuration;

/**
 */
public interface Selection {

    enum Twilight implements Selection {
        SUNRISE("Sunset/Sunrise"),
        CIVIL("Civil Tiwlight (6°)"),
        NAUTICAL("Nautical Twilight (12°)"),
        ASTRONOMICAL("Astronomical Twilight (18°)");

        private final String label;
        Twilight(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    String getLabel();

}
