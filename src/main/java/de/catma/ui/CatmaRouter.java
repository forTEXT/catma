package de.catma.ui;

import com.google.common.eventbus.Subscribe;

import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;

public interface CatmaRouter {

	@Subscribe
	void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent);

	@Subscribe
	void handleRouteToProject(RouteToProjectEvent routeToProjectEvent);
	
	Class<?> getCurrentRoute();
	
	void setCurrentRoute(Class<?> routingEventClass);
	
	default boolean isNewTarget(Class<?> routingEventClass) {
		return getCurrentRoute() == null || // either current route is null
				! getCurrentRoute().equals(routingEventClass); // or the current route and new route differs somehow
	}
}
