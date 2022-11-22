package de.catma.ui.module.dashboard;

import de.catma.project.ProjectsManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.SaveCancelListener;

public class CreateProjectDialog extends AbstractProjectDialog {

	public CreateProjectDialog(ProjectsManager projectManager, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("Create Project",projectManager, saveCancelListener);
	}

	
	@Override
	protected ProjectReference getResult() {
		try {
			return projectManager.createProject(name.getValue(), description.getValue());
			
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
}
