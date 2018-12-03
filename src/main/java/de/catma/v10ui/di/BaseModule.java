package de.catma.v10ui.di;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.guice.annotation.UIScope;
import de.catma.v10ui.authentication.DummyAuth;
import de.catma.v10ui.modules.main.ErrorLogger;

import java.util.Map;

/**
 * Base module
 */
public class BaseModule extends AbstractModule {

    @Provides @UIScope
    ErrorLogger provideErrorLogger(Map<String, String> user, Page page) {
        return new NotificationAndLogfileLogger(user, page);
    }

    @Provides @UIScope
    EventBus provideEventBus(){
        return new EventBus();
    }

    @Provides
    Page providePage(){
        return UI.getCurrent().getPage();
    }

    @Provides
    Map<String, String> provideDummyUser(){
        return DummyAuth.DUMMYIDENT;
    }
}
