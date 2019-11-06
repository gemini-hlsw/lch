package edu.gemini.lch.configuration;

import edu.gemini.lch.model.Site;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Entity
@Table(name = "lch_configuration_values")
public class ConfigurationValue {

    private static final String LIST_SEPARATOR = ",";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "site")
    private Site site;

    @Column
    private String paramValue;

    public Long getId() {
        return id;
    }

    void setValue(String string) {
        paramValue = string;
    }

    public Site getSite() {
        return site;
    }

    public Boolean isEmpty() {
        if (paramValue == null || paramValue.isEmpty()) {
            return true;
        }
        return false;
    }

    public String getAsString() {
        return paramValue;
    }
    public List<String> getAsStringList() {
        List<String> list = new ArrayList<>();
        for (String s : StringUtils.split(paramValue, LIST_SEPARATOR)) {
            list.add(s.trim());
        }
        return list;
    }
    public String[] getAsStringArray() {
        List<String> strings = getAsStringList();
        return strings.toArray(new String[strings.size()]);
    }
    public Integer getAsInteger() {
        return Integer.parseInt(paramValue);
    }
    public List<Integer> getAsIntegerList() {
        List<Integer> list = new ArrayList<>();
        for (String s : StringUtils.split(paramValue, LIST_SEPARATOR)) {
            list.add(Integer.parseInt(s.trim()));
        }
        return list;
    }
    public Double getAsDouble() {
        return Double.parseDouble(paramValue);
    }
    public List<Double> getAsDoubleList() {
        List<Double> list = new ArrayList<>();
        for (String s : StringUtils.split(paramValue, LIST_SEPARATOR)) {
            list.add(Double.parseDouble(s.trim()));
        }
        return list;
    }
    public Boolean getAsBoolean() {
        return Boolean.parseBoolean(paramValue);
    }
    public Period getAsPeriod() {
        return Period.parse(paramValue);
    }
    public List<Period> getAsPeriodList() {
        List<Period> list = new ArrayList<>();
        for (String s : StringUtils.split(paramValue, LIST_SEPARATOR)) {
            list.add(Period.parse(s.trim()));
        }
        return list;
    }

    public String getValue() {
        return paramValue;
    }

    // empty constructor needed for hibernate
    protected ConfigurationValue() {}

}
