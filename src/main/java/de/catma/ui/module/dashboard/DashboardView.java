package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.rbac.IRBACManager;

/**
 *
 * Renders a dashboard with all projects and all shared resources
 *
 * @author db
 */
public class DashboardView extends VerticalLayout {

    public DashboardView(
    		ProjectManager projectManager,
    		IRBACManager rbacManager,
    		EventBus eventBus) { 
    	
        initComponents(projectManager, eventBus, rbacManager);
    }

    private void initComponents(ProjectManager projectManager, EventBus eventBus, IRBACManager rbacManager) {
    	setSizeFull();
    	setMargin(false);
    	addStyleName("dashboard-view");
    	ProjectListView projectListView = new ProjectListView(projectManager, eventBus, rbacManager);
        addComponent(projectListView);
    }

}
