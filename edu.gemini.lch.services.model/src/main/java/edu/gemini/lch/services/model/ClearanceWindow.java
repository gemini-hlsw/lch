package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 */
@XmlRootElement
public class ClearanceWindow extends Window {

    public ClearanceWindow(Date start, Date end) {
        super(start, end);
    }

    // needed for JAXB
    public ClearanceWindow() {
    }

    // silly wrapper object needed to send lists of observations to client
    @XmlRootElement(name = "clearanceWindows")
    public static class List {
        @XmlElement(name = "clearanceWindow")
        public Collection<Window> clearanceWindows;
        public List(Collection<Window> clearanceWindows) {
            this.clearanceWindows = clearanceWindows;
        }
        public List() {
            clearanceWindows = new ArrayList<>();
        }
    }
}
