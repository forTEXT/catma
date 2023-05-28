package de.catma.ui.dialog;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.jsclipboard.JSClipboard;
import com.vaadin.jsclipboard.JSClipboardButton;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import de.catma.ui.i18n.Messages;

public class ErrorDialog extends Window {
    private final String message;
    private final Throwable exception;

    public ErrorDialog(String message, Throwable e) {
        super(VaadinIcons.EXCLAMATION_CIRCLE.getHtml() + "Error");
        setCaptionAsHtml(true);

        this.message = message;
        this.exception = e;

        initComponents();
        initActions();
    }

    private void initActions() {

    }

    private void initComponents() {
        setWidth("50%");
        setModal(true);
        addStyleName("error-dialog");

        VerticalLayout content = new VerticalLayout();
        content.setWidth("100%");

        Label lblIntro = new Label("Oops, something went wrong - sorry about that!");
        lblIntro.addStyleName("label-with-word-wrap");

        Label lblOptions = new Label(
                "The error has been logged, but if you keep having the same problem you can:<br/>"
                + "<ul>"
                + "<li>check if it's amongst our <a href=\"https://github.com/forTEXT/catma/issues\" target=\"_blank\">known issues</a></li>"
                + "<li>create a <a href=\"https://github.com/forTEXT/catma/issues/new/choose\" target=\"_blank\">new bug report or feature request</a></li>"
                + "<li>send an email to <a href=\"mailto:support@catma.de\">support@catma.de</a><br/>"
                + "<span class=\"subtext\">(please include a short description, the error message below and any other information that may be relevant, "
                + "such as screenshots, steps to reproduce, etc.)</span></li>"
                + "</ul>",
                ContentMode.HTML
        );
        lblOptions.addStyleName("label-with-word-wrap");

        HorizontalLayout errorLayout = new HorizontalLayout();
        errorLayout.setWidth("100%");
        errorLayout.setMargin(new MarginInfo(false, true, false, false));

        Label lblError = new Label(
                String.format(
                        "The underlying error message is:<br/>"
                        + "%s<br/>"
                        + "%s",
                        message,
                        exception.getMessage() == null ? "" : exception.getMessage()
                ),
                ContentMode.HTML
        );
        lblError.addStyleName("label-with-word-wrap");
        lblError.setWidth("100%");

        JSClipboardButton jsClipboardButton = new JSClipboardButton(lblError, VaadinIcons.CLIPBOARD_TEXT);
        jsClipboardButton.setDescription(Messages.getString("Dialog.copyToClipboard"));
        jsClipboardButton.addSuccessListener((JSClipboard.SuccessListener) () ->
                Notification.show(Messages.getString("Dialog.copyToClipboardSuccessful"))
        );

        errorLayout.addComponent(lblError);
        errorLayout.addComponent(jsClipboardButton);
        errorLayout.setComponentAlignment(lblError, Alignment.TOP_RIGHT);
        errorLayout.setComponentAlignment(jsClipboardButton, Alignment.TOP_RIGHT);
        errorLayout.setExpandRatio(lblError, 1f);

        content.addComponent(lblIntro);
        content.addComponent(lblOptions);
        content.addComponent(errorLayout);

        setContent(content);
    }

    public void show() {
        UI.getCurrent().addWindow(this);
    }
}
