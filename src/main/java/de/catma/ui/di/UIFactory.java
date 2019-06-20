package de.catma.ui.di;

import java.util.Set;

import com.google.inject.assistedinject.Assisted;

import de.catma.document.repository.Repository;
import de.catma.project.ProjectManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.modules.dashboard.DashboardView;
import de.catma.ui.modules.dashboard.ProjectListView;
import de.catma.ui.modules.main.MainView;
import de.catma.ui.modules.project.ProjectInvitationDialog;
import de.catma.ui.modules.project.Resource;

public interface UIFactory {
	
	ProjectListView getProjectList(@Assisted("projectManager") ProjectManager projectManager);
	
	DashboardView getDashboardView(@Assisted("projectManager") ProjectManager projectManager);

	MainView getMainview(
			@Assisted("projectManager") ProjectManager projectManager, 
			@Assisted("iRemoteGitlabManager") IRemoteGitManagerRestricted gitmanagerRestricted);
	
	ProjectInvitationDialog getProjectInvitationDialog(
			@Assisted("project") Repository project, 
			@Assisted("resources")  Set<Resource> resources);
}
