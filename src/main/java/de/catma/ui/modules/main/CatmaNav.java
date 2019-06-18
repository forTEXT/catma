package de.catma.ui.modules.main;

import java.util.Iterator;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryProperties;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.repository.event.ProjectReadyEvent;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.CatmaRouter;
import de.catma.ui.component.LargeLinkButton;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnalyzeOldEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToConflictedProjectEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.events.routing.RouteToTagsEvent;

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
	private LargeLinkButton btAnalyzeOld;
	private LargeLinkButton btAnalyze;
	private Repository currentProject;
	
	@Inject
    public CatmaNav(EventBus eventBus){ 
        eventBus.register(this);
        initComponents();
        initActions(eventBus);
        setStyleName("nav");
    }

    private void initActions(EventBus eventBus) {
		btProject.addClickListener(
			clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToProjectEvent()));
		btAnnotate.addClickListener(
			clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToAnnotateEvent(currentProject)));
		btTags.addClickListener(
			clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToTagsEvent(currentProject))); 
		btAnalyzeOld.addClickListener(
			clickEvent -> 
				handleNavigationClick(
					clickEvent, 
					eventBus, 
					new RouteToAnalyzeOldEvent((IndexedRepository)currentProject, null)));
		btAnalyze.addClickListener(
			clickEvent -> 
				handleNavigationClick(
					clickEvent, 
					eventBus, 
					new RouteToAnalyzeEvent((IndexedRepository)currentProject, null)));
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
        btAnalyzeOld = new LargeLinkButton("Analyze5");
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
			currentProject = null;
		}
	}
	
	@Override
	public void handleRouteToConflictedProject(RouteToConflictedProjectEvent routeToConflictedProjectEvent) {
		if(isNewTarget(routeToConflictedProjectEvent.getClass())) {
			removeAllComponents();
			addComponent(newH3Label("Project"));
			addComponent(newH3Label("Tags"));
			addComponent(newH3Label("Annotate"));
			addComponent(newH3Label("Analyze"));
			
			currentRoute = routeToConflictedProjectEvent.getClass();
		}
	}

	
	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
	        removeAllComponents();
	        addComponent(btProject);
	        addComponent(btTags);
	        addComponent(btAnnotate);
	        if (RepositoryPropertyKey.ShowAnalyzer5.isTrue(RepositoryProperties.INSTANCE.getProperties(), 0, false)) {
	        	addComponent(btAnalyzeOld);
	        }
	        addComponent(btAnalyze);
	        setSelectedStyle(btProject);
	        currentRoute = routeToProjectEvent.getClass();
		}
	}
	
	@Subscribe
	public void handleProjectReadyEvent(ProjectReadyEvent projectReadyEvent) {
		this.currentProject = projectReadyEvent.getProject();
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
	public void handleRouteToTags(RouteToTagsEvent routeToTagsEvent) {
		currentRoute= routeToTagsEvent.getClass();
		setSelectedStyle(btTags);
	}

	@Override
	public void handleRouteToAnalyzeOld(RouteToAnalyzeOldEvent routeToAnalyzeEvent) {
        if (RepositoryPropertyKey.ShowAnalyzer5.isTrue(RepositoryProperties.INSTANCE.getProperties(), 0, false)) {
			currentRoute = routeToAnalyzeEvent.getClass();
			setSelectedStyle(btAnalyzeOld);
        }
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
