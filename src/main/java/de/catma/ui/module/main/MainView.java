package de.catma.ui.module.main;

import java.io.Closeable;
import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.rbac.IRBACManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.CatmaRouter;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.QueryResultRowInAnnotateEvent;
import de.catma.ui.events.RegisterCloseableEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToConflictedProjectEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.events.routing.RouteToTagsEvent;
import de.catma.ui.module.analyze.AnalyzeManagerView;
import de.catma.ui.module.annotate.TaggerManagerView;
import de.catma.ui.module.annotate.TaggerView;
import de.catma.ui.module.dashboard.DashboardView;
import de.catma.ui.module.project.ConflictedProjectView;
import de.catma.ui.module.project.ProjectView;
import de.catma.ui.module.tags.TagsView;

/**
 * Main entrypoint for catma, it renders a navigation and a mainSection
 *
 * @author db
 */
public class MainView extends VerticalLayout implements CatmaRouter, Closeable {

    /**
     * Header part
     */

    private final CatmaHeader header;

    /*
     * mainSection is the combined section (nav and content) of catma
     */

    private final HorizontalLayout mainSection = new HorizontalLayout();

    /*
     * layoutSection is the content section
     */
    private final VerticalLayout viewSection = new VerticalLayout();

    /*
     * left side main navigation
     */
    private final CatmaNav navigation;

    /*
     * global communication via eventbus
     */
	private final EventBus eventBus;

    /*
     * projectmanager
     */
	private final ProjectManager projectManager;

	private Class<?> currentRoute;
	private ProjectView projectView;
	private TagsView tagsView;
	private TaggerManagerView taggerManagerView;
	private AnalyzeManagerView analyzeManagerView;

	private IRemoteGitManagerRestricted remoteGitManagerRestricted;

	
	/**
	 * 
	 * @param projectManager
	 * @param eventBus
	 * @param api 
	 */
    public MainView(ProjectManager projectManager, 
    		CatmaHeader catmaHeader, 
    		EventBus eventBus, IRemoteGitManagerRestricted remoteGitManagerRestricted){
        this.projectManager = projectManager;
        this.header = catmaHeader;
        this.eventBus = eventBus;
        this.remoteGitManagerRestricted = remoteGitManagerRestricted;
        this.navigation = new CatmaNav(eventBus);
        initComponents();
        eventBus.register(this);
        eventBus.post(new RegisterCloseableEvent(this));
        
    }

    /**
     * initialize all components
     */
    private void initComponents() {
    	addStyleName("main-view");
    	setSizeFull();
    	setSpacing(false);
        addComponent(header);
        addComponent(mainSection);
        setExpandRatio(mainSection, 1.0f);
        mainSection.setSizeFull();
        mainSection.setSpacing(false);
        viewSection.setSizeFull();
        
        mainSection.addComponent(navigation);
        mainSection.addComponent(viewSection);
        mainSection.setExpandRatio(viewSection, 1f);
        mainSection.addStyleName("main-section");
        viewSection.addStyleName("view-section");
    }

    private void setContent(Component component){
    	viewSection.removeStyleName("no-margin-view-section");
    	this.viewSection.removeAllComponents();
    	this.viewSection.addComponent(component);
    }

    private void closeViews() {
		if (this.projectView != null) {
			this.projectView.close();
			this.projectView = null;
		}
		if (this.tagsView != null) {
			this.tagsView.close();
			this.tagsView = null;
		}
		if (this.taggerManagerView != null) {
			this.taggerManagerView.closeClosables();
			this.taggerManagerView = null;
		}
		if (this.analyzeManagerView != null) {
			this.analyzeManagerView.closeClosables();
			this.analyzeManagerView = null;
		}	    	
    }
    
	@Override
	public void handleRouteToDashboard(RouteToDashboardEvent routeToDashboardEvent) {
		closeViews();
		if(isNewTarget(routeToDashboardEvent.getClass())) {
			setContent(new DashboardView(projectManager, remoteGitManagerRestricted, eventBus));
			viewSection.addStyleName("no-margin-view-section");
			eventBus.post(new HeaderContextChangeEvent());
			currentRoute = routeToDashboardEvent.getClass();
		}
	}

	@Override
	public void handleRouteToProject(RouteToProjectEvent routeToProjectEvent) {
		if(isNewTarget(routeToProjectEvent.getClass())) {
			if (this.projectView == null) {
				this.projectView = new ProjectView(projectManager, eventBus);
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
				this.taggerManagerView = new TaggerManagerView(eventBus, routeToAnnotateEvent.getProject());
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
	public void handleRouteToAnnotate(QueryResultRowInAnnotateEvent queryResultRowInAnnotateEvent) {
		if (isNewTarget(queryResultRowInAnnotateEvent.getClass())) {
			if (this.taggerManagerView == null) {
				this.taggerManagerView = new TaggerManagerView(eventBus, queryResultRowInAnnotateEvent.getProject());
			}
			
			setContent(taggerManagerView);
			
			try {
				TaggerView view = taggerManagerView.openSourceDocument(
					queryResultRowInAnnotateEvent.getProject().getSourceDocument(
							queryResultRowInAnnotateEvent.getDocumentId()),
					queryResultRowInAnnotateEvent.getProject());
				view.showQueryResultRows(
						queryResultRowInAnnotateEvent.getSelection(), 
						queryResultRowInAnnotateEvent.getRows());
			} catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("error opening Document in Annotate module", e);
			}
			currentRoute = queryResultRowInAnnotateEvent.getClass();

		}
	};
	
	@Override
	public void handleRouteToTags(RouteToTagsEvent routeToTagsEvent) {
		if (isNewTarget(routeToTagsEvent.getClass())) {
			if (this.tagsView == null) {
				this.tagsView = new TagsView(eventBus, routeToTagsEvent.getProject());
			}
			
			setContent(tagsView);
			if (routeToTagsEvent.getTagset() != null) {
				this.tagsView.setSelectedTagset(routeToTagsEvent.getTagset());
			}
			currentRoute = routeToTagsEvent.getClass();
		}
		
	}

	@Override
	public void handleRouteToAnalyze(RouteToAnalyzeEvent routeToAnalyzeEvent) {
		if (isNewTarget(routeToAnalyzeEvent.getClass())) {
			if (this.analyzeManagerView == null) {
				this.analyzeManagerView = new AnalyzeManagerView(eventBus, routeToAnalyzeEvent.getProject());
			}
			
			
			if (routeToAnalyzeEvent.getCorpus() != null) {
				analyzeManagerView.analyzeNewDocuments(
					routeToAnalyzeEvent.getCorpus(), routeToAnalyzeEvent.getProject());
			}		
			
			setContent(analyzeManagerView);
			
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
