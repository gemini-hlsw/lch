package edu.gemini.lch.web.app.windows.admin;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.*;
import edu.gemini.lch.model.EngTargetTemplate;
import edu.gemini.lch.model.Site;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.services.SiteService;
import edu.gemini.lch.web.app.components.ActionColumn;
import edu.gemini.lch.web.app.components.EditDialogWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.List;
import java.util.Vector;

/**
 * A table for displaying and editing engineering targets.
 */
@Configurable(preConstruction = true)
public class EngineeringTargetsTable extends Panel implements EditDialogWindow.Listener, ActionColumn.Listener {

    @Autowired
    private LaserNightService laserNightService;

    @Autowired
    private SiteService siteService;

    private final ActionColumn.Action editAction;
    private final ActionColumn.Action deleteAction;

    private BeanContainer<Long, EngTargetTemplate> container;
    private final AdministrationWindow parent;

    private static final Vector columns = new Vector<String>() {{
        add("active");
        add("altitude");
        add("azimuth");
    }};

    public EngineeringTargetsTable(AdministrationWindow parent) {
        this.editAction = ActionColumn.createEditAction(this);
        this.deleteAction = ActionColumn.createDeleteAction(this);
        this.parent = parent;
    }

    public Component getComponent() {
        final Table table = createTable();
        final Layout view = new VerticalLayout() {{
            addComponent(table);
            addComponent(new NewButton());
        }};
        setContent(view);
        return this;
    }

    @Override
    public void actionClick(ActionColumn.Action action, Object itemId) {
        if (action.equals(editAction)) {
            new EditDialogWindow(parent.getUI(), itemId, this);

        } else if (action.equals(deleteAction)) {
            deleted(itemId);
        } else {
            throw new RuntimeException("unknown action");
        }
    }

    private Table createTable() {
        container = createContainer();

        final ActionColumn actions = new ActionColumn(parent.getUI(), container);
        actions.addAction(editAction);
        actions.addAction(deleteAction);

        return new Table("Engineering Targets", container) {{
            addGeneratedColumn("actions", actions);
            setVisibleColumns("altitude", "azimuth", "active", "actions");
        }};
    }

    private BeanContainer<Long, EngTargetTemplate> createContainer() {
        final Site site = siteService.getSite();
        final List<EngTargetTemplate> targets = laserNightService.getEngineeringTargetsForSite(site);
        return new BeanContainer<Long, EngTargetTemplate>(EngTargetTemplate.class) {{
            setBeanIdProperty("id");
            addAll(targets);
        }};
    }

    private class NewButton extends CustomComponent implements Button.ClickListener {
        final Button button;

        public NewButton() {
            button = new Button("New");
            button.addClickListener(this);
            setCompositionRoot(button);
        }

        public void buttonClick (Button.ClickEvent event) {
            final Site site = siteService.getSite();
            final EngTargetTemplate target = new EngTargetTemplate(site, true, 0.0, 0.0);
            laserNightService.saveOrUpdate(target);
            container.addBean(target);
        }
    }

    @Override
    public Form getForm(Object itemId) {
        // Create a bean item that is bound to the bean.
        final BeanItem item = new BeanItem<>(container.getItem(itemId).getBean());

        // the form
        return new Form() {{
            setCaption("Edit me");
            setDescription("Edit this configuration entry.");
            // Bind the bean item as the data source for the form.
            setItemDataSource(item);
            setVisibleItemProperties(columns);
        }};
    }

    public void deleted(Object itemId) {
        final Object bean = container.getItem(itemId).getBean();
        laserNightService.delete(bean);
        container.removeItem(itemId);
    }

    @Override
    public void okButtonClicked(Form form, Object itemId) {
        final EngTargetTemplate bean = container.getItem(itemId).getBean();
        laserNightService.saveOrUpdate(bean);
        // this is silly, how to update table?
        container.removeItem(itemId);
        container.addBean(bean);
    }
}