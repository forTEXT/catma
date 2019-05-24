package de.catma.ui.modules.main;

import java.io.Closeable;
import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.project.ProjectManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.CatmaRouter;
import de.catma.ui.analyzenew.AnalyzeNewManagerView;
import de.catma.ui.analyzer.AnalyzerManagerViewOld;
import de.catma.ui.di.UIFactory;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.RegisterCloseableEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnalyzeOldEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToConflictedProjectEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.modules.project.ConflictedProjectView;
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
	private final EventBus eventBus;

    /**
     * projectmanager
     */
	private final ProjectManager projectManager;

	/**
	 * restricted gitmanager
	 */
	private final IRemoteGitManagerRestricted remoteGitManagerRestricted;

	/**
	 * Factory to generate Views
	 */
	private final UIFactory uiFactory;

	/**
	 * current route
	 */
	private Class<?> currentRoute;

	private ProjectView projectView;

	private TaggerManagerView taggerManagerView;

	private AnalyzerManagerViewOld analyzerManagerView;


	
	private AnalyzeNewManagerView analyzeNewManagerView;
	
	/**
	 * 
	 * @param projectManager
	 * @param eventBus
	 */
	@Inject
    public MainView(@Assisted("projectManager")ProjectManager projectManager, 
    		CatmaHeader catmaHeader, 
    		EventBus eventBus,
    		@Assisted("iRemoteGitlabManager")IRemoteGitManagerRestricted remoteGitManagerRestricted,
    		UIFactory uiFactory){
        this.projectManager = projectManager;
        this.header = catmaHeader;
        this.eventBus = eventBus;
        this.navigation = new CatmaNav(eventBus);
        this.remoteGitManagerRestricted = remoteGitManagerRestricted;
        this.uiFactory = uiFactory;
        initComponents();
        addStyleName("main-view");
        eventBus.register(this);
        eventBus.post(new RegisterCloseableEvent(this));
        
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
		closeViews();
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			setContent(uiFactory.getDashboardView(projectManager));
			eventBus.post(new HeaderContextChangeEvent(new Label("")));
			currentRoute = routeToDashboardEvent.getClass();
		}
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
			if (this.projectView == null) {
				this.projectView = new ProjectView(uiFactory, projectManager, eventBus, remoteGitManagerRestricted);
				setContent(projectView);
				this.projectView.setProjectReference(routeToProjectEvent.getProjectReference());
			}
			else {
				setContent(projectView);
				if (routeToProjectEvent.isReloadProject()) {
					this.projectView.setProjectReference(routeToProjectEvent.getProjectReference());
				}
			}
	    	currentRoute = routeToProjectEvent.getClass();
		}
	}
	
	@Override
	public void handleRouteToConflictedProject(RouteToConflictedProjectEvent routeToConflictedProjectEvent) {
		if (isNewTarget(routeToConflictedProjectEvent.getClass())) {
			ConflictedProjectView conflictedProjectView = 
				new ConflictedProjectView(
					routeToConflictedProjectEvent.getConflictedProject(), eventBus);
			setContent(conflictedProjectView);
			currentRoute = routeToConflictedProjectEvent.getClass();
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
	public void handleRouteToAnalyzeOld(RouteToAnalyzeOldEvent routeToAnalyzeEvent) {
		if (isNewTarget(routeToAnalyzeEvent.getClass())) {
			if (this.analyzerManagerView == null) {
				this.analyzerManagerView = new AnalyzerManagerViewOld();
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
	public void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeEvent) {
		if (isNewTarget(routeToAnalyzeEvent.getClass())) {
			if (this.analyzeNewManagerView == null) {
				this.analyzeNewManagerView = new AnalyzeNewManagerView(eventBus);
			}
			
			setContent(analyzeNewManagerView);
			
			if (routeToAnalyzeEvent.getCorpus() != null) {
				analyzeNewManagerView.analyzeNewDocuments(
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
