package de.catma.ui.modules.main;

import javax.inject.Inject;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.project.ProjectManager;
import de.catma.ui.CatmaRouter;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.modules.dashboard.DashboardView;
import de.catma.ui.modules.project.ProjectView;

/**
 * Main entrypoint for catma, it renders a navigation and a mainSection
 *
 * @author db
 */
public class MainView extends CssLayout implements CatmaRouter  { // implements RouterLayout, HasComponents, BeforeEnterObserver, AfterNavigationObserver {

    /**
     * Header part
     */

    private final CatmaHeader header;

    /**
     * mainSection is the combined section (nav and content) of catma
     */

    private final CssLayout mainSection = new CssLayout();

    /**
     * layoutSection is the content section
     */
    private final CssLayout viewSection = new CssLayout();

    /**
     * left side main navigation
     */
    private final CatmaNav navigation;

    /**
     * global communication via eventbus
     */
    private final EventBus eventBus;

    /**
     * projectmanager
     */
	private final ProjectManager projectManager;

	/**
	 * current route
	 */
	private Class<?> currentRoute;
	
	/**
	 * 
	 * @param projectManager
	 * @param eventBus
	 */
    @Inject
    public MainView(ProjectManager projectManager, EventBus eventBus) {
        this.eventBus = eventBus;
        this.projectManager = projectManager;
        this.header = new CatmaHeader(eventBus);
        this.navigation = new CatmaNav(eventBus);
        initComponents();
        addStyleName("main-view");
        eventBus.register(this);
    }

    /**
     * initialize all components
     */
    private void initComponents() {
        addComponent(header);
       
        mainSection.addComponent(navigation);
        mainSection.addComponent(viewSection);
        mainSection.addStyleName("main-section");
        viewSection.addStyleName("view-section");
        addComponent(mainSection);
    }

    private void setContent(Component component){
    	this.viewSection.removeAllComponents();
    	this.viewSection.addComponent(component);
    }

    
	@Override
	public void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent) {
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			setContent(new DashboardView(projectManager, eventBus));
			eventBus.post(new HeaderContextChangeEvent(new Label("")));
		}
		currentRoute = routeToDashboardEvent.getClass();
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
	    	ProjectView projectView = new ProjectView(projectManager, eventBus);
	    	projectView.handleProjectSelectedEvent(routeToProjectEvent);
	    	setContent(projectView);
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