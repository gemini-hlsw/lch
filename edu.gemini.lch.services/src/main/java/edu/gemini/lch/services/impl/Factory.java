package edu.gemini.lch.services.impl;

import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.internal.collector.TargetsCollector;
import edu.gemini.lch.services.util.EmailReader;
import edu.gemini.lch.services.util.PrmFile;
import edu.gemini.lch.services.util.TemplateEngine;
import edu.gemini.lch.services.util.WorkDayCalendar;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * Object factory for instantiating objects using the Spring framework in order to make sure that
 * all resources are properly wired up. Note that while it is possible to create objects directly
 * most of the functionality won't work since resources (e.g. services etc) will be missing.
 *
 * In general all objects that are defined as components and have scope prototype should be instantiated
 * using this factory. Note that the resources are unfortunately only available after the object has been
 * constructed, i.e. they can not be used inside of the constructor. Use @PostConstruct methods for
 * construction code that has to run after Spring has finished wiring up the component.
 */
@Component
public class Factory implements BeanFactoryAware  {

    /** BeanFactory provided by Spring for instantiation of wired objects. */
    private BeanFactory beanFactory;

    /**
     * Creates a fully wired targets collector.
     */
    public TargetsCollector createTargetsCollector(LaserNight night) {
        return (TargetsCollector) beanFactory.getBean("targetsCollector", night);
    }

    /**
     * Creates a fully wired template engine for the given night.
     */
    public TemplateEngine createTemplateEngine(LaserNight night) {
        return (TemplateEngine) beanFactory.getBean("templateEngine", night);
    }

    /**
     * Creates a fully wired template engine for the given date.
     */
    public TemplateEngine createTemplateEngine(ZonedDateTime date) {
        return (TemplateEngine) beanFactory.getBean("templateEngine", date);
    }

    /**
     * Creates a fully wired PRM file creator for RaDec files.
     */
    public PrmFile.RaDec createPrmFileRaDec(LaserNight night, Integer maxTargets) {
        return (PrmFile.RaDec) beanFactory.getBean("prmFileRaDec", night, maxTargets);
    }

    /**
     * Creates a fully wired PRM file creator for AzEl files.
     */
    public PrmFile.AzEl createPrmFileAzEl(LaserNight night, Integer maxTargets) {
        return (PrmFile.AzEl) beanFactory.getBean("prmFileAzEl", night, maxTargets);
    }

    /**
     * Creates a fully wired updater for laser nights.
     */
    public LaserNightUpdater createUpdater() {
        return (LaserNightUpdater) beanFactory.getBean("laserNightUpdater");
    }

    /**
     * Crates a fully wired calendar for work day calculations based on holidays in database.
     * @return
     */
    public WorkDayCalendar createCalendar() {
        return (WorkDayCalendar) beanFactory.getBean("workDayCalendar");
    }

    /**
     * Crates a fully wired email reader.
     * @return
     */
    public EmailReader createEmailReader() {
        return (EmailReader) beanFactory.getBean("emailReader");
    }

    /** Setter used by Spring. */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
