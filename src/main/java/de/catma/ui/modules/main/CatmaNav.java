package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.CssLayout;

import de.catma.ui.component.LargeLinkButton;
import de.catma.ui.events.ProjectSelectedEvent;

/**
 * Stateful Catma navigation
 *
 * It renders the the main navigation HTML elements, keep states of all destinations.
 *
 * @author db
 */
public class CatmaNav extends CssLayout  {

    public CatmaNav(EventBus eventBus){
        eventBus.register(this);
        refresh();
    }

    public void refresh() {
    	setStyleName("nav");
        removeAllComponents();
        addComponent(new LargeLinkButton("Project"));
        addComponent(new LargeLinkButton("Tags"));
        addComponent(new LargeLinkButton("Analyze"));
    }

    
    @Subscribe
    public void afterNavigationEvent(ProjectSelectedEvent projectSelectedEvent){
        removeAllComponents();
        addComponent(new LargeLinkButton("Project"));
        addComponent(new LargeLinkButton("Tags"));
        addComponent(new LargeLinkButton("Analyze"));
    }
}
