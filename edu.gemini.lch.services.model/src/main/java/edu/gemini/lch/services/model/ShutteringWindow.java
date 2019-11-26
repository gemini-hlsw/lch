package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement
public class ShutteringWindow extends Window {

    private boolean isBlanketClosure;

    public ShutteringWindow(Instant start, Instant end) {
        this(start, end, false);
    }

    public ShutteringWindow(Instant start, Instant end, boolean isBlanketClosure) {
        super(start, end);
        this.isBlanketClosure = isBlanketClosure;
    }

    @XmlElement
    public boolean getBlanketClosure() {
        return isBlanketClosure;
    }

    // needed for JAXB
    public ShutteringWindow() {
    }

    // silly wrapper object needed to send lists of observations to client
    @XmlRootElement(name = "shutteringWindows")
    public static class List {
        @XmlElement(name = "shutteringWindow")
        public Collection<Window> shutteringWindows;
        public List(Collection<Window> shutteringWindows) {
            this.shutteringWindows = shutteringWindows;
        }
        public List() {
            shutteringWindows = new ArrayList<>();
        }
    }
}
