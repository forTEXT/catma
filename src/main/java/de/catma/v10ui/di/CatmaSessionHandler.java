package de.catma.v10ui.di;

import com.vaadin.flow.server.*;
import de.catma.v10ui.background.UIBackgroundService;

public class CatmaSessionHandler implements SessionInitListener, SessionDestroyListener {

    public enum SessionAttributeKey {
        BACKGROUNDSERVICE,
        ;
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        if (event.getSession().getAttribute(SessionAttributeKey.BACKGROUNDSERVICE.name()) != null) {
            ((UIBackgroundService)event.getSession().getAttribute(SessionAttributeKey.BACKGROUNDSERVICE.name())).shutdown();
        }
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        if (event.getSession().getAttribute(SessionAttributeKey.BACKGROUNDSERVICE.name()) != null) {
            event.getSession().setAttribute(SessionAttributeKey.BACKGROUNDSERVICE.name(), new UIBackgroundService(true));
        }
    }
}
