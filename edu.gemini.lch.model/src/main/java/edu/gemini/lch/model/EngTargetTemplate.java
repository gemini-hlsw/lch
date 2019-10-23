package edu.gemini.lch.model;

import javax.persistence.*;

/**
 */
@NamedQueries({
        /*
            Named query to get all engineering targets of a site.
         */
        @NamedQuery(name = EngTargetTemplate.FIND_BY_SITE_QUERY,
                query = "from EngTargetTemplate e " +
                        "where e.site = :site " +
                        "order by e.id"
        )
})

@Entity
@Table(name = "lch_engineering_targets")
public class EngTargetTemplate {
    
    public static final String FIND_BY_SITE_QUERY = "engineeringTarget.findBySite";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "site")
    private Site site;

    @Column
    protected Boolean active;

    @Column
    protected Double altitude;

    @Column
    protected Double azimuth;

    public EngTargetTemplate(Site site, Boolean active, Double altitude, Double azimuth) {
        this.site = site;
        this.active = active;
        this.altitude = altitude;
        this.azimuth = azimuth;
    }

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(Double azimuth) {
        this.azimuth = azimuth;
    }

    // empty constructor needed for hibernate
    public EngTargetTemplate() {}

}
