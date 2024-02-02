package de.catma.ui.module.dashboard;

import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.events.ShowGroupsEvent;

/**
 * Renders a dashboard listing all projects the user has access to.
 */
public class DashboardView extends VerticalLayout {
	private TabSheet tabSheet;
	private Tab groupsTab;
	private EventBus eventBus;

	public DashboardView(ProjectsManager projectsManager, RemoteGitManagerRestricted remoteGitManagerRestricted, EventBus eventBus) {
		this.eventBus = eventBus;
		this.eventBus.register(this);
		initComponents(projectsManager, remoteGitManagerRestricted, eventBus);
	}

	private void initComponents(ProjectsManager projectsManager, RemoteGitManagerRestricted remoteGitManagerRestricted, EventBus eventBus) {
		setSizeFull();
		setMargin(false);
		addStyleName("dashboard-view");
		tabSheet = new TabSheet();
		addComponent(tabSheet);

		ProjectListView projectListView = new ProjectListView(projectsManager, eventBus, remoteGitManagerRestricted);
		tabSheet.addTab(projectListView, "All Projects");
		groupsTab = tabSheet.addTab(new GroupListView(projectsManager, eventBus, remoteGitManagerRestricted), "Groups");
	}
	
	@Subscribe
	public void handleShowGroupsEvent(ShowGroupsEvent showGroupsEvent) {
		tabSheet.setSelectedTab(groupsTab);
	}
	
	
	public void close() {
		eventBus.unregister(this);
	}
}
