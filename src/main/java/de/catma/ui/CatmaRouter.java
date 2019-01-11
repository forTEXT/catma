package de.catma.ui;

import com.google.common.eventbus.Subscribe;

import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnalyzeNewEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;

public interface CatmaRouter {

	@Subscribe
	void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent);

	@Subscribe
	void handleRouteToProject(RouteToProjectEvent routeToProjectEvent);
	
	@Subscribe
	void handleRouteToAnnotate(RouteToAnnotateEvent routeToAnnotateEvent);

	@Subscribe
	void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeEvent);
	
	@Subscribe
	void handleRouteToAnalyzeNew(RouteToAnalyzeNewEvent routeToAnalyzeNewEvent);
	
	Class<?> getCurrentRoute();
	
	default boolean isNewTarget(Class<?> routingEventClass) {
		return getCurrentRoute() == null || // either current route is null
				! getCurrentRoute().equals(routingEventClass); // or the current route and new route differ somehow
	}
}
