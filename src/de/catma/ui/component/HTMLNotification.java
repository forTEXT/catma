package de.catma.ui.component;

import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

public class HTMLNotification {
    public static void show(String caption, String description, Type type) {
        new Notification(caption, description, type, true).show(Page.getCurrent());
    }
}
