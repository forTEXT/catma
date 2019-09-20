package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.module.main.ErrorHandler;

/**
 *
 * Renders a dashboard with all projects and all shared resources
 *
 * @author db
 */
public class DashboardView extends VerticalLayout {

    private final ErrorHandler errorLogger;
	
	private final ProjectListView projects;

	private final EventBus eventBus;

	private final DataProvider<ProjectReference,Void> projectDataProvider;

	@Inject
    public DashboardView(
    		@Assisted("projectManager")ProjectManager projectManager,
    		ProjectListView projectList, 
    		EventBus eventBus){ 
        this.errorLogger = (ErrorHandler)(UI.getCurrent());
        this.projects = projectList;
        this.eventBus = eventBus;
        this.projectDataProvider = DataProvider.fromCallbacks(
		        (query) -> {
		            try {
		            	return projectManager.getProjectReferences().stream();
		            } catch (Exception e) {
		                errorLogger.showAndLogError("Can't get projects from ProjectManager",e);
		                return null;
		            }
		        },
		        query -> {
		            try {
		            	return projectManager.getProjectReferences().size();
		            } catch (Exception e) {
		                errorLogger.showAndLogError("Can't get projects from ProjectManager",e);
		                return 0;
		            }
		        }
        );
        this.eventBus.register(this);
        initComponents();
    }

    private void initComponents() {
    	setSizeFull();
    	setMargin(false);
    	addStyleName("dashboard-view");

        projects.setDataProvider(projectDataProvider);

        addComponent(projects);
    }

    public void handleResourceChangedEvent(ResourcesChangedEvent<Component> event){
    	projectDataProvider.refreshAll();
    }
}
