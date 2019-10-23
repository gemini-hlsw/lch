package edu.gemini.lch.configuration;

import org.apache.commons.lang.Validate;

import javax.persistence.*;
import java.util.List;

/**
 */
@NamedQueries({
        /*
            Named query to get all configuration entries of a site joined with the (one and only) value for this site.
         */
        @NamedQuery(name = Configuration.QUERY_FIND_BY_SITE_AND_GROUP,
                query = "from Configuration e " +
                        "left join fetch e.values v " +
                        "where e.groupName = :group " +
                        "  and v.site = :site "
        ),
        /*
           Named query to get a configuration entry by site, group and name with the (one and only) value.
        */
        @NamedQuery(name = Configuration.QUERY_FIND_BY_SITE_AND_NAME,
                query = "from Configuration  e " +
                        "left join fetch e.values v " +
                        "where e.paramName = :name " +
                        "  and v.site = :site "
        )
})

@Entity
@Table(name = "lch_configuration_entries")
public class Configuration {

    public static final String QUERY_FIND_BY_SITE_AND_GROUP = "configuration.findBySiteAndGroup";
    public static final String QUERY_FIND_BY_SITE_AND_NAME = "configuration.findBySiteAndName";

    private static final String LIST_SEPARATOR = ",";
    private static final String SELECTION_SPLIT_CHAR = ":";

    // all known values
    public enum Value {
        VISIBILITY_MIN_ALTITUDE,
        VISIBILITY_MIN_DURATION,
        VISIBILITY_TWILIGHT,
        NEARBY_GROUP_MAX_DISTANCE,
        HORIZONS_STEP_WIDTH,
        NEW_MAIL_POLL_INTERVAL,

        EMAILS_LCH_TO_ADDRESSES,
        EMAILS_LCH_CC_ADDRESSES,
        EMAILS_LCH_BCC_ADDRESSES,
        EMAILS_INTERNAL_TO_ADDRESSES,
        EMAILS_INTERNAL_CC_ADDRESSES,
        EMAILS_INTERNAL_BCC_ADDRESSES,
        EMAILS_ACCOUNT_USER,
        EMAILS_ACCOUNT_PASSWORD,
        EMAILS_FROM_ADDRESS,
        EMAILS_REPLY_TO_ADDRESS,
        EMAILS_PRM_EMAIL_SUBJECT_TEMPLATE,
        EMAILS_PRM_EMAIL_BODY_TEMPLATE,
        EMAILS_PRM_ADDENDUM_EMAIL_SUBJECT_TEMPLATE,
        EMAILS_PRM_ADDENDUM_EMAIL_BODY_TEMPLATE,
        EMAILS_PAM_MISSING_EMAIL_SUBJECT_TEMPLATE,
        EMAILS_PAM_MISSING_EMAIL_BODY_TEMPLATE,
        EMAILS_NEW_TARGETS_EMAIL_SUBJECT_TEMPLATE,
        EMAILS_NEW_TARGETS_EMAIL_BODY_TEMPLATE,
        EMAILS_PRM_SEND_WORK_DAYS_AHEAD,
        SPACE_TRACK_EMAIL,
        SPACE_TRACK_MAX_FILE_ID,

        PRM_HEADER_TEMPLATE,
        PRM_RADEC_TARGET_TEMPLATE,
        PRM_AZEL_TARGET_TEMPLATE,
        PRM_TARGET_SEPARATOR_TEMPLATE,
        PRM_FOOTER_TEMPLATE,
        PRM_FILENAME_TEMPLATE,
        PRM_MAX_NUMBER_OF_TARGETS,

        SCHEDULER_PROCESS_NIGHTS_SCHEDULE,

        LTCS_URL,

        HELP_URL,
        ODB_URL,
        TIME_ZONE_OFFSET,
        ENGINEERING_QUERY,
        SCIENCE_QUERY,
        TEST_SCIENCE_QUERY,
        EPICS_ADDRESS_LIST,

        LIS_BUFFER_BEFORE_SHUTTER_WINDOW,
        LIS_BUFFER_AFTER_SHUTTER_WINDOW,
        ERROR_CONE_ANGLE,

        ADMIN_USERS
        }

    public enum Type {
        STRING,
        INTEGER,
        DOUBLE,
        PERIOD,
        BOOLEAN,
        TEXT,
        SELECTION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "entry_id")
    private List<ConfigurationValue> values;

    @Enumerated(EnumType.STRING)
    @Column
    protected Type type;

    @Column
    protected Boolean isList;

    @Column
    protected Boolean canBeEmpty;

    @Column
    protected String regExp;

    @Column
    protected Double minValue;

    @Column
    protected Double maxValue;

    @Column
    private String groupName;

    @Column
    private String paramName;

    @Column
    private String label;

    @Column
    private String description;


    public Long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Boolean isList() {
        return isList;
    }

    public Boolean canBeEmpty() {
        return canBeEmpty;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getParamName() {
        return paramName;
    }

    public Selection[] getSelectionValues() {
        Validate.isTrue(type.equals(Type.SELECTION), "operation only allowed on selections");
        try {
            Class<Selection> clazz = (Class<Selection>) Class.forName("edu.gemini.lch.configuration.Selection$"+regExp);
            return (clazz.getEnumConstants());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Selection getSelection() {
        Validate.isTrue(type.equals(Type.SELECTION), "operation only allowed on selections");
        try {
            // TODO: make this useful for other selection types, too
            Class<Selection.Twilight> clazz = (Class<Selection.Twilight>) Class.forName("edu.gemini.lch.configuration.Selection$"+regExp);
            return Enum.valueOf(clazz, getAsString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public ConfigurationValue getValue() {
        Validate.isTrue(values.size() == 1);
        return values.get(0);
    }

    public String getAsString() {
        Validate.isTrue(values.size() == 1);
        return values.get(0).getAsString();
    }

    public void setFromString(String s) {
        Validate.isTrue(values.size() == 1);
        values.get(0).setValue(s);
    }

    // empty constructor for Hibernate
    private Configuration() {}

}
