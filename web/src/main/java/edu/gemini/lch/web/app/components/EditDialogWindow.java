package edu.gemini.lch.web.app.components;

import com.vaadin.ui.*;

/**
 */
public final class EditDialogWindow extends Window implements Button.ClickListener {

    private final UI parent;
    private final Object itemId;
    private final Listener listener;
    private final Form form;
    private final Button okButton;
    private final Button cancelButton;


    public EditDialogWindow(final UI parent, final Object itemId, final Listener listener) {

        setCaption("Edit");

        this.parent = parent;
        this.itemId = itemId;
        this.listener = listener;
        this.okButton = new Button("OK", this);
        this.cancelButton = new Button("Cancel", this);
        this.form = listener.getForm(itemId);

        parent.addWindow(this);
        final HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent(okButton);
        buttons.addComponent(cancelButton);

        final VerticalLayout view = new VerticalLayout();
        view.setMargin(true);
        view.addComponent(form);
        view.addComponent(buttons);

        setContent(view);
        setWidth("500px");
        setModal(true);

    }

    /** Handle button click and close the window. */
    public void buttonClick(final Button.ClickEvent event) {
        if (event.getButton() == okButton) {
            if (form.isValid()) {
                parent.removeWindow(this);
                listener.okButtonClicked(form, itemId);
            }

        } else if (event.getButton() == cancelButton) {
            parent.removeWindow(this);
        }

    }

    public interface Listener {
        Form getForm(Object itemId);
        void okButtonClicked(Form form, Object itemId);
    }

}
