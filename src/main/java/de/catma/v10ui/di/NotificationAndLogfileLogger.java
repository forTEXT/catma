package de.catma.v10ui.di;

import com.google.inject.Inject;
import com.mysql.jdbc.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.Page;
import com.vaadin.guice.annotation.UIScope;
import de.catma.v10ui.modules.main.ErrorLogger;

import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@UIScope
public class NotificationAndLogfileLogger implements ErrorLogger {

    private Map<String, String> user;

    private Page page;

    private static final Logger logger = Logger.getLogger(ErrorLogger.class.getName());

    @Inject
    NotificationAndLogfileLogger(Map<String, String> user, Page page){
        this.user=user;
        this.page=page;
    }

    public void showAndLogError(String message, Throwable e) {
        logger.log(Level.SEVERE, "[" + user + "]" + message, e); //$NON-NLS-1$ //$NON-NLS-2$

        if (message == null) {
            message = Messages.getString("CatmaApplication.internalError"); //$NON-NLS-1$
        }
        if (page != null) {
            Div content = new Div();
            content.addClassName("c__error");
            content.setText(MessageFormat.format(
                    Messages.getString("CatmaApplication.errorOccurred"), message, e.getMessage()));
            content.setSizeFull();
            Notification notification = new Notification(content);
            notification.setPosition(Notification.Position.TOP_STRETCH);
            notification.setDuration(0);
            UI.getCurrent().access(() -> notification.open());
        }
    }
}