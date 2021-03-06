package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * JAXB annotated value object for sending propagation windows to client.
 */
@XmlRootElement
@XmlType(propOrder = {"start", "end"})
public class BlanketClosure {

    @XmlElement
    public Date start;
    @XmlElement
    public Date end;

    public BlanketClosure(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    // needed for JAXB
    public BlanketClosure() {}

    // silly wrapper object needed to send lists of observations to client
    @XmlRootElement(name = "blanketClosures")
    public static class List {
        @XmlElement(name = "blanketClosure")
        public Collection<BlanketClosure> blanketClosures;
        public List(Collection<BlanketClosure> blanketClosures) {
            this.blanketClosures = blanketClosures;
        }
        // needed for JAXB
        public List() {
            blanketClosures = new ArrayList<>();
        }
    }
}
