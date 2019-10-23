package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * A simplified observation transfer object that allows to flatten the observation/observation target relation-
 * ship and send a list of observations and their targets without the laser targets to the client.
 */
@XmlRootElement(name = "observation")
@XmlType(propOrder = {"id", "target", "type", "coordinates"})
public class ShortObservation {

    @XmlElement
    private String id;
    @XmlElement
    private String target;
    @XmlElement
    private String type;
    @XmlElement
    private Coordinates coordinates;

    public ShortObservation(String id, String target, String type, Coordinates coordinates) {
        this.id = id;
        this.target = target;
        this.type = type;
        this.coordinates = coordinates;
    }

    // needed for JAXB
    public ShortObservation() {}

    // silly wrapper object needed to send lists of observations to client
    @XmlRootElement(name = "observations")
    public static class List {
        @XmlElement(name = "observation")
        private Collection<ShortObservation> observations;
        public List(Collection<ShortObservation> observations) {
            this.observations = observations;
        }
        // needed for JAXB
        public List() {
            observations = new ArrayList<>();
        }
    }
}
