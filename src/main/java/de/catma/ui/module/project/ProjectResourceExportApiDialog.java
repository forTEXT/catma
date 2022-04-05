package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.jsclipboard.JSClipboard;
import com.vaadin.jsclipboard.JSClipboardButton;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import de.catma.api.pre.ProjectResourceExportApiRequestHandler;
import de.catma.project.Project;
import de.catma.ui.CatmaApplicationServlet.DelegateQueryResultRequestHandler;
import de.catma.ui.i18n.Messages;

public class ProjectResourceExportApiDialog extends Window {
    private final Project project;
    private ProjectResourceExportApiRequestHandler projectResourceExportApiRequestHandler;

    private Button btnClose;
    private Button btnEnableApi;
    private Label lApiUrl;
    private Link linkApiUrl;
    private HorizontalLayout apiUrlDisplayPanel;

    public ProjectResourceExportApiDialog(Project project) {
        this.project = project;

        this.setCaption(Messages.getString("ProjectResourceExportApiDialog.caption"));
        initComponents();
        initActions();
    }

    private void initActions() {
        btnClose.addClickListener(evt -> close());

        btnEnableApi.addClickListener(click -> {
            projectResourceExportApiRequestHandler = new ProjectResourceExportApiRequestHandler(this.project);

            for (RequestHandler handler : VaadinService.getCurrent().getRequestHandlers()) {
                if (handler instanceof DelegateQueryResultRequestHandler) {
                    ((DelegateQueryResultRequestHandler) handler).add(projectResourceExportApiRequestHandler);
                    break;
                }
            }

            String apiUrl = projectResourceExportApiRequestHandler.getHandlerUrl();
            lApiUrl.setValue(apiUrl);
            linkApiUrl.setCaption(apiUrl);
            linkApiUrl.setResource(new ExternalResource(apiUrl));
            apiUrlDisplayPanel.setVisible(true);
        });
    }

    private void initComponents(){
        setWidth("60%");
        setModal(true);

        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setStyleName("pre-api-dialog");

        Label lDescription = new Label(Messages.getString("ProjectResourceExportApiDialog.overallDescription"), ContentMode.HTML);
        lDescription.addStyleName("label-with-word-wrap");

        btnEnableApi = new Button(Messages.getString("ProjectResourceExportApiDialog.enableApiForProject"));
        btnEnableApi.setDisableOnClick(true);

        apiUrlDisplayPanel = new HorizontalLayout();
        apiUrlDisplayPanel.setWidth("100%");
        apiUrlDisplayPanel.setVisible(false); // made visible when enable button is clicked

        lApiUrl = new Label(); // need a Label for the clipboard button to work, it doesn't work with a Link
//        lApiUrl.setVisible(false); // this removes the DOM element and breaks the clipboard button
        lApiUrl.setStyleName("jsclipboardbutton-label-invisible");

        linkApiUrl = new Link();
        linkApiUrl.setWidth("100%");
        linkApiUrl.setTargetName("_blank");

        JSClipboardButton jsClipboardButton = new JSClipboardButton(lApiUrl, VaadinIcons.CLIPBOARD_TEXT);
        jsClipboardButton.setDescription(Messages.getString("Dialog.copyToClipboard"));
        jsClipboardButton.addSuccessListener((JSClipboard.SuccessListener) () ->
                Notification.show(Messages.getString("Dialog.copyToClipboardSuccessful"))
        );

        apiUrlDisplayPanel.addComponent(linkApiUrl);
        apiUrlDisplayPanel.addComponent(jsClipboardButton);
        apiUrlDisplayPanel.setComponentAlignment(linkApiUrl, Alignment.BOTTOM_RIGHT);
        apiUrlDisplayPanel.setComponentAlignment(jsClipboardButton, Alignment.BOTTOM_RIGHT);
        apiUrlDisplayPanel.setExpandRatio(linkApiUrl, 1f);

        HorizontalLayout dialogButtonPanel = new HorizontalLayout();
        dialogButtonPanel.setWidth("100%");

        btnClose = new Button(Messages.getString("Dialog.close"));
        dialogButtonPanel.addComponent(btnClose);
        dialogButtonPanel.setComponentAlignment(btnClose, Alignment.BOTTOM_RIGHT);

        content.addComponent(lDescription);
        content.addComponent(new Label("&nbsp;", ContentMode.HTML));
        content.addComponent(btnEnableApi);
//        content.addComponent(new Label("&nbsp;", ContentMode.HTML)); // lApiUrl below is invisible and adding spacing already
        content.addComponent(lApiUrl);
        content.addComponent(apiUrlDisplayPanel);
        content.addComponent(new Label("&nbsp;", ContentMode.HTML));

        content.addComponent(dialogButtonPanel);

        setContent(content);
    }

    public void show() {
        UI.getCurrent().addWindow(this);
    }

    public void removeRequestHandlerFromVaadinService() {
        if (projectResourceExportApiRequestHandler != null) {
            for (RequestHandler handler : VaadinService.getCurrent().getRequestHandlers()) {
                if (handler instanceof DelegateQueryResultRequestHandler) {
                    ((DelegateQueryResultRequestHandler) handler).remove(projectResourceExportApiRequestHandler);
                    break;
                }
            }
        }
    }
}
