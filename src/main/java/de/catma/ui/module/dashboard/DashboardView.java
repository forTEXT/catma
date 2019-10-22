package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.rbac.IRBACManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;

/**
 *
 * Renders a dashboard with all projects and all shared resources
 *
 * @author db
 */
public class DashboardView extends VerticalLayout {

    public DashboardView(
    		ProjectManager projectManager,
    		IRemoteGitManagerRestricted remoteGitManagerRestricted,
    		EventBus eventBus) { 
    	
        initComponents(projectManager, eventBus, remoteGitManagerRestricted);
    }

    private void initComponents(ProjectManager projectManager, EventBus eventBus, IRemoteGitManagerRestricted remoteGitManagerRestricted) {
    	setSizeFull();
    	setMargin(false);
    	addStyleName("dashboard-view");
    	ProjectListView projectListView = 
    			new ProjectListView(projectManager, eventBus, remoteGitManagerRestricted);
        addComponent(projectListView);
    }

}
