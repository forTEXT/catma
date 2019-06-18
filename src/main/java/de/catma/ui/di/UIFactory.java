package de.catma.ui.di;

import java.util.Set;
import java.util.function.BiConsumer;

import com.google.inject.assistedinject.Assisted;

import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
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
			@Assisted("projectref") ProjectReference projectRef, 
			@Assisted("resources")  Set<Resource> resources,
			@Assisted("createColFunc") BiConsumer<String,SourceDocument> createCollectionFunction);
}
