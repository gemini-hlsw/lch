package edu.gemini.lch.web.app.windows.admin;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.configuration.Selection;
import edu.gemini.lch.services.ConfigurationService;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.web.app.components.ActionColumn;
import edu.gemini.lch.web.app.components.EditDialogWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * A table for displaying and editing engineering targets.
 */
@Configurable(preConstruction = true)
public class ConfigurationEntriesTable extends Panel implements EditDialogWindow.Listener, ActionColumn.Listener {

    @Autowired private ConfigurationService configurationService;
    @Autowired private LaserNightService laserNightService;

    private final ActionColumn.Action editAction;

    private BeanContainer<Long, Configuration> container;
    private Table table;
    private final Component parent;
    private final String group;

    public ConfigurationEntriesTable(Component parent, String group) {
        this.parent = parent;
        this.group = group;
        this.editAction = ActionColumn.createEditAction(this);
    }

    public Component getComponent() {
        Table table = createTable();
        setContent(table);
        return this;
    }

    private Table createTable() {
        container = createContainer();
        table = new Table("Configuration", container);
        table.setSizeFull();

        ActionColumn actions = new ActionColumn(parent.getUI(), this.container);
        actions.addAction(editAction);

        table.addGeneratedColumn("actions", actions);
        table.addGeneratedColumn("value", new ValueColumnGenerator());
        table.setColumnExpandRatio("value", 1.0f);
        table.setVisibleColumns("label", "description", "value", "actions");
        // sort table by default according to id, this keeps all entries in a well defined order
        // (otherwise after edits the order will change which is very confusing)
        table.setSortContainerPropertyId("id");
        table.setSortAscending(true);

        return table;
    }

    private BeanContainer<Long, Configuration> createContainer() {
        List<Configuration> targets = configurationService.getConfigurationEntries(group);
        BeanContainer<Long, Configuration> container = new BeanContainer<>(Configuration.class);
        container.setBeanIdProperty("id");
        container.addAll(targets);
        return container;
    }

    @Override public void actionClick(ActionColumn.Action action, Object itemId) {
        if (action.equals(editAction)) {
            new EditDialogWindow(parent.getUI(), itemId, this);
        } else {
            throw new RuntimeException("unknown action");
        }
    }


    @Override public Form getForm(Object itemId) {
        final Form form = new Form();
        Configuration bean = container.getItem(itemId).getBean();
        form.setCaption(bean.getLabel());
        form.setDescription(bean.getDescription());
        switch (bean.getType()) {
            case TEXT:
                final TextArea textArea = new TextArea();
                textArea.setSizeFull();
                textArea.setWordwrap(false);
                textArea.setRows(StringUtils.countOccurrencesOf(bean.getAsString(), "\n") + 4);
                textArea.setValue(bean.getAsString());
                form.addField("value", textArea);
                break;
            case SELECTION:
                final ComboBox select = new ComboBox();
                select.setNullSelectionAllowed(false);
                for (final Selection s : bean.getSelectionValues()) {
                    select.addItem(s.toString());                        // use the string name of the enum as id
                    select.setItemCaption(s.toString(), s.getLabel());   // use the label of the enum as the caption
                }
                select.select(bean.getSelection().toString());
                form.addField("value", select);
                break;
            default:
                final TextField textField = new TextField();
                textField.setSizeFull();
                textField.setValue(bean.getAsString());
                form.addField("value", textField);
        }
        return form;
    }

    @Override public void okButtonClicked(Form form, Object itemId) {
        Configuration bean = container.getItem(itemId).getBean();
        String newValue = (String) form.getField("value").getValue();
        configurationService.update(bean, newValue);
        table.refreshRowCache();
    }

    private class ValueColumnGenerator implements Table.ColumnGenerator {
        @Override public Object generateCell(Table source, Object itemId, Object columnId) {
            Configuration bean = container.getItem(itemId).getBean();
            switch (bean.getType()) {
                case TEXT:
                    // text: set style to preformatted so that line breaks etc. show up properly
                    return new Label(bean.getAsString(), ContentMode.PREFORMATTED);
                case SELECTION:
                    // selection: show the label of the selection
                    return bean.getSelection().getLabel();
                default:
                    // everything else: just display as string
                    return new Label(bean.getAsString());
            }
        }
    }
}
