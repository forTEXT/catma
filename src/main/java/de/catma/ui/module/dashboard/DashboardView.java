package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;

/**
 *
 * Renders a dashboard with all projects and all shared resources
 *
 * @author db
 */
public class DashboardView extends VerticalLayout {

	private final ProjectListView projects;

	private final EventBus eventBus;

	@Inject
    public DashboardView(
    		@Assisted("projectManager")ProjectManager projectManager,
    		ProjectListView projectList, 
    		EventBus eventBus){ 
        this.projects = projectList;
        this.eventBus = eventBus;
        this.eventBus.register(this);
        initComponents();
    }

    private void initComponents() {
    	setSizeFull();
    	setMargin(false);
    	addStyleName("dashboard-view");
        addComponent(projects);
    }

}
