package de.catma.ui.di;

import com.google.inject.assistedinject.Assisted;

import de.catma.project.ProjectManager;
import de.catma.ui.modules.dashboard.DashboardView;
import de.catma.ui.modules.dashboard.ProjectList;

public interface UIFactory {
	
	ProjectList getProjectList(@Assisted("projectManager") ProjectManager projectManager);
	
	DashboardView getDashboardView(@Assisted("projectManager") ProjectManager projectManager);
	
}
