package edu.gemini.lch.web.app.components;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * A simple component that adds a label to another component and wraps the two up in one single component.
 */
public class LabeledComponent extends VerticalLayout {
    private Component component;

    /**
     * Creates a labeled component.
     * @param title
     * @param component
     */
    public LabeledComponent(String title, Component component) {
        Label label = new Label(title);
        label.setStyleName(Reindeer.LABEL_H2);
        setMargin(true);
        setSpacing(true);
        setSizeFull();
        addComponent(label);
        addComponent(component);
        this.component = component;
    }

    /**
     * Replaces the main component of this labeled component.
     * @param component
     */
    public void replaceComponent(Component component) {
        replaceComponent(this.component, component);
        this.component = component;
    }
}

