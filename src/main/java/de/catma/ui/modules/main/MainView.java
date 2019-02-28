package de.catma.ui.modules.main;

import java.io.Closeable;
import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;

import de.catma.project.ProjectManager;
import de.catma.ui.CatmaRouter;
import de.catma.ui.analyzer.AnalyzerManagerView;
import de.catma.ui.events.CloseableEvent;
import de.catma.ui.events.HeaderContextChangeEvent;
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
public class MainView extends CssLayout implements CatmaRouter, Closeable {

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
	private final EventBus eventBus = VaadinSession.getCurrent().getAttribute(EventBus.class);

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
    public MainView(ProjectManager projectManager){
        this.projectManager = projectManager;
        this.header = new CatmaHeader();
        this.navigation = new CatmaNav();
        initComponents();
        addStyleName("main-view");
        eventBus.register(this);
        eventBus.post(new CloseableEvent(this));
        
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

    private void closeViews() {
		if (this.projectView != null) {
			this.projectView.close();
			this.projectView = null;
		}
		if (this.taggerManagerView != null) {
			this.taggerManagerView.closeClosables();
			this.taggerManagerView = null;
		}
		if (this.analyzerManagerView != null) {
			this.analyzerManagerView.closeClosables();
			this.analyzerManagerView = null;
		}	    	
    }
    
	@Override
	public void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent) {
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			closeViews();
			setContent(new DashboardView(projectManager));
			eventBus.post(new HeaderContextChangeEvent(new Label("")));
			currentRoute = routeToDashboardEvent.getClass();
		}
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
			if (this.projectView == null) {
				this.projectView = new ProjectView(projectManager);
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
				this.taggerManagerView = new TaggerManagerView();
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
				this.analyzerManagerView = new AnalyzerManagerView();
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

	@Override
	public void close() throws IOException {
		closeViews();
	}
    
}
