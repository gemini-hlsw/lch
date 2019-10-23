package edu.gemini.lch.web.app.components;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.List;

/**
 * A generator for a column with edit functionality.
 */
@Configurable(preConstruction = true)
public class ActionColumn implements Table.ColumnGenerator {

    private final UI parent;
    private final BeanContainer container;

    private final List<Action> actions;

    public interface Listener {
        void actionClick(Action action, Object itemId);
    }

    public static class Action {
        private final Resource icon;
        private final Listener listener;
        private final String description;
        public Action(Resource icon, Listener listener, String description) {
            this.icon = icon;
            this.listener = listener;
            this.description = description;
        }
    }

    public ActionColumn(UI parent, BeanContainer container) {
        this.actions = new ArrayList<>();
        this.parent = parent;
        this.container = container;
    }

    public ActionColumn addAction(Action action) {
        actions.add(action);
        return this;
    }

    public static Action createEditAction(Listener listener) {
        return new ActionColumn.Action(new ThemeResource("img/pencil-icon.png"), listener, "Edit this item.");
    }

    public static Action createDeleteAction(Listener listener) {
        return new ActionColumn.Action(new ThemeResource("img/cross-icon.png"), listener, "Delete this item.");

    }

    @Override
    public Object generateCell(Table source, Object itemId, Object columnId) {
        HorizontalLayout l = new HorizontalLayout();
        for (Action action : actions) {
            l.addComponent(new ActionLink(action, itemId));
        }
        return l;
    }

    private class ActionLink extends CustomComponent implements Button.ClickListener {
        private final Action action;
        private final Object itemId;

        public ActionLink(Action action, Object itemId) {
            this.action = action;
            this.itemId = itemId;
            Button button = new Button();
            button.setStyleName(Reindeer.BUTTON_LINK);
            button.setIcon(action.icon);
            button.addClickListener(this);
            if (action.description != null) {
                button.setDescription(action.description);
            }
            setCompositionRoot(button);
        }

        public void buttonClick (Button.ClickEvent event) {
            action.listener.actionClick(action, itemId);
        }
    }
}