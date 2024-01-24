package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

/**
 * Renders a dashboard listing all projects the user has access to.
 */
public class DashboardView extends VerticalLayout {
	public DashboardView(ProjectsManager projectsManager, RemoteGitManagerRestricted remoteGitManagerRestricted, EventBus eventBus) {
		initComponents(projectsManager, remoteGitManagerRestricted, eventBus);
	}

	private void initComponents(ProjectsManager projectsManager, RemoteGitManagerRestricted remoteGitManagerRestricted, EventBus eventBus) {
		setSizeFull();
		setMargin(false);
		addStyleName("dashboard-view");
		TabSheet tabSheet = new TabSheet();
		addComponent(tabSheet);

		ProjectListView projectListView = new ProjectListView(projectsManager, eventBus, remoteGitManagerRestricted);
		tabSheet.addTab(projectListView, "All Projects");
		tabSheet.addTab(new GroupListView(projectsManager, eventBus, remoteGitManagerRestricted), "Groups");
	}
}
