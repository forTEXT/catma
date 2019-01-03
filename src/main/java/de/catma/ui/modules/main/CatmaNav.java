package de.catma.ui.modules.main;

import java.util.Iterator;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.ui.CatmaRouter;
import de.catma.ui.component.LargeLinkButton;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;

/**
 * Stateful Catma navigation
 *
 * It renders the the main navigation HTML elements, keep states of all destinations.
 *
 * @author db
 */
public class CatmaNav extends CssLayout implements CatmaRouter {
	

	private Class<?> currentRoute;
	private LargeLinkButton btProject;
	private LargeLinkButton btAnnotate;
	private LargeLinkButton btTags;
	private LargeLinkButton btAnalyze;
	
    public CatmaNav(EventBus eventBus) {
        eventBus.register(this);
        initComponents();
        initActions(eventBus);
        setStyleName("nav");
    }

    private void initActions(EventBus eventBus) {
		btProject.addClickListener(clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToProjectEvent(null)));
		btAnnotate.addClickListener(clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToAnnotateEvent(null)));
		btTags.addClickListener(clickEvent -> handleNavigationClick(clickEvent, eventBus, null));
		btAnalyze.addClickListener(clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToAnalyzeEvent(null, null)));
	}

	private void handleNavigationClick(ClickEvent clickEvent, EventBus eventBus, Object routingEvent) {
		if (routingEvent != null) {
			eventBus.post(routingEvent);
		}
	}

	private void initComponents() {
        btProject = new LargeLinkButton("Project");
        btTags = new LargeLinkButton("Tags");
        btAnnotate = new LargeLinkButton("Annotate");
        btAnalyze = new LargeLinkButton("Analyze");
    }

	private Label newH3Label(String name){
    	Label result = new Label(name);
    	result.setStyleName(ValoTheme.LABEL_H3);
    	return result;
    }

	@Override
	public void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent) {
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			removeAllComponents();
			addComponent(newH3Label("Project"));
			addComponent(newH3Label("Tags"));
			addComponent(newH3Label("Annotate"));
			addComponent(newH3Label("Analyze"));
			
			currentRoute = routeToDashboardEvent.getClass();
		}
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
	        removeAllComponents();
	        addComponent(btProject);
	        addComponent(btTags);
	        addComponent(btAnnotate);
	        addComponent(btAnalyze);
	        setSelectedStyle(btProject);
	        currentRoute = routeToProjectEvent.getClass();
		}
	}
	
	private void setSelectedStyle(LargeLinkButton selectedButton) {
		Iterator<Component> componentIter = this.iterator();
		while (componentIter.hasNext()) {
			Component comp = componentIter.next();
			if (comp.equals(selectedButton)) {
				comp.addStyleName("nav-selected-entry");
			}
			else {
				comp.removeStyleName("nav-selected-entry");
			}
		}
	}

	@Override
	public void handleRouteToAnnotate(RouteToAnnotateEvent routeToAnnotateEvent) {
		currentRoute = routeToAnnotateEvent.getClass();
		setSelectedStyle(btAnnotate);
	}

	@Override
	public void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeEvent) {
		currentRoute = routeToAnalyzeEvent.getClass();
		setSelectedStyle(btAnalyze);
	}

	@Override
	public Class<?> getCurrentRoute() {
		return currentRoute;
	}

}
