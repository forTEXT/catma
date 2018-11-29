package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.ui.CatmaRouter;
import de.catma.ui.component.LargeLinkButton;
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
	
    public CatmaNav(EventBus eventBus){
        eventBus.register(this);
        setStyleName("nav");
    }

    private Label newH3Label(String name){
    	Label result = new Label(name);
    	result.setStyleName(ValoTheme.LABEL_H3);
    	return result;
    }
    
    @Subscribe
    public void handleRouteToProjectEvent(RouteToProjectEvent projectSelectedEvent){
    }
    

	@Override
	public void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent) {
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			removeAllComponents();
			addComponent(newH3Label("Project"));
			addComponent(newH3Label("Tags"));
			addComponent(newH3Label("Analyze"));
		}
		currentRoute = routeToDashboardEvent.getClass();
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
	        removeAllComponents();
	        addComponent(new LargeLinkButton("Project"));
	        addComponent(new LargeLinkButton("Tags"));
	        addComponent(new LargeLinkButton("Analyze"));
		}
		currentRoute = routeToProjectEvent.getClass();
	}

	@Override
	public Class<?> getCurrentRoute() {
		return currentRoute;
	}

	@Override
	public void setCurrentRoute(Class<?> routingEventClass) {
		currentRoute = routingEventClass;
	}
}
