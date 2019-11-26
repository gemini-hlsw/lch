package edu.gemini.lch.services.model;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlType(propOrder = {"id", "coordinates", "visibility", "clearanceWindows", "shutteringWindows"})
public class LaserTarget {
    private String id;
    private Coordinates coordinates;
    private Visibility visibility;
    private List<ClearanceWindow> clearanceWindows;
    private List<ShutteringWindow> shutteringWindows;

    public LaserTarget() {}
    public LaserTarget(Long id, Coordinates coordinates) {
        this.id = id.toString();
        this.coordinates = coordinates;
    }

    @XmlID
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @XmlElement
    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }

    @XmlElement
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    @XmlElementWrapper(name = "clearanceWindows")
    @XmlElement(name = "clearanceWindow")
    public List<ClearanceWindow> getClearanceWindows() {
        if (clearanceWindows ==  null) clearanceWindows = new ArrayList<>();
        return clearanceWindows;
    }

    @XmlElementWrapper(name = "shutteringWindows")
    @XmlElement(name = "shutteringWindow")
    public List<ShutteringWindow> getShutteringWindows() {
        if (shutteringWindows == null) shutteringWindows = new ArrayList<>();
        return shutteringWindows;
    }
}
