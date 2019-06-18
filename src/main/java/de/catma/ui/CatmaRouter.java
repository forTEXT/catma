package de.catma.ui;

import com.google.common.eventbus.Subscribe;

import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnalyzeOldEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToConflictedProjectEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.events.routing.RouteToTagsEvent;

public interface CatmaRouter {

	@Subscribe
	void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent);

	@Subscribe
	void handleRouteToProject(RouteToProjectEvent routeToProjectEvent);

	@Subscribe
	void handleRouteToConflictedProject(RouteToConflictedProjectEvent routeToConflictedProjectEvent);
	
	@Subscribe
	void handleRouteToTags(RouteToTagsEvent routeToTagsEvent);
	
	@Subscribe
	void handleRouteToAnnotate(RouteToAnnotateEvent routeToAnnotateEvent);

	@Subscribe
	void handleRouteToAnalyzeOld(RouteToAnalyzeOldEvent routeToAnalyzeEvent);
	
	@Subscribe
	void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeNewEvent);
	
	Class<?> getCurrentRoute();
	
	default boolean isNewTarget(Class<?> routingEventClass) {
		return getCurrentRoute() == null || // either current route is null
				! getCurrentRoute().equals(routingEventClass); // or the current route and new route differ somehow
	}
}
