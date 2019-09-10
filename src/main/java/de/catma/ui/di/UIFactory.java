package de.catma.ui.di;

import java.util.Set;

import com.google.inject.assistedinject.Assisted;

import de.catma.project.Project;
import de.catma.project.ProjectManager;
import de.catma.ui.module.dashboard.DashboardView;
import de.catma.ui.module.dashboard.ProjectListView;
import de.catma.ui.module.main.MainView;
import de.catma.ui.module.project.ProjectInvitationDialog;
import de.catma.ui.module.project.Resource;

public interface UIFactory {
	
	ProjectListView getProjectList(@Assisted("projectManager") ProjectManager projectManager);
	
	DashboardView getDashboardView(@Assisted("projectManager") ProjectManager projectManager);

	MainView getMainview(
			@Assisted("projectManager") ProjectManager projectManager);
	
	ProjectInvitationDialog getProjectInvitationDialog(
			@Assisted("project") Project project, 
			@Assisted("resources")  Set<Resource> resources);
}
