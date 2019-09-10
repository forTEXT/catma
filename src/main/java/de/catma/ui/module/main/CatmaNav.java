package de.catma.ui.module.main;

import java.util.Iterator;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.indexer.IndexedProject;
import de.catma.project.Project;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.ui.CatmaRouter;
import de.catma.ui.component.LargeLinkButton;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
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
public class CatmaNav extends VerticalLayout implements CatmaRouter {
	

	private Class<?> currentRoute;
	private LargeLinkButton btProject;
	private LargeLinkButton btAnnotate;
	private LargeLinkButton btTags;
	private LargeLinkButton btAnalyze;
	private Project currentProject;
	
	@Inject
    public CatmaNav(EventBus eventBus){ 
        eventBus.register(this);
        initComponents();
        initActions(eventBus);
    }

    private void initActions(EventBus eventBus) {
		btProject.addClickListener(
			clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToProjectEvent()));
		btAnnotate.addClickListener(
			clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToAnnotateEvent(currentProject)));
		btTags.addClickListener(
			clickEvent -> handleNavigationClick(clickEvent, eventBus, new RouteToTagsEvent(currentProject))); 
		btAnalyze.addClickListener(
			clickEvent -> 
				handleNavigationClick(
					clickEvent, 
					eventBus, 
					new RouteToAnalyzeEvent((IndexedProject)currentProject, null)));
	}

	private void handleNavigationClick(ClickEvent clickEvent, EventBus eventBus, Object routingEvent) {
		if (routingEvent != null) {
			eventBus.post(routingEvent);
		}
	}

	private void initComponents() {
        addStyleName("nav");
        setHeight("100%");
        setWidth("160px");
        setSpacing(false);
        btProject = new LargeLinkButton("Project");
        btProject.addStyleName("catma-nav-top-entry");
        btTags = new LargeLinkButton("Tags");
        btAnnotate = new LargeLinkButton("Annotate");
        btAnalyze = new LargeLinkButton("Analyze");
        btAnalyze.addStyleName("catma-nav-bottom-entry");
    }

	private Label newH3Label(String name){
    	Label result = new Label(name);
    	result.setStyleName(ValoTheme.LABEL_H3);
    	return result;
    }

	
	private void centerComponents() {
		Iterator<Component> compIter = iterator();
		while(compIter.hasNext()) {
			setComponentAlignment(compIter.next(), Alignment.MIDDLE_CENTER);
		}
	}
	
	@Override
	public void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent) {
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			addLabels();
			currentRoute = routeToDashboardEvent.getClass();
			currentProject = null;
		}
	}
	
	private void addLabels() {
		removeAllComponents();
		Label pLabel = newH3Label("Project");
		pLabel.addStyleName("catma-nav-top-entry");
		addComponent(pLabel);
		setExpandRatio(pLabel, 1.0f);
		addComponent(newH3Label("Tags"));
		addComponent(newH3Label("Annotate"));
		Label anaLabel = newH3Label("Analyze");
		anaLabel.addStyleName("catma-nav-bottom-entry");
		addComponent(anaLabel);
		setExpandRatio(anaLabel, 1.0f);
		centerComponents();
	}

	@Override
	public void handleRouteToConflictedProject(RouteToConflictedProjectEvent routeToConflictedProjectEvent) {
		if(isNewTarget(routeToConflictedProjectEvent.getClass())) {
			addLabels();
			
			currentRoute = routeToConflictedProjectEvent.getClass();
		}
	}

	
	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
			addButtons();
	        setSelectedStyle(btProject);
	        currentRoute = routeToProjectEvent.getClass();
		}
	}
	
	private void addButtons() {
        removeAllComponents();
        addComponent(btProject);
        setExpandRatio(btProject, 1f);
        addComponent(btTags);
        addComponent(btAnnotate);
        addComponent(btAnalyze);
        setExpandRatio(btAnalyze, 1f);
        centerComponents();
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
	public void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeEvent) {
		currentRoute = routeToAnalyzeEvent.getClass();
		setSelectedStyle(btAnalyze);
	}

	@Override
	public Class<?> getCurrentRoute() {
		return currentRoute;
	}

}
