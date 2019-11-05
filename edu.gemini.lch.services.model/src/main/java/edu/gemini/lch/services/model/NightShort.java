package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection

/**
 */
@XmlRootElement(name = "night")
public class NightShort extends Night {

    public NightShort(Long id, String site, Instant start, Instant end, Instant latestPrmSent, Instant latestPamReceived) {
        super(id, site, start, end, latestPrmSent, latestPamReceived);
    }
    // empty constructor needed for JAXB
    public NightShort() {
    }

    @XmlRootElement(name = "nights")
    public static class List {
        @XmlElement(name = "night")
        private Collection<NightShort> nights;
        public List(Collection<NightShort> nights) {
            this.nights = nights;
        }
        // needed for JAXB
        public List() {
            nights = new ArrayList<>();
        }
    }

}
