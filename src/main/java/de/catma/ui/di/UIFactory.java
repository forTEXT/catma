package de.catma.ui.di;

import com.google.inject.assistedinject.Assisted;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.modules.dashboard.DashboardView;
import de.catma.ui.modules.dashboard.ProjectList;
import de.catma.ui.modules.main.MainView;
import de.catma.ui.modules.project.ProjectInvitationDialog;

public interface UIFactory {
	
	ProjectList getProjectList(@Assisted("projectManager") ProjectManager projectManager);
	
	DashboardView getDashboardView(@Assisted("projectManager") ProjectManager projectManager);

	MainView getMainview(
			@Assisted("projectManager") ProjectManager projectManager, 
			@Assisted("iRemoteGitlabManager") IRemoteGitManagerRestricted gitmanagerRestricted);
	
	ProjectInvitationDialog getProjectInvitationDialog(@Assisted("projectref") ProjectReference projectRef);
}
