package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
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

		ProjectListView projectListView = new ProjectListView(projectsManager, eventBus, remoteGitManagerRestricted);
		addComponent(projectListView);
	}
}
