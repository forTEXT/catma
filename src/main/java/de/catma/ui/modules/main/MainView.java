package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.HorizontalLayout;

import de.catma.project.ProjectManager;
import de.catma.ui.CatmaRouter;
import de.catma.ui.analyzer.AnalyzerManagerView;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.modules.dashboard.DashboardView;
import de.catma.ui.modules.project.ProjectView;
import de.catma.ui.tagger.TaggerManagerView;

/**
 * Main entrypoint for catma, it renders a navigation and a mainSection
 *
 * @author db
 */
public class MainView extends CssLayout implements CatmaRouter  {

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

	private ProjectView projectView;

	private TaggerManagerView taggerManagerView;

	private AnalyzerManagerView analyzerManagerView;
	
	/**
	 * 
	 * @param projectManager
	 * @param eventBus
	 */
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
			this.projectView = null;
			this.taggerManagerView = null;
			setContent(new DashboardView(projectManager, eventBus));
			eventBus.post(new HeaderContextChangeEvent(new Label("")));
			currentRoute = routeToDashboardEvent.getClass();
		}
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
			if (this.projectView == null) {
				this.projectView = new ProjectView(projectManager, eventBus);
				this.projectView.setProjectReference(routeToProjectEvent.getProjectReference());
			}
	    	setContent(projectView);
	    	currentRoute = routeToProjectEvent.getClass();
		}
	}
	
	@Override
	public void handleRouteToAnnotate(RouteToAnnotateEvent routeToAnnotateEvent) {
		if (isNewTarget(routeToAnnotateEvent.getClass())) {
			if (this.taggerManagerView == null) {
				this.taggerManagerView = new TaggerManagerView(eventBus);
			}
			
			setContent(taggerManagerView);
			
			if (routeToAnnotateEvent.getDocument() != null) {
				taggerManagerView.openSourceDocument(
					routeToAnnotateEvent.getDocument(), routeToAnnotateEvent.getProject());
			}			
			currentRoute = routeToAnnotateEvent.getClass();
		}
	};
	
	@Override
	public void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeEvent) {
		if (isNewTarget(routeToAnalyzeEvent.getClass())) {
			if (this.analyzerManagerView == null) {
				this.analyzerManagerView = new AnalyzerManagerView(eventBus);
			}
			
			setContent(analyzerManagerView);
			
			if (routeToAnalyzeEvent.getCorpus() != null) {
				analyzerManagerView.analyzeDocuments(
					routeToAnalyzeEvent.getCorpus(), routeToAnalyzeEvent.getProject());
			}			
			currentRoute = routeToAnalyzeEvent.getClass();
		}
	}
    

	@Override
	public Class<?> getCurrentRoute() {
		return currentRoute;
	}

	public void close() {
		if (projectView != null) {
			projectView.close();
		}
	}
    
}
